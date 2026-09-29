package kr.fast.Jejuro.Controller;

// [5페이지 루트 짜기 - 여행당 경로 1개, 자동 저장]

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.CurrentUser;
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

    /**
     * 여행의 경로 번호 → { "routeId": 5001 }
     * 여행당 경로는 1개: 없으면 새로 만들고, 있으면 그 경로를 돌려준다(여러 번 불러도 하나만 생김).
     */
    @PostMapping("/api/travels/{travelId}/routes")
    public Map<String, Long> ensure(@PathVariable("travelId") Long travelId) {
        return Map.of("routeId", routeService.ensureRoute(travelId, currentUser.id()));
    }

    /** 여행의 경로 목록 (0개 또는 1개) */
    @GetMapping("/api/travels/{travelId}/routes")
    public List<RouteSummaryResponse> list(@PathVariable("travelId") Long travelId) {
        return routeService.list(travelId, currentUser.id());
    }

    /** 경로 상세 */
    @GetMapping("/api/routes/{routeId}")
    public RouteDetailResponse detail(@PathVariable("routeId") Long routeId) {
        return routeService.detail(routeId, currentUser.id());
    }

    /** 일정 전체 저장 (화면의 자동 저장) */
    @PutMapping("/api/routes/{routeId}")
    public RouteDetailResponse save(@PathVariable("routeId") Long routeId, @Valid @RequestBody RouteSaveRequest req) {
        return routeService.save(routeId, currentUser.id(), req);
    }
}
