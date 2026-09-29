package kr.fast.Jejuro.Entity;


//[커뮤니티 신고 · 관리자 신고 처리]

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 신고 1건 = 회원 1명이 글 또는 댓글 1개를 신고. (같은 대상은 한 번만 — DB UNIQUE)
* status: PENDING 대기 → 관리자가 처리하면 ACCEPTED(숨김·삭제) / REJECTED(문제 없음, 유지)
*/
@Entity
@Table(name = "REPORT")
public class Report {

 public static final String POST = "POST";
 public static final String COMMENT = "COMMENT";
 public static final Set<String> TARGET_TYPES = Set.of(POST, COMMENT);

 public static final String PENDING = "PENDING";
 public static final String ACCEPTED = "ACCEPTED";
 public static final String REJECTED = "REJECTED";

 /** 처리 결과 */
 public static final String KEEP = "KEEP";
 public static final String BLOCK = "BLOCK";
 public static final String DELETE = "DELETE";

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long reportId;
 private String targetType;
 private Long targetId;
 private Long targetUserId;
 private Long reporterId;
 private String reasonCode;
 private String detail;
 private String status;
 private String action;

 @Column(insertable = false, updatable = false)
 private LocalDateTime createdAt;

 private LocalDateTime handledAt;
 private Long handledBy;

 protected Report() {
 }

 public Report(String targetType, Long targetId, Long targetUserId, Long reporterId, String reasonCode, String detail) {
     this.targetType = targetType;
     this.targetId = targetId;
     this.targetUserId = targetUserId;
     this.reporterId = reporterId;
     this.reasonCode = reasonCode;
     this.detail = detail;
     this.status = PENDING;
 }

 /** 관리자 처리: KEEP이면 REJECTED, BLOCK·DELETE면 ACCEPTED */
 public void handle(String action, Long adminId, LocalDateTime now) {
     this.action = action;
     this.status = KEEP.equals(action) ? REJECTED : ACCEPTED;
     this.handledBy = adminId;
     this.handledAt = now;
 }

 public Long getReportId() { return reportId; }
 public String getTargetType() { return targetType; }
 public Long getTargetId() { return targetId; }
 public Long getTargetUserId() { return targetUserId; }
 public Long getReporterId() { return reporterId; }
 public String getReasonCode() { return reasonCode; }
 public String getDetail() { return detail; }
 public String getStatus() { return status; }
 public String getAction() { return action; }
 public LocalDateTime getCreatedAt() { return createdAt; }
 public LocalDateTime getHandledAt() { return handledAt; }
 public Long getHandledBy() { return handledBy; }
}