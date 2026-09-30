package kr.fast.Jejuro.Entity;



//[커뮤니티 게시판]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 커뮤니티 글 (여행 후기 / 질문).
* - 작성자가 탈퇴(최종 삭제)되면 userId가 NULL이 된다.
* - 삭제는 deletedAt만 기록한다(목록·상세에서 제외). 좋아요·댓글 행은 그대로 남는다.
* - 조회수는 동시에 여러 명이 봐도 틀리지 않게 레포지토리의 UPDATE 쿼리로 올린다.
*/
@Entity
@DynamicUpdate   // 바뀐 칼럼만 UPDATE (신고 가림·글 수정·제재가 동시에 일어나도 서로 덮어쓰지 않게)
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
private String imageUrl;    // 첨부 사진 1장 (/api/community/images/파일이름)

@Column(insertable = false, updatable = false)
private Integer viewCount;

@Column(insertable = false, updatable = false)
private LocalDateTime createdAt;

private LocalDateTime updatedAt;
private LocalDateTime deletedAt;
private LocalDateTime hiddenAt;     // 신고로 가림 (작성자·관리자만 내용 확인)
private String blockReason;         // 관리자 차단 사유 (신고 사유 코드). 있으면 관리자만 내용 확인

protected CommunityPost() {
}

public CommunityPost(Long userId, Long travelId, PostType postType, String title, String content, String imageUrl) {
   this.userId = userId;
   this.travelId = travelId;
   this.postType = postType;
   this.title = title;
   this.content = content;
   this.imageUrl = imageUrl;
}

public void edit(String title, String content, String imageUrl, LocalDateTime now) {
   this.title = title;
   this.content = content;
   this.imageUrl = imageUrl;
   this.updatedAt = now;
}

public void delete(LocalDateTime now) {
   this.deletedAt = now;
}

public boolean isDeleted() { return deletedAt != null; }
public boolean isHidden() { return hiddenAt != null; }

/** 신고로 가리기 (이미 가려져 있으면 처음 시각 유지) */
public void hide(LocalDateTime now) {
   if (hiddenAt == null) hiddenAt = now;
}

/** 관리자가 "문제 없음"으로 처리 → 다시 보이기 */
public void unhide() {
   hiddenAt = null;
   blockReason = null;
}

/** 관리자 차단: 가리고 사유를 남긴다 → 작성자 포함 모두에게 "○○ 사유로 차단" 안내 */
public void block(String reason, LocalDateTime now) {
   hide(now);
   blockReason = reason;
}

public boolean isBlocked() { return blockReason != null; }
public String getBlockReason() { return blockReason; }
public boolean isWrittenBy(Long loginUserId) { return userId != null && userId.equals(loginUserId); }

public Long getPostId() { return postId; }
public Long getUserId() { return userId; }
public Long getTravelId() { return travelId; }
public PostType getPostType() { return postType; }
public String getTitle() { return title; }
public String getContent() { return content; }
public String getImageUrl() { return imageUrl; }
public Integer getViewCount() { return viewCount; }
public LocalDateTime getCreatedAt() { return createdAt; }
public LocalDateTime getUpdatedAt() { return updatedAt; }
public LocalDateTime getDeletedAt() { return deletedAt; }
public LocalDateTime getHiddenAt() { return hiddenAt; }
}