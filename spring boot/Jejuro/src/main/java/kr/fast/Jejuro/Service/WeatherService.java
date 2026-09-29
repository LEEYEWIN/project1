package kr.fast.Jejuro.Service;


//[내 여행 달력 - 날씨]

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import kr.fast.Jejuro.ResponseDTO.WeatherResponse;
import kr.fast.Jejuro.ResponseDTO.WeatherResponse.Day;

/**
* 제주 날씨 예보 (Open-Meteo, API 키 없음·무료, 오늘부터 16일).
* - 제주 한 지점(기본 제주시청 근처)의 일별 예보를 1시간 동안 메모리에 보관 → 달력을 열 때마다 외부 호출하지 않음
* - 실패해도 오류를 내지 않고 빈 목록 + 안내(notice) → 달력은 날씨 없이 그대로
* - 예보 범위(16일) 밖의 여행 날짜는 화면에 날씨를 표시하지 않는다
*/
@Service
public class WeatherService {

 private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
 private static final Duration CACHE = Duration.ofHours(1);
 /** 실패하면 이 시간 동안은 다시 부르지 않음 (달력을 열 때마다 기다리지 않게) */
 private static final Duration RETRY_AFTER = Duration.ofMinutes(5);

 private final RestClient restClient;
 private final URI url;
 private final boolean enabled;

 private volatile WeatherResponse cached;
 private volatile LocalDateTime cachedAt;
 private volatile LocalDateTime failedAt;

 public WeatherService(@Value("${weather.enabled:true}") boolean enabled,
                       @Value("${weather.latitude:33.4996}") double latitude,
                       @Value("${weather.longitude:126.5312}") double longitude) {
     this.enabled = enabled;
     // URI로 만들어 넘겨야 RestClient가 %2F를 다시 인코딩하지 않는다
     this.url = URI.create("https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude
             + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
             + "&timezone=Asia%2FSeoul&forecast_days=16");
     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     factory.setConnectTimeout(Duration.ofSeconds(3));
     factory.setReadTimeout(Duration.ofSeconds(5));
     this.restClient = RestClient.builder().requestFactory(factory).build();
 }

 public WeatherResponse jeju() {
     if (!enabled) {
         return new WeatherResponse(List.of(), null, "날씨 기능이 꺼져 있습니다 (weather.enabled=false).");
     }
     WeatherResponse c = cached;
     if (c != null && cachedAt != null && cachedAt.plus(CACHE).isAfter(LocalDateTime.now())) {
         return c;
     }
     LocalDateTime f = failedAt;
     if (f != null && f.plus(RETRY_AFTER).isAfter(LocalDateTime.now())) {
         return c != null ? c : new WeatherResponse(List.of(), null, "날씨 예보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.");
     }
     try {
         WeatherResponse fresh = fetch();
         cached = fresh;
         cachedAt = LocalDateTime.now();
         failedAt = null;
         return fresh;
     } catch (RuntimeException e) {
         failedAt = LocalDateTime.now();
         log.warn("날씨 예보를 받지 못했습니다: {}", e.getMessage());
         // 예전에 받은 예보가 있으면 그대로, 없으면 빈 목록
         return c != null ? c : new WeatherResponse(List.of(), null, "날씨 예보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.");
     }
 }

 @SuppressWarnings("unchecked")
 private WeatherResponse fetch() {
     Map<String, Object> res;
     try {
         res = restClient.get().uri(url).retrieve().body(Map.class);
     } catch (RestClientException e) {
         throw new IllegalStateException("Open-Meteo 연결 실패: " + e.getMessage(), e);
     }
     Map<String, Object> daily = res == null ? null : (Map<String, Object>) res.get("daily");
     if (daily == null) {
         throw new IllegalStateException("Open-Meteo 응답에 daily가 없습니다");
     }
     List<Object> time = (List<Object>) daily.get("time");
     List<Object> code = (List<Object>) daily.get("weather_code");
     List<Object> max = (List<Object>) daily.get("temperature_2m_max");
     List<Object> min = (List<Object>) daily.get("temperature_2m_min");
     List<Object> rain = (List<Object>) daily.get("precipitation_probability_max");
     List<Day> days = new ArrayList<>();
     for (int i = 0; time != null && i < time.size(); i++) {
         Number wc = num(code, i);
         if (wc == null) continue;
         Number r = num(rain, i);
         days.add(new Day(LocalDate.parse(String.valueOf(time.get(i))), wc.intValue(),
                 dbl(max, i), dbl(min, i), r == null ? null : r.intValue()));
     }
     return new WeatherResponse(days, LocalDateTime.now(), null);
 }

 private static Number num(List<Object> list, int i) {
     return list != null && i < list.size() && list.get(i) instanceof Number n ? n : null;
 }

 private static Double dbl(List<Object> list, int i) {
     Number n = num(list, i);
     return n == null ? null : Math.round(n.doubleValue() * 10) / 10.0;
 }
}