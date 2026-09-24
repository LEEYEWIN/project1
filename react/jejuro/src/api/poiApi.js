import client from './client.js';

/** 3페이지 새로고침 복구: ID 목록으로 관광지 조회 (요청한 순서 유지) */
export async function fetchPoisByIds(ids) {
  if (ids.length === 0) return [];
  const { data } = await client.get('/pois', { params: { ids: ids.join(',') } });
  return data;
}

/** 3-1페이지: 전체 관광지 검색 → { items, page, size, totalPages, totalCount } */
export async function searchPois({ regionId, category, keyword, page = 0, size = 12 } = {}) {
  const { data } = await client.get('/pois/search', {
    params: { regionId: regionId || undefined, category: category || undefined, keyword: keyword || undefined, page, size },
  });
  return data;
}

/** 3-1페이지: 분류 필터 목록 ["BEACH", "NATURE", ...] */
export async function fetchPoiCategories() {
  const { data } = await client.get('/pois/categories');
  return data;
}