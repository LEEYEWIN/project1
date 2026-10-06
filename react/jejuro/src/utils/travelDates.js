/**
 * 여행 날짜 계산 공용 함수 ('YYYY-MM-DD' 문자열끼리는 크기 비교가 날짜 비교와 같다)
 * - 여행은 오늘부터 시작하는 날짜로만 만들 수 있다
 * - 같은 기간에는 여행을 하나만 만들 수 있다 → 이미 여행이 있는 날은 고를 수 없게 한다
 */
export function toIso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function todayIso() {
  return toIso(new Date());
}

/** 'YYYY-MM-DD' + n일 */
export function addDays(iso, n) {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + n);
  return toIso(d);
}

/** 내 여행 목록 → 이미 여행이 있는 기간 [{ start, end, name }] (excludeTravelId: 바꿔 만들 기존 여행은 빼기) */
export function busyRanges(travels, excludeTravelId) {
  return (travels ?? [])
    .filter((t) => String(t.travelId) !== String(excludeTravelId ?? ''))
    .map((t) => ({ start: t.startDate, end: t.endDate, name: t.travelName }));
}

/** 그날 이미 있는 여행 (없으면 undefined) */
export function busyOn(iso, ranges) {
  return ranges.find((r) => r.start <= iso && iso <= r.end);
}

/** [start, end] 기간과 겹치는 여행 (없으면 undefined) */
export function busyBetween(start, end, ranges) {
  return ranges.find((r) => r.start <= end && start <= r.end);
}
