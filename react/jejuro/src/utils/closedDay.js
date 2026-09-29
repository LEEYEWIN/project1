/**
 * 자연관광지가 아닌 곳(박물관·테마파크·체험 등)은 휴무일이 있으므로 담을 때 경고창을 띄운다.
 * poi: { name, categoryCode, closedDays }
 */
export function needsClosedDayCheck(poi) {
  return Boolean(poi) && poi.categoryCode !== 'NATURE';
}

export function closedDayMessage(poi) {
  const days = poi.closedDays ? `쉬는 날: ${poi.closedDays}` : '쉬는 날 정보가 없어요. 전화나 홈페이지로 꼭 확인해 주세요.';
  return `[${poi.name}] 휴무일을 확인한 뒤 방문하세요.\n${days}`;
}

/** 담기 직전에 호출: 자연관광지가 아니면 경고창 */
export function warnClosedDays(poi) {
  if (needsClosedDayCheck(poi)) window.alert(closedDayMessage(poi));
}