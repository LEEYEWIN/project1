import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchRoutes } from '../api/routeApi.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 4페이지: 찜 목록
 * 찜 취소, 추천 목록으로 돌아가기, 찜한 관광지로 경로 만들기(5페이지)
 */
export default function BookmarkPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const { bookmarks, loading, error, toggle, pending } = useBookmarks(travelId);
  const [routes, setRoutes] = useState([]);

  useEffect(() => {
    fetchRoutes(travelId).then(setRoutes).catch(() => setRoutes([]));
  }, [travelId]);

  if (loading) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>찜한 관광지 {bookmarks.length}곳</h1>
        <div className="actions">
          <Link className="btn ghost" to={`/travels/${travelId}/recommendations`}>
            ← AI 추천 목록
          </Link>
          <Link className="btn ghost" to={`/travels/${travelId}/pois`}>
            관광지 더 찾기
          </Link>
        </div>
      </div>
      <ErrorBox message={error} />

      {bookmarks.length === 0 ? (
        <p className="empty">아직 찜한 관광지가 없습니다. AI 추천 목록이나 전체 관광지에서 ♡를 눌러 보세요.</p>
      ) : (
        <div className="poi-grid">
          {bookmarks.map(({ poi }) => (
            <PoiCard
              key={poi.poiId}
              poi={poi}
              right={
                <button
                  type="button"
                  className="btn small ghost"
                  disabled={pending.has(poi.poiId)}
                  onClick={() => toggle(poi)}
                >
                  찜 취소
                </button>
              }
            />
          ))}
        </div>
      )}

      {routes.length > 0 && (
        <section className="card">
          <h2>이미 만든 경로</h2>
          <ul className="plain-list">
            {routes.map((r) => (
              <li key={r.routeId}>
                <Link to={`/travels/${travelId}/routes/${r.routeId}/edit`}>
                  {r.routeName ?? `경로 #${r.routeId}`}
                </Link>
                <span className="muted">
                  {' '}
                  · {r.dayCount}일 · {r.spotCount}곳 {r.adopted && '· 최종 채택'}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="bottom-bar">
        <span>찜 {bookmarks.length}곳으로</span>
        <button
          type="button"
          className="btn primary"
          disabled={bookmarks.length === 0}
          onClick={() => navigate(`/travels/${travelId}/routes/new`)}
        >
          새 경로 만들기 →
        </button>
      </div>
    </main>
  );
}