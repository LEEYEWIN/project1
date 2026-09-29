package kr.fast.Jejuro.RequestDTO;

// [5페이지 루트 짜기 - 저장 전 동선 미리보기·순서 추천]

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.fast.Jejuro.Service.TravelMode;

/**
 * POST /api/directions/preview, /api/directions/preview/optimize
 * 예) { "mode": "CAR", "poiIds": [1, 2, 3], "fixEnd": false }   poiIds 순서 = 화면에서 정한 방문 순서
 * - mode는 생략하면 CAR
 * - fixEnd(순서 추천에서만 사용): true면 마지막 방문지도 고정. 생략하면 false
 */
public record DirectionsPreviewRequest(
        TravelMode mode,
        @NotNull @Size(min = 1, max = 32) List<Long> poiIds,
        Boolean fixEnd) {

    public TravelMode modeOrCar() {
        return mode == null ? TravelMode.CAR : mode;
    }

    public boolean fixEndOrFalse() {
        return Boolean.TRUE.equals(fixEnd);
    }
}
