/**
 * 자연관광지가 아닌 곳(박물관·테마파크·체험 등)은 휴무일이 있으므로 담을 때 경고창을 띄운다.
 * poi: { name, categoryCode }  (쉬는 날 칼럼은 DB에서 삭제 → 항상 "방문 전 확인" 안내)
 */
export function needsClosedDayCheck(poi) {
  return Boolean(poi) && poi.categoryCode !== 'NATURE';
}

export function closedDayMessage(poi) {
  return `[${poi.name}] 휴무일을 확인한 뒤 방문하세요.\n자연관광지가 아닌 곳은 쉬는 날이 있을 수 있어요. 방문 전에 꼭 확인해 주세요.`;
}

/** 담기 직전에 호출: 자연관광지가 아니면 경고창 */
export function warnClosedDays(poi) {
  if (needsClosedDayCheck(poi)) window.alert(closedDayMessage(poi));
}