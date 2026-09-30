package kr.fast.Jejuro.ResponseDTO;


//[커뮤니티 게시판 - 댓글·대댓글]

import java.time.LocalDateTime;
import java.util.List;

/**
* 댓글 한 개 + 그 아래 대댓글(replies).
* deleted=true: 대댓글이 남아 있어서 자리만 남긴 삭제 댓글 (content·authorName은 null)
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
     List<CommentResponse> replies) {
}
