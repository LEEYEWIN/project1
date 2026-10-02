package kr.fast.Jejuro.Service;

// [5페이지 루트 짜기 (6·7페이지, 커뮤니티 경로 가져오기에서도 사용)]

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.RouteDay;
import kr.fast.Jejuro.Entity.RouteSpot;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.RouteDayRepository;
import kr.fast.Jejuro.Repository.RouteSpotRepository;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Repository.TravelRouteRepository;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest.DayReq;
import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse.DayResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse.SpotResponse;
import kr.fast.Jejuro.ResponseDTO.RouteSummaryResponse;

/**
 * 여행 경로(루트).
 * 규칙
 *  - 여행 하나에 경로는 하나 (DB UNIQUE travel_id). 처음 들어올 때 만들고, 이후에는 같은 경로를 계속 고친다.
 *  - 화면이 바뀔 때마다 자동 저장(PUT)하므로 페이지를 나갔다 와도 마지막 상태가 남는다.
 *  - 경로에는 "여행 장소"(TRAVEL_BOOKMARK)에 추가한 관광지만 넣을 수 있다.
 *  - 최종 확정(채택)하려면 여행 장소가 모두 경로에 배치되어 있어야 한다(placement 참고).
 *  - 확정한 뒤에는 경로를 고칠 수 없다.
 */
@Service
public class RouteService {

    private final TravelAccessService travelAccessService;
    private final TravelRepository travelRepository;
    private final TravelRouteRepository routeRepository;
    private final RouteDayRepository dayRepository;
    private final RouteSpotRepository spotRepository;
    private final TravelBookmarkRepository bookmarkRepository;
    private final PoiRepository poiRepository;
    private final PoiService poiService;

    public RouteService(TravelAccessService travelAccessService, TravelRepository travelRepository,
                        TravelRouteRepository routeRepository,
                        RouteDayRepository dayRepository, RouteSpotRepository spotRepository,
                        TravelBookmarkRepository bookmarkRepository, PoiRepository poiRepository,
                        PoiService poiService) {
        this.travelAccessService = travelAccessService;
        this.travelRepository = travelRepository;
        this.routeRepository = routeRepository;
        this.dayRepository = dayRepository;
        this.spotRepository = spotRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.poiRepository = poiRepository;
        this.poiService = poiService;
    }

    /**
     * 여행의 경로 번호. 없으면 빈 경로를 만든다(여행당 1개).
     * 여행 행을 먼저 잠가서(FOR UPDATE) 같은 요청이 동시에 두 번 와도 차례로 처리한다
     * → 두 번째 요청은 첫 요청이 만든 경로를 그대로 돌려받는다. (개발 모드 StrictMode의 중복 호출 대비)
     * 잠금이 이 트랜잭션의 첫 조회여야 뒤의 조회가 최신 데이터를 본다.
     */
    @Transactional
    public Long ensureRoute(Long travelId, Long userId) {
        Travel travel = travelRepository.findOwnedForUpdate(travelId, userId)
                .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
        Optional<TravelRoute> existing = routeRepository.findFirstByTravelIdOrderByRouteIdDesc(travelId);
        if (existing.isPresent()) {
            return existing.get().getRouteId();
        }
        ensureNotLocked(travel);
        return routeRepository.save(new TravelRoute(travelId, null)).getRouteId();
    }

    /** 여행의 경로 번호 (없으면 비어 있음) */
    @Transactional(readOnly = true)
    public Optional<Long> findRouteId(Long travelId) {
        return routeRepository.findFirstByTravelIdOrderByRouteIdDesc(travelId).map(TravelRoute::getRouteId);
    }

    /** 여행의 경로 요약 (없으면 null) */
    @Transactional(readOnly = true)
    public RouteSummaryResponse summary(Long travelId, Long userId) {
        Travel travel = travelAccessService.getOwned(travelId, userId);
        return routeRepository.findFirstByTravelIdOrderByRouteIdDesc(travelId)
                .map(r -> {
                    List<RouteDay> days = dayRepository.findByRouteIdOrderByDayNo(r.getRouteId());
                    int spots = days.isEmpty() ? 0
                            : spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(
                                    days.stream().map(RouteDay::getRouteDayId).toList()).size();
                    return new RouteSummaryResponse(r.getRouteId(), r.getRouteName(), r.getCreatedAt(),
                            travel.isAdopted(r.getRouteId()), days.size(), spots);
                })
                .orElse(null);
    }

    /** 경로 목록 (여행당 1개라 0개 또는 1개). 예전 화면 호환용 */
    @Transactional(readOnly = true)
    public List<RouteSummaryResponse> list(Long travelId, Long userId) {
        RouteSummaryResponse one = summary(travelId, userId);
        return one == null ? List.of() : List.of(one);
    }

