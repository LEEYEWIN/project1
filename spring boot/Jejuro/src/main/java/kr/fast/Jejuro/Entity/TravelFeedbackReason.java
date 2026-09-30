package kr.fast.Jejuro.Entity;


//[8페이지 후기]

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel_feedback_reason")
public class TravelFeedbackReason {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long feedbackReasonId;
 private Long feedbackId;

 @Enumerated(EnumType.STRING)
 private ReasonCode reasonCode;

 private String reasonText;

 protected TravelFeedbackReason() {
 }

 public TravelFeedbackReason(Long feedbackId, ReasonCode reasonCode, String reasonText) {
     this.feedbackId = feedbackId;
     this.reasonCode = reasonCode;
     this.reasonText = reasonText;
 }

 public Long getFeedbackReasonId() { return feedbackReasonId; }
 public Long getFeedbackId() { return feedbackId; }
 public ReasonCode getReasonCode() { return reasonCode; }
 public String getReasonText() { return reasonText; }
}
