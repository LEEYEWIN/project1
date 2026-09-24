package kr.fast.Jejuro.RequestDTO;


// [8페이지 후기 게시판]

import kr.fast.Jejuro.Entity.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/community/posts
 * 예) { "postType":"REVIEW", "title":"동부 2박3일 후기", "content":"...", "travelId": 3 }
 * travelId(선택): 후기에 첨부할 내 여행. 게시판에 그 여행의 최종 경로가 함께 표시된다.
 */
public record PostCreateRequest(
        @NotNull PostType postType,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        Long travelId) {
}