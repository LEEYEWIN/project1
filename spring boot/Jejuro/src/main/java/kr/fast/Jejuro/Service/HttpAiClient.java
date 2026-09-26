package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.RequestDTO.AiRequest;

/**
* 실제 AI 서버(Python FastAPI, CatBoost 18-slot 모델) 호출.
*
* 요청: POST {ai.base-url}/recommend
* {
*   "gender_code": 2, "age_group_code": 3, "income_code": 4,
*   "TRAVEL_MISSION_PRIORITY_WEB": 3,          ← 테마 1순위
*   "TRAVEL_STYL_1": 6, "TRAVEL_STYL_3": 5, "TRAVEL_STYL_5": 2,
*   "TRAVEL_STYL_6": 4, "TRAVEL_STYL_7": 6, "TRAVEL_STYL_8": 7,
*   "TRAVEL_MOTIVE_1": 2,                      ← 여행 동기 1순위
*   "region_mode": "SELECTED", "regions": ["EAST"],
*   "companions": [ { "relation_code": 3, "gender_code": 2, "age_group_code": 6 } ],
*   "top_n": 20
* }
* 응답: { "recommendations": [ { "rank": 1, "place_name": "우도올레보트", "region": "EAST", "score": 4.87, ... } ] }
*
* 반환값은 place_name 목록(추천 순서). RecommendService가 POI_SOURCE_MAP.source_poi_id로 우리 POI와 연결한다.
* JSON 라이브러리 클래스를 직접 쓰지 않고 Map으로 주고받는다(스프링부트 Jackson 2/3 차이와 무관, 응답에 칸이 늘어도 안전).
*/
@Component
@ConditionalOnProperty(name = "ai.mode", havingValue = "http")
public class HttpAiClient implements AiClient {

 private static final Logger log = LoggerFactory.getLogger(HttpAiClient.class);

 private final RestClient restClient;

 public HttpAiClient(@Value("${ai.base-url}") String baseUrl,
                     @Value("${ai.timeout-seconds:30}") int timeoutSeconds) {
     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     factory.setConnectTimeout(Duration.ofSeconds(5));
     factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
     this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
 }

 @Override
 public List<String> recommend(AiRequest request) {
     Map<String, Object> body = toFastApiBody(request);
     try {
         Map<?, ?> res = restClient.post()
                 .uri("/recommend")
                 .contentType(MediaType.APPLICATION_JSON)
                 .body(body)
                 .retrieve()
                 .body(Map.class);
         return placeNames(res);
     } catch (RestClientResponseException e) {
         // FastAPI가 400/422 등으로 거절 → 원인을 콘솔에 남기고 화면에는 짧게
         log.warn("AI 추천 실패 status={} body={} request={}", e.getStatusCode().value(),
                 e.getResponseBodyAsString(), body);
         throw new ApiException(HttpStatus.BAD_GATEWAY,
                 "AI 추천 서버가 요청을 처리하지 못했습니다. (" + e.getStatusCode().value() + ")");
     } catch (RestClientException e) {
         log.warn("AI 추천 서버 연결 실패: {}", e.getMessage());
         throw new ApiException(HttpStatus.BAD_GATEWAY,
                 "AI 추천 서버에 연결할 수 없습니다. FastAPI가 켜져 있는지 확인하세요.");
     }
 }

 /**
  * DB 문항 → 모델 피처 이름. 문항 번호와 피처 번호의 순서가 다르므로 주의.
  *   101 자연↔도시      → TRAVEL_STYL_1
  *   102 새로운↔익숙한   → TRAVEL_STYL_3
  *   103 숨은↔유명      → TRAVEL_STYL_6
  *   104 휴식↔체험      → TRAVEL_STYL_5
  *   105 사진 중요도     → TRAVEL_STYL_8
  *   106 계획↔상황      → TRAVEL_STYL_7
  */
 private Map<String, Object> toFastApiBody(AiRequest r) {
     Map<String, Object> m = new LinkedHashMap<>();
     m.put("gender_code", r.genderCode());
     m.put("age_group_code", r.ageGroupCode());      // 1~8 그대로. 20~60대 밖은 FastAPI가 연령 미반영 처리
     m.put("income_code", r.incomeCode());
     m.put("TRAVEL_MISSION_PRIORITY_WEB", r.userMission1());
     m.put("TRAVEL_STYL_1", r.styleNatureCity());
     m.put("TRAVEL_STYL_3", r.styleNewFamiliar());
     m.put("TRAVEL_STYL_5", r.styleRelaxActivity());
     m.put("TRAVEL_STYL_6", r.styleHiddenFamous());
     m.put("TRAVEL_STYL_7", r.stylePlanFree());
     m.put("TRAVEL_STYL_8", r.photoImportance());
     m.put("TRAVEL_MOTIVE_1", r.travelMotive1());
     m.put("region_mode", r.regionMode());
     m.put("regions", r.regionCodes());
     List<Map<String, Object>> companions = new ArrayList<>();
     for (AiRequest.CompanionInput c : r.companions()) {
         Map<String, Object> cm = new LinkedHashMap<>();
         cm.put("relation_code", c.relationCode());
         cm.put("gender_code", c.genderCode());
         cm.put("age_group_code", c.ageGroupCode());
         companions.add(cm);
     }
     m.put("companions", companions);
     m.put("top_n", r.limit());
     return m;
 }

 /** { "recommendations": [ { "place_name": ... }, ... ] } → place_name 목록 (응답 순서 유지) */
 private List<String> placeNames(Map<?, ?> res) {
     if (res == null || !(res.get("recommendations") instanceof List<?> list)) {
         return List.of();
     }
     List<String> names = new ArrayList<>();
     for (Object item : list) {
         if (item instanceof Map<?, ?> rec && rec.get("place_name") != null) {
             names.add(rec.get("place_name").toString().trim());
         }
     }
     return names;
 }
}