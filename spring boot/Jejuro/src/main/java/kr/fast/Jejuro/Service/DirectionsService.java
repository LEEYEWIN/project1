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

 public DirectionsService(RouteService routeService, RouteDayRepository dayRepository,
                          RouteSpotRepository spotRepository, PoiRepository poiRepository,
                          KakaoMobilityClient kakaoClient, RouteOptimizer optimizer) {
     this.routeService = routeService;
     this.dayRepository = dayRepository;
     this.spotRepository = spotRepository;
     this.poiRepository = poiRepository;
     this.kakaoClient = kakaoClient;
     this.optimizer = optimizer;
 }

 /** 6페이지: 저장된 경로의 N일차 동선 */
 public DirectionsResponse directions(Long routeId, int dayNo, TravelMode mode, Long userId) {
     return compute(loadDayPoints(routeId, dayNo, userId), mode);
 }

 /** 5페이지 미리보기: 아직 저장하지 않은 방문 순서(poiIds 순서 그대로)로 동선 계산 */
 public DirectionsResponse preview(List<Long> poiIds, TravelMode mode) {
     return compute(loadPoints(poiIds), mode);
 }

 /** 5페이지 미리보기: 저장 전 순서로 효율적인 순서 제안 */
 public OptimizeResponse previewOptimize(List<Long> poiIds) {
     return optimizeOf(loadPoints(poiIds));
 }

 private DirectionsResponse compute(List<GeoPoint> points, TravelMode mode) {
     if (points.size() < 2) {
         return new DirectionsResponse(mode.name(), false, 0, 0, List.of(), toPath(points), null);
     }
     if (mode == TravelMode.CAR && kakaoClient.isConfigured()) {
         if (points.size() > KakaoMobilityClient.MAX_POINTS) {
             throw ApiException.badRequest("하루 방문지는 최대 " + KakaoMobilityClient.MAX_POINTS + "곳까지 길찾기를 할 수 있습니다.");
         }
         try {
             return carByKakao(points);
         } catch (ApiException e) {
             // 카카오 호출이 실패해도 화면이 멈추지 않게 추정값으로 대신하고, 실패 이유를 함께 보낸다
             return estimate(points, mode, CAR_SPEED_MPS, e.getMessage() + " (직선거리 추정값으로 표시)");
         }
     }
     double speed = mode == TravelMode.WALK ? WALK_SPEED_MPS : CAR_SPEED_MPS;
     return estimate(points, mode, speed, null);
 }

 public OptimizeResponse optimize(Long routeId, int dayNo, Long userId) {
     return optimizeOf(loadDayPoints(routeId, dayNo, userId));
 }

 private OptimizeResponse optimizeOf(List<GeoPoint> points) {
     List<GeoPoint> best = optimizer.optimize(points);
     return new OptimizeResponse(
             best.stream().map(GeoPoint::poiId).toList(),
             (int) Math.round(optimizer.totalDistance(points)),
             (int) Math.round(optimizer.totalDistance(best)));
 }

 /** 경로 소유권 확인 → N일차 방문지를 방문 순서대로 좌표로 변환 */
 /** 관광지 ID 목록 → 좌표 (요청한 순서 유지) */
 private List<GeoPoint> loadPoints(List<Long> poiIds) {
     Map<Long, Poi> pois = poiRepository.findAllById(poiIds).stream()
             .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));
     return poiIds.stream()
             .map(id -> {
                 Poi p = pois.get(id);
                 if (p == null) {
                     throw ApiException.notFound("관광지를 찾을 수 없습니다: " + id);
                 }
                 return new GeoPoint(p.getPoiId(), p.getPoiName(),
                         p.getLatitude().doubleValue(), p.getLongitude().doubleValue());
             })
             .toList();
 }

 private List<GeoPoint> loadDayPoints(Long routeId, int dayNo, Long userId) {
     routeService.getOwnedRoute(routeId, userId);
     RouteDay day = dayRepository.findByRouteIdAndDayNo(routeId, dayNo)
             .orElseThrow(() -> ApiException.notFound(dayNo + "일차 일정이 없습니다."));
     List<RouteSpot> spots = spotRepository.findByRouteDayIdOrderByVisitOrder(day.getRouteDayId());
     Map<Long, Poi> pois = poiRepository.findAllById(spots.stream().map(RouteSpot::getPoiId).toList()).stream()
             .collect(Collectors.toMap(Poi::getPoiId, Function.identity()));
     return spots.stream()
             .map(s -> pois.get(s.getPoiId()))
             .map(p -> new GeoPoint(p.getPoiId(), p.getPoiName(),
                     p.getLatitude().doubleValue(), p.getLongitude().doubleValue()))
             .toList();
 }

 private DirectionsResponse carByKakao(List<GeoPoint> points) {
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
     return new DirectionsResponse(TravelMode.CAR.name(), false, totalD, totalT, legs, path, null);
 }

 private DirectionsResponse estimate(List<GeoPoint> points, TravelMode mode, double speedMps, String notice) {
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
     return new DirectionsResponse(mode.name(), true, totalD, totalT, legs, toPath(points), notice);
 }

 private Leg leg(List<GeoPoint> points, int i, int distanceM, int durationSec) {
     GeoPoint a = points.get(i);
     GeoPoint b = points.get(i + 1);
     return new Leg(i + 1, i + 2, a.name(), b.name(), a.lat(), a.lng(), b.lat(), b.lng(), distanceM, durationSec);
 }

 private List<LatLng> toPath(List<GeoPoint> points) {
     return points.stream().map(p -> new LatLng(p.lat(), p.lng())).toList();
 }
}