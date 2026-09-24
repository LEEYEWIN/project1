/**
 * 테스트 회원 ID를 브라우저(localStorage)에 기억한다.
 * 저장소가 막힌 브라우저에서도 에러가 나지 않게 try/catch.
 */
const KEY = 'testUserId';

export function getTestUserId() {
  try {
    return localStorage.getItem(KEY) || '1';
  } catch {
    return '1';
  }
}

export function setTestUserId(id) {
  try {
    localStorage.setItem(KEY, String(id));
  } catch {
    /* 무시 */
  }
}