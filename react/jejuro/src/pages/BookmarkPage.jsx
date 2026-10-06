import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchRoute } from '../api/routeApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { useAuth } from '../auth/AuthContext.jsx';
import useBookmarks, { lockText } from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 4페이지: 여행 장소 (예전 이름: 찜 목록)
 * - 이 여행의 일정(경로)에 넣을 관광지 목록. 카드마다 경로에 배치됐는지(N일차 M번 / 아직 배치 전) 표시
 * - [장소에서 빼기]: 경로에 배치되어 있었다면 경로에서도 빠진다
 * - [경로 짜기 →]: 여행당 경로 1개(/travels/:id/route). 모든 장소를 배치해야 일정을 확정할 수 있다
 * - 확정한 여행도 출발 전날까지는 장소를 바꿀 수 있다 (새로 담은 장소는 배치해야 확정 일정에 들어감)
 * - 출발일 당일부터, 또는 새 여행으로 바꾸는 중인 변경 전 여행은 장소를 바꿀 수 없다
 */
export default function BookmarkPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
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

  const confirmed = Boolean(travel?.adoptedRoute); // 일정 확정
  const locked = Boolean(travel?.editLocked); // 수정 잠금
  const placed = bookmarks.filter(({ poi }) => placedAt.has(poi.poiId)).length;
  const unplaced = bookmarks.length - placed;

  const remove = async (poi) => {
    const where = placedAt.get(poi.poiId);
    if (where && !window.confirm(`${poi.name}은(는) 경로 ${where}에 있어요.\n장소에서 빼면 경로에서도 빠집니다. 뺄까요?`)) return;
    await toggle(poi);
    reload();
  };

  return (
    <main className="page wide bookmarks-page">
      <div className="title-row">
        <h1 className="bookmark-page-title">
          <span className="bookmark-title-context">{user?.nickname || '여행자'}님이 선택한 ‘{travel?.travelName || '여행'}’의</span>{' '}
          <span>여행 장소 {bookmarks.length}곳</span>
        </h1>
        <div className="actions">
          {!travel?.imported && (
            <Link className="btn ghost" to={`/travels/${travelId}/recommendations`}>
              ← 이전
            </Link>
          )}
        </div>
      </div>

      <section className="bookmark-guide" aria-labelledby="bookmark-guide-title">
        <div className="bookmark-guide-copy">
          <span className="bookmark-guide-label">{confirmed ? '확정된 여행' : '여행 일정 준비'}</span>
          <h2 id="bookmark-guide-title">
            {confirmed ? '여행 일정이 확정되었어요' : '담아 둔 장소로 나만의 여행 일정을 만들어요'}
          </h2>
          <p>
            {locked
              ? `${lockText(travel)} 여행 상세에서 일정을 확인해 보세요.`
              : confirmed
                ? '출발 전날까지는 장소를 더하거나 빼고 경로를 고칠 수 있어요. 새로 담은 장소는 경로 짜기에서 배치해야 확정 일정에 들어가요.'
                : '아래 장소들이 이번 여행에 포함돼요. ‘경로 짜기’에서 방문 날짜와 순서를 정하고, 모든 장소를 배치하면 일정을 확정할 수 있어요.'}
          </p>
          {!locked && !travel?.imported && (
            <p className="bookmark-guide-help">장소를 더 추가하려면 상단 ‘이전’ 버튼으로 AI 추천 목록에 돌아가세요.</p>
          )}
        </div>
        {bookmarks.length > 0 && !locked && (
          <div className="bookmark-placement">
            <dl>
              <div><dt>경로에 배치</dt><dd>{placed}<small>곳</small></dd></div>
              <div className={unplaced > 0 ? 'needs-placement' : ''}><dt>아직 배치 전</dt><dd>{unplaced}<small>곳</small></dd></div>
            </dl>
            {unplaced === 0 && (
              <p className="bookmark-placement-done">
                {confirmed ? '모든 장소가 확정 일정에 들어 있어요.' : '모든 장소를 배치했어요. 경로 화면에서 일정을 확정해 주세요.'}
              </p>
            )}
            {confirmed && unplaced > 0 && (
              <p className="placement-status" role="alert">출발 전까지 배치하지 않으면 {unplaced}곳은 확정 일정에서 빠집니다.</p>
            )}
          </div>
        )}
      </section>
      <ErrorBox message={error} />

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
          {locked ? '일정은 여행 상세에서 볼 수 있어요.' : `여행 장소 ${bookmarks.length}곳으로 날짜별 경로를 짜요`}
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
