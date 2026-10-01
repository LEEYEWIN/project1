package kr.fast.Jejuro.Service;



//[6페이지 카카오맵 동선]

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Service.KakaoMobilityClient.CarRoute;
import kr.fast.Jejuro.ResponseDTO.DirectionsResponse;
import kr.fast.Jejuro.ResponseDTO.DirectionsResponse.LatLng;
import kr.fast.Jejuro.ResponseDTO.DirectionsResponse.Leg;
import kr.fast.Jejuro.ResponseDTO.DirectionsResponse.PlaceNote;
import kr.fast.Jejuro.ResponseDTO.OptimizeResponse;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Entity.RouteDay;
import kr.fast.Jejuro.Entity.RouteSpot;
import kr.fast.Jejuro.Repository.RouteDayRepository;
import kr.fast.Jejuro.Repository.RouteSpotRepository;

@Service
@Transactional(readOnly = true)
public class DirectionsService {

private static final double WALK_SPEED_MPS = 4_000 / 3600.0;  // 시속 4km
private static final double CAR_SPEED_MPS = 40_000 / 3600.0;  // 카카오 키가 없을 때 쓰는 추정 속도
private static final double DETOUR = 1.3;                       // 직선거리 → 실제 길 보정

private final RouteService routeService;
private final RouteDayRepository dayRepository;
private final RouteSpotRepository spotRepository;
private final PoiRepository poiRepository;
private final KakaoMobilityClient kakaoClient;
private final RouteOptimizer optimizer;
private final TaxiFareCalculator taxi;

public DirectionsService(RouteService routeService, RouteDayRepository dayRepository,
                        RouteSpotRepository spotRepository, PoiRepository poiRepository,
                        KakaoMobilityClient kakaoClient, RouteOptimizer optimizer,
                        TaxiFareCalculator taxi) {
   this.routeService = routeService;
   this.dayRepository = dayRepository;
   this.spotRepository = spotRepository;
   this.poiRepository = poiRepository;
   this.kakaoClient = kakaoClient;
   this.optimizer = optimizer;
   this.taxi = taxi;
}

/** 6페이지: 저장된 경로의 N일차 동선 */
public DirectionsResponse directions(Long routeId, int dayNo, TravelMode mode, Long userId) {
   return compute(loadDayPois(routeId, dayNo, userId), mode);
}

/** 5페이지 미리보기: 아직 저장하지 않은 방문 순서(poiIds 순서 그대로)로 동선 계산 */
public DirectionsResponse preview(List<Long> poiIds, TravelMode mode) {
   return compute(loadPois(poiIds), mode);
}

/** 5페이지 미리보기: 저장 전 순서로 효율적인 순서 제안 (1번 고정, fixEnd면 마지막도 고정) */
public OptimizeResponse previewOptimize(List<Long> poiIds, boolean fixEnd) {
   return optimizeOf(points(loadPois(poiIds)), fixEnd);
}

private DirectionsResponse compute(List<Poi> pois, TravelMode mode) {
   List<GeoPoint> points = points(pois);
   List<PlaceNote> notes = placeNotes(pois);
   if (points.size() < 2) {
       return new DirectionsResponse(mode.name(), false, 0, 0, List.of(), toPath(points), null, 0, notes);
   }
   if (mode == TravelMode.CAR && kakaoClient.isConfigured()) {
       if (points.size() > KakaoMobilityClient.MAX_POINTS) {
           throw ApiException.badRequest("하루 방문지는 최대 " + KakaoMobilityClient.MAX_POINTS + "곳까지 길찾기를 할 수 있습니다.");
       }
       try {
           return carByKakao(points, notes);
       } catch (ApiException e) {
           // 카카오 호출이 실패해도 화면이 멈추지 않게 추정값으로 대신하고, 실패 이유를 함께 보낸다
           return estimate(points, mode, CAR_SPEED_MPS, failureNotice(e.getMessage(), points, notes), notes);
       }
   }
   double speed = mode == TravelMode.WALK ? WALK_SPEED_MPS : CAR_SPEED_MPS;
   return estimate(points, mode, speed, null, notes);
}

public OptimizeResponse optimize(Long routeId, int dayNo, boolean fixEnd, Long userId) {
   return optimizeOf(points(loadDayPois(routeId, dayNo, userId)), fixEnd);
}

private OptimizeResponse optimizeOf(List<GeoPoint> points, boolean fixEnd) {
   RouteOptimizer.Result result = optimizer.optimize(points, fixEnd);
   List<GeoPoint> best = result.route();
   boolean endFixed = fixEnd && points.size() >= 3;
   int free = Math.max(points.size() - (endFixed ? 2 : 1), 0);
   long compared = result.method() == RouteOptimizer.Method.EXACT ? factorial(free) : 0;
   return new OptimizeResponse(
           best.stream().map(GeoPoint::poiId).toList(),
           (int) Math.round(optimizer.totalDistance(points)),
           (int) Math.round(optimizer.totalDistance(best)),
           result.method().name(),
           points.isEmpty() ? null : points.get(0).name(),
           endFixed ? points.get(points.size() - 1).name() : null,
           compared);
}

private static long factorial(int n) {
   long f = 1;
   for (int i = 2; i <= n; i++) f *= i;
   return f;
}

/** 관광지 ID 목록 → 관광지 (요청한 순서 유지) */
private List<Poi> loadPois(List<Long> poiIds) {
   Map<Long, Poi> pois = poiRepository.findAllById(poiIds).stream()
           .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));
   return poiIds.stream()
           .map(id -> {
               Poi p = pois.get(id);
               if (p == null) {
                   throw ApiException.notFound("관광지를 찾을 수 없습니다: " + id);
               }
               return p;
           })
           .toList();
}

