package kr.fast.Jejuro.ResponseDTO;


//[5페이지 루트 짜기 (6·7페이지에서도 사용)]

import java.time.LocalDate;
import java.util.List;

/**
* 경로 상세: "1일차 1번 한라산, 2번 바다" 형태.
* locked=true 이면 화면은 읽기 전용(편집·순서 추천 적용 불가).
* tripDays는 여행 전체 일수(편집 화면의 일차 탭 개수), days는 방문지가 있는 일차만 들어 있다.
*/
public record RouteDetailResponse(
     Long routeId,
     Long travelId,
     String routeName,
     boolean adopted,
     boolean locked,          // 이 여행에 최종 경로가 채택됨 → 모든 경로 수정 불가
     LocalDate startDate,
     LocalDate endDate,
     int tripDays,
     List<DayResponse> days) {

 public record DayResponse(
         Integer dayNo,
         LocalDate date,
         Integer primaryRegionId,
         List<SpotResponse> spots) {
 }

 public record SpotResponse(Integer visitOrder, PoiSummaryResponse poi) {
 }
}