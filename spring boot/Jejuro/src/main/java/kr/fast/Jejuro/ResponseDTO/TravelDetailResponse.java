package kr.fast.Jejuro.ResponseDTO;


//[7페이지 여행 기록]

import java.time.LocalDate;
import java.util.List;

/** 여행 상세(7페이지). 채택한 경로의 일정과 피드백까지 한 번에 내려준다. */
public record TravelDetailResponse(
     Long travelId,
     Integer travelNo,
     String travelName,
     LocalDate startDate,
     LocalDate endDate,
     int tripDays,
     String phase,
     List<String> regionNames,
     List<CompanionItem> companions,
     List<RouteSummaryResponse> routes,
     RouteDetailResponse adoptedRoute,
     FeedbackResponse feedback,
     boolean canWriteFeedback) {

 public record CompanionItem(int seq, String relation, String gender, String ageGroup) {
 }
}