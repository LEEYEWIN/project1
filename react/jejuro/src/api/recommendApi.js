import client from './client.js';

/** 2페이지: AI 추천 요청 → { travelId, pois: [...] }  (AI 응답을 기다리므로 넉넉한 타임아웃) */
export async function requestRecommendations(travelId) {
  const { data } = await client.post(`/travels/${travelId}/recommendations`, null, { timeout: 60_000 });
  return data;
}