package kr.fast.Jejuro.RequestDTO;


//[커뮤니티 게시판 - 댓글·대댓글]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /api/community/comments/{commentId} */
public record CommentUpdateRequest(
     @NotBlank(message = "댓글 내용을 입력해 주세요.") @Size(max = 1000, message = "댓글은 1,000자까지 쓸 수 있습니다.") String content) {
}