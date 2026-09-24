package kr.fast.Jejuro.ResponseDTO;


//[7페이지 여행 기록]

import java.time.LocalDate;
import java.util.List;

/**
* 내 여행 목록 카드 한 장.
* phase: BEFORE(여행 전) / DURING(여행 중) / AFTER(다녀옴) — 오늘 날짜 기준 계산값
*/
public record TravelSummaryResponse(
     Long travelId,
     Integer travelNo,
     String travelName,
     LocalDate startDate,
     LocalDate endDate,
     int tripDays,
     String phase,
     List<String> regionNames,
     int companionCount,
     int routeCount,
     Long adoptedRouteId,
     boolean hasFeedback) {
}