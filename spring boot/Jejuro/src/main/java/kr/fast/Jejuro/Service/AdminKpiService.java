package kr.fast.Jejuro.Service;



//[관리자 AI 추천 KPI 대시보드 - 집계]

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.DataQuality;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Dataset;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Filter;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.FunnelStep;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.LabelCount;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.MissReasons;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.PoiStat;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Reason;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Segment;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Summary;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.Trend;
import kr.fast.Jejuro.ResponseDTO.AdminKpiResponse.TrendPoint;

/**
* 관리자 AI 추천 KPI 집계. 조회 전용이며 SQL로 원자료를 읽고 자바에서 묶어 계산한다.
* 지표별 기준은 docs/18_KPI_지표_설명.md (발표용)에 정리.
*
* 원자료 1) "화면에 보여 준 추천 관광지 한 곳"(RECOMMEND_ITEM.shown = 1) 한 줄마다 (진행 중인 여행 포함)
*   added     : 추천을 받은 뒤 그 여행의 여행 장소에 담겼나 (추천 전에 이미 담았던 곳·경로 가져오기는 제외)
*   scheduled : 그 여행의 확정 일정에 들어갔나
*   visited   : 후기에서 갔어요(1) / 못 갔어요(0) / 입력 없음(null)
*   reaction  : 좋았어요(LIKE) / 아쉬워요(DISLIKE) / 없음
*   → 추천 채택률, 일정 반영률, 추이, 세그먼트, 과추천
* 원자료 2) VIEW TRAVEL_FUNNEL      : 여행별 퍼널 단계·이탈 → 사용자 여정 퍼널
* 원자료 3) VIEW AI_TRAINING_DATASET: 후기까지 끝난 여행의 학습 데이터(라벨 0~3) → 재학습 데이터셋, CSV
*/
@Service
@Transactional(readOnly = true)
public class AdminKpiService {

private static final List<Integer> ALLOWED_DAYS = List.of(7, 30, 90);
private static final Map<String, String> REASON_LABEL = new LinkedHashMap<>();
static {
   REASON_LABEL.put("TIME_SHORTAGE", "시간 부족");
   REASON_LABEL.put("WEATHER", "날씨");
   REASON_LABEL.put("CHANGE_OF_MIND", "계획·마음 변경");
   REASON_LABEL.put("POI_ISSUE", "관광지 사정");
   REASON_LABEL.put("PERSONAL_REASON", "개인 사정");
   REASON_LABEL.put("OTHER", "기타");
}
/** 재학습 라벨 0~3 (AI_TRAINING_DATASET.label) */
private static final String[] LABEL_NAME = { "추천만 됨", "일정 확정", "실제 방문", "방문 + 좋았어요" };

/** 퍼널 단계 (TRAVEL_FUNNEL 뷰의 step_* 칼럼 순서) */
private static final String[][] FUNNEL = {
       { "CREATED", "여행 생성", "step_created" }, { "SURVEYED", "설문 완료", "step_surveyed" },
       { "RECOMMENDED", "추천 받음", "step_recommended" }, { "PLACED_ANY", "장소 담음", "step_placed_any" },
       { "PLACED_ALL", "모두 배치", "step_placed_all" }, { "ADOPTED", "일정 확정", "step_adopted" },
       { "REVIEWED", "후기 작성", "step_reviewed" }, { "SHARED", "커뮤니티 공유", "step_shared" } };

/** 학습 데이터 CSV·뷰 칼럼 (AI_TRAINING_DATASET과 같은 순서) */
static final List<String> DATASET_COLUMNS = datasetColumns();

private final JdbcTemplate jdbc;
private final int retrainThreshold;

public AdminKpiService(JdbcTemplate jdbc, @Value("${admin.retrain-threshold:100}") int retrainThreshold) {
   this.jdbc = jdbc;
   this.retrainThreshold = retrainThreshold;
}

private static List<String> datasetColumns() {
   List<String> cols = new ArrayList<>(List.of("travel_id", "user_id", "GENDER", "AGE_GRP", "INCOME",
           "TRAVEL_COMPANIONS_NUM", "TRAVEL_STYL_1", "TRAVEL_STYL_3", "TRAVEL_STYL_5", "TRAVEL_STYL_6",
           "TRAVEL_STYL_7", "TRAVEL_STYL_8", "TRAVEL_MOTIVE_1", "TRAVEL_MISSION_PRIORITY_WEB"));
   for (int i = 1; i <= 18; i++) {
       cols.add("COMPANION_" + i + "_REL");
       cols.add("COMPANION_" + i + "_GENDER");
       cols.add("COMPANION_" + i + "_AGE");
   }
   cols.addAll(List.of("poi_id", "VISIT_AREA_NM", "address", "VISIT_AREA_TYPE_CD", "latitude", "longitude",
           "region_code", "recommend_rank", "model_version", "shown", "label"));
   return List.copyOf(cols);
}

// ================================================================== 원자료

/** 보여 준 추천 관광지 한 곳 */
record ItemRow(long requestId, long travelId, String model, LocalDateTime createdAt, int rank,
              Long poiId, String placeName, boolean added, boolean scheduled, Boolean visited, String reaction) {
}

record RequestRow(long travelId, String model, String status, Integer responseMs, int resultCount, int unmappedCount) {
}

record FeedbackRow(long travelId, String status, Integer score) {
}

private List<ItemRow> loadItems(LocalDateTime from, LocalDateTime to) {
   String sql = """
           SELECT q.request_id, q.travel_id, q.model_version, q.created_at, i.rank_no, i.poi_id, i.place_name,
                  EXISTS (SELECT 1 FROM travel_bookmark b WHERE b.travel_id = q.travel_id AND b.poi_id = i.poi_id
                             AND b.source <> 'IMPORT' AND b.created_at >= q.created_at) AS added,
                  (t.adopted_route_id IS NOT NULL AND EXISTS (
                       SELECT 1 FROM route_day rd JOIN route_spot rs ON rs.route_day_id = rd.route_day_id
                        WHERE rd.route_id = t.adopted_route_id AND rs.poi_id = i.poi_id)) AS scheduled,
                  (SELECT fs.visited FROM travel_feedback f JOIN travel_feedback_spot fs ON fs.feedback_id = f.feedback_id
                    WHERE f.travel_id = q.travel_id AND fs.poi_id = i.poi_id LIMIT 1) AS visited,
                  (SELECT fs.reaction FROM travel_feedback f JOIN travel_feedback_spot fs ON fs.feedback_id = f.feedback_id
                    WHERE f.travel_id = q.travel_id AND fs.poi_id = i.poi_id LIMIT 1) AS reaction
             FROM recommend_request q
             JOIN recommend_item i ON i.request_id = q.request_id
             JOIN travel t ON t.travel_id = q.travel_id
            WHERE q.status = 'SUCCESS' AND i.shown = 1
              AND q.created_at >= ? AND q.created_at < ?
            ORDER BY q.request_id, i.rank_no
           """;
   return jdbc.query(sql, (rs, n) -> new ItemRow(
           rs.getLong("request_id"), rs.getLong("travel_id"), rs.getString("model_version"),
           toLocal(rs.getTimestamp("created_at")), rs.getInt("rank_no"),
           longOrNull(rs, "poi_id"), rs.getString("place_name"),
           rs.getInt("added") == 1, rs.getInt("scheduled") == 1,
           boolOrNull(rs, "visited"), rs.getString("reaction")), from, to);
}

private List<RequestRow> loadRequests(LocalDateTime from, LocalDateTime to) {
   return jdbc.query("""
           SELECT travel_id, model_version, status, response_ms, result_count, unmapped_count
             FROM recommend_request WHERE created_at >= ? AND created_at < ?
           """, (rs, n) -> new RequestRow(rs.getLong("travel_id"), rs.getString("model_version"),
           rs.getString("status"), intOrNull(rs, "response_ms"), rs.getInt("result_count"),
           rs.getInt("unmapped_count")), from, to);
}

private List<FeedbackRow> loadFeedbacks(LocalDateTime from, LocalDateTime to) {
   return jdbc.query("""
           SELECT travel_id, execution_status, satisfaction_score FROM travel_feedback
            WHERE answered_at >= ? AND answered_at < ?
           """, (rs, n) -> new FeedbackRow(rs.getLong("travel_id"), rs.getString("execution_status"),
           intOrNull(rs, "satisfaction_score")), from, to);
}

// ================================================================== 대시보드

public AdminKpiResponse dashboard(int days) {
   if (!ALLOWED_DAYS.contains(days)) {
       throw ApiException.badRequest("기간은 7, 30, 90일 중에서 고를 수 있습니다.");
   }
   LocalDateTime to = LocalDateTime.now();
   LocalDateTime from = to.minusDays(days);
   LocalDateTime prevFrom = from.minusDays(days);

   List<ItemRow> items = loadItems(from, to);
   List<ItemRow> prevItems = loadItems(prevFrom, from);
   List<RequestRow> requests = loadRequests(from, to);
   List<FeedbackRow> feedbacks = loadFeedbacks(from, to);
   int datasetTravels = datasetTravels();

   return new AdminKpiResponse(
           new Filter(days, from.toLocalDate(), to.toLocalDate()),
           summary(items, prevItems, requests, feedbacks, from, to, datasetTravels),
           funnel(from, to),
           trend(),
           segments(items),
           overRecommended(items),
           missed(items, from, to),
           missReasons(from, to),
           dataset(datasetTravels));
}

// ------------------------------------------------------------------ 요약 카드

private Summary summary(List<ItemRow> items, List<ItemRow> prevItems, List<RequestRow> requests,
                       List<FeedbackRow> feedbacks, LocalDateTime from, LocalDateTime to, int datasetTravels) {
   List<RequestRow> ok = requests.stream().filter(r -> "SUCCESS".equals(r.status())).toList();
   int travels = (int) ok.stream().map(RequestRow::travelId).distinct().count();

   // 방문률: 이 기간에 남긴 후기의 관광지별 결과 전체
   List<Boolean> visits = jdbc.query("""
           SELECT fs.visited FROM travel_feedback f
             JOIN travel_feedback_spot fs ON fs.feedback_id = f.feedback_id
            WHERE f.answered_at >= ? AND f.answered_at < ?
           """, (rs, n) -> rs.getInt(1) == 1, from, to);

   List<Integer> scores = feedbacks.stream().map(FeedbackRow::score).filter(s -> s != null).toList();

   return new Summary(requests.size(), travels, Math.max(ok.size() - travels, 0),
           rate(items, ItemRow::added), rate(prevItems, ItemRow::added),
           rate(items, ItemRow::scheduled), rate(prevItems, ItemRow::scheduled),
           ratio(visits.stream().filter(v -> v).count(), visits.size()),
           scores.isEmpty() ? null : round(scores.stream().mapToInt(Integer::intValue).average().orElse(0), 2),
           feedbacks.size(), datasetTravels, retrainThreshold);
}

/** 재학습 데이터셋에 들어간 여행 수 (후기까지 끝난 여행) */
private int datasetTravels() {
   Integer n = jdbc.queryForObject("SELECT COUNT(DISTINCT travel_id) FROM ai_training_dataset", Integer.class);
   return n == null ? 0 : n;
}

// ------------------------------------------------------------------ 퍼널 (TRAVEL_FUNNEL 뷰)

/**
* 기간 안에 만든 여행(경로 가져온 여행 제외)의 단계별 도달 수.
* rate = 앞 단계 대비 전환율, dropped = 이 단계로 못 넘어가고 이탈 확정, waiting = 진행 중이라 이 단계 앞에서 대기
*/
private List<FunnelStep> funnel(LocalDateTime from, LocalDateTime to) {
   int n = FUNNEL.length;
   int[] counts = new int[n];
   Map<String, Integer> dropped = new HashMap<>();
   Map<String, Integer> waiting = new HashMap<>();
   jdbc.query("SELECT step_shared, reached_step, funnel_status, drop_step FROM travel_funnel "
           + "WHERE created_at >= ? AND created_at < ?", (rs, row) -> {
               // 가장 멀리 간 단계(reached_step)까지 앞 단계는 모두 지난 것으로 센다 → 단계가 내려갈수록 줄어듦
               int reached = stepIndex(rs.getString("reached_step"));
               for (int i = 0; i < n - 1; i++) {
                   if (i <= reached) counts[i]++;
               }
               if (rs.getInt("step_shared") == 1) counts[n - 1]++;   // 커뮤니티 공유는 선택 단계라 따로
               String status = rs.getString("funnel_status");
               if ("DROPPED".equals(status)) {
                   dropped.merge(rs.getString("drop_step"), 1, Integer::sum);
               } else if ("IN_PROGRESS".equals(status)) {
                   waiting.merge(nextStep(rs.getString("reached_step")), 1, Integer::sum);
               }
               return null;
           }, from, to);

   List<FunnelStep> steps = new ArrayList<>();
   for (int i = 0; i < n; i++) {
       String key = FUNNEL[i][0];
       steps.add(new FunnelStep(key, FUNNEL[i][1], counts[i], i == 0 ? null : ratio(counts[i], counts[i - 1]),
               dropped.getOrDefault(key, 0), waiting.getOrDefault(key, 0)));
   }
   return steps;
}

private static int stepIndex(String key) {
   for (int i = 0; i < FUNNEL.length; i++) {
       if (FUNNEL[i][0].equals(key)) return i;
   }
   return 0;
}

private static String nextStep(String reached) {
   for (int i = 0; i < FUNNEL.length - 1; i++) {
       if (FUNNEL[i][0].equals(reached)) return FUNNEL[i + 1][0];
   }
   return null;
}

// ------------------------------------------------------------------ 주별 추이

private Trend trend() {
   LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
   LocalDate start = thisMonday.minusWeeks(7);
   List<ItemRow> rows = loadItems(start.atStartOfDay(), LocalDateTime.now());
   Map<Integer, List<ItemRow>> byWeek = rows.stream().collect(Collectors.groupingBy(
           r -> (int) (ChronoUnit.DAYS.between(start, r.createdAt().toLocalDate()) / 7)));
   List<TrendPoint> points = new ArrayList<>();
   for (int w = 0; w < 8; w++) {
       List<ItemRow> week = byWeek.getOrDefault(w, List.of());
       points.add(new TrendPoint(start.plusWeeks(w), week.size(), rate(week, ItemRow::added)));
   }
   return new Trend(points);
}

// ------------------------------------------------------------------ 세그먼트

private Map<String, List<Segment>> segments(List<ItemRow> items) {
   Set<Long> travelIds = items.stream().map(ItemRow::travelId).collect(Collectors.toSet());
   Map<String, List<Segment>> result = new LinkedHashMap<>();
   if (travelIds.isEmpty()) {
       for (String key : List.of("companion", "age", "region", "days", "motive")) result.put(key, List.of());
       return result;
   }
   String in = placeholders(travelIds.size());
   Object[] ids = travelIds.toArray();

   Map<String, String> ageName = new HashMap<>();
   jdbc.query("SELECT code_value, code_name FROM code WHERE group_code = 'AGE'",
           (rs, n) -> ageName.put(rs.getString(1), rs.getString(2)));

   Map<Long, String> age = new HashMap<>();
   Map<Long, String> days = new HashMap<>();
   Map<Long, String> mode = new HashMap<>();
   jdbc.query("SELECT travel_id, age_group_snapshot, DATEDIFF(end_date, start_date) + 1, region_mode FROM travel WHERE travel_id IN (" + in + ")",
           (rs, n) -> {
               long id = rs.getLong(1);
               age.put(id, ageName.getOrDefault(String.valueOf(rs.getInt(2)), rs.getInt(2) + "그룹"));
               days.put(id, daysBucket(rs.getInt(3)));
               mode.put(id, rs.getString(4));
               return null;
           }, ids);

   Map<Long, List<int[]>> companions = new HashMap<>();
   jdbc.query("SELECT travel_id, relation_code, age_group_code FROM companion WHERE travel_id IN (" + in + ")",
           (rs, n) -> companions.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                   .add(new int[] { rs.getInt(2), rs.getInt(3) }), ids);

   Map<Long, List<String>> regions = new HashMap<>();
   jdbc.query("SELECT tr.travel_id, r.region_name FROM travel_region tr JOIN region r ON r.region_id = tr.region_id WHERE tr.travel_id IN (" + in + ")",
           (rs, n) -> regions.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2)), ids);

   Map<Long, String> motive = new HashMap<>();
   jdbc.query("""
           SELECT p.travel_id, o.option_name FROM travel_preference p
             JOIN preference_option o ON o.preference_id = p.preference_id AND o.option_value = p.answer_value
            WHERE p.preference_id = 201 AND p.answer_rank = 1 AND p.travel_id IN (""" + in + ")",
           (rs, n) -> motive.put(rs.getLong(1), rs.getString(2)), ids);

   Map<Long, Integer> score = new HashMap<>();
   jdbc.query("SELECT travel_id, satisfaction_score FROM travel_feedback WHERE satisfaction_score IS NOT NULL AND travel_id IN (" + in + ")",
           (rs, n) -> score.put(rs.getLong(1), rs.getInt(2)), ids);

   Double overall = rate(items, ItemRow::added);
   result.put("companion", segmentBy(items, id -> companionGroup(companions.get(id)), score, overall));
   result.put("age", segmentBy(items, id -> age.getOrDefault(id, "알 수 없음"), score, overall));
   result.put("region", segmentBy(items, id -> regionName(mode.get(id), regions.get(id)), score, overall));
   result.put("days", segmentBy(items, id -> days.getOrDefault(id, "알 수 없음"), score, overall));
   result.put("motive", segmentBy(items, id -> motive.getOrDefault(id, "응답 없음"), score, overall));
   return result;
}

