package kr.fast.Jejuro.Service;



//[커뮤니티 게시판 - 글 목록·상세·쓰기·수정·삭제·좋아요·조회수]

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.CommunityPostLike;
import kr.fast.Jejuro.Entity.PostType;
import kr.fast.Jejuro.Entity.Report;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelFeedback;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.CommunityCommentRepository;
import kr.fast.Jejuro.Repository.CommunityPostLikeRepository;
import kr.fast.Jejuro.Repository.CommunityPostRepository;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.ReportRepository;
import kr.fast.Jejuro.Repository.TravelFeedbackRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.PostCreateRequest;
import kr.fast.Jejuro.RequestDTO.PostUpdateRequest;
import kr.fast.Jejuro.ResponseDTO.LikeResponse;
import kr.fast.Jejuro.ResponseDTO.PostDetailResponse;
import kr.fast.Jejuro.ResponseDTO.PostPageResponse;
import kr.fast.Jejuro.ResponseDTO.PostSummaryResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;

@Service
public class CommunityService {

 /** 신고 검토 중인 글의 제목 자리 (관리자 확인 전까지) */
 public static final String REPORTED_TITLE = "신고된 게시글입니다";

 public static final int DEFAULT_PAGE_SIZE = 3;
 private static final int MAX_PAGE_SIZE = 20;

 private final CommunityPostRepository postRepository;
 private final CommunityPostLikeRepository likeRepository;
 private final CommunityCommentRepository commentRepository;
 private final CommunityImageService imageService;
 private final UserRepository userRepository;
 private final TravelRepository travelRepository;
 private final TravelFeedbackRepository feedbackRepository;
 private final TravelAccessService travelAccessService;
 private final RouteService routeService;
 private final UserStatusService userStatusService;
 private final ReportRepository reportRepository;
 private final PoiRepository poiRepository;

 public CommunityService(CommunityPostRepository postRepository, CommunityPostLikeRepository likeRepository,
                         CommunityCommentRepository commentRepository, CommunityImageService imageService,
                         UserRepository userRepository, TravelRepository travelRepository,
                         TravelFeedbackRepository feedbackRepository, TravelAccessService travelAccessService,
                         RouteService routeService, UserStatusService userStatusService,
                         ReportRepository reportRepository, PoiRepository poiRepository) {
     this.postRepository = postRepository;
     this.likeRepository = likeRepository;
     this.commentRepository = commentRepository;
     this.imageService = imageService;
     this.userRepository = userRepository;
     this.travelRepository = travelRepository;
     this.feedbackRepository = feedbackRepository;
     this.travelAccessService = travelAccessService;
     this.routeService = routeService;
     this.userStatusService = userStatusService;
     this.reportRepository = reportRepository;
     this.poiRepository = poiRepository;
 }

 // ------------------------------------------------------------------ 목록

 /**
  * 글 목록 (페이지 단위, page는 0부터).
  * sort = "latest"(최신순, 기본) | "likes"(좋아요 많은 순, 같으면 최신순)
  * 작성자 닉네임·좋아요 수·댓글 수·첨부 여행 경로는 글마다 따로 조회하지 않고 한 번에 모아서 붙인다.
  */
 @Transactional(readOnly = true)
 public PostPageResponse list(PostType type, String sort, int page, int size) {
     int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
     int safePage = Math.max(page, 0);
     boolean byLikes = "likes".equalsIgnoreCase(sort);

     List<CommunityPost> posts;
     long total;
     if (byLikes) {
         total = postRepository.countByPostTypeAndDeletedAtIsNullAndBlockReasonIsNull(type);
         List<Long> ids = postRepository.findIdsOrderByLikes(type.name(), safeSize, safePage * safeSize).stream()
                 .map(Number::longValue).toList();
         Map<Long, CommunityPost> byId = postRepository.findAllById(ids).stream()
                 .collect(Collectors.toMap(CommunityPost::getPostId, Function.identity()));
         posts = ids.stream().map(byId::get).filter(Objects::nonNull).toList();   // 좋아요순 순서 유지
     } else {
         Page<CommunityPost> result = postRepository
                 .findByPostTypeAndDeletedAtIsNullAndBlockReasonIsNullOrderByPostIdDesc(type, PageRequest.of(safePage, safeSize));
         posts = result.getContent();
         total = result.getTotalElements();
     }

     List<Long> postIds = posts.stream().map(CommunityPost::getPostId).toList();
     Map<Long, Long> likes = postIds.isEmpty() ? Map.of() : likeRepository.countByPostIds(postIds).stream()
             .collect(Collectors.toMap(CommunityPostLikeRepository.PostCount::getPostId,
                     CommunityPostLikeRepository.PostCount::getCnt));
     Map<Long, Long> comments = postIds.isEmpty() ? Map.of() : commentRepository.countByPostIds(postIds).stream()
             .collect(Collectors.toMap(CommunityCommentRepository.PostCount::getPostId,
                     CommunityCommentRepository.PostCount::getCnt));
     Map<Long, String> names = nicknames(posts.stream().map(CommunityPost::getUserId).toList());
     Map<Long, Travel> travels = travels(posts.stream().map(CommunityPost::getTravelId).toList());

     List<PostSummaryResponse> items = posts.stream()
             .map(p -> {
                 // 신고 검토 중인 글: 목록에는 남기되 제목·사진·경로 대신 "신고된 게시글입니다"
                 boolean reported = p.isHidden();
                 Travel t = reported || p.getTravelId() == null ? null : travels.get(p.getTravelId());
                 return new PostSummaryResponse(p.getPostId(), p.getPostType().name(),
                         reported ? REPORTED_TITLE : p.getTitle(),
                         authorName(p.getUserId(), names), p.getCreatedAt(), p.getViewCount(),
                         likes.getOrDefault(p.getPostId(), 0L), comments.getOrDefault(p.getPostId(), 0L),
                         !reported && p.getImageUrl() != null,
                         t == null ? null : t.getTravelName(),
                         t == null ? null : routeService.adoptedRouteForPublic(t),
                         reported);
             })
             .toList();

     int totalPages = (int) Math.ceil(total / (double) safeSize);
     return new PostPageResponse(items, safePage, safeSize, totalPages, total, byLikes ? "likes" : "latest");
 }

