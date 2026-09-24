package kr.fast.Jejuro.Controller;


//[4페이지 찜]

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.BookmarkRequest;
import kr.fast.Jejuro.ResponseDTO.BookmarkResponse;
import kr.fast.Jejuro.Service.BookmarkService;

@RestController
@RequestMapping("/api/travels/{travelId}/bookmarks")
public class BookmarkController {

 private final BookmarkService bookmarkService;
 private final CurrentUser currentUser;

 public BookmarkController(BookmarkService bookmarkService, CurrentUser currentUser) {
     this.bookmarkService = bookmarkService;
     this.currentUser = currentUser;
 }

 @GetMapping
 public List<BookmarkResponse> list(@PathVariable("travelId") Long travelId) {
     return bookmarkService.list(travelId, currentUser.id());
 }

 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 public BookmarkResponse add(@PathVariable("travelId") Long travelId, @Valid @RequestBody BookmarkRequest req) {
     return bookmarkService.add(travelId, currentUser.id(), req.poiId());
 }

 @DeleteMapping("/{poiId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void remove(@PathVariable("travelId") Long travelId, @PathVariable("poiId") Long poiId) {
     bookmarkService.remove(travelId, currentUser.id(), poiId);
 }
}