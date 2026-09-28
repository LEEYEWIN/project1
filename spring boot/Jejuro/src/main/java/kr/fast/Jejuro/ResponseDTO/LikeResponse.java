package kr.fast.Jejuro.ResponseDTO;


//[커뮤니티 게시판 - 좋아요]

/** 좋아요 누르기·취소 결과 */
public record LikeResponse(boolean liked, long likeCount) {
}