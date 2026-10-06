package kr.fast.Jejuro.Service;



//[7페이지 내 여행 (목록·달력·상세) · 3페이지 추천 기준 안내]

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Entity.Code;
import kr.fast.Jejuro.Entity.Companion;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.Preference;
import kr.fast.Jejuro.Entity.PreferenceOption;
import kr.fast.Jejuro.Entity.Region;
import kr.fast.Jejuro.Entity.ResponseType;
import kr.fast.Jejuro.Entity.RouteDay;
import kr.fast.Jejuro.Entity.RouteSpot;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelBookmark;
import kr.fast.Jejuro.Entity.TravelFeedback;
import kr.fast.Jejuro.Entity.TravelPreference;
import kr.fast.Jejuro.Entity.TravelRegion;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.Repository.CodeRepository;
import kr.fast.Jejuro.Repository.CompanionRepository;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.PreferenceOptionRepository;
import kr.fast.Jejuro.Repository.PreferenceRepository;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Repository.RouteDayRepository;
import kr.fast.Jejuro.Repository.RouteSpotRepository;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;
import kr.fast.Jejuro.Repository.TravelFeedbackRepository;
import kr.fast.Jejuro.Repository.TravelPreferenceRepository;
import kr.fast.Jejuro.Repository.TravelRegionRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Repository.TravelRouteRepository;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse.CompanionItem;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse.SurveyItem;
import kr.fast.Jejuro.ResponseDTO.TravelSummaryResponse;
import kr.fast.Jejuro.ResponseDTO.TravelSummaryResponse.DayPlan;

/** 7페이지 조회 전용: 내 여행 목록(달력 포함), 여행 상세 */
@Service
@Transactional(readOnly = true)
public class TravelQueryService {

private final TravelRepository travelRepository;
private final TravelAccessService travelAccessService;
private final TravelRegionRepository travelRegionRepository;
private final CompanionRepository companionRepository;
private final TravelRouteRepository routeRepository;
private final RouteDayRepository dayRepository;
private final RouteSpotRepository spotRepository;
private final TravelBookmarkRepository bookmarkRepository;
private final PoiRepository poiRepository;
private final TravelFeedbackRepository feedbackRepository;
private final TravelPreferenceRepository travelPreferenceRepository;
private final PreferenceRepository preferenceRepository;
private final PreferenceOptionRepository optionRepository;
private final RegionRepository regionRepository;
private final CodeRepository codeRepository;
private final RouteService routeService;
private final FeedbackService feedbackService;

public TravelQueryService(TravelRepository travelRepository, TravelAccessService travelAccessService,
                         TravelRegionRepository travelRegionRepository, CompanionRepository companionRepository,
                         TravelRouteRepository routeRepository, RouteDayRepository dayRepository,
                         RouteSpotRepository spotRepository, TravelBookmarkRepository bookmarkRepository,
                         PoiRepository poiRepository, TravelFeedbackRepository feedbackRepository,
                         TravelPreferenceRepository travelPreferenceRepository,
                         PreferenceRepository preferenceRepository, PreferenceOptionRepository optionRepository,
                         RegionRepository regionRepository, CodeRepository codeRepository,
                         RouteService routeService, FeedbackService feedbackService) {
   this.travelRepository = travelRepository;
   this.travelAccessService = travelAccessService;
   this.travelRegionRepository = travelRegionRepository;
   this.companionRepository = companionRepository;
   this.routeRepository = routeRepository;
   this.dayRepository = dayRepository;
   this.spotRepository = spotRepository;
   this.bookmarkRepository = bookmarkRepository;
   this.poiRepository = poiRepository;
   this.feedbackRepository = feedbackRepository;
   this.travelPreferenceRepository = travelPreferenceRepository;
   this.preferenceRepository = preferenceRepository;
   this.optionRepository = optionRepository;
   this.regionRepository = regionRepository;
   this.codeRepository = codeRepository;
   this.routeService = routeService;
   this.feedbackService = feedbackService;
}

/**
* 내 여행 목록 + 달력용 일차별 일정.
* 여행마다 쿼리를 날리지 않고 IN 조회로 한 번에 모은다.
*/
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
   Map<Long, Set<Long>> placesByTravel = bookmarkRepository.findByTravelIdIn(ids).stream()
           .collect(Collectors.groupingBy(TravelBookmark::getTravelId,
                   Collectors.mapping(TravelBookmark::getPoiId, Collectors.toSet())));
   Set<Long> withFeedback = feedbackRepository.findByTravelIdIn(ids).stream()
           .map(TravelFeedback::getTravelId).collect(Collectors.toSet());

   // 여행당 경로 1개 (예전 데이터에 여러 개면 최신 것)
   Map<Long, TravelRoute> routeByTravel = routeRepository.findByTravelIdIn(ids).stream()
           .collect(Collectors.toMap(TravelRoute::getTravelId, Function.identity(),
                   (a, b) -> a.getRouteId() > b.getRouteId() ? a : b));
   Map<Long, List<DayPlanRow>> plans = dayPlans(routeByTravel.values().stream().map(TravelRoute::getRouteId).toList());

