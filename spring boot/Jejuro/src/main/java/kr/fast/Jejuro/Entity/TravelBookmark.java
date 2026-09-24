package kr.fast.Jejuro.Entity;


//[4페이지 찜]

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel_bookmark")
public class TravelBookmark {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long bookmarkId;
 private Long travelId;
 private Long poiId;

 protected TravelBookmark() {
 }

 public TravelBookmark(Long travelId, Long poiId) {
     this.travelId = travelId;
     this.poiId = poiId;
 }

 public Long getBookmarkId() { return bookmarkId; }
 public Long getTravelId() { return travelId; }
 public Long getPoiId() { return poiId; }
}