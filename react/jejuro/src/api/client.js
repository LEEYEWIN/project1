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

// 서버 에러 응답 { message } 를 꺼내 쓰기 쉽게 만든다.
export function errorMessage(err) {
  return err?.response?.data?.message ?? '요청 중 문제가 발생했습니다. 잠시 후 다시 시도하세요.';
}

export default client;
