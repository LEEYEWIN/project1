package kr.fast.Jejuro.Repository;


//[4페이지 찜 (5페이지 검사에도 사용)]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import kr.fast.Jejuro.Entity.TravelBookmark;

public interface TravelBookmarkRepository extends JpaRepository<TravelBookmark, Long> {

 List<TravelBookmark> findByTravelIdOrderByBookmarkIdDesc(Long travelId);

 boolean existsByTravelIdAndPoiId(Long travelId, Long poiId);

 @Query("select b.poiId from TravelBookmark b where b.travelId = :travelId")
 List<Long> findPoiIds(@Param("travelId") Long travelId);

 @Modifying
 @Query("delete from TravelBookmark b where b.travelId = :travelId and b.poiId = :poiId")
 int deleteByTravelIdAndPoiId(@Param("travelId") Long travelId, @Param("poiId") Long poiId);
}