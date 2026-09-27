package kr.fast.Jejuro.Service;


//[6페이지 카카오맵 동선 - 주변 숙소 (FR-26)]

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.RouteDay;
import kr.fast.Jejuro.Entity.RouteSpot;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.Repository.AccommodationRepository;
import kr.fast.Jejuro.Repository.AccommodationRepository.NearbyRow;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.RouteDayRepository;
import kr.fast.Jejuro.Repository.RouteSpotRepository;
import kr.fast.Jejuro.ResponseDTO.LodgingResponse;
import kr.fast.Jejuro.ResponseDTO.LodgingResponse.Anchor;
import kr.fast.Jejuro.ResponseDTO.LodgingResponse.Item;

/**
* 루트 주변 숙소 안내 (FR-26). 예약·결제는 제공하지 않는다.
*
* 기준 지점
*  - 기본: 선택 날짜의 마지막 관광지. 그날 방문지가 없으면 가장 가까운 전날의 마지막 관광지
*  - 여행 마지막 날: 기본 조회를 하지 않음(LAST_DAY). 사용자가 기준 관광지를 고르면 조회
*  - 사용자가 고른 관광지(anchorPoiId): 이 경로에 들어 있는 관광지만 허용
*
* 반경
*  - 기본: 자동차 3km / 도보 1km. 고를 수 있는 값: 1·3·5·10km
*  - 자동 확장(expand=true): 5곳 미만이면 다음 단계(5km → 10km)로 넓힘. 최대 10km
*  - 최대 30곳, 가까운 순
*/
@Service
public class LodgingService {

 static final List<Double> RADIUS_STEPS_KM = List.of(1.0, 3.0, 5.0, 10.0);
 static final int MIN_RESULTS = 5;
 static final int MAX_RESULTS = 30;

 private static final Map<String, String> TYPE_NAME = Map.of(
         "HOTEL", "호텔",
         "RESORT", "리조트·콘도",
         "PENSION", "펜션·풀빌라",
         "GUESTHOUSE", "게스트하우스·민박",
         "MOTEL", "모텔",
         "CAMPING", "캠핑·글램핑",
         "ETC", "기타 숙소");

 private final RouteService routeService;
 private final TravelAccessService travelAccessService;
 private final RouteDayRepository dayRepository;
 private final RouteSpotRepository spotRepository;
 private final PoiRepository poiRepository;
 private final AccommodationRepository accommodationRepository;

 public LodgingService(RouteService routeService, TravelAccessService travelAccessService,
                       RouteDayRepository dayRepository, RouteSpotRepository spotRepository,
                       PoiRepository poiRepository, AccommodationRepository accommodationRepository) {
     this.routeService = routeService;
     this.travelAccessService = travelAccessService;
     this.dayRepository = dayRepository;
     this.spotRepository = spotRepository;
     this.poiRepository = poiRepository;
     this.accommodationRepository = accommodationRepository;
 }

