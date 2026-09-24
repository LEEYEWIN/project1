package kr.fast.Jejuro.Repository;


//[8페이지 후기 게시판]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.PostType;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
 List<CommunityPost> findTop50ByPostTypeOrderByPostIdDesc(PostType postType);
}