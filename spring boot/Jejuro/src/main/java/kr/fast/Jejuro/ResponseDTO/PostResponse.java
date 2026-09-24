package kr.fast.Jejuro.ResponseDTO;


//[8페이지 후기 게시판]

import java.time.LocalDateTime;

/**
* authorName: 탈퇴한 회원이면 "탈퇴한 회원"
* travelName·route: 후기에 여행이 첨부된 경우만 값이 있다(없거나 여행이 삭제되면 null).
*/
public record PostResponse(
     Long postId,
     String postType,
     String title,
     String content,
     String authorName,
     boolean mine,
     LocalDateTime createdAt,
     String travelName,
     RouteDetailResponse route) {
}