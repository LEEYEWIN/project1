package kr.fast.Jejuro.Repository;

// [5페이지 루트 짜기]

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelRoute;

public interface TravelRouteRepository extends JpaRepository<TravelRoute, Long> {

    List<TravelRoute> findByTravelIdOrderByRouteIdDesc(Long travelId);

    List<TravelRoute> findByTravelIdIn(Collection<Long> travelIds);
}