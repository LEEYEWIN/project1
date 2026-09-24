import client from './client.js';

/** 6페이지: N일차 이동 정보. mode = 'CAR' | 'WALK' */
export async function fetchDirections(routeId, dayNo, mode) {
  const { data } = await client.get(`/routes/${routeId}/days/${dayNo}/directions`, { params: { mode } });
  return data;
}

/** 6페이지: 동선 최적화 제안 → { poiIds, beforeDistanceM, afterDistanceM } */
export async function fetchOptimizedOrder(routeId, dayNo) {
  const { data } = await client.post(`/routes/${routeId}/days/${dayNo}/optimize`);
  return data;
}

/** 5페이지: 저장 전 순서(poiIds 순서 그대로)로 동선·이동시간 미리보기 */
export async function previewDirections(poiIds, mode) {
  const { data } = await client.post('/directions/preview', { poiIds, mode });
  return data;
}

/** 5페이지: 저장 전 순서로 효율적인 순서 제안 → { poiIds, beforeDistanceM, afterDistanceM } */
export async function previewOptimize(poiIds) {
  const { data } = await client.post('/directions/preview/optimize', { poiIds });
  return data;
}