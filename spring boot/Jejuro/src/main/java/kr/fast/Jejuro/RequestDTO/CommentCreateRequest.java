package kr.fast.Jejuro.RequestDTO;


//[커뮤니티 게시판 - 댓글·대댓글]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
* POST /api/community/posts/{postId}/comments
* parentCommentId: 대댓글이면 원댓글 ID (없으면 일반 댓글)
*/
public record CommentCreateRequest(
     @NotBlank(message = "댓글 내용을 입력해 주세요.") @Size(max = 1000, message = "댓글은 1,000자까지 쓸 수 있습니다.") String content,
     Long parentCommentId) {
}