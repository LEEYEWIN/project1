package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;

import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.ResponseDTO.RecommendResponse;
import kr.fast.Jejuro.Entity.RegionMode;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelRegion;
import kr.fast.Jejuro.Repository.TravelRegionRepository;
import kr.fast.Jejuro.Entity.AiTravelInput;
import kr.fast.Jejuro.Repository.AiTravelInputRepository;
import kr.fast.Jejuro.RequestDTO.AiRequest;
import kr.fast.Jejuro.RequestDTO.AiRequest.CompanionInput;
import kr.fast.Jejuro.Entity.Region;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Repository.CompanionRepository;

@Service
public class RecommendService {

 private static final Logger log = LoggerFactory.getLogger(RecommendService.class);

 /** 화면에 보여 줄 추천 개수 */
 private static final int SHOW_COUNT = 10;
 /** AI에 요청하는 개수: 우리 DB에 없는 장소가 빠질 것을 대비해 조금 더 받는다 */
 private static final int REQUEST_COUNT = 15;

 private final TravelAccessService travelAccessService;
 private final TravelRegionRepository travelRegionRepository;
 private final AiTravelInputRepository aiInputRepository;
 private final AiClient aiClient;
 private final PoiRepository poiRepository;
 private final PoiService poiService;
 private final RegionRepository regionRepository;
 private final CompanionRepository companionRepository;
 private final RecommendLogService recommendLogService;
 /** 지금 쓰는 AI 모델 버전 (AI_MODEL_VERSION 표와 같게) */
 private final String modelVersion;

 public RecommendService(TravelAccessService travelAccessService, TravelRegionRepository travelRegionRepository,
                         AiTravelInputRepository aiInputRepository, AiClient aiClient,
                         PoiRepository poiRepository, PoiService poiService,
                         RegionRepository regionRepository, CompanionRepository companionRepository,
                         RecommendLogService recommendLogService,
                         @Value("${ai.model-version:v1.0}") String modelVersion) {
     this.travelAccessService = travelAccessService;
     this.travelRegionRepository = travelRegionRepository;
     this.aiInputRepository = aiInputRepository;
     this.aiClient = aiClient;
     this.poiRepository = poiRepository;
     this.poiService = poiService;
     this.regionRepository = regionRepository;
     this.companionRepository = companionRepository;
     this.recommendLogService = recommendLogService;
     this.modelVersion = modelVersion;
 }

 /**
  * 1) 내 여행인지 확인  2) VIEW에서 AI 입력 조회 + 설문 완료 확인
  * 3) 권역 코드·동반자 목록을 붙여 AI 호출 → 여행지 이름 목록(place_name)
  * 4) POI.poi_name과 같은 이름으로 조회  5) POI 정보 붙여서 반환
  * 화면용 추천 결과는 sessionStorage에 보관하고, 관리자 KPI·재학습용 기록만 RECOMMEND_REQUEST/ITEM에 남긴다.
  */
 @Transactional(readOnly = true)
 public RecommendResponse recommend(Long travelId, Long userId) {
     Travel travel = travelAccessService.getOwned(travelId, userId);
     if (travel.isImported()) {
         throw ApiException.badRequest("커뮤니티에서 가져온 여행은 설문이 없어 AI 추천을 받을 수 없어요. 관광지 목록에서 장소를 추가해 주세요.");
     }

     AiTravelInput input = aiInputRepository.find(travelId, userId)
             .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
     if (!input.surveyComplete()) {
         throw ApiException.badRequest("설문이 완료되지 않았습니다. 설문을 먼저 저장하세요.");
     }

     List<Integer> regionIds = travel.getRegionMode() == RegionMode.SELECTED
             ? travelRegionRepository.findByTravelId(travelId).stream().map(TravelRegion::getRegionId).toList()
             : List.of();

     // 권역 코드(EAST/WEST/SOUTH/NORTH): FastAPI는 코드 문자열로 받는다
     List<String> regionCodes = regionRepository.findAllById(regionIds).stream()
             .map(Region::getRegionCode).toList();

     // 동반자: DB 코드 그대로 (정렬·18-slot 변환은 AI 서버)
     List<CompanionInput> companions = companionRepository.findByTravelIdOrderByCompanionSeq(travelId).stream()
             .map(c -> new CompanionInput(c.getRelationCode(), c.getGenderCode(), c.getAgeGroupCode()))
             .toList();

     AiRequest request = AiRequest.of(input, travel.getRegionMode().name(), regionCodes, regionIds,
             companions, REQUEST_COUNT);

     // FastAPI가 반환한 여행지 이름을 DB 이름과 비교한다.
     long started = System.currentTimeMillis();
     List<String> placeNames;
     try {
         placeNames = aiClient.recommend(request);
     } catch (RuntimeException e) {
         try {
             recommendLogService.fail(travelId, modelVersion, System.currentTimeMillis() - started, e.getMessage());
         } catch (RuntimeException logError) {
             log.warn("추천 실패 기록 저장 실패: {}", logError.getMessage());
         }
         throw e; // 화면에는 원래 AI 오류를 그대로
     }
     long elapsed = System.currentTimeMillis() - started;

     // 이름이 같은 관광지만 선택한다. 동명 장소는 가장 작은 poi_id를 사용한다.
     Map<String, Long> idMap = placeNames.isEmpty() ? Map.of()
             : poiRepository.findByPoiNameInOrderByPoiIdAsc(placeNames).stream()
                     .collect(Collectors.toMap(Poi::getPoiName, Poi::getPoiId, (a, b) -> a));
     List<Long> poiIds = placeNames.stream()
             .map(idMap::get)
             .filter(id -> id != null)
             .collect(Collectors.toCollection(LinkedHashSet::new))
             .stream()
             .limit(SHOW_COUNT) // AI 순위대로 10개만
             .toList();

     // 관리자 KPI·재학습용 기록 (실패해도 추천 결과에는 영향 없음)
     try {
         recommendLogService.success(travelId, modelVersion, elapsed, placeNames, idMap, new HashSet<>(poiIds));
     } catch (RuntimeException logError) {
         log.warn("추천 기록 저장 실패(추천 결과에는 영향 없음): {}", logError.getMessage());
     }

     return new RecommendResponse(travelId, poiService.findSummaries(poiIds));
 }
}