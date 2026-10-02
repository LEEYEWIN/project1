/**
 * 추천 결과(관광지 목록)는 서버 추천 기록(RECOMMEND_ITEM)에서 다시 읽으므로 여기 보관하지 않는다.
 *
 * 여행지 선택 성향(설문 첫 질문): 'POPULAR'(많이 고른 곳) | 'UNIQUE'(나만의 취향).
 * 추천 목록의 안내 문구에만 쓰고 DB·AI 학습에는 보내지 않는다. 로그아웃하면 recommend: 키와 함께 지워진다.
 * (저장소가 막힌 브라우저에서도 에러가 나지 않게 try/catch)
 */
const styleKey = (travelId) => `recommend:style:${travelId}`;

export function savePickStyle(travelId, style) {
  try {
    if (style) sessionStorage.setItem(styleKey(travelId), style);
  } catch {
    /* 저장 실패는 무시: 안내 문구만 안 보임 */
  }
}

export function loadPickStyle(travelId) {
  try {
    return sessionStorage.getItem(styleKey(travelId));
  } catch {
    return null;
  }
}
