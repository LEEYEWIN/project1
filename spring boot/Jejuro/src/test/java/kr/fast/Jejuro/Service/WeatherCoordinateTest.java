package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.RequestDTO.WeatherSpotsRequest;

/** 제주 밖 좌표·NaN은 외부 API를 부르기 전에 한글 문구로 거절 */
class WeatherCoordinateTest {
    private final WeatherService service = new WeatherService(false);

    private WeatherSpotsRequest req(double lat, double lng) {
        return new WeatherSpotsRequest(List.of(new WeatherSpotsRequest.Spot(lat, lng, LocalDate.of(2026, 10, 3))));
    }

    @Test
    void rejectsOutsideJejuAndNaN() {
        for (double[] c : new double[][] { { 999, 126.5 }, { 33.4, 10 }, { Double.NaN, 126.5 } }) {
            ApiException e = assertThrows(ApiException.class, () -> service.spots(req(c[0], c[1])));
            assertEquals("제주 지역 좌표만 사용할 수 있어요.", e.getMessage());
        }
    }

    @Test
    void acceptsJeju() {
        service.spots(req(33.4581, 126.9426)); // weather.enabled=false 라 외부 호출 없이 통과
    }
}
