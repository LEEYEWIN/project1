package kr.fast.Jejuro.Controller;


// [6페이지 카카오맵 동선]

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.ResponseDTO.DirectionsResponse;
import kr.fast.Jejuro.ResponseDTO.OptimizeResponse;
import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.DirectionsPreviewRequest;
import kr.fast.Jejuro.Service.DirectionsService;
import kr.fast.Jejuro.Service.TravelMode;

@RestController
@RequestMapping("/api")
public class DirectionsController {

    private final DirectionsService directionsService;
    private final CurrentUser currentUser;

    public DirectionsController(DirectionsService directionsService, CurrentUser currentUser) {
        this.directionsService = directionsService;
        this.currentUser = currentUser;
    }

    /** 이동수단별 구간 시간·거리 + 지도에 그릴 선 */
    @GetMapping("/routes/{routeId}/days/{dayNo}/directions")
    public DirectionsResponse directions(@PathVariable("routeId") Long routeId, @PathVariable("dayNo") int dayNo,
                                         @RequestParam(name = "mode", defaultValue = "CAR") TravelMode mode) {
        return directionsService.directions(routeId, dayNo, mode, currentUser.id());
    }

    /** 효율적인 방문 순서 제안(저장은 하지 않음. 수락하면 화면이 PUT /api/routes/{routeId}로 저장) */
    @PostMapping("/routes/{routeId}/days/{dayNo}/optimize")
    public OptimizeResponse optimize(@PathVariable("routeId") Long routeId, @PathVariable("dayNo") int dayNo) {
        return directionsService.optimize(routeId, dayNo, currentUser.id());
    }

    /** 5페이지: 저장 전 순서로 동선·이동시간 미리보기 */
    @PostMapping("/directions/preview")
    public DirectionsResponse preview(@Valid @RequestBody DirectionsPreviewRequest req) {
        return directionsService.preview(req.poiIds(), req.modeOrCar());
    }

    /** 5페이지: 저장 전 순서로 효율적인 순서 제안 */
    @PostMapping("/directions/preview/optimize")
    public OptimizeResponse previewOptimize(@Valid @RequestBody DirectionsPreviewRequest req) {
        return directionsService.previewOptimize(req.poiIds());
    }
}