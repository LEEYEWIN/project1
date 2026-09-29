package kr.fast.Jejuro.ResponseDTO;


//[관리자 신고 처리]

import java.time.LocalDateTime;
import java.util.List;

/**
* 신고 목록: 신고 1건씩이 아니라 "신고된 글·댓글" 단위로 묶어서 보여 준다.
* pendingCount: 처리 대기 중인 대상 수 (왼쪽 메뉴 숫자)
*/
public record AdminReportResponse(
     long pendingCount,
     int page,
     int totalPages,
     long totalCount,
     List<Target> items) {

 /**
  * 신고 대상 하나.
  * postId: 화면에서 글로 이동할 번호(댓글이면 그 댓글의 글), content: 글 제목·내용 또는 댓글 내용 앞부분
  * authorPriorAccepted: 작성자가 이전에 조치받은 글·댓글 수 (이 대상 제외)
  * status: PENDING / ACCEPTED / REJECTED, action: KEEP / BLOCK / DELETE (처리 전 null)
  * blockReason: 차단된 경우 차단 사유 코드 (아니면 null)
  * recommend: 처리 기준에 따른 권장 처리
  */
 public record Target(String targetType, Long targetId, Long postId, String postTitle, String content,
                      Long authorId, String authorName, int authorWarningCount,
                      LocalDateTime authorSuspendedUntil, long authorPriorAccepted,
                      boolean hidden, String blockReason, boolean deleted, long reportCount, List<ReasonCount> reasons,
                      List<String> details, LocalDateTime firstReportedAt, LocalDateTime lastReportedAt,
                      String status, String action, LocalDateTime handledAt, String handledByName,
                      Recommend recommend) {
 }

 public record ReasonCount(String code, String label, long count) {
 }

 public record Recommend(String action, String sanction, String note) {
 }
}