package kr.fast.Jejuro.Entity;


//[5페이지 루트 짜기]

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 경로의 N일차. 날짜는 저장하지 않고 여행 시작일 + (dayNo-1)로 계산한다. */
@Entity
@Table(name = "route_day")
public class RouteDay {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long routeDayId;
 private Long routeId;
 private Integer dayNo;
 private Integer primaryRegionId;

 protected RouteDay() {
 }

 public RouteDay(Long routeId, int dayNo, int primaryRegionId) {
     this.routeId = routeId;
     this.dayNo = dayNo;
     this.primaryRegionId = primaryRegionId;
 }

 public Long getRouteDayId() { return routeDayId; }
 public Long getRouteId() { return routeId; }
 public Integer getDayNo() { return dayNo; }
 public Integer getPrimaryRegionId() { return primaryRegionId; }
}