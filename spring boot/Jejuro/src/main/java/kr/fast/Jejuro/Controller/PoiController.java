package kr.fast.Jejuro.Controller;


//[관광지 목록·검색·상세 (FR-32~34) · 3페이지 추천 목록]

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.ResponseDTO.PoiPageResponse;
import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.ResponseDTO.PoiDetailResponse;
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
  * 관광지 목록·검색 (FR-32, FR-34) — 비회원도 사용
  * GET /api/pois/search?regionId=1&category=NATURE&keyword=오름&page=0&size=12
  * (조건은 모두 생략 가능, 이름 가나다순)
  */
 @GetMapping("/search")
 public PoiPageResponse search(@RequestParam(name = "regionId", required = false) Integer regionId,
                               @RequestParam(name = "category", required = false) String category,
                               @RequestParam(name = "keyword", required = false) String keyword,
                               @RequestParam(name = "page", defaultValue = "0") int page,
                               @RequestParam(name = "size", defaultValue = "12") int size) {
     return poiService.search(regionId, category, keyword, page, size);
 }

 @GetMapping("/discoveries")
 public List<PoiSummaryResponse> discoveries() {
     return poiService.homeDiscoveries();
 }

 /** 관광 유형 필터 목록 ["CULTURE", "NATURE", ...] (실제 관광지가 있는 유형만) */
 @GetMapping("/categories")
 public List<String> categories() {
     return poiService.categories();
 }

 /** 관광지 상세 (FR-33) — 비회원도 사용. 운영 정보 포함 */
 @GetMapping("/{poiId}")
 public PoiDetailResponse detail(@PathVariable("poiId") Long poiId) {
     return poiService.findDetail(poiId);
 }
}