package kr.fast.Jejuro.Repository;


//[8페이지 후기]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.TravelFeedbackReason;

public interface TravelFeedbackReasonRepository extends JpaRepository<TravelFeedbackReason, Long> {

 List<TravelFeedbackReason> findByFeedbackIdOrderByFeedbackReasonId(Long feedbackId);

 @Modifying(clearAutomatically = true, flushAutomatically = true)
 @Query("delete from TravelFeedbackReason r where r.feedbackId = :feedbackId")
 int deleteByFeedbackId(@Param("feedbackId") Long feedbackId);
}