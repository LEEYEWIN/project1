package kr.fast.Jejuro.ResponseDTO;


//[커뮤니티 게시판 - 목록]

import java.util.List;

/** GET /api/community/posts 결과. page는 0부터 */
public record PostPageResponse(
     List<PostSummaryResponse> items,
     int page,
     int size,
     int totalPages,
     long totalElements,
     String sort) {
}