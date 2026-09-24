package kr.fast.Jejuro.Repository;


// [3페이지 추천 목록 · 3-1 전체 관광지 목록 (2~7페이지 공용)]

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.Poi;

public interface PoiRepository extends JpaRepository<Poi, Long> {

    /**
     * 전체 관광지 검색(3-1페이지). 조건이 null이면 그 조건은 빼고 검색한다.
     * regionId: 권역, category: 분류 코드, keyword: 이름 일부
     */
    @Query("""
            select p from Poi p
            where (:regionId is null or p.regionId = :regionId)
              and (:category is null or p.categoryCode = :category)
              and (:keyword is null or p.poiName like concat('%', :keyword, '%'))
            """)
    Page<Poi> search(@Param("regionId") Integer regionId,
                     @Param("category") String category,
                     @Param("keyword") String keyword,
                     Pageable pageable);

    /** 분류 필터에 쓸 분류 코드 목록 */
    @Query("select distinct p.categoryCode from Poi p order by p.categoryCode")
    List<String> findAllCategoryCodes();
}