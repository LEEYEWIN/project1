package kr.fast.Jejuro.ResponseDTO;


//[커뮤니티 게시판 - 목록]

import java.time.LocalDateTime;

/**
* 목록 카드 한 개.
* travelName·route: 후기에 여행이 첨부된 경우만 값이 있다(없거나 여행이 삭제되면 null).
* reported: 신고되어 관리자 확인을 기다리는 글 → title은 "신고된 게시글입니다", 사진·경로 없음
*/
public record PostSummaryResponse(
     Long postId,
     String postType,
     String title,
     String authorName,
     LocalDateTime createdAt,
     int viewCount,
     long likeCount,
     long commentCount,
     boolean hasImage,
     String travelName,
     RouteDetailResponse route,
     boolean reported) {
}