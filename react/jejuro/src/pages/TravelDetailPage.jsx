import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { adoptRoute, deleteTravel, fetchTravelDetail } from '../api/travelApi.js';
import { fetchRoute } from '../api/routeApi.js';
import { errorMessage, showError } from '../api/client.js';
import { formatDate } from '../utils/format.js';
import { addDays } from '../utils/travelDates.js';
import { lockText } from '../hooks/useBookmarks.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import ReceiptButton from '../components/travel/ReceiptButton.jsx';
import SpotWeather, { DayWeatherNote, useRouteWeather } from '../components/travel/SpotWeather.jsx';

const STATUS = { COMPLETED: '모두 다녀옴', PARTIAL: '일부만 다녀옴', NOT_TAKEN: '가지 않음' };

/**
 * 7페이지(상세): 여행 한 개 = 경로 한 개
 * 진행 단계: ① 여행 장소 추가 → ② 장소를 날짜별로 모두 배치(경로 짜기, 자동 저장) → ③ 일정 확정 → ④ 후기
 * - 일정 확정(= 최종 경로 채택)은 여행 장소가 모두 배치되어야 가능
 * - 확정 후 수정 규칙: 경로 순서·일차·장소 추가/빼기는 출발 전날까지 가능(같은 경로를 고쳐 자동 저장),
 *   여행 날짜·동행·권역·설문은 못 바꿈 → [날짜·동행 바꿔 다시 만들기], 출발일 당일부터는 모든 수정 잠금
 * - [날짜·동행 바꿔 다시 만들기]: 기존 값이 채워진 여행 만들기 화면 → 새 여행은 기존 여행과 기간이 겹쳐도 만들 수 있고,
 *   기존 여행은 '변경 전 일정'(읽기 전용)으로 남았다가 새 여행을 확정할 때 삭제된다
 * - 확정 전에는 지금 경로를 아래에서 미리 보고 확정
 * - 커뮤니티에서 가져온 여행은 설문이 없어 AI 추천 목록 링크를 숨긴다
 * - 확정한 일정·지금 경로 미리보기: 관광지 이름 옆에 그 관광지 위치·그날 날짜 기준 오전/오후 날씨 이모지 + 최저/최고 기온
 *   (지난 31일 ~ 앞으로 16일. 지난 날짜는 실제 날씨)
 */
