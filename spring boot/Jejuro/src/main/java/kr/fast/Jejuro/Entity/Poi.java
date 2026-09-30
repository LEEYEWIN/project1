package kr.fast.Jejuro.Entity;


//[3페이지 추천 목록 (2~7페이지 공용) · 관리자 관광지 관리]

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 관광지 마스터. 대부분 원본 데이터 적재(poi_list.sql)로 채우고, 관리자 화면에서 고치거나 새로 추가한다.
* 행은 지우지 않는다: 이미 여행에 담긴 곳·추천 기록·후기가 이 관광지를 참조하기 때문.
*  - 숨김(hiddenAt): 검색·AI 추천·새로 담기에서 빠짐. 이미 담긴 여행에는 그대로 보임 (다시 보이기 가능)
*  - 삭제(deletedAt): 위와 같이 빠지고, 여행 장소·경로·후기에는 "확인 불가"로 표시, 상세 화면도 막음 (관리자가 복구 가능)
*/
@Entity
@DynamicUpdate   // 바뀐 칼럼만 UPDATE (숨김과 정보 수정이 동시에 일어나도 서로 덮어쓰지 않게)
@Table(name = "POI")
public class Poi {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)   // DB poi_id AUTO_INCREMENT (관리자가 새로 추가할 때)
 private Long poiId;
 private String poiName;
 private String address;
 private BigDecimal latitude;
 private BigDecimal longitude;
 private String categoryCode;
 private Integer regionId;
 private String description;          // 한 줄 소개 (카드)
 private String detailDescription;    // 세부 설명 (상세 화면)
 private String imageUrl;
 // 운영 정보 (없으면 null → 화면 "정보 없음")
 private String phone;
 private String homepage;
 private String openingHours;
 private String closedDays;
 private String fee;
 private String parking;
 /** 관리자가 숨긴 시각 (검색·AI 추천·새로 담기에서 제외) */
 private LocalDateTime hiddenAt;
 /** 관리자가 삭제한 시각 (화면에는 확인 불가) */
 private LocalDateTime deletedAt;
 /**
  * AI 추천 대상 (AI가 학습한 275곳만 true). false = AI 추천에는 나오지 않고 검색해서 직접 담기만 가능.
  * 관리자가 새로 추가한 관광지는 false (모델이 모르는 곳). 값은 DB(migration_14)에서만 바꾼다.
  */
 @Column(name = "ai_recommend", nullable = false)
 private boolean aiRecommend;

 /** 삭제된 관광지를 여행 장소·경로·후기에 보여 줄 이름 */
 public static final String UNAVAILABLE_NAME = "확인 불가 (삭제된 관광지)";

 protected Poi() {
 }

 /** 관리자 새 관광지 */
 public Poi(Fields f) {
     apply(f);
 }

 /** 관리자 수정 */
 public void update(Fields f) {
     apply(f);
 }

 private void apply(Fields f) {
     this.poiName = f.poiName();
     this.address = f.address();
     this.latitude = f.latitude();
     this.longitude = f.longitude();
     this.categoryCode = f.categoryCode();
     this.regionId = f.regionId();
     this.description = f.description();
     this.detailDescription = f.detailDescription();
     this.imageUrl = f.imageUrl();
     this.phone = f.phone();
     this.homepage = f.homepage();
     this.openingHours = f.openingHours();
     this.closedDays = f.closedDays();
     this.fee = f.fee();
     this.parking = f.parking();
 }

 public void hide(LocalDateTime now) {
     if (hiddenAt == null) hiddenAt = now;
 }

 public void show() {
     hiddenAt = null;
 }

 public boolean isHidden() { return hiddenAt != null; }

 public void delete(LocalDateTime now) {
     if (deletedAt == null) deletedAt = now;
 }

 public void restore() {
     deletedAt = null;
 }

 public boolean isDeleted() { return deletedAt != null; }

 /** 검색·추천·새로 담기에 쓸 수 있는가 (숨김·삭제 아님) */
 public boolean isAvailable() { return hiddenAt == null && deletedAt == null; }

 /** 화면에 보일 이름: 삭제된 곳은 "확인 불가" */
 public String displayName() { return isDeleted() ? UNAVAILABLE_NAME : poiName; }

 /** 관리자 입력값 묶음 (정리·검사는 AdminPoiService에서 끝낸 값) */
 public record Fields(String poiName, String address, BigDecimal latitude, BigDecimal longitude,
                      String categoryCode, Integer regionId, String description, String detailDescription,
                      String imageUrl, String phone, String homepage, String openingHours, String closedDays,
                      String fee, String parking) {
 }

 public Long getPoiId() { return poiId; }
 public String getPoiName() { return poiName; }
 public String getAddress() { return address; }
 public BigDecimal getLatitude() { return latitude; }
 public BigDecimal getLongitude() { return longitude; }
 public String getCategoryCode() { return categoryCode; }
 public Integer getRegionId() { return regionId; }
 public String getDescription() { return description; }
 public String getDetailDescription() { return detailDescription; }
 public String getImageUrl() { return imageUrl; }
 public String getPhone() { return phone; }
 public String getHomepage() { return homepage; }
 public String getOpeningHours() { return openingHours; }
 public String getClosedDays() { return closedDays; }
 public String getFee() { return fee; }
 public String getParking() { return parking; }
 public LocalDateTime getHiddenAt() { return hiddenAt; }
 public LocalDateTime getDeletedAt() { return deletedAt; }
 public boolean isAiRecommend() { return aiRecommend; }
}