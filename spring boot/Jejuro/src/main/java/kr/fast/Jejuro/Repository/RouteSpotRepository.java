package kr.fast.Jejuro.Repository;


//[5페이지 루트 짜기]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.RouteSpot;

public interface RouteSpotRepository extends JpaRepository<RouteSpot, Long> {
 List<RouteSpot> findByRouteDayIdInOrderByRouteDayIdAscVisitOrderAsc(Collection<Long> routeDayIds);

 List<RouteSpot> findByRouteDayIdOrderByVisitOrder(Long routeDayId);
}