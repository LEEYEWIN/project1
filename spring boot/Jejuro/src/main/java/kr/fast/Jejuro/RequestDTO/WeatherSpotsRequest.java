package kr.fast.Jejuro.RequestDTO;


//[여행 상세 - 확정한 일정 관광지별 날씨]

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
* POST /api/weather/spots
* 본문 예) { "spots": [ { "latitude": 33.4581, "longitude": 126.9426, "date": "2026-10-03" }, ... ] }
* 확정한 일정의 관광지 위치 + 그 관광지를 가는 날짜 (한 번에 최대 80곳)
*/
public record WeatherSpotsRequest(@NotEmpty @Size(max = 80) @Valid List<Spot> spots) {

 public record Spot(@NotNull Double latitude, @NotNull Double longitude, @NotNull LocalDate date) {
 }
}