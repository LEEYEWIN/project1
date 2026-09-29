package kr.fast.Jejuro.Repository;


//[커뮤니티 게시판]

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.PostType;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

 /** 최신순: 삭제·신고로 가려지지 않은 글을 글 번호 내림차순으로 (페이지 단위) */
 Page<CommunityPost> findByPostTypeAndDeletedAtIsNullAndHiddenAtIsNullOrderByPostIdDesc(PostType postType, Pageable pageable);

 /** 좋아요순에서 전체 개수(페이지 수 계산용) */
 long countByPostTypeAndDeletedAtIsNullAndHiddenAtIsNull(PostType postType);

 /**
  * 좋아요순: 좋아요 수가 많은 순, 같으면 최신 글 먼저. 이 페이지에 들어갈 글 번호만 돌려준다.
  * (글 내용은 findAllById로 한 번에 읽는다)
  */
 @Query(value = """
         SELECT p.post_id
           FROM COMMUNITY_POST p
          WHERE p.post_type = :type
            AND p.deleted_at IS NULL
            AND p.hidden_at IS NULL
          ORDER BY (SELECT COUNT(*) FROM COMMUNITY_POST_LIKE l WHERE l.post_id = p.post_id) DESC,
                   p.post_id DESC
          LIMIT :size OFFSET :offset
         """, nativeQuery = true)
 List<Number> findIdsOrderByLikes(@Param("type") String type, @Param("size") int size, @Param("offset") int offset);

 /** 신고·신고 처리: 글 행을 잠가 동시에 들어온 신고·처리가 차례로 진행되게 */
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from CommunityPost p where p.postId = :postId")
 Optional<CommunityPost> findByIdForUpdate(@Param("postId") Long postId);

 /** 조회수 +1 (동시에 여러 명이 봐도 정확하게 DB에서 바로 더한다) */
 @Modifying
 @Query(value = "UPDATE COMMUNITY_POST SET view_count = view_count + 1 WHERE post_id = :postId AND deleted_at IS NULL AND hidden_at IS NULL",
         nativeQuery = true)
 int increaseViewCount(@Param("postId") Long postId);

 @Query(value = "SELECT view_count FROM COMMUNITY_POST WHERE post_id = :postId", nativeQuery = true)
 Integer findViewCount(@Param("postId") Long postId);
}