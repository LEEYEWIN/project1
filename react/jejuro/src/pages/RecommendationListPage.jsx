import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchPoisByIds } from '../api/poiApi.js';
import { errorMessage } from '../api/client.js';
import { loadRecommendationIds } from '../utils/recommendStorage.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import HeartButton from '../components/common/HeartButton.jsx';
import RegionFilter from '../components/common/RegionFilter.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 3페이지: 추천 관광지 목록
 * 데이터 출처: ① 2페이지가 넘겨준 state.pois ② 없으면(새로고침) sessionStorage의 ID로 다시 조회
 * 카드의 하트로 바로 찜(4페이지 기능)을 할 수 있다.
 */
export default function RecommendationListPage() {
  const { travelId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [pois, setPois] = useState(location.state?.pois ?? null);
  const [error, setError] = useState('');
  const [region, setRegion] = useState('전체');
  const { bookmarks, isBookmarked, toggle, pending, error: bookmarkError } = useBookmarks(travelId);

  useEffect(() => {
    if (pois) return;
    const ids = loadRecommendationIds(travelId);
    if (!ids) {
      navigate(`/travels/${travelId}/recommending`, { replace: true }); // 추천 기록 없음 → 다시 추천
      return;
    }
    fetchPoisByIds(ids)
      .then(setPois)
      .catch((e) => setError(errorMessage(e)));
  }, [pois, travelId, navigate]);

  const regions = useMemo(() => [...new Set((pois ?? []).map((p) => p.regionName))], [pois]);
  const visible = (pois ?? []).filter((p) => region === '전체' || p.regionName === region);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!pois) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>AI 추천 관광지 {pois.length}곳</h1>
        <div className="actions">
          <button type="button" className="btn ghost" onClick={() => navigate(`/travels/${travelId}/recommending`)}>
            다시 추천 받기
          </button>
          <Link className="btn ghost" to={`/travels/${travelId}/pois`}>
            전체 관광지 보기
          </Link>
        </div>
      </div>
      <p className="hint">마음에 드는 곳의 ♡를 눌러 찜하세요. 찜한 관광지로 여행 경로를 만듭니다.</p>

      <RegionFilter regions={regions} value={region} onChange={setRegion} />
      <ErrorBox message={bookmarkError} />

      {visible.length === 0 ? (
        <p className="empty">추천 결과가 없습니다. 관광지 데이터가 적재되었는지 확인하세요.</p>
      ) : (
        <div className="poi-grid">
          {visible.map((poi) => (
            <PoiCard
              key={poi.poiId}
              poi={poi}
              right={
                <HeartButton
                  on={isBookmarked(poi.poiId)}
                  disabled={pending.has(poi.poiId)}
                  onClick={() => toggle(poi)}
                />
              }
            />
          ))}
        </div>
      )}

      <div className="bottom-bar">
        <span>찜 {bookmarks.length}곳</span>
        <Link className="btn primary" to={`/travels/${travelId}/bookmarks`}>
          찜 목록 보기 →
        </Link>
      </div>
    </main>
  );
}