import { useEffect, useState } from 'react';
import { checkNickname } from '../api/authApi.js';

/**
 * 닉네임을 입력하면 0.4초 뒤 서버에 중복 여부를 물어본다.
 * skip 이 true 이면(예: 내 원래 닉네임 그대로) 확인하지 않는다.
 * 반환: { status: 'idle' | 'checking' | 'ok' | 'taken', message }
 */
export default function useNicknameCheck(nickname, skip = false) {
  const [result, setResult] = useState({ status: 'idle', message: '' });

  useEffect(() => {
    const value = (nickname || '').trim();
    if (skip || !value) {
      setResult({ status: 'idle', message: '' });
      return undefined;
    }
    let active = true;
    setResult({ status: 'checking', message: '' });
    const timer = setTimeout(() => {
      checkNickname(value)
        .then((r) => {
          if (active) setResult({ status: r.available ? 'ok' : 'taken', message: r.message });
        })
        .catch(() => {
          if (active) setResult({ status: 'idle', message: '' });
        });
    }, 400);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [nickname, skip]);

  return result;
}