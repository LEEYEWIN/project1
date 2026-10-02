import client from './client.js';

/** 2페이지: AI 추천 요청 → { travelId, pois: [...] }  (AI 응답을 기다리므로 넉넉한 타임아웃) */
/** 이미 받은 추천 목록 → { travelId, pois }. 추천을 받은 적 없으면 404 */
export async function fetchLatestRecommendations(travelId) {
  const { data } = await client.get(`/travels/${travelId}/recommendations`);
  return data;
}

/** 이미 받은 추천이 있으면 서버가 AI를 다시 부르지 않고 그 결과를 돌려준다 */
export async function requestRecommendations(travelId) {
  const { data } = await client.post(`/travels/${travelId}/recommendations`, null, { timeout: 60_000 });
  return data;
}