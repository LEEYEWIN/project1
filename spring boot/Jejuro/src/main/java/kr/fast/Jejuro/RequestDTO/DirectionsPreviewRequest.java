package kr.fast.Jejuro.RequestDTO;


//[5페이지 루트 짜기 - 저장 전 동선 미리보기]

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.fast.Jejuro.Service.TravelMode;

/**
* POST /api/directions/preview
* 예) { "mode": "CAR", "poiIds": [1, 2, 3] }   poiIds 순서 = 화면에서 정한 방문 순서
* mode는 생략하면 CAR
*/
public record DirectionsPreviewRequest(
     TravelMode mode,
     @NotNull @Size(min = 1, max = 32) List<Long> poiIds) {

 public TravelMode modeOrCar() {
     return mode == null ? TravelMode.CAR : mode;
 }
}