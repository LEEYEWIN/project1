import client from './client.js';

/** 5페이지: 빈 경로 만들기 → { routeId } */
export async function createRoute(travelId, routeName = null) {
  const { data } = await client.post(`/travels/${travelId}/routes`, { routeName });
  return data;
}

/** 여행의 경로 목록 */
export async function fetchRoutes(travelId) {
  const { data } = await client.get(`/travels/${travelId}/routes`);
  return data;
}

/** 경로 상세 { routeId, routeName, tripDays, startDate, days: [{ dayNo, date, spots: [{ visitOrder, poi }] }] } */
export async function fetchRoute(routeId) {
  const { data } = await client.get(`/routes/${routeId}`);
  return data;
}

/** 일정 전체 저장. days: [{ dayNo, poiIds: [...] }]  poiIds 순서 = 방문 순서 */
export async function saveRoute(routeId, { routeName, days }) {
  const { data } = await client.put(`/routes/${routeId}`, { routeName, days });
  return data;
}