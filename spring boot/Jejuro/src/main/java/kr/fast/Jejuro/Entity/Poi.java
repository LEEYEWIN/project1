package kr.fast.Jejuro.Entity;


//[3페이지 추천 목록 (2~7페이지 공용)]

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 관광지 마스터. 원본 데이터 적재로만 채우므로 서비스에서는 조회만 한다. */
@Entity
@Table(name = "POI")
public class Poi {

 @Id
 private Long poiId;
 private String poiName;
 private String address;
 private BigDecimal latitude;
 private BigDecimal longitude;
 private String categoryCode;
 private Integer regionId;
 private String description;          // 한 줄 소개 (카드)
 private String detailDescription;    // 세부 설명 (카드의 "자세히" 펼치기)
 private String imageUrl;

 protected Poi() {
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
}