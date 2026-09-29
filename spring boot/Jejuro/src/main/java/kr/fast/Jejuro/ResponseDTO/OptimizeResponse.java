package kr.fast.Jejuro.ResponseDTO;

// [5·6페이지 효율적인 방문 순서 추천]

import java.util.List;

/**
 * 방문 순서 추천 결과. poiIds 순서대로 방문하면 직선거리 합이 before → after 로 줄어든다.
 * - method: EXACT(가능한 순서를 모두 비교한 최단) / HEURISTIC(9곳 이상: 근사)
 * - fixedStartName: 출발점으로 고정한 1번 방문지
 * - fixedEndName: 마지막 방문지도 고정했을 때 그 이름 (고정 안 했으면 null)
 * - comparedCount: 비교한 순서의 수 (EXACT일 때), HEURISTIC이면 0
 */
public record OptimizeResponse(
        List<Long> poiIds,
        int beforeDistanceM,
        int afterDistanceM,
        String method,
        String fixedStartName,
        String fixedEndName,
        long comparedCount) {
}
