import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ensureRoute, fetchRoute, saveRoute } from '../../api/routeApi.js';
import { errorMessage } from '../../api/client.js';

/**
 * [여행에서 들어온 관광지 화면] "루트에 추가"
 * 1) 누르면 이 여행의 경로(여행당 1개, 없으면 서버가 만듦)를 불러와 날짜(일차)를 고르게 한다
 * 2) [추가]: 여행 장소에 없으면 먼저 장소로 추가(ensureBookmarked) → 그 일차 맨 뒤에 넣고 경로 저장
 *    (경로에는 여행 장소만 넣을 수 있다는 서버 규칙 때문에 장소 추가가 먼저)
 * - 일정을 확정한 여행은 추가할 수 없음
 * - 경로 어딘가에 이미 있으면 안내만 하고 저장하지 않음(한 곳은 한 번만 방문)
 */
export default function AddToRouteButton({ travelId, poi, ensureBookmarked }) {
  const [open, setOpen] = useState(false);
  const [route, setRoute] = useState(null);
  const [dayNo, setDayNo] = useState(1);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // 열 때 경로(1개) 불러오기
  useEffect(() => {
    if (!open || route) return;
    ensureRoute(travelId)
      .then(({ routeId }) => fetchRoute(routeId))
      .then(setRoute)
      .catch((e) => setError(errorMessage(e)));
  }, [open, route, travelId]);

  const add = async () => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      // 저장 직전에 최신 경로를 다시 읽는다 (경로 짜기 화면의 자동 저장 직후여도 덮어쓰지 않게)
      const fresh = await fetchRoute(route.routeId);
      setRoute(fresh);
      const already = fresh.days.find((d) => d.spots.some((s) => s.poi.poiId === poi.poiId))?.dayNo;
      if (already) {
        setMessage(`이미 ${already}일차에 들어 있어요.`);
        return;
      }
      if (fresh.locked) {
        setError('일정을 확정한 여행이라 경로를 바꿀 수 없어요.');
        return;
      }
      await ensureBookmarked(poi);
      // 이 일차만 바꾸고 나머지 일차는 그대로 보내서 전체 저장
      const days = [];
      for (let n = 1; n <= fresh.tripDays; n += 1) {
        const d = fresh.days.find((x) => x.dayNo === n);
        const ids = d ? d.spots.map((s) => s.poi.poiId) : [];
        if (n === dayNo) ids.push(poi.poiId);
        if (ids.length > 0) days.push({ dayNo: n, poiIds: ids });
      }
      const saved = await saveRoute(fresh.routeId, { routeName: fresh.routeName ?? '', days });
      setRoute(saved);
      setMessage(`${dayNo}일차 마지막에 추가했어요.`);
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
      {!route && !error && <p className="hint small">경로를 불러오는 중…</p>}

      {route?.locked && <p className="hint small">일정을 확정한 여행이라 경로를 바꿀 수 없어요.</p>}

      {route && !route.locked && (
        <>
          <label className="add-route-field">
            날짜
            <select value={dayNo} onChange={(e) => setDayNo(Number(e.target.value))}>
              {Array.from({ length: route.tripDays }, (_, i) => i + 1).map((n) => (
                <option key={n} value={n}>
                  {n}일차 ({route.days.find((d) => d.dayNo === n)?.spots.length ?? 0}곳)
                </option>
              ))}
            </select>
          </label>
          <button type="button" className="btn primary small" disabled={busy} onClick={add}>
            {busy ? '추가 중…' : '추가'}
          </button>
        </>
      )}

      {message && (
        <p className="hint small" role="status">
          {message} <Link to={`/travels/${travelId}/route`}>경로에서 순서 바꾸기</Link>
        </p>
      )}
      {error && <p className="hint small error-text" role="alert">{error}</p>}

      <button type="button" className="btn ghost small" onClick={() => setOpen(false)}>
        닫기
      </button>
    </div>
  );
}
