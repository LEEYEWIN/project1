package kr.fast.Jejuro.ResponseDTO;


//[내 여행 달력 - 날씨]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
* GET /api/weather/jeju → 제주 날씨 예보 (오늘부터 최대 16일, Open-Meteo)
* days가 비어 있으면 예보를 못 받은 것(notice에 이유). 화면은 날씨 없이 달력만 보여 준다.
*/
public record WeatherResponse(List<Day> days, LocalDateTime updatedAt, String notice) {

 /**
  * 하루 예보. code = WMO 날씨 코드(0 맑음, 1~3 구름, 45·48 안개, 51~67 비, 71~77 눈, 80~82 소나기, 95~99 뇌우)
  * rainChance = 강수 확률 최대값(%)
  */
 public record Day(LocalDate date, int code, Double tempMax, Double tempMin, Integer rainChance) {
 }
}