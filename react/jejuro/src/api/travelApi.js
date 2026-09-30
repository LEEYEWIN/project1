import client from './client.js';

/** 1페이지: 권역·코드·설문 질문/선택지 */
export async function fetchTravelForm() {
  const { data } = await client.get('/travel-form');
  return data;
}

/** 1페이지: 여행 + 설문 저장 → { travelId } */
export async function createTravel(payload) {
  const { data } = await client.post('/travels', payload);
  return data;
}

/** 7페이지: 내 여행 목록 */
export async function fetchMyTravels() {
  const { data } = await client.get('/travels');
  return data;
}

/** 7페이지: 여행 상세 */
export async function fetchTravelDetail(travelId) {
  const { data } = await client.get(`/travels/${travelId}`);
  return data;
}

/** 7페이지: 최종 경로 채택(routeId) / 해제(null) */
export async function adoptRoute(travelId, routeId) {
  await client.patch(`/travels/${travelId}/adopted-route`, { routeId });
}

/** 7페이지: 여행 삭제 */
export async function deleteTravel(travelId) {
  await client.delete(`/travels/${travelId}`);
}