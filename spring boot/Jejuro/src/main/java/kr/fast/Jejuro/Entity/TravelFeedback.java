package kr.fast.Jejuro.Entity;


//[8페이지 후기]

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 여행당 1건. 채택한 경로를 실제로 수행했는지와 만족도. */
@Entity
@Table(name = "travel_feedback")
public class TravelFeedback {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long feedbackId;
 private Long travelId;
 private Long routeId;

 @Enumerated(EnumType.STRING)
 private ExecutionStatus executionStatus;

 private Integer satisfactionScore;
 private LocalDateTime answeredAt;

 protected TravelFeedback() {
 }

 public TravelFeedback(Long travelId, Long routeId, ExecutionStatus status, Integer score, LocalDateTime now) {
     this.travelId = travelId;
     update(routeId, status, score, now);
 }

 /** 다시 제출하면 내용을 덮어쓴다 */
 public void update(Long routeId, ExecutionStatus status, Integer score, LocalDateTime now) {
     this.routeId = routeId;
     this.executionStatus = status;
     this.satisfactionScore = score;
     this.answeredAt = now;
 }

 public Long getFeedbackId() { return feedbackId; }
 public Long getTravelId() { return travelId; }
 public Long getRouteId() { return routeId; }
 public ExecutionStatus getExecutionStatus() { return executionStatus; }
 public Integer getSatisfactionScore() { return satisfactionScore; }
 public LocalDateTime getAnsweredAt() { return answeredAt; }
}