import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchPoiDetail } from '../api/poiApi.js';
import { errorMessage } from '../api/client.js';
import useBookmarks from '../hooks/useBookmarks.js';
import KakaoMap from '../components/map/KakaoMap.jsx';
import PoiImage from '../components/poi/PoiImage.jsx';
import HeartButton from '../components/common/HeartButton.jsx';
import AddToRouteButton from '../components/poi/AddToRouteButton.jsx';
import Loading from '../components/common/Loading.jsx';
import { categoryLabel } from '../utils/format.js';
import '../styles/poi.css';

/**
 * 관광지 상세 (FR-33)
 * ① /pois/:poiId                       둘러보기 (찜 없음)
 * ② /travels/:travelId/pois/:poiId     여행에서 들어옴: ♡ 찜 + [루트에 추가]
 * - 이름·관광 유형·권역·사진·한 줄 소개·세부 설명·주소(사진 옆)·지도
 * - 좌표가 없거나 제주 밖이면 지도 대신 "위치 확인 필요"
 * - 없는 관광지(404)는 안내 후 목록으로 가는 버튼
 * - 여행 만족도 별점은 관광지 평점으로 쓰지 않으므로 표시하지 않는다
 */
export default function PoiDetailPage() {
  const { travelId, poiId } = useParams();
  const travelMode = Boolean(travelId);
  const navigate = useNavigate();
  const listPath = travelMode ? `/travels/${travelId}/pois` : '/pois';

  const [poi, setPoi] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState('');
  const { isBookmarked, toggle, ensure, pending, error: bookmarkError } = useBookmarks(travelId);

  useEffect(() => {
    let cancelled = false;
    setPoi(null);
    setNotFound(false);
    fetchPoiDetail(poiId)
      .then((p) => !cancelled && setPoi(p))
      .catch((e) => {
        if (cancelled) return;
        if (e?.response?.status === 404) setNotFound(true);
        else setError(errorMessage(e));
      });
    return () => {
      cancelled = true;
    };
  }, [poiId]);

  const points = useMemo(
    () => (poi?.locationChecked ? [{ lat: Number(poi.latitude), lng: Number(poi.longitude), name: poi.name }] : []),
    [poi]
  );
    /** 카카오맵 새 창으로 열기 */
  const openKakaoMap = () => {
    const url = `https://map.kakao.com/link/map/${encodeURIComponent(poi.name)},${poi.latitude},${poi.longitude}`;
    window.open(url, '_blank', 'noopener');
  };
  /** 목록에서 왔으면 뒤로(검색 조건 유지), 주소로 바로 왔으면 목록으로 */
  const back = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate(listPath));

  if (notFound) {
    return (
      <main className="page">
        <div className="empty">
          <p>관광지를 찾을 수 없어요. 삭제되었거나 주소가 잘못되었을 수 있어요.</p>
          <Link className="btn primary" to={listPath}>
            관광지 목록으로
          </Link>
        </div>
      </main>
    );
  }
  if (error && !poi) {
    return (
      <main className="page">
        <div className="empty">
          <p>관광지 정보를 불러오지 못했어요. {error}</p>
          <Link className="btn ghost" to={listPath}>
            관광지 목록으로
          </Link>
        </div>
      </main>
    );
  }
  if (!poi) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide poi-detail-page">
      <button type="button" className="btn ghost small" onClick={back}>
        ‹ 목록으로
      </button>

      <div className="poi-hero">
        <div className="poi-hero-img poi-img">
          <PoiImage src={poi.imageUrl} alt={poi.name} />
        </div>
        <div className="poi-hero-text">
          <p className="poi-meta">
            {poi.regionName ?? '권역 확인 필요'} · {categoryLabel(poi.categoryCode)}
          </p>
          <h1>{poi.name}</h1>
          <p className="poi-lead">{poi.description ?? '소개 정보 없음'}</p>
          {poi.address && <p className="poi-addr">{poi.address}</p>}

          {travelMode && (
            <div className="poi-actions">
              <HeartButton on={isBookmarked(poi.poiId)} disabled={pending.has(poi.poiId)} onClick={() => toggle(poi)} />
              <span className="hint small">{isBookmarked(poi.poiId) ? '찜한 관광지' : '찜하면 루트 짜기에서 쓸 수 있어요'}</span>
              <AddToRouteButton travelId={travelId} poi={poi} ensureBookmarked={ensure} />
            </div>
          )}
          {bookmarkError && <p className="hint small error-text">{bookmarkError}</p>}
        </div>
      </div>

      <section className="card">
        <h2>소개</h2>
        <p className="poi-long">{poi.detailDescription ?? poi.description ?? '소개 정보 없음'}</p>
      </section>

      <section className="card">
        <h2>위치</h2>
        {poi.locationChecked ? (
          <>
            <KakaoMap points={points} path={[]} height={320} />
            <button type="button" className="btn ghost small" onClick={openKakaoMap}>
              카카오맵에서 크게 보기
            </button>
          </>        ) : (
          <p className="missing">위치 확인 필요 — 좌표 정보가 없거나 올바르지 않아 지도를 표시하지 않아요.</p>
        )}
      </section>
    </main>
  );
}