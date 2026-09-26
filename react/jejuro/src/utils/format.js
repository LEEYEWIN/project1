/** 초 → "1시간 5분" / "12분" */
export function formatDuration(sec) {
  const min = Math.round(sec / 60);
  if (min < 60) return `${min}분`;
  const h = Math.floor(min / 60);
  const m = min % 60;
  return m ? `${h}시간 ${m}분` : `${h}시간`;
}

/** 미터 → "850m" / "12.4km" */
export function formatDistance(m) {
  return m < 1000 ? `${m}m` : `${(m / 1000).toFixed(1)}km`;
}

/** "2026-10-20" → "10.20(화)" */
export function formatDate(iso) {
  const d = new Date(`${iso}T00:00:00`);
  const week = ['일', '월', '화', '수', '목', '금', '토'][d.getDay()];
  return `${d.getMonth() + 1}.${d.getDate()}(${week})`;
}

/**
 * 관광지 분류 코드(POI.category_code) → 화면 이름.
 * AI 학습 데이터 VISIT_AREA_TYPE_CD 기준 (1~8, 체험 활동은 9)
 */
export const CATEGORY_LABEL = {
  NATURE: '자연관광지',
  HISTORY: '역사·유적·종교',
  CULTURE: '문화시설',
  COMMERCIAL: '상업지구',
  LEISURE: '레저·스포츠',
  THEME: '테마시설',
  TRAIL: '산책로·둘레길',
  FESTIVAL: '축제·행사',
  EXPERIENCE: '체험 활동',
};

export function categoryLabel(code) {
  return CATEGORY_LABEL[code] ?? code;
}