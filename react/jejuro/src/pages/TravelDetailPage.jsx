import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { adoptRoute, deleteTravel, fetchTravelDetail } from '../api/travelApi.js';
import { fetchRoute } from '../api/routeApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate } from '../utils/format.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

const STATUS = { COMPLETED: '모두 다녀옴', PARTIAL: '일부만 다녀옴', NOT_TAKEN: '가지 않음' };

/**
 * 7페이지(상세): 여행 한 개 = 경로 한 개
 * 진행 단계: ① 여행 장소 추가 → ② 장소를 날짜별로 모두 배치(경로 짜기, 자동 저장) → ③ 일정 확정 → ④ 후기
 * - 일정 확정(= 최종 경로 채택)은 여행 장소가 모두 배치되어야 가능. 확정하면 경로·장소를 바꿀 수 없다
 * - 확정 전에는 지금 경로를 아래에서 미리 보고 확정
 * - 커뮤니티에서 가져온 여행은 설문이 없어 AI 추천 목록 링크를 숨긴다
 */
export default function TravelDetailPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [travel, setTravel] = useState(null);
  const [route, setRoute] = useState(null); // 확정 전 경로 미리보기
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    fetchTravelDetail(travelId)
      .then(async (t) => {
        setTravel(t);
        if (t.route && !t.adoptedRoute) setRoute(await fetchRoute(t.route.routeId));
      })
      .catch((e) => setError(errorMessage(e)));
  }, [travelId]);

  useEffect(load, [load]);

  const adopt = async () => {
    if (!window.confirm('이 경로로 일정을 확정할까요?\n확정하면 경로와 여행 장소를 더 이상 바꿀 수 없습니다.')) return;
    setBusy(true);
    setError('');
    try {
      await adoptRoute(travelId, travel.route.routeId);
      load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!window.confirm('이 여행과 경로·여행 장소·후기를 모두 삭제할까요? 되돌릴 수 없습니다.')) return;
    try {
      await deleteTravel(travelId);
      navigate('/travels', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  if (error && !travel) return <main className="page"><ErrorBox message={error} /></main>;
  if (!travel) return <main className="page"><Loading /></main>;

  const t = travel;
  const locked = Boolean(t.adoptedRoute);
  const unplaced = t.placeCount - t.placedCount;
  const canAdopt = !locked && t.route && t.placeCount > 0 && unplaced === 0;
  const shownRoute = locked ? t.adoptedRoute : route;
  const hasSpots = (t.route?.spotCount ?? 0) > 0;
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const spotResult = new Map((locked ? t.feedback?.spots ?? [] : []).map((sp) => [sp.poiId, sp]));
  const visitedCount = (t.feedback?.spots ?? []).filter((sp) => sp.visited).length;
  const step = locked ? 3 : t.placeCount === 0 ? 0 : unplaced > 0 ? 1 : 2;
  const STEPS = ['여행 장소 추가', '날짜별로 모두 배치', '일정 확정'];

  return (
    <main className="page">
      <div className="title-row">
        <h1>{t.travelName}</h1>
        <button type="button" className="btn ghost small danger" onClick={remove}>
          여행 삭제
        </button>
      </div>
      <p className="muted">
        {formatDate(t.startDate)} ~ {formatDate(t.endDate)} ({t.tripDays}일) · {t.regionNames.join(', ')}
        {t.companions.length > 0 &&
          ` · 동반: ${t.companions.map((c) => `${c.relation}(${c.ageGroup})`).join(', ')}`}
      </p>
      {t.imported && (
        <p className="imported-note">
          커뮤니티 글의 경로를 가져와 만든 여행이에요.{' '}
          {t.sourcePostId && <Link to={`/community/posts/${t.sourcePostId}`}>원래 글 보기</Link>}
        </p>
      )}

      <nav className="quick-links">
        {!t.imported && <Link to={`/travels/${travelId}/recommendations`}>AI 추천 목록</Link>}
        <Link to={`/travels/${travelId}/bookmarks`}>여행 장소 {t.placeCount}곳</Link>
        {!locked && <Link to={`/travels/${travelId}/pois`}>관광지 더 찾기</Link>}
      </nav>

      <ErrorBox message={error} />

      <section className="card">
        <h2>여행 일정</h2>
        <ol className="progress-steps">
          {STEPS.map((label, i) => (
            <li key={label} className={i < step ? 'done' : i === step ? 'now' : ''}>
              <span>{i < step ? '✓' : i + 1}</span>
              {label}
              {i === 1 && t.placeCount > 0 && <small>{t.placedCount}/{t.placeCount}곳</small>}
            </li>
          ))}
        </ol>

        {locked ? (
          <p className="hint">일정을 확정했어요. 경로와 여행 장소는 더 이상 바꿀 수 없어요.</p>
        ) : t.placeCount === 0 ? (
          <p className="hint">먼저 AI 추천 목록이나 관광지 목록에서 [+ 장소 추가]로 이 여행에 갈 곳을 담아 주세요.</p>
        ) : unplaced > 0 ? (
          <p className="hint">
            여행 장소 {t.placeCount}곳 중 <b>{unplaced}곳</b>이 아직 경로에 없어요. 모두 날짜별로 배치하면 일정을 확정할 수 있어요.
          </p>
        ) : (
          <p className="hint">모든 장소를 배치했어요. 아래 일정을 확인하고 확정하세요.</p>
        )}

        <div className="route-actions">
          {locked ? (
            <button
              type="button"
              className="btn big ghost"
              onClick={() => navigate(`/travels/${travelId}/routes/${t.adoptedRoute.routeId}/map`)}
            >
              지도에서 동선·숙소 보기
            </button>
          ) : (
            <button
              type="button"
              className="btn big ghost"
              disabled={t.placeCount === 0}
              onClick={() => navigate(`/travels/${travelId}/route`)}
            >
              {hasSpots ? '경로 이어서 짜기' : '경로 짜기'}
            </button>
          )}
          <button
            type="button"
            className="btn big primary"
            disabled={busy || !canAdopt}
            title={!canAdopt && !locked ? '여행 장소를 모두 배치해야 확정할 수 있어요' : undefined}
            onClick={adopt}
          >
            {locked ? '일정 확정 완료' : '이 경로로 일정 확정'}
          </button>
        </div>
      </section>

      {shownRoute && shownRoute.days.length > 0 && (
        <section className="card">
          <h2>
            {locked ? '확정한 일정' : '지금 경로 미리보기'}
            <small>{shownRoute.routeName ?? ''}</small>
          </h2>
          {shownRoute.days.map((d) => (
            <div key={d.dayNo} className="timeline-day">
              <h3>
                {d.dayNo}일차 <small>{formatDate(d.date)}</small>
              </h3>
              <ol className="timeline">
                {d.spots.map((s) => {
                  const result = spotResult.get(s.poi.poiId); // 후기의 관광지별 결과
                  return (
                    <li key={s.poi.poiId} className={result?.visited === false ? 'spot-missed' : ''}>
                      <span className="order small">{s.visitOrder}</span> <span className="spot-name">{s.poi.name}</span>
                      <small className="muted"> · {s.poi.regionName}</small>
                      {result?.visited === false && <span className="tag">못 감</span>}
                      {result?.reaction === 'LIKE' && <span className="tag on">좋았어요</span>}
                      {result?.reaction === 'DISLIKE' && <span className="tag">아쉬워요</span>}
                    </li>
                  );
                })}
              </ol>
            </div>
          ))}
          {locked && (t.feedback?.spots ?? []).length > 0 && (
            <p className="hint">
              다녀온 곳 {visitedCount}/{t.feedback.spots.length}곳
            </p>
          )}
          {!locked && (
            <Link className="btn ghost small" to={`/travels/${travelId}/routes/${shownRoute.routeId}/map`}>
              지도에서 동선·숙소 보기
            </Link>
          )}
        </section>
      )}

      <section className="card">
        <h2>다녀온 후기</h2>
        {t.feedback ? (
          <p>
            {STATUS[t.feedback.executionStatus]}
            {t.feedback.satisfactionScore && ` · 만족도 ${'★'.repeat(t.feedback.satisfactionScore)}`}{' '}
            <Link to={`/travels/${travelId}/feedback`}>수정</Link>
          </p>
        ) : t.canWriteFeedback ? (
          <Link className="btn primary" to={`/travels/${travelId}/feedback`}>
            후기 남기기
          </Link>
        ) : !locked ? (
          <p className="muted">일정을 확정하면 여행 종료일부터 후기를 남길 수 있습니다.</p>
        ) : (
          <p className="muted">
            {formatDate(t.endDate)}(여행 종료일)부터 후기를 남길 수 있습니다.
            {today < t.endDate && ' 즐거운 여행 되세요!'}
          </p>
        )}
      </section>
    </main>
  );
}