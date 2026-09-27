import { useState } from 'react';
import { categoryLabel } from '../../utils/format.js';

/**
 * 관광지 카드. 추천 목록(3)·찜 목록(4)에서 같이 쓴다.
 * poi: PoiSummaryResponse { poiId, name, address, regionName, categoryCode, imageUrl, description, detailDescription }
 * description = 한 줄 소개(항상 보임), detailDescription = 세부 설명("자세히"를 누르면 펼침)
 * right: 카드 오른쪽 위에 넣을 버튼(찜 하트, 삭제 등)
 * 사진이 한국관광공사(TourAPI)·비짓제주 것이면 사진 왼쪽 아래에 출처를 표시한다.
 */
function photoCredit(url) {
  if (!url) return '';
  if (url.includes('visitkorea.or.kr')) return '한국관광공사';
  if (url.includes('visitjeju.net')) return '비짓제주';
  return '';
}

export default function PoiCard({ poi, right, children }) {
  const [open, setOpen] = useState(false);
  // 세부 설명이 한 줄 소개와 다를 때만 "자세히" 버튼
  const hasDetail = poi.detailDescription && poi.detailDescription.trim() !== (poi.description ?? '').trim();

  return (
    <article className="poi-card">
      <div className="poi-img">
        {poi.imageUrl ? <img src={poi.imageUrl} alt={poi.name} loading="lazy" /> : <span>이미지 없음</span>}
        {/* 사진 출처 표시 (TourAPI: 공공누리 / 비짓제주: 제주관광공사) */}
        {photoCredit(poi.imageUrl) && <small className="poi-credit">사진: {photoCredit(poi.imageUrl)}</small>}
        {right && <div className="poi-right">{right}</div>}
      </div>
      <div className="poi-body">
        <p className="poi-meta">
          {poi.regionName} · {categoryLabel(poi.categoryCode)}
        </p>
        <h3>{poi.name}</h3>
        <p className="poi-addr">{poi.address}</p>
        {poi.description && <p className="poi-desc">{poi.description}</p>}
        {hasDetail && (
          <>
            <button type="button" className="poi-more" onClick={() => setOpen((v) => !v)}>
              {open ? '접기 ▲' : '자세히 ▼'}
            </button>
            {open && <p className="poi-detail">{poi.detailDescription}</p>}
          </>
        )}
        {children}
      </div>
    </article>
  );
}