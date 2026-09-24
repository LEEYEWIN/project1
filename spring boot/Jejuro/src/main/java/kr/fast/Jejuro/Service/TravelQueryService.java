package kr.fast.Jejuro.Service;


//[7페이지 여행 기록]

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Entity.Code;
import kr.fast.Jejuro.Repository.CodeRepository;

import kr.fast.Jejuro.Repository.TravelFeedbackRepository;
import kr.fast.Jejuro.Entity.TravelFeedback;
import kr.fast.Jejuro.Entity.Region;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.Repository.TravelRouteRepository;

import kr.fast.Jejuro.Entity.Companion;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelRegion;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse.CompanionItem;
import kr.fast.Jejuro.ResponseDTO.TravelSummaryResponse;
import kr.fast.Jejuro.Repository.CompanionRepository;
import kr.fast.Jejuro.Repository.TravelRegionRepository;
import kr.fast.Jejuro.Repository.TravelRepository;

/** 7페이지 조회 전용: 내 여행 목록, 여행 상세 */
@Service
@Transactional(readOnly = true)
public class TravelQueryService {

 private final TravelRepository travelRepository;
 private final TravelAccessService travelAccessService;
 private final TravelRegionRepository travelRegionRepository;
 private final CompanionRepository companionRepository;
 private final TravelRouteRepository routeRepository;
 private final TravelFeedbackRepository feedbackRepository;
 private final RegionRepository regionRepository;
 private final CodeRepository codeRepository;
 private final RouteService routeService;
 private final FeedbackService feedbackService;

 public TravelQueryService(TravelRepository travelRepository, TravelAccessService travelAccessService,
                           TravelRegionRepository travelRegionRepository, CompanionRepository companionRepository,
                           TravelRouteRepository routeRepository, TravelFeedbackRepository feedbackRepository,
                           RegionRepository regionRepository, CodeRepository codeRepository,
                           RouteService routeService, FeedbackService feedbackService) {
     this.travelRepository = travelRepository;
     this.travelAccessService = travelAccessService;
     this.travelRegionRepository = travelRegionRepository;
     this.companionRepository = companionRepository;
     this.routeRepository = routeRepository;
     this.feedbackRepository = feedbackRepository;
     this.regionRepository = regionRepository;
     this.codeRepository = codeRepository;
     this.routeService = routeService;
     this.feedbackService = feedbackService;
 }

 /** 내 여행 목록. 여행마다 쿼리를 날리지 않고 IN 조회로 한 번에 모은다. */
 public List<TravelSummaryResponse> list(Long userId) {
     List<Travel> travels = travelRepository.findByUserIdOrderByStartDateDescTravelIdDesc(userId);
     if (travels.isEmpty()) {
         return List.of();
     }
     List<Long> ids = travels.stream().map(Travel::getTravelId).toList();
     Map<Integer, String> regionNames = regionNames();

     Map<Long, List<String>> regionsByTravel = travelRegionRepository.findByTravelIdIn(ids).stream()
             .collect(Collectors.groupingBy(TravelRegion::getTravelId,
                     Collectors.mapping(r -> regionNames.get(r.getRegionId()), Collectors.toList())));
     Map<Long, Long> companionCount = companionRepository.findByTravelIdIn(ids).stream()
             .collect(Collectors.groupingBy(Companion::getTravelId, Collectors.counting()));
     Map<Long, Long> routeCount = routeRepository.findByTravelIdIn(ids).stream()
             .collect(Collectors.groupingBy(TravelRoute::getTravelId, Collectors.counting()));
     Set<Long> withFeedback = feedbackRepository.findByTravelIdIn(ids).stream()
             .map(TravelFeedback::getTravelId).collect(Collectors.toSet());

     return travels.stream()
             .map(t -> new TravelSummaryResponse(t.getTravelId(), t.getTravelNo(), t.getTravelName(),
                     t.getStartDate(), t.getEndDate(), t.tripDays(), phase(t),
                     regionsByTravel.getOrDefault(t.getTravelId(), List.of("제주 전체")),
                     companionCount.getOrDefault(t.getTravelId(), 0L).intValue(),
                     routeCount.getOrDefault(t.getTravelId(), 0L).intValue(),
                     t.getAdoptedRouteId(), withFeedback.contains(t.getTravelId())))
             .toList();
 }

 /** 여행 상세: 기본정보 + 동반자 + 경로 목록 + 채택 경로 일정 + 피드백 */
 public TravelDetailResponse detail(Long travelId, Long userId) {
     Travel t = travelAccessService.getOwned(travelId, userId);
     Map<Integer, String> regionNames = regionNames();

     List<String> regions = travelRegionRepository.findByTravelId(travelId).stream()
             .map(r -> regionNames.get(r.getRegionId())).toList();

     Map<String, String> relation = codeNames("TCR");
     Map<String, String> gender = codeNames("GEN");
     Map<String, String> age = codeNames("AGE");
     List<CompanionItem> companions = companionRepository.findByTravelIdOrderByCompanionSeq(travelId).stream()
             .map(c -> new CompanionItem(c.getCompanionSeq(),
                     relation.get(String.valueOf(c.getRelationCode())),
                     gender.get(String.valueOf(c.getGenderCode())),
                     age.get(String.valueOf(c.getAgeGroupCode()))))
             .toList();

     RouteDetailResponse adopted = t.getAdoptedRouteId() == null ? null
             : routeService.detail(t.getAdoptedRouteId(), userId);
     boolean canWriteFeedback = t.getAdoptedRouteId() != null && !LocalDate.now().isBefore(t.getEndDate());

     return new TravelDetailResponse(t.getTravelId(), t.getTravelNo(), t.getTravelName(),
             t.getStartDate(), t.getEndDate(), t.tripDays(), phase(t),
             regions.isEmpty() ? List.of("제주 전체") : regions,
             companions, routeService.list(travelId, userId), adopted,
             feedbackService.find(travelId, userId).orElse(null), canWriteFeedback);
 }

 private String phase(Travel t) {
     LocalDate today = LocalDate.now();
     if (today.isBefore(t.getStartDate())) return "BEFORE";
     if (today.isAfter(t.getEndDate())) return "AFTER";
     return "DURING";
 }

 private Map<Integer, String> regionNames() {
     return regionRepository.findAll().stream()
             .collect(Collectors.toMap(Region::getRegionId, Region::getRegionName));
 }

 private Map<String, String> codeNames(String group) {
     return codeRepository.findByGroupCodeOrderByCodeId(group).stream()
             .collect(Collectors.toMap(Code::getCodeValue, Code::getCodeName));
 }
}