import client from './client.js';

/**
 * 관심없음 관광지 (회원 단위 — 내 모든 여행의 다음 AI 추천에서 제외)
 * 목록 → [{ poi: PoiSummaryResponse, createdAt }]
 */
export async function fetchDislikes() {
  const { data } = await client.get('/me/dislikes');
  return data;
}

export async function addDislike(poiId) {
  await client.post('/me/dislikes', { poiId });
}

export async function removeDislike(poiId) {
  await client.delete(`/me/dislikes/${poiId}`);
}