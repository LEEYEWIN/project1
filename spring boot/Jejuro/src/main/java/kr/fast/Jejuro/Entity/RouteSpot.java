package kr.fast.Jejuro.Entity;


//[5페이지 루트 짜기]

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** N일차의 방문지 한 곳과 방문 순서(1, 2, 3 ...) */
@Entity
@Table(name = "route_spot")
public class RouteSpot {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long routeSpotId;
 private Long routeDayId;
 private Long poiId;
 private Integer visitOrder;

 protected RouteSpot() {
 }

 public RouteSpot(Long routeDayId, Long poiId, int visitOrder) {
     this.routeDayId = routeDayId;
     this.poiId = poiId;
     this.visitOrder = visitOrder;
 }

 public Long getRouteSpotId() { return routeSpotId; }
 public Long getRouteDayId() { return routeDayId; }
 public Long getPoiId() { return poiId; }
 public Integer getVisitOrder() { return visitOrder; }
}