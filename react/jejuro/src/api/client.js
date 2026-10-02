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