 // ------------------------------------------------------------------ 상세

 /**
  * 글 상세 (조회수는 올리지 않음 → 화면이 한 번만 POST /view 호출).
  * 신고 검토 중인 글: 관리자가 아니면 (작성자 포함) 제목만 "신고된 게시글입니다", 내용·사진·경로는 비움 → hidden=true
  */
 @Transactional(readOnly = true)
 public PostDetailResponse detail(Long postId, Long loginUserId) {
     CommunityPost p = getReadable(postId, loginUserId);
     Map<Long, String> names = nicknames(java.util.Collections.singletonList(p.getUserId()));
     boolean masked = p.isHidden() && !userStatusService.isAdmin(loginUserId);

     Travel t = masked || p.getTravelId() == null ? null : travelRepository.findById(p.getTravelId()).orElse(null);
     RouteDetailResponse route = t == null ? null : routeService.adoptedRouteForPublic(t);
     Integer satisfaction = t == null ? null : feedbackRepository.findByTravelId(t.getTravelId())
             .map(TravelFeedback::getSatisfactionScore).orElse(null);

     return new PostDetailResponse(p.getPostId(), p.getPostType().name(),
             masked ? REPORTED_TITLE : p.getTitle(),
             masked ? null : p.getContent(),
             masked ? null : p.getImageUrl(),
             authorName(p.getUserId(), names), p.isWrittenBy(loginUserId),
             p.getCreatedAt(), p.getUpdatedAt(), p.getViewCount(),
             likeRepository.countByPostId(postId),
             loginUserId != null && likeRepository.existsByPostIdAndUserId(postId, loginUserId),
             commentRepository.countByPostIdAndDeletedAtIsNull(postId),
             t == null ? null : t.getTravelId(),
             t == null ? null : t.getTravelName(), satisfaction, route,
             travelRepository.countBySourcePostId(postId),
             p.isHidden(),
             loginUserId != null && reportRepository.existsByTargetTypeAndTargetIdAndReporterId(Report.POST, postId, loginUserId),
             p.isBlocked() ? ReportPolicy.label(p.getBlockReason()) : null);
 }

 /** 조회수 +1 → 올린 뒤 조회수 */
 @Transactional
 public int increaseView(Long postId) {
     CommunityPost p = getAlive(postId);
     if (p.isHidden()) {
         return p.getViewCount() == null ? 0 : p.getViewCount();   // 신고 검토 중인 글은 조회수를 올리지 않음
     }
     if (postRepository.increaseViewCount(postId) == 0) {
         throw ApiException.notFound("삭제되었거나 없는 글입니다.");
     }
     return postRepository.findViewCount(postId);
 }

 // ------------------------------------------------------------------ 쓰기·수정·삭제

 /**
  * 글 쓰기.
  * travelId를 함께 보내면(후기 글만) 규칙 확인: 내 여행 / 최종 경로 채택됨 / 여행 후기(피드백) 작성됨
  */
 @Transactional
 public Long create(Long userId, PostCreateRequest req) {
     Long travelId = req.travelId();
     if (travelId != null) {
         if (req.postType() != PostType.REVIEW) {
             throw ApiException.badRequest("여행 경로는 후기 글에만 첨부할 수 있습니다.");
         }
         Travel travel = travelAccessService.getOwned(travelId, userId);
         if (travel.getAdoptedRouteId() == null) {
             throw new ApiException(HttpStatus.CONFLICT, "일정을 확정한 여행만 첨부할 수 있습니다.");
         }
         if (!feedbackRepository.existsByTravelId(travelId)) {
             throw new ApiException(HttpStatus.CONFLICT, "여행 후기를 먼저 남겨야 게시판에 경로를 공유할 수 있습니다.");
         }
     }
     String imageUrl = imageService.validateUrl(req.imageUrl());
     return postRepository.save(new CommunityPost(userId, travelId, req.postType(),
             req.title().trim(), req.content().trim(), imageUrl)).getPostId();
 }

