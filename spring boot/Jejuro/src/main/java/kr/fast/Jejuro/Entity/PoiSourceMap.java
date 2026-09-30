package kr.fast.Jejuro.Entity;


//[2페이지 AI 추천 중]

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** AI·원본 데이터의 관광지 ID(source_poi_id) → 서비스 poi_id 변환표 */
@Entity
@Table(name = "poi_source_map")
public class PoiSourceMap {

 @Id
 private String sourcePoiId;
 private Long poiId;

 protected PoiSourceMap() {
 }

 public String getSourcePoiId() { return sourcePoiId; }
 public Long getPoiId() { return poiId; }
}