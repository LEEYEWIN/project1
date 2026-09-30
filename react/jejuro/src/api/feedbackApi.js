import client from './client.js';

/** 8페이지: 저장된 피드백. 없으면 null (서버가 204 응답) */
export async function fetchFeedback(travelId) {
  const res = await client.get(`/travels/${travelId}/feedback`);
  return res.status === 204 ? null : res.data;
}

/** 8페이지: 저장·수정 */
export async function saveFeedback(travelId, payload) {
  const { data } = await client.put(`/travels/${travelId}/feedback`, payload);
  return data;
}