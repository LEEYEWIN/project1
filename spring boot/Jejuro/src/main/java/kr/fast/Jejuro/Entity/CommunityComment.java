package kr.fast.Jejuro.Entity;


//[커뮤니티 게시판 - 댓글·대댓글]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 댓글. parentCommentId가 있으면 대댓글(한 단계만).
* 삭제는 deletedAt만 기록한다. 대댓글이 달린 원댓글은 "삭제된 댓글입니다."로 자리를 남긴다.
*/
@Entity
@Table(name = "COMMUNITY_COMMENT")
public class CommunityComment {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long commentId;
 private Long postId;
 private Long parentCommentId;
 private Long userId;
 private String content;

 @Column(insertable = false, updatable = false)
 private LocalDateTime createdAt;

 private LocalDateTime updatedAt;
 private LocalDateTime deletedAt;

 protected CommunityComment() {
 }

 public CommunityComment(Long postId, Long parentCommentId, Long userId, String content) {
     this.postId = postId;
     this.parentCommentId = parentCommentId;
     this.userId = userId;
     this.content = content;
 }

 public void edit(String content, LocalDateTime now) {
     this.content = content;
     this.updatedAt = now;
 }

 public void delete(LocalDateTime now) {
     this.deletedAt = now;
 }

 public boolean isDeleted() { return deletedAt != null; }
 public boolean isReply() { return parentCommentId != null; }
 public boolean isWrittenBy(Long loginUserId) { return userId != null && userId.equals(loginUserId); }

 public Long getCommentId() { return commentId; }
 public Long getPostId() { return postId; }
 public Long getParentCommentId() { return parentCommentId; }
 public Long getUserId() { return userId; }
 public String getContent() { return content; }
 public LocalDateTime getCreatedAt() { return createdAt; }
 public LocalDateTime getUpdatedAt() { return updatedAt; }
 public LocalDateTime getDeletedAt() { return deletedAt; }
}