private List<Segment> segmentBy(List<ItemRow> items, Function<Long, String> keyOf,
                               Map<Long, Integer> score, Double overall) {
   Map<String, List<ItemRow>> groups = items.stream().collect(Collectors.groupingBy(r -> keyOf.apply(r.travelId())));
   List<Segment> result = new ArrayList<>();
   groups.forEach((name, rows) -> {
       Set<Long> travels = rows.stream().map(ItemRow::travelId).collect(Collectors.toSet());
       Double adoption = rate(rows, ItemRow::added);
       List<ItemRow> withVisit = rows.stream().filter(r -> r.visited() != null).toList();
       List<Integer> s = travels.stream().map(score::get).filter(v -> v != null).toList();
       String level;
       if (travels.size() < 5 || adoption == null || overall == null) level = "LOW_DATA";
       else if (adoption < overall * 0.7) level = "WEAK";
       else if (adoption < overall * 0.9) level = "WATCH";
       else level = "OK";
       result.add(new Segment(name, travels.size(), adoption,
               rate(withVisit, r -> Boolean.TRUE.equals(r.visited())),
               s.isEmpty() ? null : round(s.stream().mapToInt(Integer::intValue).average().orElse(0), 2), level));
   });
   result.sort(Comparator.comparingInt(Segment::travels).reversed());
   return result;
}

