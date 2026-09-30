import client from './client.js';

export async function sendEmailCode(email) {
  const { data } = await client.post('/auth/email/send', {
    email,
  });

  return data;
}

export async function verifyEmailCode(email, code) {
  const { data } = await client.post('/auth/email/verify', {
    email,
    code,
  });

  return data;
}