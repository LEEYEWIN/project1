import client from './client.js';

/**
 * 관리자 AI 추천 KPI (관리자 계정만. 아니면 403)
 * params: { days: 7|30|90 }
 */
export async function fetchKpi({ days = 30 } = {}) {
  const { data } = await client.get('/admin/kpi', { params: { days } });
  return data;
}

/** 인증된 세션으로 CSV를 받아 저장한다. */
async function downloadCsv(url, params, fallbackName) {
  const res = await client.get(url, { params, responseType: 'blob' });
  const name = /filename="?([^"]+)"?/.exec(res.headers['content-disposition'] ?? '')?.[1] ?? fallbackName;
  const href = URL.createObjectURL(res.data);
  const link = document.createElement('a');
  link.href = href;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(href);
}

/** 재학습용 CSV 내려받기 */
export async function downloadTrainingCsv() {
  await downloadCsv('/admin/kpi/training-data.csv', {}, 'jejuro_training.csv');
}

/**
 * 퍼널 이탈 로그 (이탈 확정된 여행, 20건씩)
 * → { page, totalPages, total, counts: { SURVEYED: 1, ... }, items: [...] }
 * step: 못 간 단계 (SURVEYED | RECOMMENDED | PLACED_ANY | PLACED_ALL | ADOPTED | REVIEWED, 생략 = 전체)
 */
export async function fetchFunnelDrops({ days = 30, step = '', page = 0 } = {}) {
  const params = { days, page };
  if (step) params.step = step;
  const { data } = await client.get('/admin/kpi/funnel-drops', { params });
  return data;
}

/** 퍼널 이탈 로그 CSV */
export async function downloadFunnelDropsCsv({ days = 30, step = '' } = {}) {
  const params = { days };
  if (step) params.step = step;
  await downloadCsv('/admin/kpi/funnel-drops.csv', params, 'jejuro_funnel_drops.csv');
}

// ------------------------------------------------------------------ 신고 처리

/** 신고된 글·댓글 목록 { pendingCount, page, totalPages, totalCount, items } status: PENDING | DONE */
export async function fetchReports({ status = 'PENDING', type = '', page = 0 } = {}) {
  const params = { status, page };
  if (type) params.type = type;
  const { data } = await client.get('/admin/reports', { params });
  return data;
}

/** 처리 { action: BLOCK(차단)|KEEP(반려), blockReason?: SEXUAL|PRIVACY|ABUSE|SPAM } */
export async function handleReport(targetType, targetId, payload) {
  await client.post(`/admin/reports/${targetType}/${targetId}/handle`, payload);
}

/** 차단 해제 (잘못 차단했을 때) */
export async function unblockReport(targetType, targetId) {
  await client.post(`/admin/reports/${targetType}/${targetId}/unblock`);
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

/** 삭제 (행은 남기고 여행·경로·후기에는 "확인 불가") / 삭제 취소 */
export async function deletePoi(poiId) {
  await client.delete(`/admin/pois/${poiId}`);
}

export async function restorePoi(poiId) {
  await client.post(`/admin/pois/${poiId}/restore`);
}

export async function addPoiMapping(poiId, sourcePoiId) {
  await client.post(`/admin/pois/${poiId}/mappings`, { sourcePoiId });
}

export async function removePoiMapping(poiId, sourcePoiId) {
  await client.delete(`/admin/pois/${poiId}/mappings`, { params: { sourcePoiId } });
}

/** 사진 점검: 사진 주소가 있는 관광지 전부 [{ poiId, poiName, imageUrl, hidden }] */
export async function fetchPoiImages() {
  const { data } = await client.get('/admin/pois/images');
  return data;
}
