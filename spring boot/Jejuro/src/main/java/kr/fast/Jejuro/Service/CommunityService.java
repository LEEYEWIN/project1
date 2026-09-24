package kr.fast.Jejuro.Service;


//[8페이지 후기 게시판]

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.PostType;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.CommunityPostRepository;
import kr.fast.Jejuro.Repository.TravelFeedbackRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.PostCreateRequest;
import kr.fast.Jejuro.ResponseDTO.PostResponse;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;

@Service
public class CommunityService {

 private final CommunityPostRepository postRepository;
 private final UserRepository userRepository;
 private final TravelRepository travelRepository;
 private final TravelFeedbackRepository feedbackRepository;
 private final TravelAccessService travelAccessService;
 private final RouteService routeService;

 public CommunityService(CommunityPostRepository postRepository, UserRepository userRepository,
                         TravelRepository travelRepository, TravelFeedbackRepository feedbackRepository,
                         TravelAccessService travelAccessService, RouteService routeService) {
     this.postRepository = postRepository;
     this.userRepository = userRepository;
     this.travelRepository = travelRepository;
     this.feedbackRepository = feedbackRepository;
     this.travelAccessService = travelAccessService;
     this.routeService = routeService;
 }

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
             throw new ApiException(HttpStatus.CONFLICT, "최종 경로를 채택한 여행만 첨부할 수 있습니다.");
         }
         if (!feedbackRepository.existsByTravelId(travelId)) {
             throw new ApiException(HttpStatus.CONFLICT, "여행 후기를 먼저 남겨야 게시판에 경로를 공유할 수 있습니다.");
         }
     }
     return postRepository.save(new CommunityPost(userId, travelId, req.postType(),
             req.title().trim(), req.content().trim())).getPostId();
 }

 /** 최근 50개. 작성자 닉네임·첨부 여행은 한 번에 조회해서 붙인다(N+1 방지). 경로는 첨부된 글만 조회. */
 @Transactional(readOnly = true)
 public List<PostResponse> latest(PostType type, Long loginUserId) {
     List<CommunityPost> posts = postRepository.findTop50ByPostTypeOrderByPostIdDesc(type);

     List<Long> userIds = posts.stream().map(CommunityPost::getUserId).filter(Objects::nonNull).distinct().toList();
     Map<Long, String> names = userRepository.findAllById(userIds).stream()
             .collect(Collectors.toMap(User::getUserId, User::getNickname));

     List<Long> travelIds = posts.stream().map(CommunityPost::getTravelId).filter(Objects::nonNull).distinct().toList();
     Map<Long, Travel> travels = travelRepository.findAllById(travelIds).stream()
             .collect(Collectors.toMap(Travel::getTravelId, Function.identity()));
     Map<Long, RouteDetailResponse> routes = travels.values().stream()
             .map(t -> Map.entry(t.getTravelId(), routeService.adoptedRouteForPublic(t)))
             .filter(e -> e.getValue() != null)
             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

     return posts.stream()
             .map(p -> {
                 Travel t = p.getTravelId() == null ? null : travels.get(p.getTravelId());
                 return new PostResponse(p.getPostId(), p.getPostType().name(), p.getTitle(), p.getContent(),
                         p.getUserId() == null ? "탈퇴한 회원" : names.getOrDefault(p.getUserId(), "알 수 없음"),
                         Objects.equals(p.getUserId(), loginUserId), p.getCreatedAt(),
                         t == null ? null : t.getTravelName(),
                         t == null ? null : routes.get(t.getTravelId()));
             })
             .toList();
 }
}