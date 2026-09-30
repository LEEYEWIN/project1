package kr.fast.Jejuro.Entity;


//[4페이지 여행 장소 (예전 이름: 찜)]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TRAVEL_BOOKMARK")
public class TravelBookmark {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long bookmarkId;
 private Long travelId;
 private Long poiId;

 /** 어디서 담았나: RECOMMEND(AI 추천) / SEARCH(관광지 검색) / IMPORT(경로 가져오기) — 관리자 KPI용 */
 private String source;

 @Column(insertable = false, updatable = false)
 private LocalDateTime createdAt;

 public static final String SOURCE_RECOMMEND = "RECOMMEND";
 public static final String SOURCE_SEARCH = "SEARCH";
 public static final String SOURCE_IMPORT = "IMPORT";

 protected TravelBookmark() {
 }

 public TravelBookmark(Long travelId, Long poiId, String source) {
     this.travelId = travelId;
     this.poiId = poiId;
     this.source = source;
 }

 public Long getBookmarkId() { return bookmarkId; }
 public Long getTravelId() { return travelId; }
 public Long getPoiId() { return poiId; }
 public String getSource() { return source; }
 public LocalDateTime getCreatedAt() { return createdAt; }
}