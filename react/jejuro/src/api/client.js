import axios from 'axios';
import { getTestUserId } from '../utils/testUser.js';

// 모든 API 호출이 공유하는 axios 인스턴스
const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// 요청마다 테스트 회원 ID를 헤더에 붙인다 → 서버 CurrentUser가 읽는다 (로그인 대신)
client.interceptors.request.use((config) => {
  config.headers['X-User-Id'] = getTestUserId();
  return config;
});

// 서버 에러 응답 { message } 를 꺼내 쓰기 쉽게 만든다.
export function errorMessage(err) {
  return err?.response?.data?.message ?? '요청 중 문제가 발생했습니다. 잠시 후 다시 시도하세요.';
}

export default client;