/** 동반자 구성 → 세그먼트 이름 (TCR 코드: 1 배우자 2 자녀 3 부모 4 조부모 5 형제 6 친인척 7 친구 8 연인 …) */
private String companionGroup(List<int[]> list) {
   if (list == null || list.isEmpty()) return "혼자";
   if (list.stream().anyMatch(c -> c[0] == 2 && c[1] <= 2)) return "아이 동반 가족";   // 자녀 + 10대 이하
   if (list.stream().anyMatch(c -> c[0] == 3 || c[0] == 4)) return "부모 동반";
   if (list.stream().anyMatch(c -> c[0] == 1 || c[0] == 8)) return "연인·부부";
   if (list.stream().anyMatch(c -> c[0] == 7)) return "친구";
   if (list.stream().anyMatch(c -> c[0] == 2 || c[0] == 5 || c[0] == 6)) return "가족·친척";
   return "동료·모임·기타";
}

private String regionName(String mode, List<String> names) {
   if (!"SELECTED".equals(mode) || names == null || names.isEmpty()) return "제주 전체";
   return names.size() == 1 ? names.get(0) : "여러 권역";
}

private String daysBucket(int d) {
   if (d <= 1) return "당일";
   if (d == 2) return "1박 2일";
   if (d == 3) return "2박 3일";
   if (d == 4) return "3박 4일";
   return "4박 이상";
}

