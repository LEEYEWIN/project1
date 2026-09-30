package kr.fast.Jejuro.ResponseDTO;



// [커뮤니티 게시판 - 댓글·대댓글]

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 한 개 + 그 아래 대댓글(replies).
 * deleted=true: 대댓글이 남아 있어서 자리만 남긴 삭제 댓글 (content·authorName은 null)
 * hidden=true: 신고된 댓글(검토 중) 또는 차단된 댓글. 관리자가 아니면 content·authorName은 null ("신고된 댓글입니다." / 차단 안내)
 * reportedByMe: 로그인 회원이 이미 신고한 댓글
 * blockReason: 관리자가 차단한 사유 이름 ("욕설·비방 등의 사유로 차단된 댓글입니다." 표시용, 아니면 null)
 */
public record CommentResponse(
        Long commentId,
        Long parentCommentId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine,
        boolean deleted,
        boolean hidden,
        boolean reportedByMe,
        String blockReason,
        List<CommentResponse> replies) {
}