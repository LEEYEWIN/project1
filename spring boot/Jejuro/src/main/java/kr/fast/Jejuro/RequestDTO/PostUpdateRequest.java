package kr.fast.Jejuro.RequestDTO;


//[커뮤니티 게시판]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
* PUT /api/community/posts/{postId}
* imageUrl: 그대로 두려면 기존 주소, 바꾸려면 새로 올린 주소, 지우려면 null
* (글 종류와 첨부 여행은 바꿀 수 없다)
*/
public record PostUpdateRequest(
     @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자까지 쓸 수 있습니다.") String title,
     @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 10000, message = "내용은 10,000자까지 쓸 수 있습니다.") String content,
     String imageUrl) {
}