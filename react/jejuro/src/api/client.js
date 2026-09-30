import axios from 'axios';

const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

// 쿠키 세션 인증. 변경 요청에는 서버가 발급한 CSRF 토큰을 함께 보낸다.
client.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) {
    const { data } = await axios.get('/api/auth/csrf', { withCredentials: true });
    config.headers[data.headerName] = data.token;
  }
  return config;
});
client.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) {
    window.dispatchEvent(new Event('auth:expired'));
  }
  return Promise.reject(error);
});

export function errorMessage(err) {
  return err?.response?.data?.message ?? '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.';
}
export default client;
