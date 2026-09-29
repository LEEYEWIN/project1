package kr.fast.Jejuro.RequestDTO;


// [관리자 관광지 관리 - 추가·수정]

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/admin/pois, PUT /api/admin/pois/{poiId}
 * 필수: 이름·주소·위도·경도·분류·권역·한 줄 소개. 나머지는 비우면 "정보 없음"
 * 좌표는 제주 범위(위도 33.0~34.1, 경도 126.0~127.1)여야 한다.
 */
public record AdminPoiRequest(
        @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 200) String poiName,
        @NotBlank(message = "주소를 입력해 주세요.") @Size(max = 500) String address,
        @NotNull(message = "위도를 입력해 주세요.") BigDecimal latitude,
        @NotNull(message = "경도를 입력해 주세요.") BigDecimal longitude,
        @NotBlank(message = "분류를 골라 주세요.") String categoryCode,
        @NotNull(message = "권역을 골라 주세요.") Integer regionId,
        @NotBlank(message = "한 줄 소개를 입력해 주세요.") @Size(max = 1000) String description,
        @Size(max = 10000) String detailDescription,
        @Size(max = 2048) String imageUrl,
        @Size(max = 100) String phone,
        @Size(max = 500) String homepage,
        @Size(max = 1000) String openingHours,
        @Size(max = 1000) String closedDays,
        @Size(max = 1000) String fee,
        @Size(max = 1000) String parking) {
}