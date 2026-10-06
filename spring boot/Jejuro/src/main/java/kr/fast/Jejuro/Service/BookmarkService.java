package kr.fast.Jejuro.Service;



//[4페이지 여행 장소 (화면 문구: 장소 추가 / 장소에서 빼기) — 테이블 이름은 TRAVEL_BOOKMARK 그대로]

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.ResponseDTO.BookmarkResponse;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Repository.PoiRepository;

import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.TravelBookmark;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;
import kr.fast.Jejuro.Repository.TravelRepository;

@Service
public class BookmarkService {

 private final TravelAccessService travelAccessService;
 private final TravelRepository travelRepository;
 private final TravelBookmarkRepository bookmarkRepository;
 private final PoiRepository poiRepository;
 private final PoiService poiService;
 private final RouteService routeService;

 public BookmarkService(TravelAccessService travelAccessService, TravelRepository travelRepository,
                        TravelBookmarkRepository bookmarkRepository,
                        PoiRepository poiRepository, PoiService poiService, RouteService routeService) {
     this.travelAccessService = travelAccessService;
     this.travelRepository = travelRepository;
     this.bookmarkRepository = bookmarkRepository;
     this.poiRepository = poiRepository;
     this.poiService = poiService;
     this.routeService = routeService;
 }

/** 여행 장소 목록 (최근에 추가한 순서) */
@Transactional(readOnly = true)
public List<BookmarkResponse> list(Long travelId, Long userId) {
   travelAccessService.getOwned(travelId, userId);
   List<TravelBookmark> bookmarks = bookmarkRepository.findByTravelIdOrderByBookmarkIdDesc(travelId);

   Map<Long, PoiSummaryResponse> pois = poiService
           .findSummaries(bookmarks.stream().map(TravelBookmark::getPoiId).toList()).stream()
           .collect(Collectors.toMap(PoiSummaryResponse::poiId, Function.identity()));

   return bookmarks.stream()
           .map(b -> new BookmarkResponse(b.getBookmarkId(), pois.get(b.getPoiId())))
           .toList();
}

/**
 * 여행 장소 추가. 이미 추가했으면 409, 수정이 잠긴 여행(확정 후 출발일부터 · 바꾸는 중인 변경 전 여행)이면 409.
 * 확정한 여행에 새로 담은 장소는 경로에 배치해야 확정 일정에 들어간다(화면에서 안내). source = 담은 화면(RECOMMEND/SEARCH)
 */
@Transactional(isolation = Isolation.READ_COMMITTED)   // 여행 행 잠금 뒤 최신 데이터를 읽도록 (REPEATABLE READ면 잠금 전 스냅샷을 읽음)
public BookmarkResponse add(Long travelId, Long userId, Long poiId, String source) {
   routeService.ensureNotLocked(lockOwned(travelId, userId));
   Poi poi = poiRepository.findById(poiId)
           .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
   if (!poi.isAvailable()) {   // 관리자가 숨기거나 삭제한 곳(폐업·정보 오류 등)은 새로 담을 수 없음
       throw new ApiException(HttpStatus.CONFLICT, "지금은 안내하지 않는 관광지라 추가할 수 없어요.");
   }
   if (bookmarkRepository.existsByTravelIdAndPoiId(travelId, poiId)) {
       throw new ApiException(HttpStatus.CONFLICT, "이미 여행 장소에 추가한 관광지입니다.");
   }
   TravelBookmark saved = bookmarkRepository.save(new TravelBookmark(travelId, poiId, source));
   return new BookmarkResponse(saved.getBookmarkId(), poiService.findOne(poiId));
}

/**
* 여행 장소에서 빼기. 경로에 배치되어 있었다면 경로에서도 함께 빠진다
* (경로에는 여행 장소만 들어갈 수 있고, 확정하려면 장소가 모두 배치되어야 하므로).
*/
@Transactional(isolation = Isolation.READ_COMMITTED)   // 여행 행 잠금 뒤 최신 데이터를 읽도록 (REPEATABLE READ면 잠금 전 스냅샷을 읽음)
public void remove(Long travelId, Long userId, Long poiId) {
   Travel travel = lockOwned(travelId, userId);
   routeService.ensureNotLocked(travel);
   routeService.ensureKeepsSpot(travel, poiId);
   int deleted = bookmarkRepository.deleteByTravelIdAndPoiId(travelId, poiId);
   if (deleted == 0) {
       throw ApiException.notFound("여행 장소에 없는 관광지입니다.");
   }
   routeService.removePoiFromRoute(travelId, poiId);
}

/** 여행 행을 잠그고 소유권 확인 (일정 확정·경로 저장과 동시에 장소가 바뀌지 않게) */
private Travel lockOwned(Long travelId, Long userId) {
    return travelRepository.findOwnedForUpdate(travelId, userId)
            .orElseThrow(() -> ApiException.notFound("여행을 찾을 수 없습니다."));
}
}
