package kr.fast.Jejuro.RequestDTO;


//[5페이지 루트 짜기]

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
* PUT /api/routes/{routeId}  일정 전체 저장(덮어쓰기).
* 예) { "routeName": "동부 2박3일",
*       "days": [ { "dayNo": 1, "poiIds": [2001, 2005, 2010] },
*                 { "dayNo": 2, "poiIds": [2003] } ] }
* poiIds의 순서가 곧 방문 순서(visit_order 1, 2, 3 ...)다. 방문지가 없는 일차는 저장하지 않는다.
*/
public record RouteSaveRequest(
     @Size(max = 100) String routeName,
     @NotNull @Valid List<DayReq> days) {

 public record DayReq(
         @NotNull @Min(1) Integer dayNo,
         @NotNull List<Long> poiIds) {
 }
}