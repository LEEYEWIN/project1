package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import kr.fast.Jejuro.Repository.PoiSourceMapRepository;
import kr.fast.Jejuro.RequestDTO.AiRequest;

/** AI 서버가 준비되기 전에 쓰는 가짜 AI: 선택 권역 안의 관광지를 무작위로 돌려준다. */
@Component
@ConditionalOnProperty(name = "ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {

 private final PoiSourceMapRepository sourceMapRepository;

 public MockAiClient(PoiSourceMapRepository sourceMapRepository) {
     this.sourceMapRepository = sourceMapRepository;
 }

 @Override
 public List<String> recommend(AiRequest request) {
     try {
         Thread.sleep(1500); // 로딩 화면을 확인할 수 있게 일부러 1.5초 기다린다
     } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
     }
     if (request.regionIds().isEmpty()) {
         return sourceMapRepository.findRandomSourceIds(request.limit());
     }
     return sourceMapRepository.findRandomSourceIdsInRegions(request.regionIds(), request.limit());
 }
}