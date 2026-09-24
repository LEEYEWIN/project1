package kr.fast.Jejuro.ResponseDTO;


//[3페이지 추천 목록 (2~7페이지 공용)]

import java.math.BigDecimal;

/** 관광지 카드·지도 마커에 쓰는 공통 응답. 추천 목록·찜·루트에서 모두 같은 모양을 쓴다. */
public record PoiSummaryResponse(
     Long poiId,
     String name,
     String address,
     BigDecimal latitude,
     BigDecimal longitude,
     String categoryCode,
     Integer regionId,
     String regionName,
     String imageUrl,
     String description) {
}