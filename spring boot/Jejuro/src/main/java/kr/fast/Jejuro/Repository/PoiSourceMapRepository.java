package kr.fast.Jejuro.Repository;


// [2페이지 AI 추천 중]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import kr.fast.Jejuro.Entity.PoiSourceMap;

public interface PoiSourceMapRepository extends JpaRepository<PoiSourceMap, String> {

    List<PoiSourceMap> findBySourcePoiIdIn(Collection<String> sourcePoiIds);

    /** 가짜 AI용: 제주 전체에서 무작위 N개 */
    @Query(value = """
            SELECT m.source_poi_id FROM POI_SOURCE_MAP m
            ORDER BY RAND() LIMIT :size
            """, nativeQuery = true)
    List<String> findRandomSourceIds(@Param("size") int size);

    /** 가짜 AI용: 고른 권역 안에서 무작위 N개 */
    @Query(value = """
            SELECT m.source_poi_id FROM POI_SOURCE_MAP m
            JOIN POI p ON p.poi_id = m.poi_id
            WHERE p.region_id IN (:regionIds)
            ORDER BY RAND() LIMIT :size
            """, nativeQuery = true)
    List<String> findRandomSourceIdsInRegions(@Param("regionIds") Collection<Integer> regionIds,
                                              @Param("size") int size);
}