/**
 * 관광지 카드. 추천 목록(3)·찜 목록(4)에서 같이 쓴다.
 * poi: PoiSummaryResponse { poiId, name, address, regionName, categoryCode, imageUrl, description }
 * right: 카드 오른쪽 위에 넣을 버튼(찜 하트, 삭제 등)
 */
export default function PoiCard({ poi, right, children }) {
  return (
    <article className="poi-card">
      <div className="poi-img">
        {poi.imageUrl ? <img src={poi.imageUrl} alt={poi.name} loading="lazy" /> : <span>이미지 없음</span>}
        {right && <div className="poi-right">{right}</div>}
      </div>
      <div className="poi-body">
        <p className="poi-meta">
          {poi.regionName} · {poi.categoryCode}
        </p>
        <h3>{poi.name}</h3>
        <p className="poi-addr">{poi.address}</p>
        {poi.description && <p className="poi-desc">{poi.description}</p>}
        {children}
      </div>
    </article>
  );
}