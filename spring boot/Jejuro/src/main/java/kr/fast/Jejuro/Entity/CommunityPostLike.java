package kr.fast.Jejuro.Entity;


//[커뮤니티 게시판 - 좋아요]

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** 회원 1명이 글 1개에 좋아요 1번. 기본키 = (postId, userId) */
@Entity
@Table(name = "COMMUNITY_POST_LIKE")
@IdClass(CommunityPostLike.Key.class)
public class CommunityPostLike {

 @Id
 private Long postId;

 @Id
 private Long userId;

 protected CommunityPostLike() {
 }

 public CommunityPostLike(Long postId, Long userId) {
     this.postId = postId;
     this.userId = userId;
 }

 public Long getPostId() { return postId; }
 public Long getUserId() { return userId; }

 /** 복합 기본키 */
 public static class Key implements Serializable {
     private Long postId;
     private Long userId;

     public Key() {
     }

     public Key(Long postId, Long userId) {
         this.postId = postId;
         this.userId = userId;
     }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof Key k)) return false;
         return Objects.equals(postId, k.postId) && Objects.equals(userId, k.userId);
     }

     @Override
     public int hashCode() {
         return Objects.hash(postId, userId);
     }
 }
}