package kr.fast.Jejuro.Repository;


//[8페이지 후기 - 관광지별 결과]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.TravelFeedbackSpot;

public interface TravelFeedbackSpotRepository extends JpaRepository<TravelFeedbackSpot, Long> {

 List<TravelFeedbackSpot> findByFeedbackIdOrderByFeedbackSpotId(Long feedbackId);

 @Modifying(clearAutomatically = true, flushAutomatically = true)
 @Query("delete from TravelFeedbackSpot s where s.feedbackId = :feedbackId")
 int deleteByFeedbackId(@Param("feedbackId") Long feedbackId);
}