   return travels.stream()
           .map(t -> {
               Long tid = t.getTravelId();
               TravelRoute route = routeByTravel.get(tid);
               List<DayPlanRow> rows = route == null ? List.of() : plans.getOrDefault(route.getRouteId(), List.of());
               List<DayPlan> days = rows.stream()
                       .map(r -> new DayPlan(r.dayNo(), t.dateOf(r.dayNo()), r.spots()))
                       .toList();
               // 배치 수 = 경로에 들어 있는 "여행 장소" 수 (확정 가능 여부와 같은 기준)
               Set<Long> places = placesByTravel.getOrDefault(tid, Set.of());
               int spotCount = (int) rows.stream().flatMap(r -> r.poiIds().stream())
                       .filter(places::contains).distinct().count();
               return new TravelSummaryResponse(tid, t.getTravelNo(), t.getTravelName(),
                       t.getStartDate(), t.getEndDate(), t.tripDays(), phase(t),
                       regionsByTravel.getOrDefault(tid, List.of("제주 전체")),
                       companionCount.getOrDefault(tid, 0L).intValue(),
                       route == null ? 0 : 1,
                       t.getAdoptedRouteId(), withFeedback.contains(tid), t.isImported(),
                       places.size(), spotCount, days);
           })
           .toList();
}

private record DayPlanRow(int dayNo, List<Long> poiIds, List<String> spots) {
}

/** 경로 번호 → 일차별 방문지 이름 (방문 순서대로) */
private Map<Long, List<DayPlanRow>> dayPlans(List<Long> routeIds) {
   if (routeIds.isEmpty()) return Map.of();
   List<RouteDay> days = dayRepository.findByRouteIdIn(routeIds);
   if (days.isEmpty()) return Map.of();
   List<RouteSpot> spots = spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(
           days.stream().map(RouteDay::getRouteDayId).toList());
   Map<Long, String> poiNames = poiRepository.findAllById(spots.stream().map(RouteSpot::getPoiId).distinct().toList())
           .stream().collect(Collectors.toMap(Poi::getPoiId, Poi::displayName));
   Map<Long, List<Long>> idsByDay = spots.stream()
           .collect(Collectors.groupingBy(RouteSpot::getRouteDayId,
                   Collectors.mapping(RouteSpot::getPoiId, Collectors.toList())));
   return days.stream()
           .sorted(Comparator.comparing(RouteDay::getDayNo))
           .collect(Collectors.groupingBy(RouteDay::getRouteId,
                   Collectors.mapping(d -> {
                       List<Long> poiIds = idsByDay.getOrDefault(d.getRouteDayId(), List.of());
                       return new DayPlanRow(d.getDayNo(), poiIds,
                               poiIds.stream().map(id -> poiNames.getOrDefault(id, "관광지")).toList());
                   }, Collectors.toList())));
}

/** 여행 상세: 기본정보 + 동반자 + 설문 요약 + 경로(1개)·배치 현황 + 확정 일정 + 후기 */
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
   RouteService.Placement placement = routeService.placement(travelId);
   var feedback = feedbackService.find(travelId, userId).orElse(null);

   // 기간이 겹치는 내 다른 여행 ("날짜·동행 바꿔 다시 만들기"로만 생김: 번호가 큰 쪽이 새 여행)
   List<TravelDetailResponse.OverlapItem> overlaps = routeService.overlapping(t).stream()
           .map(o -> new TravelDetailResponse.OverlapItem(o.getTravelId(), o.getTravelName(), o.getStartDate(),
                   o.getEndDate(), o.getAdoptedRouteId() != null, o.getTravelId() > t.getTravelId()))
           .toList();
   boolean replacing = overlaps.stream().anyMatch(TravelDetailResponse.OverlapItem::newer);
   boolean canReplace = !t.isImported() && feedback == null && !LocalDate.now().isAfter(t.getEndDate()) && !replacing;

   return new TravelDetailResponse(t.getTravelId(), t.getTravelNo(), t.getTravelName(),
           t.getStartDate(), t.getEndDate(), t.tripDays(), phase(t),
           regions.isEmpty() ? List.of("제주 전체") : regions,
           companions, t.isImported(), t.getSourcePostId(), survey(travelId),
           routeService.summary(travelId, userId),
           placement.placeCount(), placement.placedCount(),
           adopted, feedback, canWriteFeedback,
           routeService.isEditLocked(t), canReplace, overlaps);
}

/** 설문 답변 요약: 질문 이름 + 고른 선택지 이름(순위 순) */
private List<SurveyItem> survey(Long travelId) {
   List<TravelPreference> answers = travelPreferenceRepository.findByTravelIdOrderByPreferenceIdAscAnswerRankAsc(travelId);
   if (answers.isEmpty()) return List.of();
   Map<Long, Preference> questions = preferenceRepository.findAll().stream()
           .collect(Collectors.toMap(Preference::getPreferenceId, Function.identity()));
   Map<String, String> optionNames = optionRepository.findAllByOrderByPreferenceIdAscOptionValueAsc().stream()
           .collect(Collectors.toMap(o -> o.getPreferenceId() + ":" + o.getOptionValue(), PreferenceOption::getOptionName));

   Map<Long, List<TravelPreference>> byQuestion = answers.stream()
           .collect(Collectors.groupingBy(TravelPreference::getPreferenceId, java.util.TreeMap::new, Collectors.toList()));
   List<SurveyItem> items = new ArrayList<>();
   byQuestion.forEach((pid, list) -> {
       Preference q = questions.get(pid);
       if (q == null) return;
       List<String> names = list.stream()
               .map(a -> optionNames.getOrDefault(pid + ":" + a.getAnswerValue(), String.valueOf(a.getAnswerValue())))
               .toList();
       items.add(new SurveyItem(pid, q.getPreferenceName(), names, q.getResponseType() == ResponseType.MULTI_SELECT));
   });
   return items;
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