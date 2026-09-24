package kr.fast.Jejuro.Service;


// [5페이지 루트 짜기 (6·7페이지에서도 사용)]

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Repository.TravelBookmarkRepository;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Repository.PoiRepository;

import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.Entity.RouteDay;
import kr.fast.Jejuro.Entity.RouteSpot;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse.DayResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse.SpotResponse;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest.DayReq;
import kr.fast.Jejuro.ResponseDTO.RouteSummaryResponse;
import kr.fast.Jejuro.Repository.RouteDayRepository;
import kr.fast.Jejuro.Repository.RouteSpotRepository;
import kr.fast.Jejuro.Repository.TravelRouteRepository;
import kr.fast.Jejuro.Entity.Travel;

@Service
public class RouteService {

    private final TravelAccessService travelAccessService;
    private final TravelRouteRepository routeRepository;
    private final RouteDayRepository dayRepository;
    private final RouteSpotRepository spotRepository;
    private final TravelBookmarkRepository bookmarkRepository;
    private final PoiRepository poiRepository;
    private final PoiService poiService;

    public RouteService(TravelAccessService travelAccessService, TravelRouteRepository routeRepository,
                        RouteDayRepository dayRepository, RouteSpotRepository spotRepository,
                        TravelBookmarkRepository bookmarkRepository, PoiRepository poiRepository,
                        PoiService poiService) {
        this.travelAccessService = travelAccessService;
        this.routeRepository = routeRepository;
        this.dayRepository = dayRepository;
        this.spotRepository = spotRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.poiRepository = poiRepository;
        this.poiService = poiService;
    }

    /** 빈 경로 만들기 → routeId를 받아 편집 화면으로 이동 */
    @Transactional
    public Long create(Long travelId, Long userId, String routeName) {
        ensureNotLocked(travelAccessService.getOwned(travelId, userId));
        String name = routeName == null || routeName.isBlank() ? null : routeName.trim();
        return routeRepository.save(new TravelRoute(travelId, name)).getRouteId();
    }

    /** 여행의 경로 목록 */
    @Transactional(readOnly = true)
    public List<RouteSummaryResponse> list(Long travelId, Long userId) {
        Travel travel = travelAccessService.getOwned(travelId, userId);
        List<TravelRoute> routes = routeRepository.findByTravelIdOrderByRouteIdDesc(travelId);
        if (routes.isEmpty()) {
            return List.of();
        }
        List<RouteDay> days = dayRepository.findByRouteIdIn(routes.stream().map(TravelRoute::getRouteId).toList());
        Map<Long, Long> dayCount = days.stream()
                .collect(Collectors.groupingBy(RouteDay::getRouteId, Collectors.counting()));
        Map<Long, Long> routeOfDay = days.stream()
                .collect(Collectors.toMap(RouteDay::getRouteDayId, RouteDay::getRouteId));
        Map<Long, Long> spotCount = days.isEmpty() ? Map.of()
                : spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(routeOfDay.keySet()).stream()
                        .collect(Collectors.groupingBy(s -> routeOfDay.get(s.getRouteDayId()), Collectors.counting()));

        return routes.stream()
                .map(r -> new RouteSummaryResponse(r.getRouteId(), r.getRouteName(), r.getCreatedAt(),
                        travel.isAdopted(r.getRouteId()),
                        dayCount.getOrDefault(r.getRouteId(), 0L).intValue(),
                        spotCount.getOrDefault(r.getRouteId(), 0L).intValue()))
                .toList();
    }

    /** 경로 상세: 일차별 방문지를 순서대로 */
    @Transactional(readOnly = true)
    public RouteDetailResponse detail(Long routeId, Long userId) {
        TravelRoute route = getOwnedRoute(routeId, userId);
        Travel travel = travelAccessService.getOwned(route.getTravelId(), userId);
        return build(route, travel);
    }

    /**
     * [8페이지 후기 게시판] 후기 글에 첨부된 여행의 "최종(채택) 경로"를 누구나 볼 수 있게 읽는다.
     * 소유자 검사를 하지 않으므로, 후기 글에 첨부된 여행에만 사용한다. 채택 경로가 없으면 null.
     */
    @Transactional(readOnly = true)
    public RouteDetailResponse adoptedRouteForPublic(Travel travel) {
        if (travel.getAdoptedRouteId() == null) return null;
        return routeRepository.findById(travel.getAdoptedRouteId())
                .map(route -> build(route, travel))
                .orElse(null);
    }

