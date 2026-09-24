package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.util.List;
import kr.fast.Jejuro.RequestDTO.AiRequest;

/**
* AI 추천 호출 규격. 반환값은 원본 관광지 ID(source_poi_id) 목록이며 추천 순위 순서다.
* 구현체 두 개 중 application.properties의 ai.mode 값으로 하나가 선택된다.
*   - mock : MockAiClient (AI 서버 없이 개발)
*   - http : HttpAiClient (실제 AI 서버 호출)
*/
public interface AiClient {
 List<String> recommend(AiRequest request);
}