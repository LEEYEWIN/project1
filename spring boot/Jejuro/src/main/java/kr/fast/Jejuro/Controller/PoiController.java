package kr.fast.Jejuro.Controller;


// [3페이지 추천 목록 · 3-1 전체 관광지 목록]

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.ResponseDTO.PoiPageResponse;
import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.Service.PoiService;

@RestController
@RequestMapping("/api/pois")
public class PoiController {

    private final PoiService poiService;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    /** 3페이지 새로고침 복구용: GET /api/pois?ids=2001,2005,2010 */
    @GetMapping
    public List<PoiSummaryResponse> findByIds(@RequestParam("ids") List<Long> ids) {
        if (ids.size() > 100) {
            throw ApiException.badRequest("한 번에 100개까지 조회할 수 있습니다.");
        }
        return poiService.findSummaries(ids);
    }

    /**
     * 3-1페이지: 전체 관광지 검색
     * GET /api/pois/search?regionId=1&category=BEACH&keyword=해수욕장&page=0&size=12
     * (조건은 모두 생략 가능)
     */
    @GetMapping("/search")
    public PoiPageResponse search(@RequestParam(name = "regionId", required = false) Integer regionId,
                                  @RequestParam(name = "category", required = false) String category,
                                  @RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                  @RequestParam(name = "size", defaultValue = "12") int size) {
        return poiService.search(regionId, category, keyword, page, size);
    }

    /** 3-1페이지: 분류 필터 목록 ["BEACH", "NATURE", ...] */
    @GetMapping("/categories")
    public List<String> categories() {
        return poiService.categories();
    }

    /** 관광지 상세 */
    @GetMapping("/{poiId}")
    public PoiSummaryResponse findOne(@PathVariable("poiId") Long poiId) {
        return poiService.findOne(poiId);
    }
}