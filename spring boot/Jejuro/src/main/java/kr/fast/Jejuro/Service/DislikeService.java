package kr.fast.Jejuro.Service;


//[관심없음 관광지 - AI 추천 제외]

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Poi;
import kr.fast.Jejuro.Repository.PoiRepository;
import kr.fast.Jejuro.ResponseDTO.DislikeResponse;
import kr.fast.Jejuro.ResponseDTO.PoiSummaryResponse;

/**
* 관심없음 관광지 (USER_POI_DISLIKE)
* - 회원 단위: 한 번 표시하면 내 모든 여행의 다음 AI 추천에서 빠진다.
* - 관광지 검색·상세에서는 그대로 보인다(직접 찾아 담는 것은 막지 않음).
* - 관심없음 관리 화면(/dislikes)에서 해제하면 다시 추천될 수 있다.
*/
@Service
public class DislikeService {

 private final JdbcTemplate jdbc;
 private final PoiRepository poiRepository;
 private final PoiService poiService;

 public DislikeService(JdbcTemplate jdbc, PoiRepository poiRepository, PoiService poiService) {
     this.jdbc = jdbc;
     this.poiRepository = poiRepository;
     this.poiService = poiService;
 }

 /** 관심없음 표시 (이미 표시했으면 그대로) */
 @Transactional
 public void add(Long userId, Long poiId) {
     Poi poi = poiRepository.findById(poiId)
             .orElseThrow(() -> ApiException.notFound("관광지를 찾을 수 없습니다."));
     if (poi.isDeleted()) {
         throw ApiException.notFound("삭제된 관광지입니다.");
     }
     jdbc.update("INSERT IGNORE INTO USER_POI_DISLIKE (user_id, poi_id) VALUES (?, ?)", userId, poiId);
 }

 /** 관심없음 해제 */
 @Transactional
 public void remove(Long userId, Long poiId) {
     jdbc.update("DELETE FROM USER_POI_DISLIKE WHERE user_id = ? AND poi_id = ?", userId, poiId);
 }

 /** 관심없음 목록 (최근에 표시한 순) */
 @Transactional(readOnly = true)
 public List<DislikeResponse> list(Long userId) {
     record Row(Long poiId, java.time.LocalDateTime createdAt) {
     }
     List<Row> rows = jdbc.query(
             "SELECT poi_id, created_at FROM USER_POI_DISLIKE WHERE user_id = ? ORDER BY created_at DESC, poi_id DESC",
             (rs, n) -> new Row(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime()), userId);
     Map<Long, PoiSummaryResponse> pois = poiService.findSummaries(rows.stream().map(Row::poiId).toList()).stream()
             .collect(Collectors.toMap(PoiSummaryResponse::poiId, Function.identity()));
     return rows.stream()
             .filter(r -> pois.containsKey(r.poiId()))
             .map(r -> new DislikeResponse(pois.get(r.poiId()), r.createdAt()))
             .toList();
 }

 /** 추천에서 뺄 관광지 번호 */
 @Transactional(readOnly = true)
 public Set<Long> ids(Long userId) {
     return new HashSet<>(jdbc.query("SELECT poi_id FROM USER_POI_DISLIKE WHERE user_id = ?",
             (rs, n) -> rs.getLong(1), userId));
 }
}