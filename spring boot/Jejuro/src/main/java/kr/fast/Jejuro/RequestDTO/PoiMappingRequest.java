package kr.fast.Jejuro.RequestDTO;


// [관리자 관광지 관리 - AI 이름 연결]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/admin/pois/{poiId}/mappings  { "sourcePoiId": "성산일출봉" }
 * AI(FastAPI)가 추천 결과로 주는 장소 이름(VISIT_AREA_NM)을 이 관광지와 연결한다.
 */
public record PoiMappingRequest(
        @NotBlank(message = "AI 장소 이름을 입력해 주세요.") @Size(max = 255) String sourcePoiId) {
}