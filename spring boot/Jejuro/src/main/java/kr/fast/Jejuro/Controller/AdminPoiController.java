package kr.fast.Jejuro.Controller;


//[관리자 관광지 관리]

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.AdminGuard;
import kr.fast.Jejuro.RequestDTO.AdminPoiRequest;
import kr.fast.Jejuro.RequestDTO.PoiMappingRequest;
import kr.fast.Jejuro.ResponseDTO.AdminPoiResponse;
import kr.fast.Jejuro.Service.AdminPoiService;

/** 관리자 전용 (USER.role = ADMIN). 아니면 403 */
@RestController
@RequestMapping("/api/admin/pois")
public class AdminPoiController {

 private final AdminPoiService poiService;
 private final AdminGuard adminGuard;

 public AdminPoiController(AdminPoiService poiService, AdminGuard adminGuard) {
     this.poiService = poiService;
     this.adminGuard = adminGuard;
 }

 /** 폼 선택지: 권역·분류 */
 @GetMapping("/options")
 public AdminPoiResponse.Options options() {
     adminGuard.check();
     return poiService.options();
 }

 /** GET /api/admin/pois?keyword=&regionId=&category=&visibility=ALL|VISIBLE|HIDDEN|DELETED&issue=AI|MANUAL|NO_IMAGE|NO_DESC|OUT_OF_JEJU&page=0 */
 @GetMapping
 public AdminPoiResponse list(@RequestParam(name = "keyword", required = false) String keyword,
                              @RequestParam(name = "regionId", required = false) Integer regionId,
                              @RequestParam(name = "category", required = false) String category,
                              @RequestParam(name = "visibility", defaultValue = "ALL") String visibility,
                              @RequestParam(name = "issue", required = false) String issue,
                              @RequestParam(name = "page", defaultValue = "0") int page) {
     adminGuard.check();
     return poiService.list(keyword, regionId, category, visibility, issue, page);
 }

 /** 사진 점검용: 삭제하지 않은 관광지 중 사진 주소가 있는 곳 전부 [{ poiId, poiName, imageUrl, hidden }] (깨진 사진은 화면이 직접 불러 보며 찾음) */
 @GetMapping("/images")
 public java.util.List<AdminPoiResponse.ImageRow> images() {
     adminGuard.check();
     return poiService.images();
 }

 @GetMapping("/{poiId}")
 public AdminPoiResponse.Detail detail(@PathVariable("poiId") Long poiId) {
     adminGuard.check();
     return poiService.detail(poiId);
 }

 /** 새 관광지 → { "poiId": 537 } */
 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Long> create(@Valid @RequestBody AdminPoiRequest req) {
     adminGuard.check();
     return Map.of("poiId", poiService.create(req));
 }

 @PutMapping("/{poiId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void update(@PathVariable("poiId") Long poiId, @Valid @RequestBody AdminPoiRequest req) {
     adminGuard.check();
     poiService.update(poiId, req);
 }

 /** 숨기기/다시 보이기 { "hidden": true } */
 @PutMapping("/{poiId}/hidden")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void hidden(@PathVariable("poiId") Long poiId, @RequestBody Map<String, Boolean> body) {
     adminGuard.check();
     poiService.setHidden(poiId, Boolean.TRUE.equals(body.get("hidden")));
 }

 /** 삭제 (행은 남기고 여행·경로·후기에는 "확인 불가") */
 @DeleteMapping("/{poiId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable("poiId") Long poiId) {
     adminGuard.check();
     poiService.delete(poiId);
 }

 /** 삭제 취소 */
 @PostMapping("/{poiId}/restore")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void restore(@PathVariable("poiId") Long poiId) {
     adminGuard.check();
     poiService.restore(poiId);
 }

 /** AI 이름 연결 추가 { "sourcePoiId": "성산일출봉" } */
 @PostMapping("/{poiId}/mappings")
 @ResponseStatus(HttpStatus.CREATED)
 public void addMapping(@PathVariable("poiId") Long poiId, @Valid @RequestBody PoiMappingRequest req) {
     adminGuard.check();
     poiService.addMapping(poiId, req.sourcePoiId());
 }

 /** AI 이름 연결 빼기: DELETE /api/admin/pois/12/mappings?sourcePoiId=성산일출봉 (이름에 / 가 있을 수 있어 쿼리로) */
 @DeleteMapping("/{poiId}/mappings")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void removeMapping(@PathVariable("poiId") Long poiId, @RequestParam("sourcePoiId") String sourcePoiId) {
     adminGuard.check();
     poiService.removeMapping(poiId, sourcePoiId);
 }
}