package kr.fast.Jejuro.Entity;


//[6페이지 카카오맵 동선 - 주변 숙소 (FR-26)]

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
* 루트 주변 숙소 안내용 숙소. tools/build_lodging.py로만 채우므로 서비스에서는 조회만 한다.
* sourceId = 출처 접두어 + 원본 ID (TOUR:… 한국관광공사 / VJ:… 비짓제주 / KAKAO:… 카카오)
*/
@Entity
@Table(name = "ACCOMMODATION")
public class Accommodation {

 @Id
 private Long accommodationId;
 private String sourceId;
 private String name;
 private String accommodationType;   // CODE ACCOM_TYPE: HOTEL/RESORT/PENSION/GUESTHOUSE/MOTEL/CAMPING/ETC
 private String address;
 private BigDecimal latitude;
 private BigDecimal longitude;
 private String phone;
 private String imageUrl;

 protected Accommodation() {
 }

 public Long getAccommodationId() { return accommodationId; }
 public String getSourceId() { return sourceId; }
 public String getName() { return name; }
 public String getAccommodationType() { return accommodationType; }
 public String getAddress() { return address; }
 public BigDecimal getLatitude() { return latitude; }
 public BigDecimal getLongitude() { return longitude; }
 public String getPhone() { return phone; }
 public String getImageUrl() { return imageUrl; }
}