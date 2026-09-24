package kr.fast.Jejuro.ResponseDTO;


//[5페이지 루트 짜기]

import java.time.LocalDateTime;

/** 여행의 경로 목록 한 줄 */
public record RouteSummaryResponse(
     Long routeId,
     String routeName,
     LocalDateTime createdAt,
     boolean adopted,
     int dayCount,
     int spotCount) {
}