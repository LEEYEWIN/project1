package kr.fast.Jejuro.Entity;


//[8페이지 후기 게시판]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 커뮤니티 글. 작성자가 탈퇴(최종 삭제)되면 userId가 NULL이 된다. */
@Entity
@Table(name = "COMMUNITY_POST")
public class CommunityPost {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long postId;
 private Long userId;
 private Long travelId;      // 후기에 첨부한 여행(선택). 여행이 삭제되면 DB가 NULL로 바꾼다.

 @Enumerated(EnumType.STRING)
 private PostType postType;

 private String title;
 private String content;

 @Column(insertable = false, updatable = false)
 private LocalDateTime createdAt;

 protected CommunityPost() {
 }

 public CommunityPost(Long userId, Long travelId, PostType postType, String title, String content) {
     this.userId = userId;
     this.travelId = travelId;
     this.postType = postType;
     this.title = title;
     this.content = content;
 }

 public Long getPostId() { return postId; }
 public Long getUserId() { return userId; }
 public Long getTravelId() { return travelId; }
 public PostType getPostType() { return postType; }
 public String getTitle() { return title; }
 public String getContent() { return content; }
 public LocalDateTime getCreatedAt() { return createdAt; }
}