 /** 글 수정 (작성자만). 사진을 바꾸거나 지우면 예전 사진 파일도 정리 */
 @Transactional
 public void update(Long postId, Long userId, PostUpdateRequest req) {
     CommunityPost p = getAlive(postId);
     checkOwner(p, userId);
     if (p.isBlocked()) {
         throw new ApiException(HttpStatus.GONE, ReportPolicy.blockedMessage(p.getBlockReason()));
     }
     if (p.isHidden()) {
         throw new ApiException(HttpStatus.CONFLICT, "신고된 게시글은 관리자 확인이 끝난 뒤 수정할 수 있습니다.");
     }
     String oldImage = p.getImageUrl();
     String newImage = imageService.validateUrl(req.imageUrl());
     p.edit(req.title().trim(), req.content().trim(), newImage, LocalDateTime.now());
     // 예전 사진 파일 정리. 관리자가 관광지 사진으로 쓰는 파일이면 지우지 않는다
     if (oldImage != null && !oldImage.equals(newImage) && !poiRepository.existsByImageUrl(oldImage)) {
         imageService.deleteQuietly(oldImage);
     }
 }

 /** 글 삭제 (작성자만). 삭제 표시만 하고, 댓글·좋아요도 함께 조회되지 않는다 */
 @Transactional
 public void delete(Long postId, Long userId) {
     CommunityPost p = getAlive(postId);
     checkOwner(p, userId);
     p.delete(LocalDateTime.now());
 }

 // ------------------------------------------------------------------ 좋아요

 @Transactional
 public LikeResponse like(Long postId, Long userId) {
     if (getReadable(postId, userId).isHidden()) {
         throw ApiException.badRequest("신고된 게시글에는 좋아요를 누를 수 없습니다.");
     }
     if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
         likeRepository.save(new CommunityPostLike(postId, userId));
     }
     return new LikeResponse(true, likeRepository.countByPostId(postId));
 }

 @Transactional
 public LikeResponse unlike(Long postId, Long userId) {
     getAlive(postId);
     likeRepository.deleteOne(postId, userId);
     return new LikeResponse(false, likeRepository.countByPostId(postId));
 }

 // ------------------------------------------------------------------ 공통

 /**
  * 읽을 수 있는 글: 삭제되지 않았고
  *  - 관리자가 차단한 글: 관리자만. 작성자 포함 다른 회원은 410 + "욕설·비방 등의 사유로 차단되었습니다."
  *    (화면은 이 문구를 알림으로 띄우고 목록으로 돌아감)
  *  - 신고 검토 중인 글(hidden_at): 읽을 수는 있지만 detail()에서 "신고된 게시글입니다"로 가린다
  *  - 관리자가 반려하면 hidden_at이 지워져 다시 정상으로 보인다
  */
 public CommunityPost getReadable(Long postId, Long loginUserId) {
     CommunityPost p = getAlive(postId);
     if (p.isBlocked() && !userStatusService.isAdmin(loginUserId)) {
         throw new ApiException(HttpStatus.GONE, ReportPolicy.blockedMessage(p.getBlockReason()));
     }
     return p;
 }

 /** 삭제되지 않은 글 (없거나 삭제됐으면 404) — 댓글 서비스에서도 사용 */
 public CommunityPost getAlive(Long postId) {
     CommunityPost p = postRepository.findById(postId)
             .orElseThrow(() -> ApiException.notFound("삭제되었거나 없는 글입니다."));
     if (p.isDeleted()) {
         throw ApiException.notFound("삭제되었거나 없는 글입니다.");
     }
     return p;
 }

 private void checkOwner(CommunityPost p, Long userId) {
     if (!p.isWrittenBy(userId)) {
         throw new ApiException(HttpStatus.FORBIDDEN, "본인이 쓴 글만 수정·삭제할 수 있습니다.");
     }
 }

 /** 회원 ID → 닉네임 (여러 명 한 번에). 탈퇴 대기(ACTIVE가 아닌) 회원은 바로 "탈퇴한 회원"으로 표시 */
 public Map<Long, String> nicknames(Collection<Long> userIds) {
     List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
     if (ids.isEmpty()) return Map.of();
     return userRepository.findAllById(ids).stream()
             .collect(Collectors.toMap(User::getUserId,
                     u -> "ACTIVE".equals(u.getStatus()) ? u.getNickname() : WITHDRAWN_NAME));
 }

 private static final String WITHDRAWN_NAME = "탈퇴한 회원";

 /** 완전 삭제된 회원은 user_id가 NULL, 탈퇴 대기 회원은 nicknames()에서 이미 "탈퇴한 회원" */
 public static String authorName(Long userId, Map<Long, String> names) {
     return userId == null ? WITHDRAWN_NAME : names.getOrDefault(userId, "알 수 없음");
 }

 private Map<Long, Travel> travels(List<Long> travelIds) {
     List<Long> ids = travelIds.stream().filter(Objects::nonNull).distinct().toList();
     if (ids.isEmpty()) return Map.of();
     return travelRepository.findAllById(ids).stream()
             .collect(Collectors.toMap(Travel::getTravelId, Function.identity()));
 }
}