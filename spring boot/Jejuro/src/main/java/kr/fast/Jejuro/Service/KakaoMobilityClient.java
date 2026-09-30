package kr.fast.Jejuro.Service;


//[6페이지 카카오맵 동선]

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.fast.Jejuro.Config.ApiException;

/**
* 카카오모빌리티 "다중 경유지 길찾기"(자동차) 호출.
* - REST API 키는 서버에만 둔다(브라우저에 노출 금지).
* - 좌표는 x = 경도(lng), y = 위도(lat) 순서다. 헷갈리기 쉬우니 주의.
* - 경유지는 최대 30개 → 출발+도착 포함 하루 최대 32곳.
* - 응답은 Map/List로 받는다(스프링부트 버전의 Jackson 2/3 차이와 무관).
*/
@Component
public class KakaoMobilityClient {

 private static final Logger log = LoggerFactory.getLogger(KakaoMobilityClient.class);
 private static final String URL = "https://apis-navi.kakaomobility.com/v1/waypoints/directions";
 public static final int MAX_POINTS = 32;

 private final String restKey;
 private final RestClient restClient;

 public KakaoMobilityClient(@Value("${kakao.mobility.rest-key:}") String restKey) {
     this.restKey = restKey;
     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     factory.setConnectTimeout(Duration.ofSeconds(5));
     factory.setReadTimeout(Duration.ofSeconds(10));
     this.restClient = RestClient.builder().requestFactory(factory).build();
 }

 /** 키가 없으면 서비스가 추정값으로 대신 계산한다(로컬 개발용). */
 public boolean isConfigured() {
     return restKey != null && !restKey.isBlank();
 }

 /** 구간별 거리·시간과 도로 좌표를 돌려준다. sections.size() == points.size() - 1 */
 @SuppressWarnings("unchecked")
 public CarRoute route(List<GeoPoint> points) {
     Map<String, Object> body = new LinkedHashMap<>();
     body.put("origin", xy(points.get(0)));
     body.put("destination", xy(points.get(points.size() - 1)));
     if (points.size() > 2) { // 경유지가 있을 때만 보낸다
         body.put("waypoints", points.subList(1, points.size() - 1).stream().map(this::xy).toList());
     }
     body.put("priority", "RECOMMEND");

     Map<String, Object> res;
     try {
         res = restClient.post()
                 .uri(URL)
                 .header("Authorization", "KakaoAK " + restKey)
                 .contentType(MediaType.APPLICATION_JSON)
                 .body(body)
                 .retrieve()
                 .body(Map.class);
     } catch (RestClientResponseException e) {
         // 카카오가 에러 응답을 준 경우: 상태 코드와 내용을 콘솔에 남긴다 (키 오류 401, 권한 없음 403 등)
         log.warn("카카오 길찾기 실패 {} : {}", e.getStatusCode(), e.getResponseBodyAsString());
         throw new ApiException(HttpStatus.BAD_GATEWAY,
                 "카카오 길찾기 실패(" + e.getStatusCode().value() + ")");
     } catch (RestClientException e) {
         // 네트워크 문제(인터넷 차단, 타임아웃 등)
         log.warn("카카오 길찾기 연결 실패: {}", e.getMessage());
         throw new ApiException(HttpStatus.BAD_GATEWAY, "카카오 길찾기 서버에 연결하지 못했습니다");
     }

     List<Map<String, Object>> routes = list(res == null ? null : res.get("routes"));
     Map<String, Object> route = routes.isEmpty() ? null : routes.get(0);
     if (route == null || num(route.get("result_code"), -1) != 0) {
         String msg = route == null ? "응답 없음" : String.valueOf(route.getOrDefault("result_msg", "길찾기 실패"));
         throw new ApiException(HttpStatus.BAD_GATEWAY, "자동차 경로를 찾지 못했습니다: " + msg);
     }

     List<Section> sections = new ArrayList<>();
     List<double[]> path = new ArrayList<>();
     for (Map<String, Object> s : list(route.get("sections"))) {
         sections.add(new Section(num(s.get("distance"), 0), num(s.get("duration"), 0)));
         for (Map<String, Object> road : list(s.get("roads"))) {
             List<?> v = road.get("vertexes") instanceof List<?> l ? l : List.of();   // [x1, y1, x2, y2, ...]
             for (int i = 0; i + 1 < v.size(); i += 2) {
                 double x = ((Number) v.get(i)).doubleValue();
                 double y = ((Number) v.get(i + 1)).doubleValue();
                 path.add(new double[] { y, x }); // [lat, lng]
             }
         }
     }
     return new CarRoute(sections, path);
 }

 /** JSON 배열(List) 안의 객체(Map)들. 없으면 빈 리스트 */
 @SuppressWarnings("unchecked")
 private static List<Map<String, Object>> list(Object o) {
     return o instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
 }

 private static int num(Object o, int defaultValue) {
     return o instanceof Number n ? n.intValue() : defaultValue;
 }

 private Map<String, Object> xy(GeoPoint p) {
     return Map.of("x", p.lng(), "y", p.lat());
 }

 public record Section(int distanceM, int durationSec) {
 }

 public record CarRoute(List<Section> sections, List<double[]> path) {
 }
}