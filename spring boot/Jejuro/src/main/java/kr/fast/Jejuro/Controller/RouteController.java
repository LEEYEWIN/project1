package kr.fast.Jejuro.Controller;

//[5페이지 루트 짜기]

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.RouteCreateRequest;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest;
import kr.fast.Jejuro.ResponseDTO.RouteSummaryResponse;
import kr.fast.Jejuro.Service.RouteService;

import jakarta.validation.Valid;

@RestController
public class RouteController {

 private final RouteService routeService;
 private final CurrentUser currentUser;

 public RouteController(RouteService routeService, CurrentUser currentUser) {
     this.routeService = routeService;
     this.currentUser = currentUser;
 }

 /** 새 경로 만들기 → { "routeId": 5001 } */
 @PostMapping("/api/travels/{travelId}/routes")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Long> create(@PathVariable("travelId") Long travelId, @Valid @RequestBody RouteCreateRequest req) {
     return Map.of("routeId", routeService.create(travelId, currentUser.id(), req.routeName()));
 }

 /** 여행의 경로 목록 */
 @GetMapping("/api/travels/{travelId}/routes")
 public List<RouteSummaryResponse> list(@PathVariable("travelId") Long travelId) {
     return routeService.list(travelId, currentUser.id());
 }

 /** 경로 상세 */
 @GetMapping("/api/routes/{routeId}")
 public RouteDetailResponse detail(@PathVariable("routeId") Long routeId) {
     return routeService.detail(routeId, currentUser.id());
 }

 /** 일정 전체 저장 */
 @PutMapping("/api/routes/{routeId}")
 public RouteDetailResponse save(@PathVariable("routeId") Long routeId, @Valid @RequestBody RouteSaveRequest req) {
     return routeService.save(routeId, currentUser.id(), req);
 }
}