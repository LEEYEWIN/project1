package kr.fast.Jejuro.Controller;


//[커뮤니티 게시판 - 글·좋아요·조회수·사진]

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.Entity.PostType;
import kr.fast.Jejuro.RequestDTO.PostCreateRequest;
import kr.fast.Jejuro.RequestDTO.PostUpdateRequest;
import kr.fast.Jejuro.ResponseDTO.LikeResponse;
import kr.fast.Jejuro.ResponseDTO.PostDetailResponse;
import kr.fast.Jejuro.ResponseDTO.PostPageResponse;
import kr.fast.Jejuro.Service.CommunityImageService;
import kr.fast.Jejuro.Service.CommunityService;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

 private final CommunityService communityService;
 private final CommunityImageService imageService;
 private final CurrentUser currentUser;

 public CommunityController(CommunityService communityService, CommunityImageService imageService,
                            CurrentUser currentUser) {
     this.communityService = communityService;
     this.imageService = imageService;
     this.currentUser = currentUser;
 }

 /** 목록: GET /api/community/posts?type=REVIEW&sort=latest|likes&page=0&size=3 */
 @GetMapping("/posts")
 public PostPageResponse list(@RequestParam(name = "type", defaultValue = "REVIEW") PostType type,
                              @RequestParam(name = "sort", defaultValue = "latest") String sort,
                              @RequestParam(name = "page", defaultValue = "0") int page,
                              @RequestParam(name = "size", defaultValue = "3") int size) {
     return communityService.list(type, sort, page, size);
 }

 /** 상세 */
 @GetMapping("/posts/{postId}")
 public PostDetailResponse detail(@PathVariable("postId") Long postId) {
     return communityService.detail(postId, currentUser.id());
 }

 /** 조회수 +1 → { "viewCount": 129 } (화면이 상세에 들어갈 때 한 번만 호출) */
 @PostMapping("/posts/{postId}/view")
 public Map<String, Integer> view(@PathVariable("postId") Long postId) {
     return Map.of("viewCount", communityService.increaseView(postId));
 }

 /** 글 쓰기 → { "postId": 12 } */
 @PostMapping("/posts")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Long> create(@Valid @RequestBody PostCreateRequest req) {
     return Map.of("postId", communityService.create(currentUser.id(), req));
 }

 /** 글 수정 (작성자만) */
 @PutMapping("/posts/{postId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void update(@PathVariable("postId") Long postId, @Valid @RequestBody PostUpdateRequest req) {
     communityService.update(postId, currentUser.id(), req);
 }

 /** 글 삭제 (작성자만) */
 @DeleteMapping("/posts/{postId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable("postId") Long postId) {
     communityService.delete(postId, currentUser.id());
 }

 /** 좋아요 누르기 → { "liked": true, "likeCount": 25 } */
 @PostMapping("/posts/{postId}/like")
 public LikeResponse like(@PathVariable("postId") Long postId) {
     return communityService.like(postId, currentUser.id());
 }

 /** 좋아요 취소 */
 @DeleteMapping("/posts/{postId}/like")
 public LikeResponse unlike(@PathVariable("postId") Long postId) {
     return communityService.unlike(postId, currentUser.id());
 }

 /** 사진 올리기 (multipart, 필드 이름 image) → { "imageUrl": "/api/community/images/…jpg" } */
 @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, String> upload(@RequestPart("image") MultipartFile image) {
     currentUser.id();   // 로그인 회원만
     return Map.of("imageUrl", imageService.store(image));
 }

 /** 사진 보기 (img 태그의 src) */
 @GetMapping("/images/{fileName}")
 public ResponseEntity<Resource> image(@PathVariable("fileName") String fileName) {
     Resource file = imageService.load(fileName);
     return ResponseEntity.ok()
             .contentType(MediaType.parseMediaType(imageService.contentTypeOf(fileName)))
             .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
             .body(file);
 }
}