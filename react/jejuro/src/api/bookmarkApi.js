import client from './client.js';

/** 4페이지: 찜 목록 → [{ bookmarkId, poi }] */
export async function fetchBookmarks(travelId) {
  const { data } = await client.get(`/travels/${travelId}/bookmarks`);
  return data;
}

export async function addBookmark(travelId, poiId) {
  const { data } = await client.post(`/travels/${travelId}/bookmarks`, { poiId });
  return data;
}

export async function removeBookmark(travelId, poiId) {
  await client.delete(`/travels/${travelId}/bookmarks/${poiId}`);
}