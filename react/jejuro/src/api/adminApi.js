import client from './client.js';

/**
 * 관리자 AI 추천 KPI (관리자 계정만. 아니면 403)
 * params: { days: 7|30|90 }
 */
export async function fetchKpi({ days = 30 } = {}) {
  const { data } = await client.get('/admin/kpi', { params: { days } });
  return data;
}

/** 재학습용 CSV 내려받기 (헤더에 테스트 회원이 붙어야 해서 주소를 바로 열지 않고 받아서 저장) */
export async function downloadTrainingCsv() {
  const res = await client.get('/admin/kpi/training-data.csv', { responseType: 'blob' });
  const name = /filename="?([^"]+)"?/.exec(res.headers['content-disposition'] ?? '')?.[1] ?? 'jejuro_training.csv';
  const url = URL.createObjectURL(res.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

// ------------------------------------------------------------------ 신고 처리

/** 신고된 글·댓글 목록 { pendingCount, page, totalPages, totalCount, items } status: PENDING | DONE */
export async function fetchReports({ status = 'PENDING', type = '', page = 0 } = {}) {
  const params = { status, page };
  if (type) params.type = type;
  const { data } = await client.get('/admin/reports', { params });
  return data;
}

/** 처리 { action: KEEP|HIDE|DELETE, sanction: NONE|WARNING|SUSPEND_7D|SUSPEND_30D|BAN, memo } */
export async function handleReport(targetType, targetId, payload) {
  await client.post(`/admin/reports/${targetType}/${targetId}/handle`, payload);
}

// ------------------------------------------------------------------ 회원 관리

/** 회원 목록 { page, totalPages, totalCount, counts, items } filter: ALL|SUSPENDED|REPORTED|ADMIN */
export async function fetchUsers({ keyword = '', filter = 'ALL', page = 0 } = {}) {
  const params = { filter, page };
  if (keyword.trim()) params.keyword = keyword.trim();
  const { data } = await client.get('/admin/users', { params });
  return data;
}

export async function fetchUserDetail(userId) {
  const { data } = await client.get(`/admin/users/${userId}`);
  return data;
}

/** 제재 { type: WARNING|SUSPEND_7D|SUSPEND_30D|BAN|RELEASE, reason } */
export async function sanctionUser(userId, payload) {
  await client.post(`/admin/users/${userId}/sanctions`, payload);
}

export async function changeUserRole(userId, role) {
  await client.put(`/admin/users/${userId}/role`, { role });
}

// ------------------------------------------------------------------ 관광지 관리

/** 권역·분류 선택지 { regions: [{value,label}], categories: [{value,label}] } */
export async function fetchPoiOptions() {
  const { data } = await client.get('/admin/pois/options');
  return data;
}

/** 관광지 목록 { page, totalPages, totalCount, checks, items } */
export async function fetchAdminPois({ keyword = '', regionId = '', category = '', visibility = 'ALL', issue = '', page = 0 } = {}) {
  const params = { visibility, page };
  if (keyword.trim()) params.keyword = keyword.trim();
  if (regionId) params.regionId = regionId;
  if (category) params.category = category;
  if (issue) params.issue = issue;
  const { data } = await client.get('/admin/pois', { params });
  return data;
}

export async function fetchAdminPoi(poiId) {
  const { data } = await client.get(`/admin/pois/${poiId}`);
  return data;
}

/** 새 관광지 → { poiId } */
export async function createPoi(payload) {
  const { data } = await client.post('/admin/pois', payload);
  return data;
}

export async function updatePoi(poiId, payload) {
  await client.put(`/admin/pois/${poiId}`, payload);
}

export async function setPoiHidden(poiId, hidden) {
  await client.put(`/admin/pois/${poiId}/hidden`, { hidden });
}

export async function addPoiMapping(poiId, sourcePoiId) {
  await client.post(`/admin/pois/${poiId}/mappings`, { sourcePoiId });
}

export async function removePoiMapping(poiId, sourcePoiId) {
  await client.delete(`/admin/pois/${poiId}/mappings`, { params: { sourcePoiId } });
}