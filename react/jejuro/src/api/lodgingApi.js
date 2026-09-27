import client from './client.js';

/**
 * 6페이지: N일차 주변 숙소 (FR-26)
 * options: { anchorPoiId, radiusKm, mode: 'CAR'|'WALK', expand }
 *  - anchorPoiId 없음 → 그날 마지막 관광지 (마지막 날은 status 'LAST_DAY')
 *  - radiusKm 없음 → 자동차 3km / 도보 1km, expand=true 이면 5곳 미만일 때 5km → 10km로 넓힘
 * → { status, message, dayNo, date, anchor, anchorLabel, requestedRadiusKm, radiusKm, expanded, items }
 */
export async function fetchLodgings(routeId, dayNo, { anchorPoiId, radiusKm, mode, expand = true } = {}) {
  const params = { mode, expand };
  if (anchorPoiId) params.anchorPoiId = anchorPoiId;
  if (radiusKm) params.radiusKm = radiusKm;
  const { data } = await client.get(`/routes/${routeId}/days/${dayNo}/lodgings`, { params });
  return data;
}