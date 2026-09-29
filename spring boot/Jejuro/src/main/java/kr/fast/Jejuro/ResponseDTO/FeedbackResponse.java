package kr.fast.Jejuro.ResponseDTO;



//[8페이지 후기]

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackResponse(
     Long feedbackId,
     Long routeId,
     String executionStatus,
     Integer satisfactionScore,
     LocalDateTime answeredAt,
     List<Reason> reasons,
     List<Spot> spots) {

 public record Reason(String reasonCode, String reasonText) {
 }

 /** 관광지별 결과. reaction = LIKE / DISLIKE / null */
 public record Spot(Long poiId, boolean visited, String reaction) {
 }
}