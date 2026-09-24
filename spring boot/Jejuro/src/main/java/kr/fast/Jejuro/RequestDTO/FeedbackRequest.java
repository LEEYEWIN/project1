package kr.fast.Jejuro.RequestDTO;


//[8페이지 후기]

import java.util.List;

import kr.fast.Jejuro.Entity.ExecutionStatus;
import kr.fast.Jejuro.Entity.ReasonCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
* PUT /api/travels/{travelId}/feedback  (처음 저장·수정 모두 PUT)
* 예) { "executionStatus": "PARTIAL", "satisfactionScore": 4,
*       "reasons": [ { "reasonCode": "WEATHER" }, { "reasonCode": "OTHER", "reasonText": "가족 일정" } ] }
*/
public record FeedbackRequest(
     @NotNull ExecutionStatus executionStatus,
     @Min(1) @Max(5) Integer satisfactionScore,
     @Valid List<ReasonReq> reasons) {

 public record ReasonReq(
         @NotNull ReasonCode reasonCode,
         @Size(max = 50) String reasonText) {
 }

 public List<ReasonReq> reasonsOrEmpty() {
     return reasons == null ? List.of() : reasons;
 }
}