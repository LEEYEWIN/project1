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