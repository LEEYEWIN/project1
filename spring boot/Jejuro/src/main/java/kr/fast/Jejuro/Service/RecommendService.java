package kr.fast.Jejuro.Service;



//[2페이지 AI 추천 중]

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import kr.fast.Jejuro.Config.ApiException;

import kr.fast.Jejuro.Entity.PoiSourceMap;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.PoiSourceMapRepository;
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

 /**
  * 화면에 보여 줄 추천 개수 = AI에 요청하는 개수.
  * 관심없음·숨김 관광지는 AI 서버가 점수 예측 후 빼고 상위 10개를 고르므로, 관심없음이 많아도 10곳이 유지된다.
  */
 private static final int SHOW_COUNT = 10;

 private final TravelAccessService travelAccessService;
 private final TravelRegionRepository travelRegionRepository;
 private final AiTravelInputRepository aiInputRepository;
 private final AiClient aiClient;
 private final PoiSourceMapRepository sourceMapRepository;
 private final PoiService poiService;
 private final RegionRepository regionRepository;
 private final CompanionRepository companionRepository;
 private final RecommendLogService recommendLogService;
 private final PoiRepository poiRepository;
 private final DislikeService dislikeService;
 /** 지금 쓰는 AI 모델 버전 → RECOMMEND_REQUEST.model_version (학습 데이터의 model_version 칼럼) */
 private final String modelVersion;

 public RecommendService(TravelAccessService travelAccessService, TravelRegionRepository travelRegionRepository,
                         AiTravelInputRepository aiInputRepository, AiClient aiClient,
                         PoiSourceMapRepository sourceMapRepository, PoiService poiService,
                         RegionRepository regionRepository, CompanionRepository companionRepository,
                         RecommendLogService recommendLogService, PoiRepository poiRepository,
                         DislikeService dislikeService,
                         @Value("${ai.model-version:v1.0}") String modelVersion) {
     this.travelAccessService = travelAccessService;
     this.travelRegionRepository = travelRegionRepository;
     this.aiInputRepository = aiInputRepository;
     this.aiClient = aiClient;
     this.sourceMapRepository = sourceMapRepository;
     this.poiService = poiService;
     this.regionRepository = regionRepository;
     this.companionRepository = companionRepository;
     this.recommendLogService = recommendLogService;
     this.poiRepository = poiRepository;
     this.dislikeService = dislikeService;
     this.modelVersion = modelVersion;
 }

 /**
  * 1) 내 여행인지 확인  2) VIEW에서 AI 입력 조회 + 설문 완료 확인
  * 3) 권역 코드·동반자 목록을 붙여 AI 호출 → 원본 ID 목록(FastAPI는 place_name)
  * 4) 원본 ID → poi_id 변환(POI_SOURCE_MAP)  5) POI 정보 붙여서 반환
  * 추천 결과는 RECOMMEND_REQUEST/ITEM에 남고(관리자 KPI·재학습), 화면도 이 기록으로 다시 연다.
  * 추천은 여행당 한 번: 이미 성공한 추천이 있으면 AI를 다시 부르지 않고 그 결과를 돌려준다.
  */
 @Transactional(readOnly = true)
 public RecommendResponse recommend(Long travelId, Long userId) {
     Travel travel = travelAccessService.getOwned(travelId, userId);
     Optional<RecommendResponse> saved = savedResult(travelId);
     if (saved.isPresent()) {
         return saved.get();
     }
     if (travel.isImported()) {
         throw new ApiException(HttpStatus.CONFLICT, "커뮤니티에서 가져온 여행은 설문이 없어 AI 추천을 받을 수 없어요. 관광지 목록에서 장소를 추가해 주세요.");
     }

     AiTravelInput input = aiInputRepository.find(travelId, userId)
             .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
     if (!input.surveyComplete()) {
         throw new ApiException(HttpStatus.CONFLICT, "설문이 완료되지 않았습니다. 설문을 먼저 저장하세요.");
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

     // 추천에서 뺄 장소(관심없음 + 숨김·삭제·추천 대상 아님)의 AI용 이름 → AI 서버가 점수 예측 후 제외
     List<String> excluded = sourceMapRepository.findExcludedSourceIds(userId);

     AiRequest request = AiRequest.of(input, travel.getRegionMode().name(), regionCodes, regionIds,
             companions, excluded, SHOW_COUNT);

     // 결과 = 원본 ID 목록(FastAPI는 place_name). POI_SOURCE_MAP.source_poi_id로 우리 관광지와 연결
     long started = System.currentTimeMillis();
     List<String> sourceIds;
     try {
         sourceIds = aiClient.recommend(request);
     } catch (RuntimeException e) {
         try {
             recommendLogService.fail(travelId, modelVersion, System.currentTimeMillis() - started, e.getMessage());
         } catch (RuntimeException logError) {
             log.warn("추천 실패 기록 저장 실패: {}", logError.getMessage());
         }
         throw e; // 화면에는 원래 AI 오류를 그대로
     }
     long elapsed = System.currentTimeMillis() - started;

     // 원본 ID → poi_id (AI가 준 순서 유지, 매핑 없는 ID는 버림, 중복 제거)
     // 제외 대상은 AI 서버에서 이미 빠졌다. 아래 필터는 요청과 응답 사이에 바뀐 경우를 막는 안전장치.
     Map<String, Long> idMap = sourceMapRepository.findBySourcePoiIdIn(sourceIds).stream()
             .collect(Collectors.toMap(PoiSourceMap::getSourcePoiId, PoiSourceMap::getPoiId, (a, b) -> a));
     Set<Long> hidden = idMap.isEmpty() ? new HashSet<>() : new HashSet<>(poiRepository.findNotRecommendableIds(new HashSet<>(idMap.values())));
     hidden.addAll(dislikeService.ids(userId));
     List<Long> poiIds = sourceIds.stream()
             .map(idMap::get)
             .filter(id -> id != null && !hidden.contains(id))
             .collect(Collectors.toCollection(LinkedHashSet::new))
             .stream()
             .limit(SHOW_COUNT) // AI 순위대로 10개만
             .toList();

     // AI는 장소를 돌려줬는데 POI_SOURCE_MAP으로 하나도 연결되지 않음 → 정상적인 "추천 0개"가 아니라 매핑 장애
     // (연결은 됐지만 숨김·관심없음으로 모두 빠진 경우는 정상 결과로 본다)
     if (!sourceIds.isEmpty() && idMap.isEmpty()) {
         String reason = "AI 추천 장소를 DB 관광지와 연결하지 못함 (" + sourceIds.size() + "곳)";
         try {
             recommendLogService.fail(travelId, modelVersion, elapsed, reason);
         } catch (RuntimeException logError) {
             log.warn("추천 실패 기록 저장 실패: {}", logError.getMessage());
         }
         log.warn("{} ids={}", reason, sourceIds);
         throw new ApiException(HttpStatus.BAD_GATEWAY, "추천 결과를 관광지 정보와 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
     }

     // 관리자 KPI·재학습용 기록 (실패해도 추천 결과에는 영향 없음)
     try {
         recommendLogService.success(travelId, modelVersion, elapsed, sourceIds, idMap, new HashSet<>(poiIds));
     } catch (RuntimeException logError) {
         log.warn("추천 기록 저장 실패(추천 결과에는 영향 없음): {}", logError.getMessage());
     }

     return new RecommendResponse(travelId, poiService.findSummaries(poiIds));
 }

 /**
  * 이미 받은 추천 목록 (새로고침·다른 탭·다른 기기에서 다시 열 때). 추천을 받은 적 없으면 404.
  * 그 사이 관리자가 숨기거나 삭제한 관광지는 뺀다.
  */
 @Transactional(readOnly = true)
 public RecommendResponse latest(Long travelId, Long userId) {
     travelAccessService.getOwned(travelId, userId);
     return savedResult(travelId)
             .orElseThrow(() -> ApiException.notFound("아직 AI 추천을 받지 않은 여행입니다."));
 }

 private Optional<RecommendResponse> savedResult(Long travelId) {
     return recommendLogService.latestShown(travelId).map(ids -> {
         Set<Long> hidden = ids.isEmpty() ? Set.of() : new HashSet<>(poiRepository.findHiddenIds(ids));
         List<Long> visible = ids.stream().filter(id -> !hidden.contains(id)).toList();
         return new RecommendResponse(travelId, poiService.findSummaries(visible));
     });
 }
}