// ------------------------------------------------------------------ 관광지 표

/** 과추천: 추천 3회 이상 중 채택률 낮은 순 */
private List<PoiStat> overRecommended(List<ItemRow> items) {
   Map<Long, List<ItemRow>> byPoi = items.stream().filter(r -> r.poiId() != null)
           .collect(Collectors.groupingBy(ItemRow::poiId));
   List<Long> ids = byPoi.entrySet().stream()
           .filter(e -> e.getValue().size() >= 3)
           .sorted(Comparator.<Map.Entry<Long, List<ItemRow>>>comparingDouble(e -> rateOrZero(e.getValue(), ItemRow::added))
                   .thenComparing(e -> -e.getValue().size()))
           .limit(5).map(Map.Entry::getKey).toList();
   Map<Long, String> names = poiNames(ids);
   return ids.stream().map(id -> {
       List<ItemRow> rows = byPoi.get(id);
       int added = (int) rows.stream().filter(ItemRow::added).count();
       return new PoiStat(id, names.getOrDefault(id, "관광지 " + id), rows.size(), added, 0, rate(rows, ItemRow::added));
   }).toList();
}

/** AI가 놓친 곳: 관광지 검색으로 직접 담은 수가 많은 순 (추천 수와 함께) */
private List<PoiStat> missed(List<ItemRow> items, LocalDateTime from, LocalDateTime to) {
   Map<Long, Integer> searchAdds = new HashMap<>();
   jdbc.query("SELECT travel_id, poi_id FROM travel_bookmark WHERE source = 'SEARCH' AND created_at >= ? AND created_at < ?",
           (rs, n) -> searchAdds.merge(rs.getLong(2), 1, Integer::sum), from, to);
   Map<Long, Long> recommended = items.stream().filter(r -> r.poiId() != null)
           .collect(Collectors.groupingBy(ItemRow::poiId, Collectors.counting()));
   List<Long> ids = searchAdds.entrySet().stream()
           .sorted(Comparator.<Map.Entry<Long, Integer>>comparingInt(e -> -e.getValue())
                   .thenComparingLong(e -> recommended.getOrDefault(e.getKey(), 0L)))
           .limit(5).map(Map.Entry::getKey).toList();
   Map<Long, String> names = poiNames(ids);
   return ids.stream().map(id -> new PoiStat(id, names.getOrDefault(id, "관광지 " + id),
           recommended.getOrDefault(id, 0L).intValue(), 0, searchAdds.get(id), null)).toList();
}