 @Transactional(readOnly = true)
 public LodgingResponse nearby(Long routeId, int dayNo, Long anchorPoiId, Double radiusKm,
                               TravelMode mode, boolean expand, Long userId) {
     TravelRoute route = routeService.getOwnedRoute(routeId, userId);
     Travel travel = travelAccessService.getOwned(route.getTravelId(), userId);
     int tripDays = travel.tripDays();
     if (dayNo < 1 || dayNo > tripDays) {
         throw ApiException.badRequest(dayNo + "일차는 여행 기간(" + tripDays + "일)을 벗어납니다.");
     }

     double requested = radiusKm != null ? radiusKm : (mode == TravelMode.WALK ? 1.0 : 3.0);
     if (!RADIUS_STEPS_KM.contains(requested)) {
         throw ApiException.badRequest("검색 반경은 1, 3, 5, 10km 중에서 고를 수 있습니다.");
     }

     // 경로의 방문지 전체 (일차 → 방문 순서)
     List<RouteDay> days = dayRepository.findByRouteIdOrderByDayNo(routeId);
     Map<Long, Integer> dayNoOf = days.stream()
             .collect(java.util.stream.Collectors.toMap(RouteDay::getRouteDayId, RouteDay::getDayNo));
     List<RouteSpot> spots = days.isEmpty() ? List.of()
             : spotRepository.findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(dayNoOf.keySet()).stream()
                     .sorted(Comparator.comparing((RouteSpot s) -> dayNoOf.get(s.getRouteDayId()))
                             .thenComparing(RouteSpot::getVisitOrder))
                     .toList();

     // 기준 관광지 정하기
     RouteSpot anchorSpot;
     String anchorLabel;
     if (anchorPoiId != null) {
         anchorSpot = spots.stream().filter(s -> s.getPoiId().equals(anchorPoiId)).findFirst()
                 .orElseThrow(() -> ApiException.badRequest("이 경로에 없는 관광지는 기준으로 고를 수 없습니다."));
         anchorLabel = "직접 고른 관광지";
     } else if (dayNo == tripDays) {
         return empty("LAST_DAY", "여행 마지막 날이에요. 숙소가 필요하면 기준 관광지를 골라 주세요.",
                 travel, dayNo, requested);
     } else {
         Optional<RouteSpot> last = lastSpotOnOrBefore(spots, dayNoOf, dayNo);
         if (last.isEmpty()) {
             return empty("NO_SPOT", "이 날짜까지 방문할 관광지가 없어 기준을 정할 수 없어요. 경로에 관광지를 추가하거나 기준 관광지를 골라 주세요.",
                     travel, dayNo, requested);
         }
         anchorSpot = last.get();
         int d = dayNoOf.get(anchorSpot.getRouteDayId());
         anchorLabel = d == dayNo ? d + "일차 마지막 관광지" : d + "일차 마지막 관광지 (" + dayNo + "일차에 방문지가 없어요)";
     }

     Poi poi = poiRepository.findById(anchorSpot.getPoiId())
             .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다: " + anchorSpot.getPoiId()));
     double lat = poi.getLatitude().doubleValue();
     double lng = poi.getLongitude().doubleValue();
     Anchor anchor = new Anchor(poi.getPoiId(), poi.getPoiName(), lat, lng,
             dayNoOf.get(anchorSpot.getRouteDayId()), anchorSpot.getVisitOrder());

     // 반경 검색 (+ 자동 확장)
     double radius = requested;
     List<NearbyRow> rows = search(lat, lng, radius);
     while (expand && rows.size() < MIN_RESULTS && radius < RADIUS_STEPS_KM.get(RADIUS_STEPS_KM.size() - 1)) {
         radius = RADIUS_STEPS_KM.get(RADIUS_STEPS_KM.indexOf(radius) + 1);
         rows = search(lat, lng, radius);
     }

     List<Item> items = rows.stream().map(this::toItem).toList();
     String status = items.isEmpty() ? "EMPTY" : "OK";
     String message = items.isEmpty()
             ? "반경 " + fmt(radius) + "km 안에 등록된 숙소가 없어요." + (radius < 10 ? " 반경을 넓혀 보세요." : "")
             : (radius > requested ? fmt(requested) + "km 안에 숙소가 적어서 " + fmt(radius) + "km로 넓혔어요." : null);

     return new LodgingResponse(status, message, dayNo, travel.dateOf(dayNo), anchor, anchorLabel,
             requested, radius, radius > requested, items);
 }

 /** dayNo일차(없으면 그 전날들)의 마지막 방문지 */
 private Optional<RouteSpot> lastSpotOnOrBefore(List<RouteSpot> spots, Map<Long, Integer> dayNoOf, int dayNo) {
     RouteSpot best = null;
     for (RouteSpot s : spots) {           // 일차 → 방문 순서로 정렬되어 있음
         if (dayNoOf.get(s.getRouteDayId()) <= dayNo) {
             best = s;
         }
     }
     return Optional.ofNullable(best);
 }

 private List<NearbyRow> search(double lat, double lng, double radiusKm) {
     double dLat = radiusKm / 111.32;
     double dLng = radiusKm / (111.32 * Math.cos(Math.toRadians(lat)));
     return accommodationRepository.findNearby(lat, lng, lat - dLat, lat + dLat, lng - dLng, lng + dLng,
             radiusKm * 1000, MAX_RESULTS);
 }

 private Item toItem(NearbyRow r) {
     String sourceId = r.getSourceId() == null ? "" : r.getSourceId();
     String source = sourceId.contains(":") ? sourceId.substring(0, sourceId.indexOf(':')) : "";
     String type = r.getType() == null ? "ETC" : r.getType();
     return new Item(r.getId().longValue(), r.getName(), type, TYPE_NAME.getOrDefault(type, "기타 숙소"),
             r.getAddress(), r.getLatitude().doubleValue(), r.getLongitude().doubleValue(),
             r.getPhone(), r.getImageUrl(), source, (int) Math.round(r.getDistanceM().doubleValue()));
 }

 private LodgingResponse empty(String status, String message, Travel travel, int dayNo, double requested) {
     return new LodgingResponse(status, message, dayNo, travel.dateOf(dayNo), null, null,
             requested, requested, false, List.of());
 }

 private static String fmt(double km) {
     return km == Math.floor(km) ? String.valueOf((int) km) : String.valueOf(km);
 }
}