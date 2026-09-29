package kr.fast.Jejuro.Service;


//[관리자 회원 관리 - 목록·상세·제재·역할]

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Report;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Entity.UserSanction;
import kr.fast.Jejuro.Repository.ReportRepository;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.Repository.UserSanctionRepository;
import kr.fast.Jejuro.ResponseDTO.AdminUserResponse;

/**
* 회원 관리 규칙
* - 제재: 경고(누적 3회마다 자동 7일 정지) / 7일 정지 / 30일 정지 / 영구 정지 / 정지 해제
*   정지 중인 회원은 커뮤니티 글·댓글 쓰기·수정, 신고를 할 수 없다(읽기·여행 기능은 그대로).
*   이미 더 긴 정지가 있으면 짧은 정지로 줄어들지 않는다(줄이려면 해제 후 다시).
* - 관리자 자신과 다른 관리자에게는 제재할 수 없다(먼저 일반 회원으로 바꾼 뒤).
* - 역할(USER/ADMIN) 변경은 자기 자신은 불가(관리자가 0명이 되는 일 방지).
* - 모든 제재는 USER_SANCTION에 기록한다.
*/
@Service
public class AdminUserService {

 private static final int PAGE_SIZE = 20;
 /** 영구 정지 = 이 시각까지 정지 */
 public static final LocalDateTime BAN_UNTIL = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

 private final JdbcTemplate jdbc;
 private final UserRepository userRepository;
 private final UserSanctionRepository sanctionRepository;
 private final ReportRepository reportRepository;

 public AdminUserService(JdbcTemplate jdbc, UserRepository userRepository,
                         UserSanctionRepository sanctionRepository, ReportRepository reportRepository) {
     this.jdbc = jdbc;
     this.userRepository = userRepository;
     this.sanctionRepository = sanctionRepository;
     this.reportRepository = reportRepository;
 }

 // ------------------------------------------------------------------ 목록

 private static final String ROW_SELECT = """
         SELECT u.user_id, u.email, u.nickname, u.role, u.status, u.created_at, u.suspended_until, u.warning_count,
                (SELECT COUNT(*) FROM TRAVEL t WHERE t.user_id = u.user_id) AS travel_count,
                (SELECT COUNT(*) FROM COMMUNITY_POST p WHERE p.user_id = u.user_id AND p.deleted_at IS NULL) AS post_count,
                (SELECT COUNT(*) FROM COMMUNITY_COMMENT c WHERE c.user_id = u.user_id AND c.deleted_at IS NULL) AS comment_count,
                (SELECT COUNT(DISTINCT r.target_type, r.target_id) FROM REPORT r
                  WHERE r.target_user_id = u.user_id AND r.status = 'ACCEPTED') AS accepted_count,
                (SELECT COUNT(DISTINCT r.target_type, r.target_id) FROM REPORT r
                  WHERE r.target_user_id = u.user_id AND r.status = 'PENDING') AS pending_count
           FROM `USER` u
         """;

 /** 필터별 WHERE 조건 */
 private static final Map<String, String> FILTERS = Map.of(
         "ALL", "1 = 1",
         "SUSPENDED", "u.suspended_until > NOW()",
         "REPORTED", "EXISTS (SELECT 1 FROM REPORT r WHERE r.target_user_id = u.user_id AND r.status IN ('PENDING', 'ACCEPTED'))",
         "ADMIN", "u.role = 'ADMIN'");

 /**
  * 회원 목록 (최근 가입 순, 20명씩)
  * @param keyword 닉네임·이메일 일부 (생략 가능)
  * @param filter  ALL / SUSPENDED / REPORTED / ADMIN
  */
 @Transactional(readOnly = true)
 public AdminUserResponse list(String keyword, String filter, int page) {
     String cond = FILTERS.getOrDefault(filter == null ? "ALL" : filter.toUpperCase(), FILTERS.get("ALL"));
     String kw = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
     String where = " WHERE " + cond + (kw == null ? "" : " AND (u.nickname LIKE ? OR u.email LIKE ?)");
     Object[] args = kw == null ? new Object[0] : new Object[] { kw, kw };

     long total = count("SELECT COUNT(*) FROM `USER` u" + where, args);
     int safePage = Math.max(page, 0);
     List<Object> pageArgs = new ArrayList<>(List.of(args));
     pageArgs.add(PAGE_SIZE);
     pageArgs.add(safePage * PAGE_SIZE);
     List<AdminUserResponse.Row> rows = jdbc.query(ROW_SELECT + where + " ORDER BY u.user_id DESC LIMIT ? OFFSET ?",
             (rs, n) -> toRow(rs), pageArgs.toArray());

     AdminUserResponse.Counts counts = new AdminUserResponse.Counts(
             count("SELECT COUNT(*) FROM `USER` u WHERE " + FILTERS.get("ALL")),
             count("SELECT COUNT(*) FROM `USER` u WHERE " + FILTERS.get("SUSPENDED")),
             count("SELECT COUNT(*) FROM `USER` u WHERE " + FILTERS.get("REPORTED")),
             count("SELECT COUNT(*) FROM `USER` u WHERE " + FILTERS.get("ADMIN")));
     return new AdminUserResponse(safePage, (int) Math.ceil(total / (double) PAGE_SIZE), total, counts, rows);
 }

 // ------------------------------------------------------------------ 상세