// ------------------------------------------------------------------ 못 간 이유

private MissReasons missReasons(LocalDateTime from, LocalDateTime to) {
   Set<Long> partial = new HashSet<>();
   jdbc.query("SELECT feedback_id, travel_id FROM travel_feedback WHERE execution_status = 'PARTIAL' AND answered_at >= ? AND answered_at < ?",
           (rs, n) -> partial.add(rs.getLong(1)), from, to);
   Map<String, Integer> counts = new HashMap<>();
   if (!partial.isEmpty()) {
       jdbc.query("SELECT reason_code FROM travel_feedback_reason WHERE feedback_id IN (" + placeholders(partial.size()) + ")",
               (rs, n) -> counts.merge(rs.getString(1), 1, Integer::sum), partial.toArray());
   }
   List<Reason> reasons = REASON_LABEL.keySet().stream()
           .map(code -> new Reason(code, REASON_LABEL.get(code), counts.getOrDefault(code, 0),
                   ratio(counts.getOrDefault(code, 0), partial.size())))
           .sorted(Comparator.comparingInt(Reason::count).reversed())
           .toList();

   // 자주 빠지는 관광지 (못 갔어요가 많은 순 3곳)
   Map<Long, Integer> missedPoi = new HashMap<>();
   jdbc.query("""
           SELECT f.travel_id, fs.poi_id FROM travel_feedback f
             JOIN travel_feedback_spot fs ON fs.feedback_id = f.feedback_id
            WHERE fs.visited = 0 AND f.answered_at >= ? AND f.answered_at < ?
           """, (rs, n) -> missedPoi.merge(rs.getLong(2), 1, Integer::sum), from, to);
   List<Long> top = missedPoi.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
           .limit(3).map(Map.Entry::getKey).toList();
   Map<Long, String> names = poiNames(top);
   List<String> often = top.stream().map(id -> names.getOrDefault(id, "관광지 " + id) + " " + missedPoi.get(id) + "회").toList();
   return new MissReasons(partial.size(), reasons, often);
}

