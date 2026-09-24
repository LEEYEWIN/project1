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
     List<Reason> reasons) {

 public record Reason(String reasonCode, String reasonText) {
 }
}