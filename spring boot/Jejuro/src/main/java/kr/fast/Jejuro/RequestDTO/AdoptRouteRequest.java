package kr.fast.Jejuro.RequestDTO;


//[7페이지 여행 기록]

/**
* PATCH /api/travels/{travelId}/adopted-route
* { "routeId": 5001 } → 채택 / { "routeId": null } → 채택 해제
*/
public record AdoptRouteRequest(Long routeId) {
}