// ------------------------------------------------------------------ 재학습 데이터셋 (AI_TRAINING_DATASET 뷰)

private Dataset dataset(int datasetTravels) {
   int[] counts = new int[LABEL_NAME.length];
   jdbc.query("SELECT label, COUNT(*) FROM ai_training_dataset GROUP BY label", (rs, n) -> {
       int label = rs.getInt(1);
       if (label >= 0 && label < counts.length) counts[label] = rs.getInt(2);
       return null;
   });
   List<LabelCount> labels = new ArrayList<>();
   int total = 0;
   for (int i = 0; i < counts.length; i++) {
       labels.add(new LabelCount(i, LABEL_NAME[i], counts[i]));
       total += counts[i];
   }
   return new Dataset(labels, total, datasetTravels, dataQuality());
}

private DataQuality dataQuality() {
   Integer ended = jdbc.queryForObject(
           "SELECT COUNT(*) FROM travel WHERE adopted_route_id IS NOT NULL AND end_date < CURRENT_DATE", Integer.class);
   Integer reviewed = jdbc.queryForObject("""
           SELECT COUNT(*) FROM travel t JOIN travel_feedback f ON f.travel_id = t.travel_id
            WHERE t.adopted_route_id IS NOT NULL AND t.end_date < CURRENT_DATE
           """, Integer.class);
   Integer visitedFeedbacks = jdbc.queryForObject(
           "SELECT COUNT(*) FROM travel_feedback WHERE execution_status <> 'NOT_TAKEN'", Integer.class);
   Integer withSpots = jdbc.queryForObject("""
           SELECT COUNT(DISTINCT f.feedback_id) FROM travel_feedback f
             JOIN travel_feedback_spot fs ON fs.feedback_id = f.feedback_id
            WHERE f.execution_status <> 'NOT_TAKEN'
           """, Integer.class);
   Integer surveyTravels = jdbc.queryForObject("SELECT COUNT(*) FROM travel WHERE source_post_id IS NULL", Integer.class);
   Integer noSurvey = jdbc.queryForObject("""
           SELECT COUNT(*) FROM travel t WHERE t.source_post_id IS NULL
              AND NOT EXISTS (SELECT 1 FROM travel_preference p WHERE p.travel_id = t.travel_id)
           """, Integer.class);
   Integer outliers = jdbc.queryForObject(
           "SELECT COUNT(*) FROM travel WHERE DATEDIFF(end_date, start_date) + 1 > 15", Integer.class);
   return new DataQuality(ratio(nz(reviewed), nz(ended)), ratio(nz(withSpots), nz(visitedFeedbacks)),
           ratio(nz(noSurvey), nz(surveyTravels)), nz(outliers));
}

