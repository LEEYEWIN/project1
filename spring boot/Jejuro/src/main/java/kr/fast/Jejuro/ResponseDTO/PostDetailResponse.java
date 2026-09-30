package kr.fast.Jejuro.ResponseDTO;

// [커뮤니티 게시판 - 상세]

import java.time.LocalDateTime;

/**
 * 글 상세.
 * mine: 로그인 회원이 쓴 글 → 수정·삭제 버튼 표시
 * liked: 로그인 회원이 좋아요를 눌렀는지
 * satisfaction: 첨부한 여행의 만족도 별점(1~5, 없으면 null)
 * route: 첨부한 여행의 최종 경로(없으면 null) → 있으면 [링크 공유]·[내 여행으로 가져오기] 표시
 * importCount: 이 경로를 가져가 여행을 만든 수
 */
public record PostDetailResponse(
        Long postId,
        String postType,
        String title,
        String content,
        String imageUrl,
        String authorName,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int viewCount,
        long likeCount,
        boolean liked,
        long commentCount,
        Long travelId,
        String travelName,
        Integer satisfaction,
        RouteDetailResponse route,
        long importCount) {
}
