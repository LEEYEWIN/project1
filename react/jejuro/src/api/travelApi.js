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

/**
 * 7페이지: 일정 확정(최종 경로 채택)
 * deleteOverlapping=true: 기간이 겹치는 변경 전 여행("날짜·동행 바꿔 다시 만들기")을 삭제하고 확정
 */
export async function adoptRoute(travelId, routeId, deleteOverlapping = false) {
  await client.patch(`/travels/${travelId}/adopted-route`, { routeId, deleteOverlapping });
}

/** 여행 상세 "날짜·동행 바꿔 다시 만들기": 여행 만들기 화면에 채울 기존 값 (TravelCreateRequest 모양) */
export async function fetchTravelCopy(travelId) {
  const { data } = await client.get(`/travels/${travelId}/copy`);
  return data;
}

/** 7페이지: 여행 삭제 */
export async function deleteTravel(travelId) {
  await client.delete(`/travels/${travelId}`);
}