/** 경로 소유권 확인 → N일차 방문지를 방문 순서대로 */
private List<Poi> loadDayPois(Long routeId, int dayNo, Long userId) {
   routeService.getOwnedRoute(routeId, userId);
   RouteDay day = dayRepository.findByRouteIdAndDayNo(routeId, dayNo)
           .orElseThrow(() -> ApiException.notFound(dayNo + "일차 일정이 없습니다."));
   List<RouteSpot> spots = spotRepository.findByRouteDayIdOrderByVisitOrder(day.getRouteDayId());
   Map<Long, Poi> pois = poiRepository.findAllById(spots.stream().map(RouteSpot::getPoiId).toList()).stream()
           .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));
   return spots.stream().map(s -> pois.get(s.getPoiId())).toList();
}

private static List<GeoPoint> points(List<Poi> pois) {
   return pois.stream()
           .map(p -> new GeoPoint(p.getPoiId(), p.displayName(),
                   p.getLatitude().doubleValue(), p.getLongitude().doubleValue()))
           .toList();
}

/** 섬(배편)·산(주차·등산) 안내. 방문 순서는 1부터 */
private static List<PlaceNote> placeNotes(List<Poi> pois) {
   List<PlaceNote> notes = new ArrayList<>();
   for (int i = 0; i < pois.size(); i++) {
       Poi p = pois.get(i);
       int order = i + 1;
       PlaceAccess.of(p.displayName(), p.getAddress(), p.getCategoryCode()).ifPresent(kind ->
               notes.add(new PlaceNote(order, p.displayName(), kind.name(), PlaceAccess.message(kind))));
   }
   return notes;
}

/** 카카오 실패 메시지("도착 지점 주변의 도로를 탐색할 수 없음" 등)를 사용자가 이해할 수 있게 풀어 쓴다 */
private static String failureNotice(String raw, List<GeoPoint> points, List<PlaceNote> notes) {
   String msg = raw == null ? "" : raw;
   String where = null;
   if (msg.contains("도착")) where = "도착 지점(" + points.get(points.size() - 1).name() + ")";
   else if (msg.contains("출발") || msg.contains("시작")) where = "출발 지점(" + points.get(0).name() + ")";
   else if (msg.contains("경유")) where = "경유지";
   if (where == null || !msg.contains("도로")) {
       return msg + " (직선거리 추정값으로 표시)";
   }
   return "자동차 경로를 찾지 못했어요. " + where + " 주변에 차가 다닐 수 있는 도로가 없어요. "
           + "섬이나 산 정상·탐방로처럼 차로 바로 갈 수 없는 곳이 있으면 이렇게 돼요. "
           + "그래서 지도 선과 거리·시간·택시비는 직선거리로 계산한 추정값이에요."
           + (notes.isEmpty() ? "" : " 아래 관광지 안내를 확인하세요.");
}

private DirectionsResponse carByKakao(List<GeoPoint> points, List<PlaceNote> notes) {
   CarRoute car = kakaoClient.route(points);
   List<Leg> legs = new ArrayList<>();
   int totalD = 0;
   int totalT = 0;
   for (int i = 0; i < car.sections().size() && i + 1 < points.size(); i++) {
       var s = car.sections().get(i);
       legs.add(leg(points, i, s.distanceM(), s.durationSec()));
       totalD += s.distanceM();
       totalT += s.durationSec();
   }
   List<LatLng> path = car.path().stream().map(p -> new LatLng(p[0], p[1])).toList();
   return new DirectionsResponse(TravelMode.CAR.name(), false, totalD, totalT, legs, path, null, totalFare(legs), notes);
}

private DirectionsResponse estimate(List<GeoPoint> points, TravelMode mode, double speedMps, String notice,
                                   List<PlaceNote> notes) {
   List<Leg> legs = new ArrayList<>();
   int totalD = 0;
   int totalT = 0;
   for (int i = 0; i < points.size() - 1; i++) {
       int d = (int) Math.round(points.get(i).distanceTo(points.get(i + 1)) * DETOUR);
       int t = (int) Math.round(d / speedMps);
       legs.add(leg(points, i, d, t));
       totalD += d;
       totalT += t;
   }
   return new DirectionsResponse(mode.name(), true, totalD, totalT, legs, toPath(points), notice, totalFare(legs), notes);
}

private Leg leg(List<GeoPoint> points, int i, int distanceM, int durationSec) {
   GeoPoint a = points.get(i);
   GeoPoint b = points.get(i + 1);
   return new Leg(i + 1, i + 2, a.name(), b.name(), a.lat(), a.lng(), b.lat(), b.lng(), distanceM, durationSec,
           taxi.fare(distanceM));
}

private static int totalFare(List<Leg> legs) {
   return legs.stream().mapToInt(Leg::taxiFare).sum();
}

private List<LatLng> toPath(List<GeoPoint> points) {
   return points.stream().map(p -> new LatLng(p.lat(), p.lng())).toList();
}
}