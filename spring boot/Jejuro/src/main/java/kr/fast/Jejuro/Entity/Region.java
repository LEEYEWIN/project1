package kr.fast.Jejuro.Entity;


//[공통]

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 제주 4개 권역. 조회 전용. */
@Entity
@Table(name = "REGION")
public class Region {

 @Id
 private Integer regionId;
 private String regionCode;
 private String regionName;

 protected Region() {
 }

 public Integer getRegionId() { return regionId; }
 public String getRegionCode() { return regionCode; }
 public String getRegionName() { return regionName; }
}