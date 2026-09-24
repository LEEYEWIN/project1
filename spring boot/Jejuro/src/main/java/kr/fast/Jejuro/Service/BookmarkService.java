package kr.fast.Jejuro.Service;


//[4페이지 찜]

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
import kr.fast.Jejuro.Entity.TravelBookmark;
import kr.fast.Jejuro.Repository.TravelBookmarkRepository;

@Service
public class BookmarkService {

 private final TravelAccessService travelAccessService;
 private final TravelBookmarkRepository bookmarkRepository;
 private final PoiRepository poiRepository;
 private final PoiService poiService;

 public BookmarkService(TravelAccessService travelAccessService, TravelBookmarkRepository bookmarkRepository,
                        PoiRepository poiRepository, PoiService poiService) {
     this.travelAccessService = travelAccessService;
     this.bookmarkRepository = bookmarkRepository;
     this.poiRepository = poiRepository;
     this.poiService = poiService;
 }

 /** 찜 목록 (최근에 찜한 순서) */
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

 /** 찜 추가. 이미 찜했으면 409 */
 @Transactional
 public BookmarkResponse add(Long travelId, Long userId, Long poiId) {
     travelAccessService.getOwned(travelId, userId);
     if (!poiRepository.existsById(poiId)) {
         throw ApiException.notFound("관광지를 찾을 수 없습니다.");
     }
     if (bookmarkRepository.existsByTravelIdAndPoiId(travelId, poiId)) {
         throw new ApiException(HttpStatus.CONFLICT, "이미 찜한 관광지입니다.");
     }
     TravelBookmark saved = bookmarkRepository.save(new TravelBookmark(travelId, poiId));
     return new BookmarkResponse(saved.getBookmarkId(), poiService.findOne(poiId));
 }

 /** 찜 취소. 이미 저장된 루트의 방문지는 지우지 않는다(루트 편집 화면에서 따로 정리). */
 @Transactional
 public void remove(Long travelId, Long userId, Long poiId) {
     travelAccessService.getOwned(travelId, userId);
     int deleted = bookmarkRepository.deleteByTravelIdAndPoiId(travelId, poiId);
     if (deleted == 0) {
         throw ApiException.notFound("찜하지 않은 관광지입니다.");
     }
 }
}