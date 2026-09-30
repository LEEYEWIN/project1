package kr.fast.Jejuro.Repository;


//[8페이지 후기]

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelFeedback;

public interface TravelFeedbackRepository extends JpaRepository<TravelFeedback, Long> {

 Optional<TravelFeedback> findByTravelId(Long travelId);

 boolean existsByTravelId(Long travelId);

 List<TravelFeedback> findByTravelIdIn(Collection<Long> travelIds);
}