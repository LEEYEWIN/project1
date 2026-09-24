import client from './client.js';

/** 테스트 회원 목록 [{ userId, nickname, email }] */
export async function fetchTestUsers() {
  const { data } = await client.get('/test-users');
  return data;
}