    /** 경로 상세: 일차별 방문지를 순서대로 */
    @Transactional(readOnly = true)
    public RouteDetailResponse detail(Long routeId, Long userId) {
        TravelRoute route = getOwnedRoute(routeId, userId);
        Travel travel = travelAccessService.getOwned(route.getTravelId(), userId);
        return build(route, travel);
    }

    /**
     * [커뮤니티] 글에 첨부된 여행의 "최종(채택) 경로"를 누구나 볼 수 있게 읽는다.
     * 소유자 검사를 하지 않으므로, 글에 첨부된 여행에만 사용한다. 채택 경로가 없으면 null.
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

        List<DayReq> current = dayResponses.stream()
                .map(d -> new DayReq(d.dayNo(), d.spots().stream().map(s -> s.poi() == null ? null : s.poi().poiId()).toList()))
                .toList();
        return new RouteDetailResponse(routeId, travel.getTravelId(), route.getRouteName(),
                travel.isAdopted(routeId), travel.getAdoptedRouteId() != null, travel.getStartDate(), travel.getEndDate(),
                travel.tripDays(), dayResponses, version(route.getRouteName(), current));
    }

    /**
     * 일정 전체 저장(덮어쓰기). 화면이 자동 저장할 때마다 호출한다.
     * 기존 일차를 모두 지우고(방문지는 DB CASCADE로 함께 삭제) 요청대로 다시 넣는다.
     * → 순서를 바꿔도 UNIQUE(route_day_id, visit_order) 충돌이 생기지 않는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)   // 여행 행 잠금 뒤 최신 데이터를 읽도록 (REPEATABLE READ면 잠금 전 스냅샷을 읽음)
    public RouteDetailResponse save(Long routeId, Long userId, RouteSaveRequest req) {
        // 여행 행을 먼저 잠근다: 다른 탭의 저장·장소 빼기·일정 확정과 겹치지 않게 차례로 처리
        Travel travel = travelRepository.findOwnedByRouteForUpdate(routeId, userId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        TravelRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        ensureNotLocked(travel);
        // 화면이 불러온 뒤에 다른 탭(또는 장소 빼기)이 경로를 바꿨으면 덮어쓰지 않고 알린다
        if (req.baseVersion() != null && !req.baseVersion().equals(version(route.getRouteName(), currentDays(routeId)))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "다른 화면(탭)에서 이 경로가 먼저 바뀌었어요. 최신 경로를 불러온 뒤 다시 수정해 주세요.");
        }
        validate(travel, req.days());

        if (req.routeName() != null) {
            route.rename(req.routeName().isBlank() ? null : req.routeName().trim());
        }
        writeDays(routeId, req.days());
        return build(route, travel);
    }

    /**
     * [커뮤니티 경로 가져오기] 방금 만든 내 여행의 경로에 일정을 그대로 넣는다.
     * 여행 장소 추가·소유권 확인은 호출하는 쪽(RouteImportService)이 끝낸 상태여야 한다.
     */
    @Transactional
    public void writeImportedDays(Long routeId, String routeName, List<DayReq> days) {
        TravelRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        route.rename(routeName);
        writeDays(routeId, days);
    }

    /**
     * 여행 장소에서 관광지를 뺐을 때: 경로에서도 그 관광지를 빼고 뒤 순번을 당긴다.
     * (여행 장소에 없는 관광지가 경로에 남지 않게)
     */
    @Transactional
    public void removePoiFromRoute(Long travelId, Long poiId) {
        Optional<TravelRoute> route = routeRepository.findFirstByTravelIdOrderByRouteIdDesc(travelId);
        if (route.isEmpty()) return;
        Long routeId = route.get().getRouteId();
        List<DayReq> days = currentDays(routeId);
        boolean contains = days.stream().anyMatch(d -> d.poiIds().contains(poiId));
        if (!contains) return;
        List<DayReq> next = days.stream()
                .map(d -> new DayReq(d.dayNo(), d.poiIds().stream().filter(id -> !id.equals(poiId)).toList()))
                .toList();
        writeDays(routeId, next);
    }

