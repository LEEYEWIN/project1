package kr.fast.Jejuro.Controller;


//[관심없음 관광지 관리]

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.ResponseDTO.DislikeResponse;
import kr.fast.Jejuro.Service.DislikeService;

/** 로그인 회원의 관심없음 관광지 (다음 AI 추천에서 제외) */
@RestController
@RequestMapping("/api/me/dislikes")
public class DislikeController {

 private final DislikeService dislikeService;
 private final CurrentUser currentUser;

 public DislikeController(DislikeService dislikeService, CurrentUser currentUser) {
     this.dislikeService = dislikeService;
     this.currentUser = currentUser;
 }

 /** 목록 [{ poi, createdAt }] */
 @GetMapping
 public List<DislikeResponse> list() {
     return dislikeService.list(currentUser.id());
 }

 /** 관심없음 표시 { "poiId": 12 } */
 @PostMapping
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void add(@RequestBody Map<String, Long> body) {
     Long poiId = body.get("poiId");
     if (poiId == null) {
         throw ApiException.badRequest("poiId가 필요합니다.");
     }
     dislikeService.add(currentUser.id(), poiId);
 }

 /** 관심없음 해제 */
 @DeleteMapping("/{poiId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void remove(@PathVariable("poiId") Long poiId) {
     dislikeService.remove(currentUser.id(), poiId);
 }
}