import client from './client.js';

/**
 * 4페이지: 여행 장소 (화면 이름: "여행 장소", 예전 이름 "찜". 서버 주소는 /bookmarks 그대로)
 * 여행 장소 = 이 여행의 경로(일정)에 넣을 관광지. 여기 담은 곳을 일차별로 배치해 경로를 만든다.
 */

/** 여행 장소 목록 → [{ bookmarkId, poi }] */
export async function fetchBookmarks(travelId) {
  const { data } = await client.get(`/travels/${travelId}/bookmarks`);
  return data;
}

/**
 * 장소 추가. source = 담은 화면: 'RECOMMEND'(AI 추천 목록) / 'SEARCH'(관광지 목록·상세, 기본)
 * (관리자 KPI의 추천 채택률·AI가 놓친 관광지 계산에 쓰임)
 */
export async function addBookmark(travelId, poiId, source = 'SEARCH') {
  const { data } = await client.post(`/travels/${travelId}/bookmarks`, { poiId, source });
  return data;
}

/** 장소에서 빼기 (경로에 배치되어 있었다면 경로에서도 빠짐) */
export async function removeBookmark(travelId, poiId) {
  await client.delete(`/travels/${travelId}/bookmarks/${poiId}`);
}