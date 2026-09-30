package kr.fast.Jejuro.ResponseDTO;


//[관광지 상세 (FR-33)]

import java.math.BigDecimal;

/**
* 관광지 상세. 없는 정보는 null → 화면에 "정보 없음".
* locationChecked=false: 좌표가 없거나 제주 범위 밖 → 지도를 그리지 않고 "위치 확인 필요"
*/
public record PoiDetailResponse(
     Long poiId,
     String name,
     String address,
     BigDecimal latitude,
     BigDecimal longitude,
     boolean locationChecked,
     String categoryCode,
     Integer regionId,
     String regionName,
     String imageUrl,
     String description,
     String detailDescription) {
}
