package kr.fast.Jejuro.Service;


//[4페이지 여행 장소 (화면 문구: 장소 추가 / 장소에서 빼기) — 테이블 이름은 TRAVEL_BOOKMARK 그대로]

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.ResponseDTO.BookmarkResponse;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Repository.PoiRepository;

import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Entity.TravelBookmark;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;

@Service
public class BookmarkService {

 private final TravelAccessService travelAccessService;
 private final TravelBookmarkRepository bookmarkRepository;
 private final PoiRepository poiRepository;
 private final PoiService poiService;
 private final RouteService routeService;

 public BookmarkService(TravelAccessService travelAccessService, TravelBookmarkRepository bookmarkRepository,
                        PoiRepository poiRepository, PoiService poiService, RouteService routeService) {
     this.travelAccessService = travelAccessService;
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

 /** 여행 장소 추가. 이미 추가했으면 409, 일정을 확정한 여행이면 409. source = 담은 화면(RECOMMEND/SEARCH) */
 @Transactional
 public BookmarkResponse add(Long travelId, Long userId, Long poiId, String source) {
     routeService.ensureNotLocked(travelAccessService.getOwned(travelId, userId));
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
 @Transactional
 public void remove(Long travelId, Long userId, Long poiId) {
     routeService.ensureNotLocked(travelAccessService.getOwned(travelId, userId));
     int deleted = bookmarkRepository.deleteByTravelIdAndPoiId(travelId, poiId);
     if (deleted == 0) {
         throw ApiException.notFound("여행 장소에 없는 관광지입니다.");
     }
     routeService.removePoiFromRoute(travelId, poiId);
 }
}