// ------------------------------------------------------------------ 학습 데이터 CSV

/**
* AI 재학습용 CSV = AI_TRAINING_DATASET 뷰 그대로 (후기까지 끝난 여행만, 라벨 0~3).
* VISIT_AREA_NM은 FastAPI 후보 이름과 같은 값이라 AI 담당이 그대로 합칠 수 있다.
*/
public String trainingCsv() {
   String cols = DATASET_COLUMNS.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
   StringBuilder sb = new StringBuilder("﻿"); // 엑셀에서 한글이 깨지지 않게 BOM
   sb.append(String.join(",", DATASET_COLUMNS)).append('\n');
   jdbc.query("SELECT " + cols + " FROM ai_training_dataset ORDER BY travel_id, shown DESC, recommend_rank",
           (rs, n) -> {
               for (int i = 1; i <= DATASET_COLUMNS.size(); i++) {
                   if (i > 1) sb.append(',');
                   Object v = rs.getObject(i);
                   sb.append(v == null ? "" : csv(v.toString()));
               }
               sb.append('\n');
               return null;
           });
   return sb.toString();
}

// ------------------------------------------------------------------ 도우미

private Map<Long, String> poiNames(Collection<Long> ids) {
   if (ids.isEmpty()) return Map.of();
   Map<Long, String> names = new HashMap<>();
   jdbc.query("SELECT poi_id, poi_name FROM poi WHERE poi_id IN (" + placeholders(ids.size()) + ")",
           (rs, n) -> names.put(rs.getLong(1), rs.getString(2)), ids.toArray());
   return names;
}

