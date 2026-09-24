package kr.fast.Jejuro.Controller;

// [1·7페이지 여행 (설문 / 목록·상세·채택·삭제)]

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.AdoptRouteRequest;
import kr.fast.Jejuro.RequestDTO.TravelCreateRequest;
import kr.fast.Jejuro.ResponseDTO.TravelCreateResponse;
import kr.fast.Jejuro.ResponseDTO.TravelDetailResponse;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse;
import kr.fast.Jejuro.ResponseDTO.TravelSummaryResponse;
import kr.fast.Jejuro.Service.TravelFormService;
import kr.fast.Jejuro.Service.TravelQueryService;
import kr.fast.Jejuro.Service.TravelService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TravelController {

    private final TravelFormService travelFormService;
    private final TravelService travelService;
    private final TravelQueryService travelQueryService;
    private final CurrentUser currentUser;

    public TravelController(TravelFormService travelFormService, TravelService travelService,
                            TravelQueryService travelQueryService, CurrentUser currentUser) {
        this.travelFormService = travelFormService;
        this.travelService = travelService;
        this.travelQueryService = travelQueryService;
        this.currentUser = currentUser;
    }

    /** 1페이지 진입 시: 권역·코드·설문 질문/선택지 */
    @GetMapping("/travel-form")
    public TravelFormResponse getForm() {
        return travelFormService.getForm();
    }

    /** 1페이지 제출: 여행 + 설문 저장 → travelId 반환 */
    @PostMapping("/travels")
    @ResponseStatus(HttpStatus.CREATED)
    public TravelCreateResponse create(@Valid @RequestBody TravelCreateRequest req) {
        Long travelId = travelService.create(currentUser.id(), req);
        return new TravelCreateResponse(travelId);
    }

    /** 7페이지: 내 여행 목록 */
    @GetMapping("/travels")
    public List<TravelSummaryResponse> list() {
        return travelQueryService.list(currentUser.id());
    }

    /** 7페이지: 여행 상세 */
    @GetMapping("/travels/{travelId}")
    public TravelDetailResponse detail(@PathVariable("travelId") Long travelId) {
        return travelQueryService.detail(travelId, currentUser.id());
    }

    /** 7페이지: 최종 경로 채택·해제 */
    @PatchMapping("/travels/{travelId}/adopted-route")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adopt(@PathVariable("travelId") Long travelId, @RequestBody AdoptRouteRequest req) {
        travelService.adoptRoute(travelId, currentUser.id(), req.routeId());
    }

    /** 7페이지: 여행 삭제 */
    @DeleteMapping("/travels/{travelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("travelId") Long travelId) {
        travelService.delete(travelId, currentUser.id());
    }
}