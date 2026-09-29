import { Link, useNavigate } from 'react-router-dom';
import { categoryLabel } from '../../utils/format.js';
import PoiImage from '../poi/PoiImage.jsx';

/**
 * 관광지 카드. 관광지 목록·추천 목록·여행 장소 목록에서 같이 쓴다.
 * poi: PoiSummaryResponse { poiId, name, address, regionName, categoryCode, imageUrl, description }
 * to: 상세 화면 주소 — 있으면 카드 아무 곳이나 누르면 상세로 이동 (장소 추가 버튼 등을 누를 때는 이동 안 함)
 * right: 카드 오른쪽 위에 넣을 버튼([+ 장소 추가], 장소에서 빼기 등)
 * children: 카드 아래쪽에 넣을 버튼(루트에 추가 등)
 * linkState: 상세로 갈 때 함께 넘길 값 (예: AI 추천 목록에서 왔음 { source: 'RECOMMEND' })
 * 세부 설명은 카드에서 펼치지 않고 상세 화면에서 본다.
 */
export default function PoiCard({ poi, to, right, children, linkState }) {
  const navigate = useNavigate();

  /** 카드 클릭 → 상세. 안쪽의 버튼·링크·입력칸·루트 추가 상자를 누른 경우는 그 동작만 */
  const openDetail = (e) => {
    if (!to) return;
    if (e.target.closest('button, a, input, select, textarea, label, .add-route')) return;
    navigate(to, { state: linkState });
  };

  return (
    <article className={to ? 'poi-card clickable' : 'poi-card'} onClick={openDetail}>
      <div className="poi-img">
        <PoiImage src={poi.imageUrl} alt={poi.name} />
        {right && <div className="poi-right">{right}</div>}
      </div>
      <div className="poi-body">
        <p className="poi-meta">
          {poi.regionName} · {categoryLabel(poi.categoryCode)}
        </p>
        {/* 이름은 링크로 둬서 키보드(Tab·Enter)로도 상세에 갈 수 있게 */}
        <h3>{to ? <Link to={to} state={linkState}>{poi.name}</Link> : poi.name}</h3>
        <p className="poi-addr">{poi.address}</p>
        {poi.description && <p className="poi-desc">{poi.description}</p>}
        {children}
      </div>
    </article>
  );
}