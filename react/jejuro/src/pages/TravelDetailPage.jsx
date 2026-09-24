import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { adoptRoute, deleteTravel, fetchTravelDetail } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate } from '../utils/format.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

const STATUS = { COMPLETED: '모두 다녀옴', PARTIAL: '일부만 다녀옴', NOT_TAKEN: '가지 않음' };

/**
 * 7페이지(상세): 여행 기록
 * - 만든 경로 중 하나를 골라 → 아래 큰 버튼으로 [지도 보기] / [최종 경로로 채택]
 * - 채택은 확정: 이후 경로 편집·새 경로 만들기·채택 변경 불가
 * - 후기는 여행 종료일부터 작성 가능
 * - 채택한 경로의 일차별 일정 표시
 * - 후기(8페이지)로 이동, 여행 삭제
 */
export default function TravelDetailPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [travel, setTravel] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [selected, setSelected] = useState(null); // 선택한 경로 id (지도 보기·채택 대상)

  const load = useCallback(() => {
    fetchTravelDetail(travelId)
      .then((t) => {
        setTravel(t);
        // 채택된 경로가 있으면 그 경로, 없으면 관광지가 있는 첫 경로를 기본 선택
        const first = t.routes.find((r) => r.adopted) ?? t.routes.find((r) => r.spotCount > 0);
        setSelected((cur) => cur ?? first?.routeId ?? null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [travelId]);

  useEffect(load, [load]);

  const adopt = async () => {
    if (!selected) return;
    if (!window.confirm('이 경로를 최종 경로로 채택할까요?\n채택하면 이 여행의 경로는 더 이상 수정할 수 없습니다.')) return;
    setBusy(true);
    setError('');
    try {
      await adoptRoute(travelId, selected);
      load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!window.confirm('이 여행과 경로·찜·후기를 모두 삭제할까요? 되돌릴 수 없습니다.')) return;
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
  const locked = t.routes.some((r) => r.adopted); // 채택 후에는 경로 수정 불가
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
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

      <nav className="quick-links">
        <Link to={`/travels/${travelId}/recommendations`}>추천 목록</Link>
        <Link to={`/travels/${travelId}/bookmarks`}>찜 목록</Link>
        {!locked && <Link to={`/travels/${travelId}/routes/new`}>새 경로 만들기</Link>}
      </nav>

      <ErrorBox message={error} />

      {/* 경로 목록: 하나를 골라 아래 큰 버튼으로 지도 보기 / 최종 채택 */}
      <section className="card">
        <h2>만든 경로</h2>
        {locked && <p className="hint">최종 경로가 채택되어 경로를 더 이상 수정할 수 없습니다.</p>}
        {t.routes.length === 0 ? (
          <p className="empty">아직 경로가 없습니다. 찜 목록에서 경로를 만들어 보세요.</p>
        ) : (
          <ul className="route-list selectable">
            {t.routes.map((r) => {
              const empty = r.spotCount === 0;
              const on = selected === r.routeId;
              return (
                <li
                  key={r.routeId}
                  className={[r.adopted && 'adopted', on && 'on', empty && 'empty-route'].filter(Boolean).join(' ')}
                  onClick={() => !empty && setSelected(r.routeId)}
                >
                  <label className="route-pick">
                    <input
                      type="radio"
                      name="route"
                      checked={on}
                      disabled={empty}
                      onChange={() => setSelected(r.routeId)}
                    />
                    <span>
                      <strong>{r.routeName ?? `경로 #${r.routeId}`}</strong>
                      {r.adopted && <span className="badge">최종 경로</span>}
                      <small>{empty ? '관광지가 없는 경로 · 편집해서 채워 주세요' : `${r.dayCount}일 · ${r.spotCount}곳`}</small>
                    </span>
                  </label>
                  {!locked && (
                    <Link
                      className="btn small ghost"
                      to={`/travels/${travelId}/routes/${r.routeId}/edit`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      편집
                    </Link>
                  )}
                </li>
              );
            })}
          </ul>
        )}

        {t.routes.length > 0 && (
          <div className="route-actions">
            <button
              type="button"
              className="btn big ghost"
              disabled={!selected}
              onClick={() => navigate(`/travels/${travelId}/routes/${selected}/map`)}
            >
              지도에서 동선 보기
            </button>
            <button
              type="button"
              className="btn big primary"
              disabled={busy || !selected || locked}
              onClick={adopt}
            >
              {locked ? '최종 경로 채택 완료' : '최종 경로로 채택'}
            </button>
          </div>
        )}
      </section>

      {/* 채택한 경로 일정 */}
      {t.adoptedRoute && (
        <section className="card">
          <h2>최종 일정 · {t.adoptedRoute.routeName ?? `경로 #${t.adoptedRoute.routeId}`}</h2>
          {t.adoptedRoute.days.map((d) => (
            <div key={d.dayNo} className="timeline-day">
              <h3>
                {d.dayNo}일차 <small>{formatDate(d.date)}</small>
              </h3>
              <ol className="timeline">
                {d.spots.map((s) => (
                  <li key={s.poi.poiId}>
                    <span className="order small">{s.visitOrder}</span> {s.poi.name}
                    <small className="muted"> · {s.poi.regionName}</small>
                  </li>
                ))}
              </ol>
            </div>
          ))}
        </section>
      )}

      {/* 후기 */}
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
          <p className="muted">최종 경로를 채택하면 여행 종료일부터 후기를 남길 수 있습니다.</p>
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