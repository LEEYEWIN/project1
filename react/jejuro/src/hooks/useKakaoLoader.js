import { useEffect, useState } from 'react';

/**
 * 카카오맵 JavaScript SDK를 한 번만 불러온다.
 * - 키: frontend/.env.local 에  VITE_KAKAO_JS_KEY=발급받은_JavaScript_키
 * - 카카오 개발자 콘솔 > 앱 > 플랫폼 > Web 에 http://localhost:5173 등록 필요
 * 반환: { ready, error }
 */
let loadingPromise = null;

function loadSdk() {
  if (window.kakao?.maps?.LatLng) return Promise.resolve();
  if (loadingPromise) return loadingPromise;

  const key = import.meta.env.VITE_KAKAO_JS_KEY;
  if (!key) return Promise.reject(new Error('VITE_KAKAO_JS_KEY가 설정되지 않았습니다 (.env.local 확인).'));

  loadingPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false`;
    script.async = true;
    script.onload = () => {
      // 키·도메인이 틀리거나 카카오맵 사용 설정이 꺼져 있으면 스크립트는 받아와도 kakao.maps가 없다
      if (!window.kakao?.maps?.load) {
        loadingPromise = null;
        reject(new Error('카카오맵 SDK 인증 실패: JavaScript 키, Web 도메인(http://localhost:5173), 카카오맵 사용 설정(ON)을 확인하세요.'));
        return;
      }
      window.kakao.maps.load(resolve); // autoload=false → 직접 load 호출
    };
    script.onerror = () => {
      loadingPromise = null;
      reject(new Error(`카카오맵 SDK 요청 실패(${location.origin}). F12 > Network 에서 sdk.js 상태코드를 확인하세요. 401=키/도메인, 403=카카오맵 사용 설정 OFF`));
    };
    document.head.appendChild(script);
  });
  return loadingPromise;
}

export default function useKakaoLoader() {
  const [ready, setReady] = useState(Boolean(window.kakao?.maps?.LatLng));
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    loadSdk()
      .then(() => alive && setReady(true))
      .catch((e) => alive && setError(e.message));
    return () => {
      alive = false;
    };
  }, []);

  return { ready, error };
}