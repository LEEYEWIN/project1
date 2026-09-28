import { useState } from 'react';
import { Link } from 'react-router-dom';
import { categoryLabel } from '../../utils/format.js';
import PoiImage from '../poi/PoiImage.jsx';

/**
 * 관광지 카드. 관광지 목록·추천 목록·찜 목록에서 같이 쓴다.
 * poi: PoiSummaryResponse { poiId, name, address, regionName, categoryCode, imageUrl, description, detailDescription }
 * description = 한 줄 소개(항상 보임), detailDescription = 세부 설명("자세히"를 누르면 펼침)
 * to: 상세 화면 주소(있으면 사진·이름을 누르면 이동)
 * right: 카드 오른쪽 위에 넣을 버튼(찜 하트, 삭제 등)
 * children: 카드 아래쪽에 넣을 버튼(루트에 추가 등)
 * 사진이 없으면 기본 그림, 사진 출처(한국관광공사·비짓제주)는 사진 위에 표시.
 */
export default function PoiCard({ poi, to, right, children }) {
  const [open, setOpen] = useState(false);
  // 세부 설명이 한 줄 소개와 다를 때만 "자세히" 버튼 (상세 화면이 있으면 상세에서 봄)
  const hasDetail = !to && poi.detailDescription && poi.detailDescription.trim() !== (poi.description ?? '').trim();

  const image = <PoiImage src={poi.imageUrl} alt={poi.name} />;

  return (
    <article className="poi-card">
      <div className="poi-img">
        {to ? (
          <Link to={to} className="poi-img-link" tabIndex={-1} aria-hidden="true">
            {image}
          </Link>
        ) : (
          image
        )}
        {right && <div className="poi-right">{right}</div>}
      </div>
      <div className="poi-body">
        <p className="poi-meta">
          {poi.regionName} · {categoryLabel(poi.categoryCode)}
        </p>
        <h3>{to ? <Link to={to}>{poi.name}</Link> : poi.name}</h3>
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