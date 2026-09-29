package kr.fast.Jejuro.ResponseDTO;


//[3페이지 추천 목록 (2~7페이지 공용)]

import java.math.BigDecimal;

/**
* 관광지 카드·지도 마커에 쓰는 공통 응답. 추천 목록·찜·루트에서 모두 같은 모양을 쓴다.
* closedDays: 쉬는 날 (없으면 null) → 자연관광지가 아니면 담을 때 "휴무일 확인" 경고에 표시
* unavailable: 관리자가 삭제한 관광지 → 이름 "확인 불가 (삭제된 관광지)", 상세로 이동 불가
*/
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
     String description,          // 한 줄 소개
     String detailDescription,    // 세부 설명 (없으면 null)
     String closedDays,
     boolean unavailable) {
}