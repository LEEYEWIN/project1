package kr.fast.Jejuro.Entity;


//[8페이지 후기 - 관광지별 결과]

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 후기의 관광지 한 곳: 갔어요/못 갔어요 + (간 곳만) 좋았어요/아쉬워요 */
@Entity
@Table(name = "TRAVEL_FEEDBACK_SPOT")
public class TravelFeedbackSpot {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long feedbackSpotId;
 private Long feedbackId;
 private Long poiId;
 private Boolean visited;
 /** LIKE 좋았어요 / DISLIKE 아쉬워요 / null 선택 안 함 */
 private String reaction;

 protected TravelFeedbackSpot() {
 }

 public TravelFeedbackSpot(Long feedbackId, Long poiId, boolean visited, String reaction) {
     this.feedbackId = feedbackId;
     this.poiId = poiId;
     this.visited = visited;
     this.reaction = visited ? reaction : null;
 }

 public Long getFeedbackSpotId() { return feedbackSpotId; }
 public Long getFeedbackId() { return feedbackId; }
 public Long getPoiId() { return poiId; }
 public Boolean getVisited() { return visited; }
 public String getReaction() { return reaction; }
}