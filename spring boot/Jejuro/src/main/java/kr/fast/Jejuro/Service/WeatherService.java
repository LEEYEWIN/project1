package kr.fast.Jejuro.Service;


//[여행 상세 - 확정한 일정 관광지별 날씨]

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import kr.fast.Jejuro.RequestDTO.WeatherSpotsRequest;
import kr.fast.Jejuro.ResponseDTO.WeatherResponse;

/**
* 관광지 위치·날짜별 날씨 (Open-Meteo, API 키 없음·무료, 지난 31일 ~ 앞으로 16일. 지난 날짜는 실제 날씨).
* - 가까운 관광지는 같은 예보를 쓰도록 좌표를 0.05°(약 5km) 격자로 묶는다 → 제주 전체라도 격자 수십 개
* - 격자별 예보를 1시간 동안 메모리에 보관. 없는 격자만 한 번의 요청(좌표 여러 개)으로 받는다
* - 오전 = 06~11시, 오후 = 12~17시 시간별 날씨 코드 중 가장 궂은 것 / 최저·최고 기온은 일별 값
* - 실패해도 오류를 내지 않고 빈 칸 + 안내(notice). 실패하면 5분 동안은 다시 부르지 않는다
* - 범위(지난 31일 ~ 앞으로 16일) 밖 날짜는 null → 화면에 날씨 없음
*/
@Service
public class WeatherService {

 private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
 private static final Duration CACHE = Duration.ofHours(1);
 private static final Duration RETRY_AFTER = Duration.ofMinutes(5);
 private static final double GRID = 0.05;
 private static final int MAX_GRIDS = 500;
 /** 지난 날짜도 보여 줌 (이미 다녀온·지난 여행). Open-Meteo는 최대 92일 */
 private static final int PAST_DAYS = 31;
 private static final int AM_FROM = 6, AM_TO = 11, PM_FROM = 12, PM_TO = 17;

 private final RestClient restClient;
 private final boolean enabled;

 /** 격자 키("33.45,126.55") → 예보 */
 private final Map<String, Forecast> cache = new ConcurrentHashMap<>();
 private volatile LocalDateTime failedAt;

 private record Forecast(LocalDateTime fetchedAt, Map<LocalDate, WeatherResponse.Spot> days) {
 }

 public WeatherService(@Value("${weather.enabled:true}") boolean enabled) {
     this.enabled = enabled;
     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     factory.setConnectTimeout(Duration.ofSeconds(3));
     factory.setReadTimeout(Duration.ofSeconds(8));
     this.restClient = RestClient.builder().requestFactory(factory).build();
 }

 public WeatherResponse spots(WeatherSpotsRequest req) {
     List<WeatherSpotsRequest.Spot> spots = req.spots();
     if (!enabled) {
         return new WeatherResponse(nulls(spots.size()), null, "날씨 기능이 꺼져 있습니다 (weather.enabled=false).");
     }
     LocalDateTime now = LocalDateTime.now();

     // 1) 필요한 격자 중 보관 중이 아닌 것만 받기
     Map<String, double[]> need = new LinkedHashMap<>();
     for (WeatherSpotsRequest.Spot s : spots) {
         String key = key(s.latitude(), s.longitude());
         Forecast f = cache.get(key);
         if (f == null || f.fetchedAt().plus(CACHE).isBefore(now)) {
             need.putIfAbsent(key, new double[] { snap(s.latitude()), snap(s.longitude()) });
         }
     }
     String notice = null;
     if (!need.isEmpty()) {
         LocalDateTime f = failedAt;
         if (f != null && f.plus(RETRY_AFTER).isAfter(now)) {
             notice = "날씨 예보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.";
         } else {
             try {
                 if (cache.size() > MAX_GRIDS) {   // 오래된 예보 정리 (제주는 격자 수백 개 이하)
                     cache.values().removeIf(x -> x.fetchedAt().plus(CACHE).isBefore(now));
                 }
                 cache.putAll(fetch(need));
                 failedAt = null;
             } catch (RuntimeException e) {
                 failedAt = now;
                 log.warn("날씨 예보를 받지 못했습니다: {}", e.getMessage());
                 notice = "날씨 예보를 불러오지 못했어요. 잠시 후 다시 확인해 주세요.";
             }
         }
     }

     // 2) 요청 순서대로 채우기 (예전에 받은 예보가 있으면 실패해도 그걸 씀)
     List<WeatherResponse.Spot> items = new ArrayList<>(spots.size());
     for (WeatherSpotsRequest.Spot s : spots) {
         Forecast f = cache.get(key(s.latitude(), s.longitude()));
         items.add(f == null ? null : f.days().get(s.date()));
     }
     return new WeatherResponse(items, now, notice);
 }

 // ------------------------------------------------------------------ Open-Meteo

