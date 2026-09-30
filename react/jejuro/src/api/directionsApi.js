import client from './client.js';

/** 6페이지: N일차 이동 정보. mode = 'CAR' | 'WALK' */
export async function fetchDirections(routeId, dayNo, mode) {
  const { data } = await client.get(`/routes/${routeId}/days/${dayNo}/directions`, { params: { mode } });
  return data;
}

/** 5페이지: 저장 전 순서(poiIds 순서 그대로)로 동선·이동시간 미리보기 */
export async function previewDirections(poiIds, mode) {
  const { data } = await client.post('/directions/preview', { poiIds, mode });
  return data;
}

/**
 * 5페이지: 효율적인 방문 순서 추천 (1번 방문지는 출발점으로 고정)
 * fixEnd=true 이면 마지막 방문지도 고정
 * → { poiIds, beforeDistanceM, afterDistanceM, method: 'EXACT'|'HEURISTIC', fixedStartName, fixedEndName, comparedCount }
 */
export async function previewOptimize(poiIds, fixEnd = false) {
  const { data } = await client.post('/directions/preview/optimize', { poiIds, fixEnd });
  return data;
}
