package kr.fast.Jejuro.Repository;



//[2페이지 AI 추천 중]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import kr.fast.Jejuro.Entity.PoiSourceMap;

public interface PoiSourceMapRepository extends JpaRepository<PoiSourceMap, String> {

List<PoiSourceMap> findBySourcePoiIdIn(Collection<String> sourcePoiIds);

/** 관리자 관광지 상세: 이 관광지에 연결된 AI 이름·원본 ID */
List<PoiSourceMap> findByPoiIdOrderBySourcePoiId(Long poiId);

/**
* AI 추천에서 뺄 장소의 AI용 이름: 이 회원이 관심없음으로 표시한 곳 + 관리자가 숨기거나 삭제한 곳 + AI 추천 대상이 아닌 곳.
* AI 서버가 점수를 매긴 뒤 이 이름을 빼고 상위 N개를 고르므로, 관심없음이 많아도 보여 주는 개수가 줄지 않는다.
*/
@Query(value = """
       SELECT m.source_poi_id FROM poi_source_map m
       JOIN poi p ON p.poi_id = m.poi_id
       WHERE m.source_poi_id NOT LIKE '%:%'
         AND (p.hidden_at IS NOT NULL OR p.deleted_at IS NOT NULL OR p.ai_recommend = 0
              OR p.poi_id IN (SELECT d.poi_id FROM user_poi_dislike d WHERE d.user_id = :userId))
       """, nativeQuery = true)
List<String> findExcludedSourceIds(@Param("userId") Long userId);

/** 가짜 AI용: 제주 전체에서 무작위 N개 (AI용 이름만. 'VJ:…', 'TOUR:…' 출처 ID는 제외) */
@Query(value = """
       SELECT m.source_poi_id FROM poi_source_map m
       WHERE m.source_poi_id NOT LIKE '%:%'
       ORDER BY RAND() LIMIT :size
       """, nativeQuery = true)
List<String> findRandomSourceIds(@Param("size") int size);

/** 가짜 AI용: 고른 권역 안에서 무작위 N개 (AI용 이름만) */
@Query(value = """
       SELECT m.source_poi_id FROM poi_source_map m
       JOIN poi p ON p.poi_id = m.poi_id
       WHERE p.region_id IN (:regionIds)
         AND m.source_poi_id NOT LIKE '%:%'
       ORDER BY RAND() LIMIT :size
       """, nativeQuery = true)
List<String> findRandomSourceIdsInRegions(@Param("regionIds") Collection<Integer> regionIds,
                                         @Param("size") int size);
}