package kr.fast.Jejuro.Repository;


import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelRegion;

public interface TravelRegionRepository extends JpaRepository<TravelRegion, Long> {

    List<TravelRegion> findByTravelId(Long travelId);

    List<TravelRegion> findByTravelIdIn(Collection<Long> travelIds);
}