 @Transactional(readOnly = true)
 public AdminUserResponse.Detail detail(Long userId) {
     List<AdminUserResponse.Row> found = jdbc.query(ROW_SELECT + " WHERE u.user_id = ?", (rs, n) -> toRow(rs), userId);
     if (found.isEmpty()) {
         throw ApiException.notFound("회원을 찾을 수 없습니다.");
     }
     List<UserSanction> sanctions = sanctionRepository.findByUserIdOrderBySanctionIdDesc(userId);
     Map<Long, String> adminNames = new LinkedHashMap<>();
     userRepository.findAllById(sanctions.stream().map(UserSanction::getAdminId).filter(id -> id != null).distinct().toList())
             .forEach(u -> adminNames.put(u.getUserId(), u.getNickname()));

     // 같은 글·댓글에 신고가 여러 건이면 한 줄로
     Map<String, AdminUserResponse.Reported> reported = new LinkedHashMap<>();
     for (Report r : reportRepository.findTop20ByTargetUserIdAndStatusOrderByReportIdDesc(userId, Report.ACCEPTED)) {
         reported.putIfAbsent(r.getTargetType() + ":" + r.getTargetId(), new AdminUserResponse.Reported(
                 r.getTargetType(), r.getTargetId(), ReportPolicy.label(r.getReasonCode()), r.getAction(), r.getHandledAt()));
     }

     return new AdminUserResponse.Detail(found.get(0),
             sanctions.stream().map(s -> new AdminUserResponse.Sanction(s.getSanctionId(), s.getSanctionType(),
                     s.getReason(), s.getTargetType(), s.getTargetId(), s.getUntilAt(),
                     s.getAdminId() == null ? null : adminNames.getOrDefault(s.getAdminId(), "알 수 없음"),
                     s.getCreatedAt())).toList(),
             new ArrayList<>(reported.values()));
 }

 // ------------------------------------------------------------------ 제재

 /** 관리자 화면에서 직접 제재 */
 @Transactional
 public void sanction(Long adminId, Long userId, String type, String reason) {
     apply(adminId, userId, type, reason.trim(), null, null);
 }

 /**
  * 제재 적용 + 기록 (신고 처리에서도 호출).
  * @return 적용 후 회원 (경고 누적으로 자동 정지됐는지 화면에 알려 줄 때 사용)
  */
 @Transactional
 public User apply(Long adminId, Long userId, String type, String reason, String targetType, Long targetId) {
     if (userId.equals(adminId)) {
         throw ApiException.badRequest("자기 자신에게는 제재할 수 없습니다.");
     }
     User u = userRepository.findByIdForUpdate(userId)
             .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
     if (u.isAdmin() && !"RELEASE".equals(type)) {
         throw new ApiException(HttpStatus.CONFLICT, "관리자에게는 제재할 수 없습니다. 먼저 일반 회원으로 바꿔 주세요.");
     }
     LocalDateTime now = LocalDateTime.now();
     LocalDateTime until = switch (type) {
         case "SUSPEND_7D" -> now.plusDays(7);
         case "SUSPEND_30D" -> now.plusDays(30);
         case "BAN" -> BAN_UNTIL;
         default -> null;
     };

     switch (type) {
         case "WARNING" -> {
             int warnings = u.addWarning();
             sanctionRepository.save(new UserSanction(userId, "WARNING", reason, targetType, targetId, null, adminId));
             // 경고 3회마다 자동 7일 정지
             if (warnings % ReportPolicy.WARNINGS_FOR_SUSPEND == 0) {
                 LocalDateTime auto = now.plusDays(7);
                 u.suspendUntil(auto);
                 sanctionRepository.save(new UserSanction(userId, "SUSPEND_7D",
                         "경고 " + warnings + "회 누적 자동 정지", targetType, targetId, auto, adminId));
             }
         }
         case "SUSPEND_7D", "SUSPEND_30D", "BAN" -> {
             u.suspendUntil(until);
             sanctionRepository.save(new UserSanction(userId, type, reason, targetType, targetId, until, adminId));
         }
         case "RELEASE" -> {
             if (!u.isSuspended(now)) {
                 throw ApiException.badRequest("정지 중인 회원이 아닙니다.");
             }
             u.release();
             sanctionRepository.save(new UserSanction(userId, "RELEASE", reason, null, null, null, adminId));
         }
         default -> throw ApiException.badRequest("알 수 없는 제재입니다: " + type);
     }
     return u;
 }

 /** 역할 변경 (자기 자신 불가) */
 @Transactional
 public void changeRole(Long adminId, Long userId, String role) {
     if (userId.equals(adminId)) {
         throw ApiException.badRequest("자기 자신의 역할은 바꿀 수 없습니다.");
     }
     User u = userRepository.findByIdForUpdate(userId)
             .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
     u.changeRole(role);
 }

 // ------------------------------------------------------------------ 공통

 private long count(String sql, Object... args) {
     Long n = jdbc.queryForObject(sql, Long.class, args);
     return n == null ? 0 : n;
 }

 private static AdminUserResponse.Row toRow(ResultSet rs) throws SQLException {
     LocalDateTime until = time(rs.getTimestamp("suspended_until"));
     LocalDateTime now = LocalDateTime.now();
     return new AdminUserResponse.Row(rs.getLong("user_id"), rs.getString("email"), rs.getString("nickname"),
             rs.getString("role"), rs.getString("status"), time(rs.getTimestamp("created_at")), until,
             until != null && until.isAfter(now), until != null && until.getYear() >= 9999,
             rs.getInt("warning_count"), rs.getLong("travel_count"), rs.getLong("post_count"),
             rs.getLong("comment_count"), rs.getLong("accepted_count"), rs.getLong("pending_count"));
 }

 private static LocalDateTime time(Timestamp t) {
     return t == null ? null : t.toLocalDateTime();
 }
}