package kr.fast.Jejuro.Repository;


//[커뮤니티 게시판 - 댓글·대댓글]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.CommunityComment;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

 /** 삭제되지 않은 댓글 수 (글별, 목록에서 여러 글을 한 번에) */
 interface PostCount {
     Long getPostId();
     Long getCnt();
 }

 /** 글의 댓글 전체(삭제 표시 포함), 오래된 순 */
 List<CommunityComment> findByPostIdOrderByCommentIdAsc(Long postId);

 long countByPostIdAndDeletedAtIsNull(Long postId);

 @Query("select c.postId as postId, count(c) as cnt from CommunityComment c "
         + "where c.postId in :postIds and c.deletedAt is null group by c.postId")
 List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);
}