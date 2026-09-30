import client from './client.js';

/** 로그인한 회원 { userId, nickname, role } */
export async function fetchMe() {
  const { data } = await client.get('/me');
  return data;
}
