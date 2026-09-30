package kr.fast.Jejuro.ResponseDTO;


// [여행 상세 - 확정한 일정 관광지별 날씨]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/weather/spots → 요청한 순서 그대로 관광지 위치·날짜별 날씨 (Open-Meteo)
 * items[i]가 null이면 그 날짜의 예보가 없음(오늘부터 16일 밖, 지난 날짜, 받기 실패) → 화면은 날씨를 표시하지 않는다.
 * notice: 전체를 못 받았을 때 이유
 */
public record WeatherResponse(List<Spot> items, LocalDateTime updatedAt, String notice) {

    /**
     * 한 관광지·하루 날씨.
     * amCode / pmCode = 오전(06~11시) / 오후(12~17시) 중 가장 궂은 WMO 날씨 코드
     *   (0 맑음, 1~3 구름, 45·48 안개, 51~57 이슬비, 61~67 비, 71~77 눈, 80~82 소나기, 85·86 눈 소나기, 95~99 뇌우)
     * tempMin / tempMax = 그날 최저 / 최고 기온(°C)
     */
    public record Spot(LocalDate date, Integer amCode, Integer pmCode, Double tempMin, Double tempMax) {
    }
}