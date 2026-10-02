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

// ─── 소셜 계정 연동 ───────────────────────────────────────
// 제공자 로그인은 화면 이동이라 CRA 프록시를 거치지 않는다. 스프링 서버 주소로 바로 보낸다.
export const SOCIAL_ORIGIN =
  process.env.REACT_APP_SOCIAL_ORIGIN || 'http://localhost:8080';

export const SOCIAL_PROVIDERS = [
  { code: 'KAKAO', name: '카카오' },
  { code: 'NAVER', name: '네이버' },
  { code: 'GOOGLE', name: '구글' },
];

export const socialName = (code) =>
  SOCIAL_PROVIDERS.find((p) => p.code === code)?.name ?? code;

// 키가 설정된 제공자만 돌려준다. 예: ['KAKAO']
export async function getSocialProviders() {
  const { data } = await client.get('/auth/social/providers');
  return data.providers ?? [];
}

// 로그인 화면의 소셜 로그인 버튼 주소
export function socialLoginUrl(code) {
  return `${SOCIAL_ORIGIN}/oauth2/authorization/${code.toLowerCase()}`;
}

// 회원정보 수정의 [연동]: 서버에 연동 표시를 남긴 뒤 제공자 로그인 주소로 이동한다.
export async function startSocialLink(code) {
  const { data } = await client.post(`/me/social/${code.toLowerCase()}/link`);
  window.location.assign(`${SOCIAL_ORIGIN}${data.url}`);
}

export async function unlinkSocial(code) {
  await client.delete(`/me/social/${code.toLowerCase()}`);
}

// 서버가 돌려보낸 ?socialError= 코드를 안내 문구로 바꾼다.
export function socialErrorMessage(code, provider) {
  const name = provider ? socialName(provider) : '소셜';
  switch (code) {
    case 'NOT_LINKED':
      return `연동된 ${name} 계정이 없어요. 이메일로 로그인한 뒤 내 정보 수정에서 연동해 주세요.`;
    case 'INACTIVE':
      return '이용이 중단된 계정이에요.';
    case 'ALREADY_LINKED_OTHER':
      return `이 ${name} 계정은 이미 다른 제주로 회원에 연동되어 있어요. 다른 ${name} 계정으로 연동하거나, 그 회원으로 로그인해 연동을 해제한 뒤 다시 시도해 주세요.`;
    case 'PROVIDER_ALREADY_LINKED':
      return `이미 연동한 ${name} 계정이 있어요. 해제한 뒤 다시 연동해 주세요.`;
    case 'EXPIRED':
      return '연동 시간이 지났어요. 다시 시도해 주세요.';
    default:
      return '소셜 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.';
  }
}

// 닉네임 사용 가능 여부 { available, message } (로그인 상태면 내 닉네임은 제외)
export async function checkNickname(nickname) {
  const { data } = await client.get('/auth/nickname/check', { params: { nickname } });
  return data;
}