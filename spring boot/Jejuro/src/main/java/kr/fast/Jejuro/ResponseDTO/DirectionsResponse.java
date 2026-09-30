package kr.fast.Jejuro.ResponseDTO;


//[6페이지 카카오맵 동선]

import java.util.List;

/**
* GET /api/routes/{routeId}/days/{dayNo}/directions?mode=CAR
* estimated=true 이면 실제 길찾기가 아니라 직선거리로 계산한 추정값이다(도보, 또는 카카오 키 미설정).
* path는 지도에 그릴 선의 좌표 목록.
* notice: 카카오 호출이 실패해 추정값으로 대신했을 때 그 이유(정상이면 null).
*/
public record DirectionsResponse(
     String mode,
     boolean estimated,
     int totalDistanceM,
     int totalDurationSec,
     List<Leg> legs,
     List<LatLng> path,
     String notice) {

 /** 구간: fromOrder번 → toOrder번 */
 public record Leg(int fromOrder, int toOrder, String fromName, String toName,
                   double fromLat, double fromLng, double toLat, double toLng,
                   int distanceM, int durationSec) {
 }

 public record LatLng(double lat, double lng) {
 }
}