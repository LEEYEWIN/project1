package kr.fast.Jejuro.Service;



//[커뮤니티 - 다른 사람 경로를 내 새 여행으로 가져오기]

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.CommunityPost;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Entity.TravelBookmark;
import kr.fast.Jejuro.Entity.TravelRoute;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.CommunityPostRepository;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;
import kr.fast.Jejuro.Repository.TravelRepository;
import kr.fast.Jejuro.Repository.TravelRouteRepository;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.RequestDTO.RouteImportRequest;
import kr.fast.Jejuro.RequestDTO.RouteSaveRequest.DayReq;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;

/**
* 커뮤니티 글에 첨부된 "최종 경로"를 내 새 여행으로 복사한다.
*  1) 새 여행: 입력한 이름·시작일, 종료일 = 시작일 + (원래 일수 - 1), 권역 = 제주 전체
*     설문을 거치지 않으므로 이 여행은 AI 추천을 받지 않는다(TRAVEL.source_post_id로 표시)
*  2) 여행 장소: 경로에 들어 있는 관광지를 모두 추가 (관리자가 숨긴 관광지는 빼고)
*  3) 경로: 일차·방문 순서를 그대로 복사. 확정(채택)은 하지 않으므로 가져온 뒤 자유롭게 고칠 수 있다.
*/
@Service
public class RouteImportService {

private final CommunityPostRepository postRepository;
private final TravelRepository travelRepository;
private final UserRepository userRepository;
private final TravelBookmarkRepository bookmarkRepository;
private final TravelRouteRepository routeRepository;
private final RouteService routeService;
private final PoiRepository poiRepository;

public RouteImportService(CommunityPostRepository postRepository, TravelRepository travelRepository,
                         UserRepository userRepository, TravelBookmarkRepository bookmarkRepository,
                         TravelRouteRepository routeRepository, RouteService routeService,
                         PoiRepository poiRepository) {
   this.postRepository = postRepository;
   this.travelRepository = travelRepository;
   this.userRepository = userRepository;
   this.bookmarkRepository = bookmarkRepository;
   this.routeRepository = routeRepository;
   this.routeService = routeService;
   this.poiRepository = poiRepository;
}

/** 가져오기 → 새 여행 번호 */
@Transactional
public Long importToNewTravel(Long postId, Long userId, RouteImportRequest req) {
   CommunityPost post = postRepository.findById(postId)
           .filter(p -> !p.isDeleted() && !p.isHidden())   // 신고로 가려진 글은 가져올 수 없음
           .orElseThrow(() -> ApiException.notFound("삭제되었거나 없는 글입니다."));
   Travel source = post.getTravelId() == null ? null : travelRepository.findById(post.getTravelId()).orElse(null);
   RouteDetailResponse route = source == null ? null : routeService.adoptedRouteForPublic(source);
   if (route == null || route.days().isEmpty()) {
       throw ApiException.badRequest("가져올 경로가 없는 글입니다.");
   }
   // 관리자가 숨긴 관광지(폐업 등)는 가져오지 않는다
   Set<Long> allIds = new HashSet<>();
   route.days().forEach(d -> d.spots().forEach(s -> allIds.add(s.poi().poiId())));
   Set<Long> hidden = allIds.isEmpty() ? Set.of() : new HashSet<>(poiRepository.findHiddenIds(allIds));
   if (!allIds.isEmpty() && hidden.containsAll(allIds)) {
       throw ApiException.badRequest("이 경로의 관광지가 모두 지금은 안내하지 않는 곳이라 가져올 수 없어요.");
   }

   // 1) 새 여행 (회원 행을 잠가 여행 순번이 겹치지 않게)
   User user = userRepository.findByIdForUpdate(userId)
           .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없습니다."));
   if (!"ACTIVE".equals(user.getStatus())) {
       throw new ApiException(HttpStatus.FORBIDDEN, "탈퇴 처리 중인 계정입니다.");
   }
   int tripDays = source.tripDays();
   Travel travel = travelRepository.save(Travel.createImported(userId, travelRepository.nextTravelNo(userId),
           req.travelName().trim(), req.startDate(), req.startDate().plusDays(tripDays - 1L),
           AgeGroup.of(user.getBirthDate(), req.startDate()), postId));
   Long travelId = travel.getTravelId();

   // 2) 일정 복사용 데이터 + 여행 장소 (경로 순서대로, 중복 없이)
   Set<Long> poiIds = new LinkedHashSet<>();
   List<DayReq> days = route.days().stream()
           .map(d -> new DayReq(d.dayNo(), d.spots().stream()
                   .map(s -> s.poi().poiId())
                   .filter(id -> !hidden.contains(id))
                   .filter(poiIds::add)                  // 여러 날에 같은 곳이 있으면 처음 한 번만
                   .toList()))
           .toList();
   bookmarkRepository.saveAll(poiIds.stream().map(id -> new TravelBookmark(travelId, id, TravelBookmark.SOURCE_IMPORT)).toList());

   // 3) 경로 (여행당 1개)
   String routeName = route.routeName() != null ? route.routeName() : req.travelName().trim();
   TravelRoute newRoute = routeRepository.save(new TravelRoute(travelId, routeName));
   routeService.writeImportedDays(newRoute.getRouteId(), routeName, days);
   return travelId;
}

/** 이 글의 경로를 가져가 만든 여행 수 */
@Transactional(readOnly = true)
public long importCount(Long postId) {
   return travelRepository.countBySourcePostId(postId);
}
}