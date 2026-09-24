package kr.fast.Jejuro.Entity;


//[5페이지 루트 짜기]

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 여행 경로 한 개. 실제 일정은 RouteDay/RouteSpot에 있다. */
@Entity
@Table(name = "travel_route")
public class TravelRoute {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long routeId;
 private Long travelId;
 private String routeName;

 @Column(insertable = false, updatable = false)
 private LocalDateTime createdAt;

 protected TravelRoute() {
 }

 public TravelRoute(Long travelId, String routeName) {
     this.travelId = travelId;
     this.routeName = routeName;
 }

 public void rename(String routeName) {
     this.routeName = routeName;
 }

 public Long getRouteId() { return routeId; }
 public Long getTravelId() { return travelId; }
 public String getRouteName() { return routeName; }
 public LocalDateTime getCreatedAt() { return createdAt; }
}