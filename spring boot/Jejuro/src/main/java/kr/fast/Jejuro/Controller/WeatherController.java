package kr.fast.Jejuro.Controller;

//[내 여행 달력 - 날씨]

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.ResponseDTO.WeatherResponse;
import kr.fast.Jejuro.Service.WeatherService;

@RestController
public class WeatherController {

 private final WeatherService weatherService;

 public WeatherController(WeatherService weatherService) {
     this.weatherService = weatherService;
 }

 /** 제주 날씨 예보 (오늘부터 16일) → 달력에서 여행 날짜에만 이모지로 표시 */
 @GetMapping("/api/weather/jeju")
 public WeatherResponse jeju() {
     return weatherService.jeju();
 }
}