package kr.fast.Jejuro.Controller;

// [여행 상세 - 확정한 일정 관광지별 날씨]

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.RequestDTO.WeatherSpotsRequest;
import kr.fast.Jejuro.ResponseDTO.WeatherResponse;
import kr.fast.Jejuro.Service.WeatherService;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 관광지 위치·날짜별 날씨 (오전/오후 날씨 코드, 최저/최고 기온)
     * 본문 { spots: [{ latitude, longitude, date }] } → { items: [ {date, amCode, pmCode, tempMin, tempMax} | null ], notice }
     */
    @PostMapping("/api/weather/spots")
    public WeatherResponse spots(@Valid @RequestBody WeatherSpotsRequest req) {
        return weatherService.spots(req);
    }
}