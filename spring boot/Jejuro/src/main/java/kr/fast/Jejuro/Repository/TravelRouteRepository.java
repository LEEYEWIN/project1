package kr.fast.Jejuro.Repository;

// [5페이지 루트 짜기]

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelRoute;

public interface TravelRouteRepository extends JpaRepository<TravelRoute, Long> {

    List<TravelRoute> findByTravelIdOrderByRouteIdDesc(Long travelId);

    List<TravelRoute> findByTravelIdIn(Collection<Long> travelIds);

    /** 여행의 경로(여행당 1개). migration_09 이전 데이터에 여러 개가 남아 있어도 최신 1개만 쓴다. */
    Optional<TravelRoute> findFirstByTravelIdOrderByRouteIdDesc(Long travelId);
}