private static String placeholders(int n) {
   return String.join(",", java.util.Collections.nCopies(n, "?"));
}

private static Double rate(List<ItemRow> rows, Predicate<ItemRow> hit) {
   return ratio(rows.stream().filter(hit).count(), rows.size());
}

private static double rateOrZero(List<ItemRow> rows, Predicate<ItemRow> hit) {
   Double r = rate(rows, hit);
   return r == null ? 0 : r;
}

private static Double ratio(long num, long den) {
   return den == 0 ? null : round((double) num / den, 4);
}

private static double round(double v, int digits) {
   double f = Math.pow(10, digits);
   return Math.round(v * f) / f;
}

private static int nz(Integer v) {
   return v == null ? 0 : v;
}

private static String csv(String s) {
   if (s == null) return "";
   return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
}

private static LocalDateTime toLocal(Timestamp t) {
   return t == null ? null : t.toLocalDateTime();
}

private static Long longOrNull(ResultSet rs, String col) throws SQLException {
   long v = rs.getLong(col);
   return rs.wasNull() ? null : v;
}

private static Integer intOrNull(ResultSet rs, String col) throws SQLException {
   int v = rs.getInt(col);
   return rs.wasNull() ? null : v;
}

private static Boolean boolOrNull(ResultSet rs, String col) throws SQLException {
   int v = rs.getInt(col);
   return rs.wasNull() ? null : v == 1;
}
}
