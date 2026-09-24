package kr.fast.Jejuro.Repository;


//[5페이지 루트 짜기]

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.RouteDay;

public interface RouteDayRepository extends JpaRepository<RouteDay, Long> {

 List<RouteDay> findByRouteIdOrderByDayNo(Long routeId);

 List<RouteDay> findByRouteIdIn(Collection<Long> routeIds);

 Optional<RouteDay> findByRouteIdAndDayNo(Long routeId, Integer dayNo);

 /**
  * 경로의 일차를 한 번에 삭제한다. ROUTE_SPOT은 DB의 ON DELETE CASCADE로 함께 지워진다.
  * clearAutomatically: 삭제 후 JPA 1차 캐시를 비워 지워진 엔티티가 남지 않게 한다.
  */
 @Modifying(clearAutomatically = true, flushAutomatically = true)
 @Query("delete from RouteDay d where d.routeId = :routeId")
 int deleteByRouteId(@Param("routeId") Long routeId);
}