export default function TravelDetailPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [travel, setTravel] = useState(null);
  const [route, setRoute] = useState(null); // 확정 전 경로 미리보기
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const weather = useRouteWeather(travel?.adoptedRoute ?? route); // 확정한 일정·지금 경로의 관광지별 날씨 (위치·날짜 기준)

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
    const older = travel.overlaps.filter((o) => !o.newer); // 이 여행이 대신할 변경 전 여행
    const message = older.length > 0
      ? `기존 ${older.map((o) => `'${o.travelName}'(${o.startDate} ~ ${o.endDate})`).join(', ')} 여행을 삭제하고 이 일정으로 확정할까요?\n삭제한 여행은 되돌릴 수 없습니다.`
      : '이 경로로 일정을 확정할까요?\n확정한 뒤에도 출발 전날까지는 경로 순서와 여행 장소를 고칠 수 있어요.\n여행 날짜·동행·권역·설문은 바꿀 수 없어요.';
    if (!window.confirm(message)) return;
    setBusy(true);
    setError('');
    try {
      await adoptRoute(travelId, travel.route.routeId, older.length > 0);
      load();
    } catch (e) {
      showError(e, setError); // 배치 안 된 장소·이미 확정 등(409)은 알림창
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
  const locked = Boolean(t.adoptedRoute); // 일정 확정
  const editLocked = t.editLocked; // 수정 잠금 (확정 후 출발일부터 · 바꾸는 중인 변경 전 여행)
  const unplaced = t.placeCount - t.placedCount;
  const canAdopt = !locked && !editLocked && t.route && t.placeCount > 0 && unplaced === 0;
  const older = t.overlaps.filter((o) => !o.newer); // 이 여행으로 바꿀 변경 전 여행
  const newer = t.overlaps.find((o) => o.newer); // 이 여행을 바꿔 만든 새 여행
  const shownRoute = locked ? t.adoptedRoute : route;
  const hasSpots = (t.route?.spotCount ?? 0) > 0;
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const spotResult = new Map((locked ? t.feedback?.spots ?? [] : []).map((sp) => [sp.poiId, sp]));
  const visitedCount = (t.feedback?.spots ?? []).filter((sp) => sp.visited).length;
  const step = locked ? 3 : t.placeCount === 0 ? 0 : unplaced > 0 ? 1 : 2;
  const STEPS = ['여행 장소 추가', '날짜별로 모두 배치', '일정 확정'];

  return (
    <main className="page travel-detail-page">
      <div className="title-row">
        <h1>{t.travelName}</h1>
        <div className="actions">
          {t.canReplace && (
            <button type="button" className="btn ghost small" onClick={() => navigate(`/travels/new?replace=${travelId}`)}
              title="여행 날짜·동행·권역·설문은 확정 후 바꿀 수 없어 새 여행으로 다시 만들어요">
              날짜·동행 바꿔 다시 만들기
            </button>
          )}
          <button type="button" className="btn ghost small danger" onClick={remove}>
            여행 삭제
          </button>
        </div>
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

      {newer && (
        <p className="overlap-note" role="note">
          📌 변경 전 일정이에요. <Link to={`/travels/${newer.travelId}`}>‘{newer.travelName}’</Link>
          ({formatDate(newer.startDate)} ~ {formatDate(newer.endDate)}) 여행으로 바꿔 만드는 중이라 이 여행은 보기만 할 수 있어요.
          새 여행을 확정하면 이 여행은 삭제돼요.
        </p>
      )}
      {older.length > 0 && (
        <p className="overlap-note" role="note">
          📌 {older.map((o) => (
            <span key={o.travelId}>
              <Link to={`/travels/${o.travelId}`}>‘{o.travelName}’</Link>({formatDate(o.startDate)} ~ {formatDate(o.endDate)}){' '}
            </span>
          ))}
          여행을 바꿔 만드는 새 여행이에요. 이 여행을 확정하면 기존 여행은 삭제돼요.
        </p>
      )}

      <nav className="quick-links">
        {!t.imported && <Link to={`/travels/${travelId}/recommendations`}>AI 추천 목록</Link>}
        <Link to={`/travels/${travelId}/bookmarks`}>여행 장소 {t.placeCount}곳</Link>
        {!editLocked && (
          <Link
            to={`/travels/${travelId}/pois?from=detail`}
            onClick={() => {
              try {
                sessionStorage.removeItem(`poiVisitBase:${travelId}`); // 새로 들어갈 때마다 기준을 다시 잡음
              } catch {
                /* 무시 */
              }
            }}
          >
            관광지 더 찾기
          </Link>
        )}
      </nav>

      <ErrorBox message={error} />

      <section className="card travel-plan-card">
        <div className="travel-plan-heading">
          <h2>여행 일정</h2>
          <span>{step}/3 단계 완료</span>
        </div>
        <ol className="progress-steps" aria-label="여행 일정 진행 단계">
          {STEPS.map((label, i) => (
            <li key={label} className={i < step ? 'done' : i === step ? 'now' : ''}>
              <span>{i < step ? '✓' : i + 1}</span>
              {label}
              {i === 1 && t.placeCount > 0 && <small>{t.placedCount}/{t.placeCount}곳</small>}
            </li>
          ))}
        </ol>

        {locked ? (
          <div className="travel-plan-message complete" role="status">
            <strong>모든 여행 일정 계획이 완료되었어요 😊</strong>
            <span>
              {editLocked
                ? lockText(t)
                : `출발 전날(${formatDate(addDays(t.startDate, -1))})까지는 경로 순서·일차를 바꾸고 장소를 더하거나 뺄 수 있어요. 고친 내용은 확정 일정에 바로 반영돼요.`}
            </span>
            {!editLocked && unplaced > 0 && (
              <span className="placement-status" role="alert">
                확정 후 담은 장소 {unplaced}곳이 아직 경로에 없어요. 출발 전까지 배치하지 않으면 확정 일정에서 빠집니다.
              </span>
            )}
          </div>
        ) : editLocked ? (
          <p className="travel-plan-message">{lockText(t)}</p>
        ) : t.placeCount === 0 ? (
          <p className="travel-plan-message">먼저 AI 추천 목록이나 관광지 목록에서 [+ 장소 추가]로 이 여행에 갈 곳을 담아 주세요.</p>
        ) : unplaced > 0 ? (
          <p className="travel-plan-message">
            여행 장소 {t.placeCount}곳 중 <b>{unplaced}곳</b>이 아직 경로에 없어요. 모두 날짜별로 배치하면 일정을 확정할 수 있어요.
          </p>
        ) : (
          <p className="travel-plan-message ready">모든 장소를 배치했어요. 아래 일정을 확인하고 확정하세요.</p>
        )}

        <div className={locked ? 'route-actions is-complete' : 'route-actions'}>
          {locked ? (
            <>
              <button
                type="button"
                className="btn big ghost"
                onClick={() => navigate(`/travels/${travelId}/routes/${t.adoptedRoute.routeId}/map`)}
              >
                지도에서 동선·숙소 보기
              </button>
              {!editLocked && (
                <button type="button" className="btn big primary" onClick={() => navigate(`/travels/${travelId}/route`)}>
                  확정 일정 수정
                </button>
              )}
            </>
          ) : editLocked ? null : (
            <button
              type="button"
              className="btn big ghost"
              disabled={t.placeCount === 0}
              onClick={() => navigate(`/travels/${travelId}/route`)}
            >
              {hasSpots ? '경로 이어서 짜기' : '경로 짜기'}
            </button>
          )}
          {!locked && !editLocked && (
            <button
              type="button"
              className="btn big primary"
              disabled={busy || !canAdopt}
              title={!canAdopt ? '여행 장소를 모두 배치해야 확정할 수 있어요' : undefined}
              onClick={adopt}
            >
              이 경로로 일정 확정
            </button>
          )}
        </div>
      </section>

      {shownRoute && shownRoute.days.length > 0 && (
        <section className="card travel-route-preview">
          <h2>
            {locked ? '확정한 일정' : '지금 경로 미리보기'}
            <small>{shownRoute.routeName ?? ''}</small>
          </h2>
          {shownRoute.days.map((d) => (
            <div key={d.dayNo} className="timeline-day">
              <h3>
                {d.dayNo}일차 <small>{formatDate(d.date)}</small>
                <DayWeatherNote date={d.date} />
              </h3>
              <ol className="timeline">
                {d.spots.map((s) => {
                  const result = spotResult.get(s.poi.poiId); // 후기의 관광지별 결과
                  return (
                    <li key={s.poi.poiId} className={result?.visited === false ? 'spot-missed' : ''}>
                      <span className="order small">{s.visitOrder}</span> <span className="spot-name">{s.poi.name}</span>
                      <small className="muted"> · {s.poi.regionName}</small>
                      <SpotWeather weather={weather.get(`${d.dayNo}-${s.poi.poiId}`)} />
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
          <ReceiptButton travel={t} route={shownRoute} />
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
