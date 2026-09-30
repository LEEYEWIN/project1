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