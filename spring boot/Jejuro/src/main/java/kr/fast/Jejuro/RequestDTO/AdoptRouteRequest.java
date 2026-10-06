package kr.fast.Jejuro.RequestDTO;


//[7페이지 여행 기록]

/**
* PATCH /api/travels/{travelId}/adopted-route
* { "routeId": 5001 } → 일정 확정
* { "routeId": 5001, "deleteOverlapping": true } → 기간이 겹치는 변경 전 여행을 삭제하고 확정 ("날짜·동행 바꿔 다시 만들기")
*/
public record AdoptRouteRequest(Long routeId, Boolean deleteOverlapping) {

    public boolean deleteOverlappingOrFalse() {
        return Boolean.TRUE.equals(deleteOverlapping);
    }
}
