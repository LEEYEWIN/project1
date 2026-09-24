import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchRoute, saveRoute } from '../api/routeApi.js';
import { fetchDirections, fetchOptimizedOrder } from '../api/directionsApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate, formatDistance, formatDuration } from '../utils/format.js';
import KakaoMap from '../components/map/KakaoMap.jsx';
import LegList from '../components/map/LegList.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 6페이지: 카카오맵으로 일차별 동선과 이동수단별 시간 보기
 * - 자동차: 서버가 카카오모빌리티 길찾기 호출(실제 도로 경로)
 * - 도보: 직선거리 기반 추정(카카오 도보 API는 제휴 전용)
 * - 동선 최적화: 서버가 추천 순서를 계산 → "이 순서로 저장"하면 5페이지 저장 API 재사용
 */
export default function RouteMapPage() {
  const { travelId, routeId } = useParams();
  const [route, setRoute] = useState(null);
  const [dayNo, setDayNo] = useState(null);
  const [mode, setMode] = useState('CAR');
  const [dir, setDir] = useState(null);
  const [suggest, setSuggest] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const loadRoute = useCallback(async () => {
    const detail = await fetchRoute(routeId);
    setRoute(detail);
    setDayNo((cur) => cur ?? detail.days[0]?.dayNo ?? null);
    return detail;
  }, [routeId]);

  useEffect(() => {
    loadRoute().catch((e) => setError(errorMessage(e)));
  }, [loadRoute]);

  // 일차·이동수단이 바뀔 때마다 이동 정보 다시 조회
  useEffect(() => {
    if (!dayNo) return;
    setDir(null);
    setSuggest(null);
    fetchDirections(routeId, dayNo, mode)
      .then(setDir)
      .catch((e) => setError(errorMessage(e)));
  }, [routeId, dayNo, mode]);

  const day = route?.days.find((d) => d.dayNo === dayNo);
  const points = useMemo(
    () => (day ? day.spots.map((s) => ({ lat: Number(s.poi.latitude), lng: Number(s.poi.longitude), name: s.poi.name })) : []),
    [day]
  );

  const askOptimize = async () => {
    setBusy(true);
    try {
      setSuggest(await fetchOptimizedOrder(routeId, dayNo));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  /** 제안 순서로 저장: 이 일차만 바꾸고 나머지 일차는 그대로 보내서 전체 저장 */
  const applySuggestion = async () => {
    setBusy(true);
    try {
      const days = route.days.map((d) => ({
        dayNo: d.dayNo,
        poiIds: d.dayNo === dayNo ? suggest.poiIds : d.spots.map((s) => s.poi.poiId),
      }));
      await saveRoute(routeId, { routeName: route.routeName ?? '', days });
      await loadRoute();
      setSuggest(null);
      setDir(await fetchDirections(routeId, dayNo, mode));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (error && !route) return <main className="page"><ErrorBox message={error} /></main>;
  if (!route) return <main className="page"><Loading /></main>;
  if (route.days.length === 0) {
    return (
      <main className="page">
        <p className="empty">저장된 일정이 없습니다.</p>
        {route.locked ? (
          <Link className="btn primary" to={`/travels/${travelId}`}>여행으로 돌아가기</Link>
        ) : (
          <Link className="btn primary" to={`/travels/${travelId}/routes/${routeId}/edit`}>
            경로 편집하기
          </Link>
        )}
      </main>
    );
  }

  const nameOf = (poiId) => day?.spots.find((s) => s.poi.poiId === poiId)?.poi.name ?? poiId;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>{route.routeName ?? '여행 경로'} · 동선</h1>
        {route.locked ? (
          <span className="badge">최종 경로 · 수정 불가</span>
        ) : (
          <Link className="btn ghost" to={`/travels/${travelId}/routes/${routeId}/edit`}>
            경로 편집
          </Link>
        )}
      </div>

      <div className="day-tabs">
        {route.days.map((d) => (
          <button
            key={d.dayNo}
            type="button"
            className={d.dayNo === dayNo ? 'day-tab on' : 'day-tab'}
            onClick={() => setDayNo(d.dayNo)}
          >
            <strong>{d.dayNo}일차</strong>
            <small>
              {formatDate(d.date)} · {d.spots.length}곳
            </small>
          </button>
        ))}
      </div>

      <div className="chips">
        <button type="button" className={mode === 'CAR' ? 'chip on' : 'chip'} onClick={() => setMode('CAR')}>
          🚗 자동차
        </button>
        <button type="button" className={mode === 'WALK' ? 'chip on' : 'chip'} onClick={() => setMode('WALK')}>
          🚶 도보
        </button>
      </div>

      <div className="map-layout">
        <KakaoMap points={points} path={dir?.path ?? points} />

        <section className="card">
          {!dir ? (
            <Loading text="이동 정보를 계산하는 중…" />
          ) : (
            <>
              <p className="total">
                총 {formatDuration(dir.totalDurationSec)} · {formatDistance(dir.totalDistanceM)}
                {dir.estimated && <span className="badge">추정값</span>}
              </p>
              {dir.notice && <p className="hint">⚠ {dir.notice}</p>}
              {dir.estimated && !dir.notice && (
                <p className="hint">
                  {mode === 'WALK'
                    ? '도보 시간은 직선거리×1.3, 시속 4km로 계산한 값입니다.'
                    : '카카오 REST 키가 없어 직선거리로 추정했습니다.'}
                </p>
              )}
              <LegList legs={dir.legs} mode={mode} />
            </>
          )}

          {points.length >= 3 && !route.locked && (
            <div className="optimize">
              {!suggest ? (
                <button type="button" className="btn ghost" onClick={askOptimize} disabled={busy}>
                  효율적인 순서 추천받기
                </button>
              ) : (
                <div className="suggest">
                  <p>
                    추천 순서로 바꾸면 이동 거리가{' '}
                    <strong>
                      {formatDistance(suggest.beforeDistanceM)} → {formatDistance(suggest.afterDistanceM)}
                    </strong>
                    (직선 기준)
                  </p>
                  <ol>
                    {suggest.poiIds.map((id) => (
                      <li key={id}>{nameOf(id)}</li>
                    ))}
                  </ol>
                  <div className="actions">
                    <button type="button" className="btn ghost" onClick={() => setSuggest(null)}>
                      취소
                    </button>
                    <button
                      type="button"
                      className="btn primary"
                      onClick={applySuggestion}
                      disabled={busy || suggest.afterDistanceM >= suggest.beforeDistanceM}
                    >
                      이 순서로 저장
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>
      </div>

      <ErrorBox message={error} />

      <div className="bottom-bar">
        <span>마음에 드는 경로라면 여행 상세에서 최종 경로로 채택하세요.</span>
        <Link className="btn primary" to={`/travels/${travelId}`}>
          여행 상세로 →
        </Link>
      </div>
    </main>
  );
}