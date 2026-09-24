package kr.fast.Jejuro.ResponseDTO;


//[6페이지 카카오맵 동선]

import java.util.List;

/** 동선 최적화 제안. poiIds 순서대로 방문하면 총 직선거리가 before → after 로 줄어든다. */
public record OptimizeResponse(
     List<Long> poiIds,
     int beforeDistanceM,
     int afterDistanceM) {
}