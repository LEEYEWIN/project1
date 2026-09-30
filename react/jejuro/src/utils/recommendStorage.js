/**
 * 추천 결과는 DB에 저장하지 않는다. 새로고침해도 3페이지가 유지되도록 sessionStorage에 보관한다.
 * (탭을 닫으면 사라짐. 저장소가 막힌 브라우저에서도 에러가 나지 않게 try/catch)
 */
const key = (travelId) => `recommend:${travelId}`;

export function saveRecommendation(travelId, pois) {
  try {
    sessionStorage.setItem(key(travelId), JSON.stringify(pois.map((p) => p.poiId)));
  } catch {
    /* 저장 실패는 무시: 화면은 메모리 값으로 계속 동작 */
  }
}

export function loadRecommendationIds(travelId) {
  try {
    const raw = sessionStorage.getItem(key(travelId));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}