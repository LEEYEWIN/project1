package kr.fast.Jejuro.Repository;


//[커뮤니티 게시판 - 좋아요]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.CommunityPostLike;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, CommunityPostLike.Key> {

 /** 글별 개수 (목록에서 여러 글을 한 번에) */
 interface PostCount {
     Long getPostId();
     Long getCnt();
 }

 boolean existsByPostIdAndUserId(Long postId, Long userId);

 long countByPostId(Long postId);

 @Query("select l.postId as postId, count(l) as cnt from CommunityPostLike l where l.postId in :postIds group by l.postId")
 List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

 @Modifying
 @Query("delete from CommunityPostLike l where l.postId = :postId and l.userId = :userId")
 int deleteOne(@Param("postId") Long postId, @Param("userId") Long userId);
}
