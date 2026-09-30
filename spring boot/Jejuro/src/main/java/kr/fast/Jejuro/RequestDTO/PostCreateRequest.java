package kr.fast.Jejuro.RequestDTO;


// [커뮤니티 게시판]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.fast.Jejuro.Entity.PostType;

/**
 * POST /api/community/posts
 * 예) { "postType":"REVIEW", "title":"동부 2박3일 후기", "content":"...", "travelId": 3,
 *       "imageUrl": "/api/community/images/0b6c…e1.jpg" }
 * travelId(선택): 후기에 첨부할 내 여행 → 게시판에 그 여행의 최종 경로가 표시된다.
 * imageUrl(선택): 먼저 POST /api/community/images 로 사진을 올리고 받은 주소
 */
public record PostCreateRequest(
        @NotNull(message = "글 종류를 골라 주세요.") PostType postType,
        @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자까지 쓸 수 있습니다.") String title,
        @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 10000, message = "내용은 10,000자까지 쓸 수 있습니다.") String content,
        Long travelId,
        String imageUrl) {
}