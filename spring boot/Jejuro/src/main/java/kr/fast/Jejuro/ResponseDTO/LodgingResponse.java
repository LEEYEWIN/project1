package kr.fast.Jejuro.ResponseDTO;


//[6페이지 카카오맵 동선 - 주변 숙소 (FR-26)]

import java.time.LocalDate;
import java.util.List;

/**
* 루트 주변 숙소.
* status
*  - OK       : 숙소 있음
*  - EMPTY    : 최대 반경(10km)까지 찾았는데 없음 (또는 사용자가 고른 반경 안에 없음)
*  - LAST_DAY : 여행 마지막 날이라 기본 조회를 하지 않음 → 기준 관광지를 고르면 조회
*  - NO_SPOT  : 이 날짜와 그 전날까지 방문지가 없어 기준을 정할 수 없음
* anchor는 LAST_DAY·NO_SPOT이면 null.
* expanded=true 이면 결과가 적어서 requestedRadiusKm → radiusKm로 넓혀서 찾은 것.
*/
public record LodgingResponse(
     String status,
     String message,
     int dayNo,
     LocalDate date,
     Anchor anchor,
     String anchorLabel,          // 예) "2일차 마지막 관광지", "직접 고른 관광지"
     double requestedRadiusKm,
     double radiusKm,
     boolean expanded,
     List<Item> items) {

 public record Anchor(Long poiId, String name, double latitude, double longitude, int dayNo, int visitOrder) {
 }

 public record Item(
         Long accommodationId,
         String name,
         String type,             // HOTEL, PENSION …
         String typeName,         // 호텔, 펜션·풀빌라 …
         String address,
         double latitude,
         double longitude,
         String phone,
         String imageUrl,
         String source,           // TOUR / VJ / KAKAO (화면의 출처 표시)
         int distanceM) {
 }
}