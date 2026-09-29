package kr.fast.Jejuro.Service;


//[커뮤니티 게시판 - 댓글·대댓글]

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.CommunityComment;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.Report;
import kr.fast.Jejuro.Repository.CommunityCommentRepository;
import kr.fast.Jejuro.Repository.ReportRepository;
import kr.fast.Jejuro.RequestDTO.CommentCreateRequest;
import kr.fast.Jejuro.RequestDTO.CommentUpdateRequest;
import kr.fast.Jejuro.ResponseDTO.CommentResponse;

/**
* 댓글 규칙
* - 대댓글은 한 단계만: 대댓글에 답글을 달면 같은 원댓글 아래에 붙는다.
* - 삭제는 표시만(deletedAt). 대댓글이 남아 있는 원댓글은 "삭제된 댓글입니다."로 자리만 남기고,
*   그렇지 않은 삭제 댓글은 목록에서 뺀다. 댓글 수는 삭제되지 않은 댓글만 센다.
* - 수정·삭제는 댓글 작성자만 (글 작성자라도 남의 댓글은 못 고침).
* - 신고로 가려진 댓글은 작성자·관리자만 내용을 보고, 다른 회원에게는 "신고로 가려진 댓글입니다."
* - 이용 정지 회원은 댓글 쓰기·수정 불가.
*/
@Service
public class CommunityCommentService {

 private final CommunityCommentRepository commentRepository;
 private final CommunityService communityService;
 private final UserStatusService userStatusService;
 private final ReportRepository reportRepository;

 public CommunityCommentService(CommunityCommentRepository commentRepository, CommunityService communityService,
                                UserStatusService userStatusService, ReportRepository reportRepository) {
     this.commentRepository = commentRepository;
     this.communityService = communityService;
     this.userStatusService = userStatusService;
     this.reportRepository = reportRepository;
 }

 /** 글의 댓글 목록: 원댓글 오래된 순, 각 원댓글 아래 대댓글 오래된 순 */
 @Transactional(readOnly = true)
 public List<CommentResponse> list(Long postId, Long loginUserId) {
     communityService.getReadable(postId, loginUserId);
     List<CommunityComment> all = commentRepository.findByPostIdOrderByCommentIdAsc(postId);
     Map<Long, String> names = communityService.nicknames(all.stream().map(CommunityComment::getUserId).toList());
     View view = new View(loginUserId, userStatusService.isAdmin(loginUserId),
             loginUserId == null || all.isEmpty() ? Set.of() : new HashSet<>(reportRepository.findReportedTargetIds(
                     Report.COMMENT, loginUserId, all.stream().map(CommunityComment::getCommentId).toList())));

     Map<Long, List<CommentResponse>> repliesOf = new LinkedHashMap<>();
     for (CommunityComment c : all) {
         if (c.isReply() && !c.isDeleted()) {
             repliesOf.computeIfAbsent(c.getParentCommentId(), k -> new ArrayList<>())
                     .add(toResponse(c, view, names, List.of()));
         }
     }

     List<CommentResponse> result = new ArrayList<>();
     for (CommunityComment c : all) {
         if (c.isReply()) continue;
         List<CommentResponse> replies = repliesOf.getOrDefault(c.getCommentId(), List.of());
         if (c.isDeleted() && replies.isEmpty()) continue;       // 지워졌고 답글도 없으면 숨김
         result.add(toResponse(c, view, names, replies));
     }
     return result;
 }

 @Transactional
 public Long create(Long postId, Long userId, CommentCreateRequest req) {
     userStatusService.checkCanWrite(userId);
     CommunityPost post = communityService.getReadable(postId, userId);
     if (post.isHidden()) {
         throw ApiException.badRequest("신고로 가려진 글에는 댓글을 달 수 없습니다.");
     }
     Long parentId = null;
     if (req.parentCommentId() != null) {
         CommunityComment parent = commentRepository.findById(req.parentCommentId())
                 .filter(c -> c.getPostId().equals(postId))
                 .orElseThrow(() -> ApiException.notFound("답글을 달 댓글을 찾을 수 없습니다."));
         if (parent.isDeleted()) {
             throw ApiException.badRequest("삭제된 댓글에는 답글을 달 수 없습니다.");
         }
         if (parent.isHidden()) {
             throw ApiException.badRequest("신고로 가려진 댓글에는 답글을 달 수 없습니다.");
         }
         parentId = parent.isReply() ? parent.getParentCommentId() : parent.getCommentId();   // 한 단계만
     }
     return commentRepository.save(new CommunityComment(postId, parentId, userId, req.content().trim()))
             .getCommentId();
 }

 @Transactional
 public void update(Long commentId, Long userId, CommentUpdateRequest req) {
     CommunityComment c = getOwnedAlive(commentId, userId);
     if (c.isBlocked()) {
         throw ApiException.badRequest("관리자가 차단한 댓글은 수정할 수 없습니다.");
     }
     userStatusService.checkCanWrite(userId);
     c.edit(req.content().trim(), LocalDateTime.now());
 }

 @Transactional
 public void delete(Long commentId, Long userId) {
     CommunityComment c = getOwnedAlive(commentId, userId);
     c.delete(LocalDateTime.now());
 }

 private CommunityComment getOwnedAlive(Long commentId, Long userId) {
     CommunityComment c = commentRepository.findById(commentId)
             .filter(x -> !x.isDeleted())
             .orElseThrow(() -> ApiException.notFound("삭제되었거나 없는 댓글입니다."));
     communityService.getAlive(c.getPostId());     // 지워진 글의 댓글은 수정·삭제 불가
     if (!c.isWrittenBy(userId)) {
         throw new ApiException(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 수정·삭제할 수 있습니다.");
     }
     return c;
 }

 /** 보는 사람: 로그인 회원, 관리자 여부, 이미 신고한 댓글 번호 */
 private record View(Long loginUserId, boolean admin, Set<Long> reported) {
 }

 private CommentResponse toResponse(CommunityComment c, View v, Map<Long, String> names,
                                    List<CommentResponse> replies) {
     if (c.isDeleted()) {
         return new CommentResponse(c.getCommentId(), c.getParentCommentId(), null, null,
                 c.getCreatedAt(), null, false, true, false, false, null, replies);
     }
     boolean mine = c.isWrittenBy(v.loginUserId());
     boolean reported = v.reported().contains(c.getCommentId());
     String blockLabel = c.isBlocked() ? ReportPolicy.label(c.getBlockReason()) : null;
     // 차단: 관리자만 내용 확인 (작성자 포함) / 자동 가림: 작성자·관리자만
     if ((c.isBlocked() && !v.admin()) || (c.isHidden() && !mine && !v.admin())) {
         return new CommentResponse(c.getCommentId(), c.getParentCommentId(), null, null,
                 c.getCreatedAt(), null, false, false, true, reported, blockLabel, replies);
     }
     return new CommentResponse(c.getCommentId(), c.getParentCommentId(),
             CommunityService.authorName(c.getUserId(), names), c.getContent(),
             c.getCreatedAt(), c.getUpdatedAt(), mine, false, c.isHidden(), reported, blockLabel, replies);
 }
}