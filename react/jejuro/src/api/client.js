import axios from 'axios';

// 모든 API 호출이 공유하는 axios 인스턴스
const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

// Spring Security가 쓰기 요청에 요구하는 CSRF 토큰을 현재 세션에서 받는다.
// 로그인 때 세션 ID와 토큰이 바뀔 수 있으므로 요청마다 새로 읽는다.
client.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes(config.method?.toLowerCase())) {
    const { data } = await client.get('/auth/csrf');
    config.headers[data.headerName] = data.token;
  }
  return config;
});

// 로그인 세션이 만료·종료되어 401이 오면 화면의 로그인 상태도 비운다.
// AuthContext가 'auth:expired'를 받아 user를 지우면, 로그인이 필요한 화면은 로그인 화면으로 이동하고
// 로그인 후 원래 화면으로 돌아온다. (로그인·가입 실패와 첫 /me 확인의 401은 만료가 아니므로 제외)
const NOT_EXPIRY = ['/auth/login', '/auth/signup', '/me'];
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url ?? '';
    if (err.response?.status === 401 && !NOT_EXPIRY.includes(url)) {
      window.dispatchEvent(new CustomEvent('auth:expired', { detail: { message: err.response.data?.message } }));
    }
    return Promise.reject(err);
  }
);

// 서버 에러 응답 { message } 를 꺼내 쓰기 쉽게 만든다.
export function errorMessage(err) {
  return err?.response?.data?.message ?? '요청 중 문제가 발생했습니다. 잠시 후 다시 시도하세요.';
}

export default client;

/**
 * 오류를 알림창으로 띄울지, 화면 메시지로 보여 줄지 정하는 공통 기준 (모든 화면이 같은 기준을 쓴다)
 *
 *  알림창(window.alert) ─ 누른 동작이 서비스 규칙·상태 때문에 거부되어, 지금 화면의 입력을 고쳐도 해결되지 않을 때
 *    403 권한 없음 / 409 상태 충돌(확정된 일정, 기간이 겹치는 여행, 다른 탭에서 바뀐 경로, 신고된 글…)
 *    410 차단·만료(차단된 글, 인증 시간 지남) / 429 횟수·시간 제한(재전송 대기, 인증번호 5번 틀림)
 *  화면 메시지(입력 칸 아래·폼 오류 상자) ─ 입력을 고치면 되는 오류(400)와 서버·네트워크 오류(5xx, 다시 시도)
 *    단, 입력 칸에서 바로 고칠 수 있는 409(사용 중인 닉네임, 이미 가입된 이메일)는 화면 메시지로 보여 준다.
 */
export function isBlocked(err) {
  return [403, 409, 410, 429].includes(err?.response?.status);
}

/** 위 기준대로 오류를 보여 준다. 알림창으로 띄웠으면 화면 메시지는 비운다. */
export function showError(err, setError) {
  const msg = errorMessage(err);
  if (isBlocked(err)) {
    window.alert(msg);
    setError?.('');
  } else {
    setError?.(msg);
  }
  return msg;
}
