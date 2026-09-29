package kr.fast.Jejuro.Service;


//[관리자 AI KPI - 퍼널 이탈 로그]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.ResponseDTO.FunnelDropResponse;
import kr.fast.Jejuro.ResponseDTO.FunnelDropResponse.Item;

/**
* 퍼널 이탈 로그: VIEW TRAVEL_FUNNEL에서 이탈 확정(DROPPED)된 여행을 한 건씩, 판단 근거와 함께.
* 대상 = 대시보드와 같은 기간(created_at) 안에 만든 여행. 최근에 종료된 여행부터.
* 이탈 확정 기준: 일정 확정 전에 멈춘 채 종료일이 지남 / 확정했는데 종료일 + 14일까지 후기 없음
*/
@Service
@Transactional(readOnly = true)
public class FunnelDropService {

 private static final int PAGE_SIZE = 20;
 private static final int CSV_LIMIT = 5000;
 private static final Set<Integer> ALLOWED_DAYS = Set.of(7, 30, 90);
 /** 못 간 단계 (TRAVEL_FUNNEL.drop_step 값) */
 private static final List<String> STEPS = List.of("SURVEYED", "RECOMMENDED", "PLACED_ANY", "PLACED_ALL", "ADOPTED", "REVIEWED");
 /** 후기 기한 = 종료일 + 이 일수 (TRAVEL_FUNNEL 뷰와 같게) */
 private static final int REVIEW_GRACE_DAYS = 14;

 private static final String SELECT = """
         SELECT f.travel_id, t.travel_name, u.nickname, f.reached_step, f.drop_step, f.created_at, f.end_date,
                f.place_count, f.placed_count,
                (SELECT COUNT(*) FROM RECOMMEND_REQUEST q
                  WHERE q.travel_id = f.travel_id AND q.status = 'SUCCESS') AS recommend_count,
                (SELECT COUNT(*) FROM RECOMMEND_REQUEST q JOIN RECOMMEND_ITEM i ON i.request_id = q.request_id
                  WHERE q.travel_id = f.travel_id AND q.status = 'SUCCESS' AND i.shown = 1) AS shown_count,
                (SELECT COUNT(*) FROM ROUTE_DAY rd JOIN ROUTE_SPOT s ON s.route_day_id = rd.route_day_id
                  WHERE rd.route_id = t.adopted_route_id) AS scheduled_count
           FROM TRAVEL_FUNNEL f
           JOIN TRAVEL t ON t.travel_id = f.travel_id
           JOIN `USER` u ON u.user_id = f.user_id
         """;

 private final JdbcTemplate jdbc;

 public FunnelDropService(JdbcTemplate jdbc) {
     this.jdbc = jdbc;
 }

 /** @param step 못 간 단계로 거르기 (생략 = 전체) */
 public FunnelDropResponse list(int days, String step, int page) {
     Range r = range(days, step);
     Map<String, Integer> counts = new LinkedHashMap<>();
     STEPS.forEach(s -> counts.put(s, 0));
     jdbc.query("SELECT drop_step, COUNT(*) FROM TRAVEL_FUNNEL WHERE funnel_status = 'DROPPED' "
             + "AND created_at >= ? AND created_at < ? GROUP BY drop_step", (rs, n) -> {
                 counts.put(rs.getString(1), rs.getInt(2));
                 return null;
             }, r.from(), r.to());
     long total = r.step() == null ? counts.values().stream().mapToLong(Integer::longValue).sum()
             : counts.getOrDefault(r.step(), 0);

     int safePage = Math.max(page, 0);
     List<Object> args = new ArrayList<>(r.args());
     args.add(PAGE_SIZE);
     args.add(safePage * PAGE_SIZE);
     List<Item> items = jdbc.query(SELECT + r.where() + " ORDER BY f.end_date DESC, f.travel_id DESC LIMIT ? OFFSET ?",
             (rs, n) -> toItem(rs), args.toArray());
     return new FunnelDropResponse(safePage, (int) Math.ceil(total / (double) PAGE_SIZE), total, counts, items);
 }

 /** 이탈 로그 CSV (최대 5,000건) */
 public String csv(int days, String step) {
     Range r = range(days, step);
     List<Object> args = new ArrayList<>(r.args());
     args.add(CSV_LIMIT);
     StringBuilder sb = new StringBuilder("﻿");
     sb.append("travel_id,travel_name,nickname,reached_step,drop_step,created_at,end_date,deadline,overdue_days,"
             + "place_count,placed_count,recommend_count,shown_count,scheduled_count\n");
     for (Item i : jdbc.query(SELECT + r.where() + " ORDER BY f.end_date DESC, f.travel_id DESC LIMIT ?",
             (rs, n) -> toItem(rs), args.toArray())) {
         sb.append(i.travelId()).append(',').append(csv(i.travelName())).append(',').append(csv(i.nickname()))
                 .append(',').append(i.reachedStep()).append(',').append(i.dropStep())
                 .append(',').append(i.createdAt()).append(',').append(i.endDate()).append(',').append(i.deadline())
                 .append(',').append(i.overdueDays()).append(',').append(i.placeCount()).append(',')
                 .append(i.placedCount()).append(',').append(i.recommendCount()).append(',')
                 .append(i.shownCount()).append(',').append(i.scheduledCount()).append('\n');
     }
     return sb.toString();
 }

 private record Range(LocalDateTime from, LocalDateTime to, String step, String where, List<Object> args) {
 }

 private Range range(int days, String step) {
     if (!ALLOWED_DAYS.contains(days)) {
         throw ApiException.badRequest("기간은 7, 30, 90일 중에서 고를 수 있습니다.");
     }
     String s = step == null || step.isBlank() ? null : step.trim().toUpperCase();
     if (s != null && !STEPS.contains(s)) {
         throw ApiException.badRequest("알 수 없는 단계입니다: " + step);
     }
     LocalDateTime to = LocalDateTime.now();
     LocalDateTime from = to.minusDays(days);
     List<Object> args = new ArrayList<>(List.of(from, to));
     String where = " WHERE f.funnel_status = 'DROPPED' AND f.created_at >= ? AND f.created_at < ?";
     if (s != null) {
         where += " AND f.drop_step = ?";
         args.add(s);
     }
     return new Range(from, to, s, where, args);
 }

 private static Item toItem(java.sql.ResultSet rs) throws java.sql.SQLException {
     LocalDate end = rs.getDate("end_date").toLocalDate();
     String drop = rs.getString("drop_step");
     LocalDate deadline = "REVIEWED".equals(drop) ? end.plusDays(REVIEW_GRACE_DAYS) : end;
     return new Item(rs.getLong("travel_id"), rs.getString("travel_name"), rs.getString("nickname"),
             rs.getString("reached_step"), drop, rs.getTimestamp("created_at").toLocalDateTime(), end, deadline,
             Math.max(ChronoUnit.DAYS.between(deadline, LocalDate.now()), 0),
             rs.getInt("place_count"), rs.getInt("placed_count"), rs.getInt("recommend_count"),
             rs.getInt("shown_count"), rs.getInt("scheduled_count"));
 }

 private static String csv(String s) {
     if (s == null) return "";
     return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
 }
}