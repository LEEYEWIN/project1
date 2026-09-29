package kr.fast.Jejuro.ResponseDTO;


//[관리자 회원 관리]

import java.time.LocalDateTime;
import java.util.List;

/** 회원 목록 한 페이지 + 필터별 인원 */
public record AdminUserResponse(
     int page,
     int totalPages,
     long totalCount,
     Counts counts,
     List<Row> items) {

 /** 필터 칩 옆 숫자: 전체 / 정지 중 / 신고 받은 회원 / 관리자 */
 public record Counts(long all, long suspended, long reported, long admin) {
 }

 /**
  * 회원 한 줄.
  * acceptedCount: 신고가 받아들여져 숨김·삭제된 글·댓글 수, pendingCount: 처리 대기 중인 신고 대상 수
  */
 public record Row(Long userId, String email, String nickname, String role, String status,
                   LocalDateTime createdAt, LocalDateTime suspendedUntil, boolean suspended, boolean banned,
                   int warningCount, long travelCount, long postCount, long commentCount,
                   long acceptedCount, long pendingCount) {
 }

 /** 회원 상세: 기본 정보 + 제재 이력 + 조치된 신고 */
 public record Detail(Row user, List<Sanction> sanctions, List<Reported> reported) {
 }

 public record Sanction(Long sanctionId, String type, String reason, String targetType, Long targetId,
                        LocalDateTime untilAt, String adminName, LocalDateTime createdAt) {
 }

 public record Reported(String targetType, Long targetId, String reasonLabel, String action, LocalDateTime handledAt) {
 }
}