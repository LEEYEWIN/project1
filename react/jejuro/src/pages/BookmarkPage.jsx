import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchRoute } from '../api/routeApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import PlaceGuide from '../components/common/PlaceGuide.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 4페이지: 여행 장소 (예전 이름: 찜 목록)
 * - 이 여행의 일정(경로)에 넣을 관광지 목록. 카드마다 경로에 배치됐는지(N일차 M번 / 아직 배치 전) 표시
 * - [장소에서 빼기]: 경로에 배치되어 있었다면 경로에서도 빠진다
 * - [경로 짜기 →]: 여행당 경로 1개(/travels/:id/route). 모든 장소를 배치해야 일정을 확정할 수 있다
 * - 일정을 확정한 여행은 장소를 바꿀 수 없다
 */
export default function BookmarkPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const { bookmarks, loading, error, toggle, pending, reload } = useBookmarks(travelId);
  const [travel, setTravel] = useState(null);
  const [route, setRoute] = useState(null);

  // 여행(확정 여부·경로 번호) → 경로 상세(배치 위치)
  useEffect(() => {
    let cancelled = false;
    fetchTravelDetail(travelId)
      .then(async (t) => {
        if (cancelled) return;
        setTravel(t);
        if (t.route) {
          const r = await fetchRoute(t.route.routeId);
          if (!cancelled) setRoute(r);
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [travelId, bookmarks.length]);

  // poiId → "N일차 M번"
  const placedAt = useMemo(() => {
    const map = new Map();
    route?.days.forEach((d) => d.spots.forEach((s) => map.set(s.poi.poiId, `${d.dayNo}일차 ${s.visitOrder}번`)));
    return map;
  }, [route]);

  if (loading) return <main className="page"><Loading /></main>;

  const locked = Boolean(travel?.adoptedRoute);
  const placed = bookmarks.filter(({ poi }) => placedAt.has(poi.poiId)).length;
  const unplaced = bookmarks.length - placed;

  const remove = async (poi) => {
    const where = placedAt.get(poi.poiId);
    if (where && !window.confirm(`${poi.name}은(는) 경로 ${where}에 있어요.\n장소에서 빼면 경로에서도 빠집니다. 뺄까요?`)) return;
    await toggle(poi);
    reload();
  };

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>여행 장소 {bookmarks.length}곳</h1>
        <div className="actions">
          {!travel?.imported && (
            <Link className="btn ghost" to={`/travels/${travelId}/recommendations`}>
              ← AI 추천 목록
            </Link>
          )}
          {!locked && (
            <Link className="btn ghost" to={`/travels/${travelId}/pois`}>
              관광지 더 찾기
            </Link>
          )}
        </div>
      </div>

      {locked ? (
        <p className="hint">일정을 확정한 여행이라 장소를 바꿀 수 없어요.</p>
      ) : (
        <PlaceGuide travelId={travelId} count={bookmarks.length} showLink={false} />
      )}
      <ErrorBox message={error} />

      {bookmarks.length > 0 && !locked && (
        <p className={unplaced === 0 ? 'placement-status done' : 'placement-status'}>
          {unplaced === 0
            ? `모든 장소(${bookmarks.length}곳)가 경로에 배치됐어요. 경로 화면에서 일정을 확정할 수 있어요.`
            : `경로에 배치 ${placed}곳 · 아직 배치 전 ${unplaced}곳`}
        </p>
      )}

      {bookmarks.length === 0 ? (
        <p className="empty">아직 추가한 장소가 없어요. AI 추천 목록이나 전체 관광지에서 [+ 장소 추가]를 눌러 보세요.</p>
      ) : (
        <div className="poi-grid">
          {bookmarks.map(({ poi }) => (
            <PoiCard
              key={poi.poiId}
              poi={poi}
              to={`/travels/${travelId}/pois/${poi.poiId}`}
              right={
                !locked && (
                  <button
                    type="button"
                    className="btn small ghost"
                    disabled={pending.has(poi.poiId)}
                    onClick={() => remove(poi)}
                  >
                    장소에서 빼기
                  </button>
                )
              }
            >
              <span className={placedAt.has(poi.poiId) ? 'tag on' : 'tag'}>
                {placedAt.has(poi.poiId) ? `경로 ${placedAt.get(poi.poiId)}` : '아직 배치 전'}
              </span>
            </PoiCard>
          ))}
        </div>
      )}

      <div className="bottom-bar">
        <span>
          {locked ? '확정된 일정은 여행 상세에서 볼 수 있어요.' : `여행 장소 ${bookmarks.length}곳으로 날짜별 경로를 짜요`}
        </span>
        {locked ? (
          <Link className="btn primary" to={`/travels/${travelId}`}>
            여행 상세로 →
          </Link>
        ) : (
          <button
            type="button"
            className="btn primary"
            disabled={bookmarks.length === 0}
            onClick={() => navigate(`/travels/${travelId}/route`)}
          >
            {route && route.days.length > 0 ? '경로 이어서 짜기 →' : '경로 짜기 →'}
          </button>
        )}
      </div>
    </main>
  );
}
