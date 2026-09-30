package kr.fast.Jejuro.Service;


// [관심없음 관광지 - AI 추천 제외]

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
import kr.fast.Jejuro.ResponseDTO.RecommendedPoiResponse;

/**
 * 관심없음 관광지 (USER_POI_DISLIKE)
 * - 회원 단위: 한 번 표시하면 내 모든 여행의 다음 AI 추천에서 빠진다.
 * - 관광지 검색·상세에서는 그대로 보인다(직접 찾아 담는 것은 막지 않음).
 * - 관심없음 관리 화면(/dislikes)에서 해제하면 다시 추천될 수 있다. 해제한 뒤 다시 표시하는 것도 몇 번이든 가능
 *   (표시 = 행 추가 INSERT IGNORE, 해제 = 행 삭제 → 상태는 "행이 있나 없나" 하나뿐)
 * - 관리 화면에는 이전에 추천받은 관광지(RECOMMEND_ITEM.shown = 1)도 함께 보여 줘서 거기서 바로 표시·해제할 수 있다.
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

    /**
     * 이전에 추천받은 관광지 (내 모든 여행, 화면에 보여 준 10곳 기준, 최근 추천 순, 최대 100곳)
     * + 지금 관심없음인지 표시. 삭제된 관광지는 뺀다.
     */
    @Transactional(readOnly = true)
    public List<RecommendedPoiResponse> recommended(Long userId) {
        record Row(Long poiId, java.time.LocalDateTime lastAt, long times) {
        }
        List<Row> rows = jdbc.query("""
                SELECT i.poi_id, MAX(r.created_at) AS last_at, COUNT(DISTINCT r.request_id) AS times
                  FROM RECOMMEND_ITEM i
                  JOIN RECOMMEND_REQUEST r ON r.request_id = i.request_id
                  JOIN TRAVEL t ON t.travel_id = r.travel_id
                  JOIN POI p ON p.poi_id = i.poi_id
                 WHERE t.user_id = ? AND i.shown = 1 AND p.deleted_at IS NULL
                 GROUP BY i.poi_id
                 ORDER BY last_at DESC, i.poi_id DESC
                 LIMIT 100
                """, (rs, n) -> new Row(rs.getLong("poi_id"), rs.getTimestamp("last_at").toLocalDateTime(), rs.getLong("times")),
                userId);
        Set<Long> disliked = ids(userId);
        Map<Long, PoiSummaryResponse> pois = poiService.findSummaries(rows.stream().map(Row::poiId).toList()).stream()
                .collect(Collectors.toMap(PoiSummaryResponse::poiId, Function.identity()));
        return rows.stream()
                .filter(r -> pois.containsKey(r.poiId()))
                .map(r -> new RecommendedPoiResponse(pois.get(r.poiId()), disliked.contains(r.poiId()), r.lastAt(), r.times()))
                .toList();
    }

    /** 추천에서 뺄 관광지 번호 */
    @Transactional(readOnly = true)
    public Set<Long> ids(Long userId) {
        return new HashSet<>(jdbc.query("SELECT poi_id FROM USER_POI_DISLIKE WHERE user_id = ?",
                (rs, n) -> rs.getLong(1), userId));
    }
}