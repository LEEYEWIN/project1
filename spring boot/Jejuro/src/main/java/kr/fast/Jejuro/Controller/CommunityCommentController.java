package kr.fast.Jejuro.Controller;


//[커뮤니티 게시판 - 댓글·대댓글]

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.CommentCreateRequest;
import kr.fast.Jejuro.RequestDTO.CommentUpdateRequest;
import kr.fast.Jejuro.ResponseDTO.CommentResponse;
import kr.fast.Jejuro.Service.CommunityCommentService;

@RestController
@RequestMapping("/api/community")
public class CommunityCommentController {

 private final CommunityCommentService commentService;
 private final CurrentUser currentUser;

 public CommunityCommentController(CommunityCommentService commentService, CurrentUser currentUser) {
     this.commentService = commentService;
     this.currentUser = currentUser;
 }

 /** 댓글 목록 (대댓글은 각 댓글의 replies 안에) */
 @GetMapping("/posts/{postId}/comments")
 public List<CommentResponse> list(@PathVariable("postId") Long postId) {
     return commentService.list(postId, currentUser.id());
 }

 /** 댓글·대댓글 쓰기 → { "commentId": 31 }. 대댓글은 body에 parentCommentId */
 @PostMapping("/posts/{postId}/comments")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Long> create(@PathVariable("postId") Long postId, @Valid @RequestBody CommentCreateRequest req) {
     return Map.of("commentId", commentService.create(postId, currentUser.id(), req));
 }

 /** 댓글 수정 (작성자만) */
 @PutMapping("/comments/{commentId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void update(@PathVariable("commentId") Long commentId, @Valid @RequestBody CommentUpdateRequest req) {
     commentService.update(commentId, currentUser.id(), req);
 }

 /** 댓글 삭제 (작성자만) */
 @DeleteMapping("/comments/{commentId}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable("commentId") Long commentId) {
     commentService.delete(commentId, currentUser.id());
 }
}
