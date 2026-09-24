package kr.fast.Jejuro.Service;


//[6페이지 카카오맵 동선]

/** 계산용 좌표 + 이름 */
public record GeoPoint(Long poiId, String name, double lat, double lng) {

 private static final double EARTH_RADIUS_M = 6_371_000;

 /** 두 좌표 사이 직선거리(m) - 하버사인 공식 */
 public double distanceTo(GeoPoint o) {
     double dLat = Math.toRadians(o.lat - lat);
     double dLng = Math.toRadians(o.lng - lng);
     double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(o.lat))
             * Math.sin(dLng / 2) * Math.sin(dLng / 2);
     return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
 }
}