package kr.fast.Jejuro.Service;


//[관리자 AI KPI - AI 추천 기록 저장]

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
* AI 추천 1회 = RECOMMEND_REQUEST 1행 + RECOMMEND_ITEM N행(순위 순).
* - 기록 저장은 별도 트랜잭션(REQUIRES_NEW). 저장 중 오류가 나면 기록 전체를 되돌리고(요청 행만 남는 일 없음)
*   예외를 던진다 → 호출하는 RecommendService가 잡아서 추천 결과에는 영향이 없게 한다.
* - 추천이 실패한 경우도 FAIL로 남겨 오류율을 계산한다.
*/
@Service
public class RecommendLogService {

 private final JdbcTemplate jdbc;

 public RecommendLogService(JdbcTemplate jdbc) {
     this.jdbc = jdbc;
 }

 /**
  * 성공 기록.
  * @param names     AI가 준 순서대로의 장소 이름(원본 ID)
  * @param poiOfName 이름 → 우리 관광지 번호 (없으면 매핑 실패)
  * @param shownIds  화면에 보여 준 관광지 번호
  */
 @Transactional(propagation = Propagation.REQUIRES_NEW)
 public void success(Long travelId, String modelVersion, long elapsedMs,
                     List<String> names, Map<String, Long> poiOfName, Set<Long> shownIds) {
     int unmapped = (int) names.stream().filter(n -> !poiOfName.containsKey(n)).count();
     Long requestId = insertRequest(travelId, modelVersion, "SUCCESS", elapsedMs, names.size(), unmapped, null);
     List<Object[]> rows = new ArrayList<>();
     Set<Long> marked = new HashSet<>(); // 같은 관광지가 두 이름으로 와도 "보여 줌"은 처음 한 번만
     for (int i = 0; i < names.size(); i++) {
         String name = names.get(i);
         Long poiId = poiOfName.get(name);
         boolean shown = poiId != null && shownIds.contains(poiId) && marked.add(poiId);
         rows.add(new Object[] { requestId, i + 1, cut(name, 200), poiId, shown ? 1 : 0 });
     }
     if (!rows.isEmpty()) {
         jdbc.batchUpdate("INSERT INTO recommend_item (request_id, rank_no, place_name, poi_id, shown) VALUES (?, ?, ?, ?, ?)", rows);
     }
 }

 /** 실패 기록 (오류율 계산용) */
 @Transactional(propagation = Propagation.REQUIRES_NEW)
 public void fail(Long travelId, String modelVersion, long elapsedMs, String message) {
     insertRequest(travelId, modelVersion, "FAIL", elapsedMs, 0, 0, cut(message, 200));
 }

 /**
  * 이 여행의 마지막 성공 추천에서 화면에 보여 준 관광지 번호(AI 순위대로).
  * 성공 기록이 없으면 비어 있음. (삭제된 관광지는 poi_id가 NULL이 되어 빠진다)
  */
 @Transactional(readOnly = true)
 public Optional<List<Long>> latestShown(Long travelId) {
     Long requestId = jdbc.query(
             "SELECT MAX(request_id) FROM recommend_request WHERE travel_id = ? AND status = 'SUCCESS'",
             rs -> rs.next() ? (Long) rs.getObject(1, Long.class) : null, travelId);
     if (requestId == null) return Optional.empty();
     return Optional.of(jdbc.queryForList(
             "SELECT poi_id FROM recommend_item WHERE request_id = ? AND shown = 1 AND poi_id IS NOT NULL ORDER BY rank_no",
             Long.class, requestId));
 }

 private Long insertRequest(Long travelId, String modelVersion, String status, long elapsedMs,
                            int resultCount, int unmapped, String error) {
     KeyHolder key = new GeneratedKeyHolder();
     jdbc.update(con -> {
         PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO recommend_request (travel_id, model_version, status, response_ms, result_count, unmapped_count, error_message) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS);
         ps.setLong(1, travelId);
         ps.setString(2, modelVersion);
         ps.setString(3, status);
         ps.setLong(4, elapsedMs);
         ps.setInt(5, resultCount);
         ps.setInt(6, unmapped);
         ps.setString(7, error);
         return ps;
     }, key);
     return key.getKey().longValue();
 }

 private static String cut(String s, int max) {
     if (s == null) return null;
     return s.length() <= max ? s : s.substring(0, max);
 }
}