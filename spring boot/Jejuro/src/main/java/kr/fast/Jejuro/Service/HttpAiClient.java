package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.RequestDTO.AiRequest;

/**
* 실제 AI 서버(예: Python FastAPI) 호출.
* 요청:  POST {ai.base-url}/recommend   본문 = AiRequest(JSON)
* 응답:  { "poiIds": ["POI_3821", "POI_0042", ...] }   ← AI 팀과 이 규격을 맞춘다
*/
@Component
@ConditionalOnProperty(name = "ai.mode", havingValue = "http")
public class HttpAiClient implements AiClient {

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
     try {
         AiResponse res = restClient.post()
                 .uri("/recommend")
                 .contentType(MediaType.APPLICATION_JSON)
                 .body(request)
                 .retrieve()
                 .body(AiResponse.class);
         return res == null || res.poiIds() == null ? List.of() : res.poiIds();
     } catch (RestClientException e) {
         throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                 "AI 추천 서버에 연결할 수 없습니다. 잠시 후 다시 시도하세요.");
     }
 }

 record AiResponse(List<String> poiIds) {
 }
}