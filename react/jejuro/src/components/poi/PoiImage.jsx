/**
 * 관광지 사진. 사진이 없거나 불러오지 못하면 기본 그림(한라산·바다)을 보여 준다 (FR-32).
 * 사진이 한국관광공사(TourAPI)·비짓제주 것이면 왼쪽 아래에 출처 표시.
 */
import { useState } from 'react';

export function photoCredit(url) {
  if (!url) return '';
  if (url.includes('visitkorea.or.kr')) return '한국관광공사';
  if (url.includes('visitjeju.net')) return '비짓제주';
  return '';
}

export default function PoiImage({ src, alt }) {
  const [broken, setBroken] = useState(false);
  if (!src || broken) {
    return (
      <div className="poi-fallback" role="img" aria-label={`${alt} (사진 없음)`}>
        <svg viewBox="0 0 120 90" aria-hidden="true">
          <rect width="120" height="90" fill="#d6e8ee" />
          <circle cx="92" cy="24" r="10" fill="#ffb36b" />
          <path d="M0 62 L34 40 Q60 18 84 40 L120 58 L120 66 L0 66 Z" fill="#9dbb68" />
          <rect y="64" width="120" height="26" fill="#6fb3cf" />
          <path d="M0 74 Q10 70 20 74 T40 74 T60 74 T80 74 T100 74 T120 74" fill="none" stroke="#d6e8ee" strokeWidth="1.5" />
        </svg>
        <span>사진 준비 중</span>
      </div>
    );
  }
  return (
    <>
      <img src={src} alt={alt} loading="lazy" onError={() => setBroken(true)} />
      {photoCredit(src) && <small className="poi-credit">사진: {photoCredit(src)}</small>}
    </>
  );
}