    private RouteDetailResponse build(TravelRoute route, Travel travel) {
        Long routeId = route.getRouteId();
        List<RouteDay> days = dayRepository.findByRouteIdOrderByDayNo(routeId);
        List<RouteSpot> spots = days.isEmpty() ? List.of()
                : spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(
                        days.stream().map(RouteDay::getRouteDayId).toList());

        Map<Long, PoiSummaryResponse> pois = poiService
                .findSummaries(spots.stream().map(RouteSpot::getPoiId).distinct().toList()).stream()
                .collect(Collectors.toMap(PoiSummaryResponse::poiId, Function.identity()));
        Map<Long, List<RouteSpot>> spotsByDay = spots.stream()
                .collect(Collectors.groupingBy(RouteSpot::getRouteDayId));

        List<DayResponse> dayResponses = days.stream()
                .map(d -> new DayResponse(d.getDayNo(), travel.dateOf(d.getDayNo()), d.getPrimaryRegionId(),
                        spotsByDay.getOrDefault(d.getRouteDayId(), List.of()).stream()
                                .sorted(Comparator.comparing(RouteSpot::getVisitOrder))
                                .map(s -> new SpotResponse(s.getVisitOrder(), pois.get(s.getPoiId())))
                                .toList()))
                .toList();

        return new RouteDetailResponse(routeId, travel.getTravelId(), route.getRouteName(),
                travel.isAdopted(routeId), travel.getAdoptedRouteId() != null, travel.getStartDate(), travel.getEndDate(),
                travel.tripDays(), dayResponses);
    }

    /**
     * 일정 전체 저장(덮어쓰기).
     * 기존 일차를 모두 지우고(방문지는 DB CASCADE로 함께 삭제) 요청대로 다시 넣는다.
     * → 순서를 바꿔도 UNIQUE(route_day_id, visit_order) 충돌이 생기지 않는다.
     */
    @Transactional
    public RouteDetailResponse save(Long routeId, Long userId, RouteSaveRequest req) {
        TravelRoute route = getOwnedRoute(routeId, userId);
        Travel travel = travelAccessService.getOwned(route.getTravelId(), userId);
        ensureNotLocked(travel);
        validate(travel, req);

        if (req.routeName() != null) {
            route.rename(req.routeName().isBlank() ? null : req.routeName().trim());
        }

        // 필요한 POI 정보를 한 번에 읽는다(권역 계산용)
        Set<Long> allPoiIds = req.days().stream().flatMap(d -> d.poiIds().stream()).collect(Collectors.toSet());
        Map<Long, Poi> poiMap = poiRepository.findAllById(allPoiIds).stream()
                .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));

        dayRepository.deleteByRouteId(routeId);

        for (DayReq d : req.days()) {
            if (d.poiIds().isEmpty()) {
                continue; // 빈 일차는 저장하지 않음
            }
            int primaryRegion = mostFrequentRegion(d.poiIds(), poiMap);
            RouteDay day = dayRepository.save(new RouteDay(routeId, d.dayNo(), primaryRegion));
            List<RouteSpot> spots = new ArrayList<>();
            for (int i = 0; i < d.poiIds().size(); i++) {
                spots.add(new RouteSpot(day.getRouteDayId(), d.poiIds().get(i), i + 1)); // visit_order 1부터
            }
            spotRepository.saveAll(spots);
        }
        return detail(routeId, userId);
    }

    /** 경로를 읽고, 그 경로의 여행이 내 것인지까지 확인 */
    public TravelRoute getOwnedRoute(Long routeId, Long userId) {
        TravelRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        travelAccessService.getOwned(route.getTravelId(), userId);
        return route;
    }

    /** 7페이지 규칙: 최종 경로를 채택한 여행은 경로를 만들거나 고칠 수 없다. */
    private void ensureNotLocked(Travel travel) {
        if (travel.getAdoptedRouteId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "최종 경로를 채택한 여행은 경로를 수정할 수 없습니다.");
        }
    }

    private void validate(Travel travel, RouteSaveRequest req) {
        int tripDays = travel.tripDays();
        Set<Long> bookmarked = new HashSet<>(bookmarkRepository.findPoiIds(travel.getTravelId()));
        Set<Integer> dayNos = new HashSet<>();

        for (DayReq d : req.days()) {
            if (d.dayNo() > tripDays) {
                throw ApiException.badRequest(d.dayNo() + "일차는 여행 기간(" + tripDays + "일)을 벗어납니다.");
            }
            if (!dayNos.add(d.dayNo())) {
                throw ApiException.badRequest(d.dayNo() + "일차가 두 번 들어왔습니다.");
            }
            if (new HashSet<>(d.poiIds()).size() != d.poiIds().size()) {
                throw ApiException.badRequest(d.dayNo() + "일차에 같은 관광지가 두 번 있습니다.");
            }
            for (Long poiId : d.poiIds()) {
                if (!bookmarked.contains(poiId)) {
                    throw ApiException.badRequest("찜하지 않은 관광지가 포함되어 있습니다: " + poiId);
                }
            }
        }
    }

    /** 그날 방문지들의 권역 중 가장 많은 권역(동률이면 먼저 방문하는 곳의 권역) */
    private int mostFrequentRegion(List<Long> poiIds, Map<Long, Poi> poiMap) {
        Map<Integer, Integer> count = new HashMap<>();
        Integer best = null;
        for (Long id : poiIds) {
            Poi poi = poiMap.get(id);
            if (poi == null) {
                throw ApiException.notFound("관광지를 찾을 수 없습니다: " + id);
            }
            int c = count.merge(poi.getRegionId(), 1, Integer::sum);
            if (best == null || c > count.get(best)) {
                best = poi.getRegionId();
            }
        }
        return best;
    }
}