 @SuppressWarnings("unchecked")
 private Map<String, Forecast> fetch(Map<String, double[]> grids) {
     List<String> keys = new ArrayList<>(grids.keySet());
     String lats = keys.stream().map(k -> String.valueOf(grids.get(k)[0])).collect(Collectors.joining(","));
     String lngs = keys.stream().map(k -> String.valueOf(grids.get(k)[1])).collect(Collectors.joining(","));
     // URI로 만들어 넘겨야 RestClient가 %2F·%2C를 다시 인코딩하지 않는다
     URI uri = URI.create("https://api.open-meteo.com/v1/forecast?latitude=" + lats.replace(",", "%2C")
             + "&longitude=" + lngs.replace(",", "%2C")
             + "&hourly=weather_code&daily=temperature_2m_max%2Ctemperature_2m_min"
             + "&timezone=Asia%2FSeoul&past_days=" + PAST_DAYS + "&forecast_days=16");
     Object body;
     try {
         body = restClient.get().uri(uri).retrieve().body(Object.class);
     } catch (RestClientException e) {
         throw new IllegalStateException("Open-Meteo 연결 실패: " + e.getMessage(), e);
     }
     // 좌표가 1개면 객체, 여러 개면 배열로 온다
     List<Object> list = body instanceof List<?> l ? (List<Object>) l : List.of(body);
     if (list.size() != keys.size()) {
         throw new IllegalStateException("Open-Meteo 응답 개수가 다릅니다: " + list.size() + " / " + keys.size());
     }
     LocalDateTime now = LocalDateTime.now();
     Map<String, Forecast> result = new HashMap<>();
     for (int i = 0; i < keys.size(); i++) {
         result.put(keys.get(i), new Forecast(now, parse((Map<String, Object>) list.get(i))));
     }
     return result;
 }

 @SuppressWarnings("unchecked")
 private static Map<LocalDate, WeatherResponse.Spot> parse(Map<String, Object> res) {
     Map<String, Object> hourly = res == null ? null : (Map<String, Object>) res.get("hourly");
     Map<String, Object> daily = res == null ? null : (Map<String, Object>) res.get("daily");
     if (hourly == null || daily == null) {
         throw new IllegalStateException("Open-Meteo 응답에 hourly/daily가 없습니다");
     }
     // 시간별 → 날짜별 오전·오후 가장 궂은 코드
     Map<LocalDate, Integer> am = new HashMap<>();
     Map<LocalDate, Integer> pm = new HashMap<>();
     List<Object> times = (List<Object>) hourly.get("time");
     List<Object> codes = (List<Object>) hourly.get("weather_code");
     for (int i = 0; times != null && i < times.size(); i++) {
         Number c = num(codes, i);
         if (c == null) continue;
         String t = String.valueOf(times.get(i));             // "2026-10-03T09:00"
         LocalDate d = LocalDate.parse(t.substring(0, 10));
         int hour = Integer.parseInt(t.substring(11, 13));
         if (hour >= AM_FROM && hour <= AM_TO) am.merge(d, c.intValue(), WeatherService::worse);
         if (hour >= PM_FROM && hour <= PM_TO) pm.merge(d, c.intValue(), WeatherService::worse);
     }
     Map<LocalDate, WeatherResponse.Spot> days = new HashMap<>();
     List<Object> dTime = (List<Object>) daily.get("time");
     List<Object> max = (List<Object>) daily.get("temperature_2m_max");
     List<Object> min = (List<Object>) daily.get("temperature_2m_min");
     for (int i = 0; dTime != null && i < dTime.size(); i++) {
         LocalDate d = LocalDate.parse(String.valueOf(dTime.get(i)));
         if (!am.containsKey(d) && !pm.containsKey(d)) continue;
         days.put(d, new WeatherResponse.Spot(d, am.get(d), pm.get(d), dbl(min, i), dbl(max, i)));
     }
     return days;
 }

 /** 더 궂은 날씨 코드 (뇌우 > 눈 > 비·소나기 > 이슬비 > 안개 > 흐림 > 구름 > 맑음) */
 static int worse(int a, int b) {
     return severity(a) >= severity(b) ? a : b;
 }

 static int severity(int c) {
     if (c >= 95) return 8;
     if ((c >= 71 && c <= 77) || c == 85 || c == 86) return 7;
     if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return 6;
     if (c >= 51 && c <= 57) return 5;
     if (c == 45 || c == 48) return 4;
     return Math.min(c, 3);   // 0 맑음, 1·2 구름 조금, 3 흐림
 }

 private static double snap(double v) {
     return Math.round(Math.round(v / GRID) * GRID * 100) / 100.0;   // 0.05° 격자, 소수 둘째 자리
 }

 private static String key(double lat, double lng) {
     return String.format(Locale.ROOT, "%.2f,%.2f", snap(lat), snap(lng));
 }

 private static List<WeatherResponse.Spot> nulls(int n) {
     List<WeatherResponse.Spot> list = new ArrayList<>(n);
     for (int i = 0; i < n; i++) list.add(null);
     return list;
 }

 private static Number num(List<Object> list, int i) {
     return list != null && i < list.size() && list.get(i) instanceof Number n ? n : null;
 }

 private static Double dbl(List<Object> list, int i) {
     Number n = num(list, i);
     return n == null ? null : Math.round(n.doubleValue() * 10) / 10.0;
 }
}