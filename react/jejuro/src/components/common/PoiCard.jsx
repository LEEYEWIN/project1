import { categoryLabel } from '../../utils/format.js';

/**
 * 관광지 카드. 추천 목록(3)·찜 목록(4)에서 같이 쓴다.
 * poi: PoiSummaryResponse { poiId, name, address, regionName, categoryCode, imageUrl, description }
 * right: 카드 오른쪽 위에 넣을 버튼(찜 하트, 삭제 등)
 * 사진이 한국관광공사(TourAPI) 것이면 사진 아래쪽에 출처를 표시한다.
 */
export default function PoiCard({ poi, right, children }) {
  return (
    <article className="poi-card">
      <div className="poi-img">
        {poi.imageUrl ? <img src={poi.imageUrl} alt={poi.name} loading="lazy" /> : <span>이미지 없음</span>}
        {/* TourAPI 사진은 공공누리 조건 → 출처 표시 */}
        {poi.imageUrl?.includes('visitkorea.or.kr') && <small className="poi-credit">사진: 한국관광공사</small>}
        {right && <div className="poi-right">{right}</div>}
      </div>
      <div className="poi-body">
        <p className="poi-meta">
          {poi.regionName} · {categoryLabel(poi.categoryCode)}
        </p>
        <h3>{poi.name}</h3>
        <p className="poi-addr">{poi.address}</p>
        {poi.description && <p className="poi-desc">{poi.description}</p>}
        {children}
      </div>
    </article>
  );
}