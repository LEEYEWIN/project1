import client from './client.js';

/** 로그인한 회원 { userId, nickname, role } (지금은 테스트 회원 기준) */
export async function fetchMe() {
  const { data } = await client.get('/me');
  return data;
}