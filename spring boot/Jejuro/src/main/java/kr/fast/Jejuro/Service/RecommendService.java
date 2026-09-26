package kr.fast.Jejuro.Service;


//[2페이지 AI 추천 중]

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;

import kr.fast.Jejuro.Entity.PoiSourceMap;
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

 private static final int LIMIT = 20;

 private final TravelAccessService travelAccessService;
 private final TravelRegionRepository travelRegionRepository;
 private final AiTravelInputRepository aiInputRepository;
 private final AiClient aiClient;
 private final PoiSourceMapRepository sourceMapRepository;
 private final PoiService poiService;
 private final RegionRepository regionRepository;
 private final CompanionRepository companionRepository;

 public RecommendService(TravelAccessService travelAccessService, TravelRegionRepository travelRegionRepository,
                         AiTravelInputRepository aiInputRepository, AiClient aiClient,
                         PoiSourceMapRepository sourceMapRepository, PoiService poiService,
                         RegionRepository regionRepository, CompanionRepository companionRepository) {
     this.travelAccessService = travelAccessService;
     this.travelRegionRepository = travelRegionRepository;
     this.aiInputRepository = aiInputRepository;
     this.aiClient = aiClient;
     this.sourceMapRepository = sourceMapRepository;
     this.poiService = poiService;
     this.regionRepository = regionRepository;
     this.companionRepository = companionRepository;
 }

 /**
  * 1) 내 여행인지 확인  2) VIEW에서 AI 입력 조회 + 설문 완료 확인
  * 3) 권역 코드·동반자 목록을 붙여 AI 호출 → 원본 ID 목록(FastAPI는 place_name)
  * 4) 원본 ID → poi_id 변환(POI_SOURCE_MAP)  5) POI 정보 붙여서 반환
  * 추천 결과는 DB에 저장하지 않는다(설계 결정). 화면이 sessionStorage에 보관한다.
  */
 @Transactional(readOnly = true)
 public RecommendResponse recommend(Long travelId, Long userId) {
     Travel travel = travelAccessService.getOwned(travelId, userId);

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
             companions, LIMIT);

     // 결과 = 원본 ID 목록(FastAPI는 place_name). POI_SOURCE_MAP.source_poi_id로 우리 관광지와 연결
     List<String> sourceIds = aiClient.recommend(request);

     // 원본 ID → poi_id (AI가 준 순서 유지, 매핑 없는 ID는 버림, 중복 제거)
     Map<String, Long> idMap = sourceMapRepository.findBySourcePoiIdIn(sourceIds).stream()
             .collect(Collectors.toMap(PoiSourceMap::getSourcePoiId, PoiSourceMap::getPoiId));
     List<Long> poiIds = sourceIds.stream()
             .map(idMap::get)
             .filter(id -> id != null)
             .collect(Collectors.toCollection(LinkedHashSet::new))
             .stream().toList();

     return new RecommendResponse(travelId, poiService.findSummaries(poiIds));
 }
}