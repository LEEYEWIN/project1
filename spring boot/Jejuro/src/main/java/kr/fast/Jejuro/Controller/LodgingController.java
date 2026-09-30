package kr.fast.Jejuro.Controller;


//[6페이지 카카오맵 동선 - 주변 숙소 (FR-26)]

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.ResponseDTO.LodgingResponse;
import kr.fast.Jejuro.Service.LodgingService;
import kr.fast.Jejuro.Service.TravelMode;

@RestController
@RequestMapping("/api")
public class LodgingController {

 private final LodgingService lodgingService;
 private final CurrentUser currentUser;

 public LodgingController(LodgingService lodgingService, CurrentUser currentUser) {
     this.lodgingService = lodgingService;
     this.currentUser = currentUser;
 }

 /**
  * N일차 주변 숙소.
  * GET /api/routes/{routeId}/days/{dayNo}/lodgings?anchorPoiId=&radiusKm=&mode=CAR&expand=true
  * - anchorPoiId 없음 → 그날 마지막 관광지(마지막 날은 조회 안 함)
  * - radiusKm 없음 → CAR 3km / WALK 1km
  * - expand=true 이고 5곳 미만이면 5km → 10km로 넓힘 (사용자가 반경을 직접 고르면 화면이 false로 보냄)
  */
 @GetMapping("/routes/{routeId}/days/{dayNo}/lodgings")
 public LodgingResponse lodgings(@PathVariable("routeId") Long routeId,
                                 @PathVariable("dayNo") int dayNo,
                                 @RequestParam(name = "anchorPoiId", required = false) Long anchorPoiId,
                                 @RequestParam(name = "radiusKm", required = false) Double radiusKm,
                                 @RequestParam(name = "mode", defaultValue = "CAR") TravelMode mode,
                                 @RequestParam(name = "expand", defaultValue = "true") boolean expand) {
     return lodgingService.nearby(routeId, dayNo, anchorPoiId, radiusKm, mode, expand, currentUser.id());
 }
}
