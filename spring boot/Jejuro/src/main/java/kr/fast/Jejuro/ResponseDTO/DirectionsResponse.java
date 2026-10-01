package kr.fast.Jejuro.ResponseDTO;



//[6페이지 카카오맵 동선]

import java.util.List;

/**
* GET /api/routes/{routeId}/days/{dayNo}/directions?mode=CAR
* estimated=true 이면 실제 길찾기가 아니라 직선거리로 계산한 추정값이다(도보, 또는 카카오 키 미설정).
* path는 지도에 그릴 선의 좌표 목록.
* notice: 카카오 호출이 실패해 추정값으로 대신했을 때 그 이유(정상이면 null).
* placeNotes: 섬(배편 확인)·산(주차·등산 가능 여부 확인) 관광지 안내.
* totalTaxiFare / Leg.taxiFare: 구간별 예상 택시비(원, 제주 중형택시 거리요금 기준 — 구간마다 따로 탄다고 가정)
*/
public record DirectionsResponse(
   String mode,
   boolean estimated,
   int totalDistanceM,
   int totalDurationSec,
   List<Leg> legs,
   List<LatLng> path,
   String notice,
   int totalTaxiFare,
   List<PlaceNote> placeNotes) {

/** 구간: fromOrder번 → toOrder번 */
public record Leg(int fromOrder, int toOrder, String fromName, String toName,
                 double fromLat, double fromLng, double toLat, double toLng,
                 int distanceM, int durationSec, int taxiFare) {
}

/** 차로 바로 가기 어려운 관광지 안내 (order = 그날 방문 순서, kind = ISLAND / MOUNTAIN) */
public record PlaceNote(int order, String name, String kind, String message) {
}

public record LatLng(double lat, double lng) {
}
}