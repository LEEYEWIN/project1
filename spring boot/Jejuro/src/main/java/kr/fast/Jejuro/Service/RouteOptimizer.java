package kr.fast.Jejuro.Service;


//[6페이지 카카오맵 동선]

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
* 하루 방문 순서 최적화 (외부 API 없이 계산).
* 1) 최근접 이웃: 첫 방문지에서 출발해 가장 가까운 곳을 차례로 고른다.
* 2) 2-opt: 두 구간을 뒤집어 보고 총거리가 줄면 바꾸기를 반복한다.
* 첫 방문지(숙소·공항 등)는 출발점으로 고정한다. 방문지 10여 곳이면 즉시 계산된다.
*/
@Component
public class RouteOptimizer {

 public List<GeoPoint> optimize(List<GeoPoint> points) {
     if (points.size() <= 3) {
         return nearestNeighbor(points);
     }
     return twoOpt(nearestNeighbor(points));
 }

 public double totalDistance(List<GeoPoint> route) {
     double sum = 0;
     for (int i = 0; i < route.size() - 1; i++) {
         sum += route.get(i).distanceTo(route.get(i + 1));
     }
     return sum;
 }

 private List<GeoPoint> nearestNeighbor(List<GeoPoint> points) {
     List<GeoPoint> left = new ArrayList<>(points);
     List<GeoPoint> route = new ArrayList<>();
     GeoPoint current = left.remove(0); // 출발점 고정
     route.add(current);
     while (!left.isEmpty()) {
         GeoPoint nearest = left.get(0);
         for (GeoPoint p : left) {
             if (current.distanceTo(p) < current.distanceTo(nearest)) {
                 nearest = p;
             }
         }
         left.remove(nearest);
         route.add(nearest);
         current = nearest;
     }
     return route;
 }

 private List<GeoPoint> twoOpt(List<GeoPoint> route) {
     List<GeoPoint> best = new ArrayList<>(route);
     boolean improved = true;
     while (improved) {
         improved = false;
         for (int i = 1; i < best.size() - 1; i++) {        // 0번(출발점)은 움직이지 않음
             for (int k = i + 1; k < best.size(); k++) {
                 List<GeoPoint> candidate = reverse(best, i, k);
                 if (totalDistance(candidate) + 1 < totalDistance(best)) { // 1m 이상 줄 때만
                     best = candidate;
                     improved = true;
                 }
             }
         }
     }
     return best;
 }

 private List<GeoPoint> reverse(List<GeoPoint> route, int i, int k) {
     List<GeoPoint> result = new ArrayList<>(route.subList(0, i));
     List<GeoPoint> middle = new ArrayList<>(route.subList(i, k + 1));
     java.util.Collections.reverse(middle);
     result.addAll(middle);
     result.addAll(route.subList(k + 1, route.size()));
     return result;
 }
}