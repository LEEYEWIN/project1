import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchRoute, fetchRoutes, saveRoute } from '../../api/routeApi.js';
import { errorMessage } from '../../api/client.js';

/**
 * [여행에서 들어온 관광지 화면] "루트에 추가"
 * 1) 누르면 이 여행의 경로 목록을 불러와 경로·일차를 고르게 한다
 * 2) [추가]: 아직 찜하지 않았으면 먼저 찜(ensureBookmarked) → 그 일차 맨 뒤에 넣고 경로 저장
 *    (루트에는 찜한 관광지만 넣을 수 있다는 서버 규칙 때문에 찜이 먼저)
 * - 최종 경로를 채택한 여행은 추가할 수 없음
 * - 경로가 없으면 "새 경로 만들기"로 안내
 * - 같은 일차에 이미 있으면 안내만 하고 저장하지 않음
 */
export default function AddToRouteButton({ travelId, poi, ensureBookmarked }) {
  const [open, setOpen] = useState(false);
  const [routes, setRoutes] = useState(null);
  const [routeId, setRouteId] = useState('');
  const [route, setRoute] = useState(null);
  const [dayNo, setDayNo] = useState(1);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // 열 때 경로 목록
  useEffect(() => {
    if (!open || routes) return;
    fetchRoutes(travelId)
      .then((list) => {
        setRoutes(list);
        if (list.length > 0) setRouteId(String(list[0].routeId));
      })
      .catch((e) => setError(errorMessage(e)));
  }, [open, routes, travelId]);

  // 경로를 고르면 그 경로의 일차 정보
  useEffect(() => {
    if (!routeId) return;
    setRoute(null);
    fetchRoute(routeId)
      .then((r) => {
        setRoute(r);
        setDayNo(1);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [routeId]);

  const locked = routes?.some((r) => r.adopted) || route?.locked;

  const add = async () => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const target = route.days.find((d) => d.dayNo === dayNo);
      if (target?.spots.some((s) => s.poi.poiId === poi.poiId)) {
        setMessage(`${dayNo}일차에 이미 들어 있어요.`);
        return;
      }
      await ensureBookmarked(poi);
      // 이 일차만 바꾸고 나머지 일차는 그대로 보내서 전체 저장
      const days = [];
      for (let n = 1; n <= route.tripDays; n += 1) {
        const d = route.days.find((x) => x.dayNo === n);
        const ids = d ? d.spots.map((s) => s.poi.poiId) : [];
        if (n === dayNo) ids.push(poi.poiId);
        if (ids.length > 0) days.push({ dayNo: n, poiIds: ids });
      }
      const saved = await saveRoute(route.routeId, { routeName: route.routeName ?? '', days });
      setRoute(saved);
      setMessage(`${route.routeName ?? `경로 #${route.routeId}`} ${dayNo}일차 마지막에 추가했어요.`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (!open) {
    return (
      <button type="button" className="btn ghost small add-route-btn" onClick={() => setOpen(true)}>
        + 루트에 추가
      </button>
    );
  }

  return (
    <div className="add-route" role="group" aria-label={`${poi.name} 루트에 추가`}>
      {!routes && !error && <p className="hint small">경로를 불러오는 중…</p>}

      {routes && routes.length === 0 && (
        <p className="hint small">
          아직 경로가 없어요. <Link to={`/travels/${travelId}/routes/new`}>새 경로 만들기</Link>
        </p>
      )}

      {routes && routes.length > 0 && locked && (
        <p className="hint small">최종 경로를 채택한 여행이라 루트를 바꿀 수 없어요.</p>
      )}

      {routes && routes.length > 0 && !locked && (
        <>
          <label className="add-route-field">
            경로
            <select value={routeId} onChange={(e) => setRouteId(e.target.value)}>
              {routes.map((r) => (
                <option key={r.routeId} value={r.routeId}>
                  {r.routeName ?? `경로 #${r.routeId}`} ({r.spotCount}곳)
                </option>
              ))}
            </select>
          </label>
          {route && (
            <label className="add-route-field">
              일차
              <select value={dayNo} onChange={(e) => setDayNo(Number(e.target.value))}>
                {Array.from({ length: route.tripDays }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>
                    {n}일차 ({route.days.find((d) => d.dayNo === n)?.spots.length ?? 0}곳)
                  </option>
                ))}
              </select>
            </label>
          )}
          <button type="button" className="btn primary small" disabled={!route || busy} onClick={add}>
            {busy ? '추가 중…' : '추가'}
          </button>
        </>
      )}

      {message && (
        <p className="hint small" role="status">
          {message}{' '}
          {route && <Link to={`/travels/${travelId}/routes/${route.routeId}/edit`}>순서 바꾸러 가기</Link>}
        </p>
      )}
      {error && <p className="hint small error-text" role="alert">{error}</p>}

      <button type="button" className="btn ghost small" onClick={() => setOpen(false)}>
        닫기
      </button>
    </div>
  );
}