    /**
     * 여행 장소 배치 현황: 장소 수, 경로에 배치된 수, 아직 배치 안 된 관광지.
     * 최종 확정(채택)은 unplaced가 비어 있을 때만 가능하다.
     */
    @Transactional(readOnly = true)
    public Placement placement(Long travelId) {
        List<Long> places = bookmarkRepository.findPoiIds(travelId);
        Set<Long> placed = routeRepository.findFirstByTravelIdOrderByRouteIdDesc(travelId)
                .map(r -> currentDays(r.getRouteId()).stream()
                        .flatMap(d -> d.poiIds().stream())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        List<Long> unplaced = places.stream().filter(id -> !placed.contains(id)).toList();
        int placedPlaces = (int) places.stream().filter(placed::contains).count();
        return new Placement(places.size(), placedPlaces, unplaced);
    }

    /** 장소 수, 배치된 장소 수, 배치 안 된 관광지 번호 */
    public record Placement(int placeCount, int placedCount, List<Long> unplacedPoiIds) {
        public boolean complete() {
            return placeCount > 0 && unplacedPoiIds.isEmpty();
        }
    }

    /** 경로를 읽고, 그 경로의 여행이 내 것인지까지 확인 */
    public TravelRoute getOwnedRoute(Long routeId, Long userId) {
        TravelRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> ApiException.notFound("경로를 찾을 수 없습니다."));
        travelAccessService.getOwned(route.getTravelId(), userId);
        return route;
    }

    /** 최종 경로를 확정(채택)한 여행은 경로를 고칠 수 없다. */
    public void ensureNotLocked(Travel travel) {
        if (travel.getAdoptedRouteId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "일정을 확정한 여행은 경로를 수정할 수 없습니다.");
        }
    }

    /** 저장된 일정 → [일차, 방문 순서대로 관광지] */
    private List<DayReq> currentDays(Long routeId) {
        List<RouteDay> days = dayRepository.findByRouteIdOrderByDayNo(routeId);
        if (days.isEmpty()) return List.of();
        Map<Long, List<Long>> spots = spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(
                        days.stream().map(RouteDay::getRouteDayId).toList()).stream()
                .collect(Collectors.groupingBy(RouteSpot::getRouteDayId,
                        Collectors.mapping(RouteSpot::getPoiId, Collectors.toList())));
        return days.stream()
                .map(d -> new DayReq(d.getDayNo(), spots.getOrDefault(d.getRouteDayId(), List.of())))
                .toList();
    }

    /** 일차 전체를 지우고 다시 쓴다. 방문지가 없는 일차는 저장하지 않는다. */
    private void writeDays(Long routeId, List<DayReq> days) {
        Set<Long> allPoiIds = days.stream().flatMap(d -> d.poiIds().stream()).collect(Collectors.toSet());
        Map<Long, Poi> poiMap = poiRepository.findAllById(allPoiIds).stream()
                .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));

        dayRepository.deleteByRouteId(routeId);

        for (DayReq d : days) {
            if (d.poiIds().isEmpty()) {
                continue;
            }
            int primaryRegion = mostFrequentRegion(d.poiIds(), poiMap);
            RouteDay day = dayRepository.save(new RouteDay(routeId, d.dayNo(), primaryRegion));
            List<RouteSpot> spots = new ArrayList<>();
            for (int i = 0; i < d.poiIds().size(); i++) {
                spots.add(new RouteSpot(day.getRouteDayId(), d.poiIds().get(i), i + 1)); // visit_order 1부터
            }
            spotRepository.saveAll(spots);
        }
    }

    private void validate(Travel travel, List<DayReq> days) {
        int tripDays = travel.tripDays();
        Set<Long> places = new HashSet<>(bookmarkRepository.findPoiIds(travel.getTravelId()));
        Set<Integer> dayNos = new HashSet<>();
        Set<Long> used = new LinkedHashSet<>();

        for (DayReq d : days) {
            if (d.dayNo() > tripDays) {
                throw ApiException.badRequest(d.dayNo() + "일차는 여행 기간(" + tripDays + "일)을 벗어납니다.");
            }
            if (!dayNos.add(d.dayNo())) {
                throw ApiException.badRequest(d.dayNo() + "일차가 두 번 들어왔습니다.");
            }
            for (Long poiId : d.poiIds()) {
                if (!places.contains(poiId)) {
                    throw ApiException.badRequest("여행 장소에 추가하지 않은 관광지가 포함되어 있습니다: " + poiId);
                }
                if (!used.add(poiId)) {
                    throw ApiException.badRequest("같은 관광지를 두 번 넣을 수 없습니다: " + poiId);
                }
            }
        }
    }

    /**
     * 경로 내용의 버전 = (이름, 방문지가 있는 일차와 방문 순서)의 해시.
     * DB에 칸을 추가하지 않고, 저장된 내용이 바뀌면 값도 바뀐다.
     */
    static String version(String routeName, List<DayReq> days) {
        StringBuilder sb = new StringBuilder(routeName == null ? "" : routeName).append('|');
        days.stream()
                .filter(d -> !d.poiIds().isEmpty())
                .sorted(Comparator.comparing(DayReq::dayNo))
                .forEach(d -> sb.append(d.dayNo()).append(':').append(d.poiIds()).append(';'));
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
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
