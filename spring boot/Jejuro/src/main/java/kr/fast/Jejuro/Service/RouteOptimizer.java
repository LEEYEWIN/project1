package kr.fast.Jejuro.Service;

// [5·6페이지 효율적인 방문 순서 추천]

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 하루 방문 순서 추천 (외부 API 없이 서버에서 계산).
 *
 * 기준
 *  - 1번 방문지는 출발점으로 "고정"한다. (숙소·공항에서 제일 먼저 갈 곳을 사용자가 1번에 둔다)
 *  - fixEnd=true 이면 마지막 방문지도 고정한다. (그날 숙소 근처에서 끝내고 싶을 때)
 *  - 비교 기준은 "방문지 사이 직선거리의 합"이 가장 짧은 순서. 실제 도로 사정·운영 시간·머무는 시간은 보지 않는다.
 *
 * 계산 방법
 *  - 순서를 바꿀 수 있는 곳이 8곳 이하: 가능한 순서를 모두 비교 → 가장 짧은 순서(정확한 답, EXACT)
 *    (8곳이면 40,320가지. 즉시 계산된다)
 *  - 9곳 이상: 최근접 이웃(가장 가까운 곳부터 잇기) → 2-opt(꼬인 구간을 뒤집어 줄어들면 바꾸기) 반복
 *    → 충분히 짧지만 최단이 아닐 수 있음(근사, HEURISTIC)
 */
@Component
public class RouteOptimizer {

    /** 모든 순서를 비교하는 최대 개수 (고정되지 않은 방문지 수) */
    static final int EXACT_LIMIT = 8;

    public enum Method { EXACT, HEURISTIC }

    public record Result(List<GeoPoint> route, Method method) {
    }

    public Result optimize(List<GeoPoint> points, boolean fixEnd) {
        int n = points.size();
        boolean keepEnd = fixEnd && n >= 3;
        if (n <= 2 || (keepEnd && n == 3)) {
            return new Result(new ArrayList<>(points), Method.EXACT); // 바꿀 수 있는 순서가 없음
        }
        GeoPoint start = points.get(0);
        GeoPoint end = keepEnd ? points.get(n - 1) : null;
        List<GeoPoint> middle = new ArrayList<>(points.subList(1, keepEnd ? n - 1 : n));

        if (middle.size() <= EXACT_LIMIT) {
            return new Result(exact(start, middle, end), Method.EXACT);
        }
        return new Result(twoOpt(nearestNeighbor(start, middle, end), keepEnd), Method.HEURISTIC);
    }

    public double totalDistance(List<GeoPoint> route) {
        double sum = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            sum += route.get(i).distanceTo(route.get(i + 1));
        }
        return sum;
    }

    // ---------- 정확한 계산: 가운데 방문지의 모든 순서를 비교 ----------

    private List<GeoPoint> exact(GeoPoint start, List<GeoPoint> middle, GeoPoint end) {
        int m = middle.size();
        int[] order = new int[m];
        for (int i = 0; i < m; i++) order[i] = i;

        int[] best = order.clone();
        double bestDist = cost(start, middle, end, order);
        while (nextPermutation(order)) {
            double d = cost(start, middle, end, order);
            if (d + 1e-6 < bestDist) {
                bestDist = d;
                best = order.clone();
            }
        }
        List<GeoPoint> route = new ArrayList<>();
        route.add(start);
        for (int idx : best) route.add(middle.get(idx));
        if (end != null) route.add(end);
        return route;
    }

    private double cost(GeoPoint start, List<GeoPoint> middle, GeoPoint end, int[] order) {
        double sum = 0;
        GeoPoint prev = start;
        for (int idx : order) {
            GeoPoint p = middle.get(idx);
            sum += prev.distanceTo(p);
            prev = p;
        }
        if (end != null) sum += prev.distanceTo(end);
        return sum;
    }

    /** 사전순 다음 순열. 더 없으면 false */
    private boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) {
            t = a[l]; a[l] = a[r]; a[r] = t;
        }
        return true;
    }

    // ---------- 근사 계산 (9곳 이상) ----------

    private List<GeoPoint> nearestNeighbor(GeoPoint start, List<GeoPoint> middle, GeoPoint end) {
        List<GeoPoint> left = new ArrayList<>(middle);
        List<GeoPoint> route = new ArrayList<>();
        GeoPoint current = start;
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
        if (end != null) route.add(end);
        return route;
    }

    private List<GeoPoint> twoOpt(List<GeoPoint> route, boolean keepEnd) {
        List<GeoPoint> best = new ArrayList<>(route);
        int last = keepEnd ? best.size() - 2 : best.size() - 1; // 뒤집을 수 있는 마지막 위치
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < last; i++) {                    // 0번(출발점)은 움직이지 않음
                for (int k = i + 1; k <= last; k++) {
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
        Collections.reverse(middle);
        result.addAll(middle);
        result.addAll(route.subList(k + 1, route.size()));
        return result;
    }
}
