import { useMemo, useState } from 'react';
import KakaoMap from '../map/KakaoMap.jsx';
import { formatDate } from '../../utils/format.js';

/**
 * [8페이지 후기 게시판] 후기 글에 첨부된 여행의 최종 경로
 * route: RouteDetailResponse { tripDays, days: [{ dayNo, date, spots: [{ visitOrder, poi }] }] }
 * 처음엔 접혀 있고, "경로 보기"를 누르면 일차 탭 + 지도 + 방문 순서가 펼쳐진다.
 */
export default function PostRoute({ travelName, route }) {
  const [open, setOpen] = useState(false);
  const [dayNo, setDayNo] = useState(route.days[0]?.dayNo ?? null);

  const spotCount = route.days.reduce((sum, d) => sum + d.spots.length, 0);
  const day = route.days.find((d) => d.dayNo === dayNo);
  const points = useMemo(
    () => (day ? day.spots.map((s) => ({ lat: Number(s.poi.latitude), lng: Number(s.poi.longitude), name: s.poi.name })) : []),
    [day]
  );

  if (route.days.length === 0) return null;

  return (
    <div className="post-route">
      <button type="button" className="post-route-toggle" onClick={() => setOpen((v) => !v)}>
        <span>
          🗺 <strong>{travelName ?? '여행'}</strong> 경로 · {route.tripDays}일 · {spotCount}곳
        </span>
        <span>{open ? '접기 ▲' : '경로 보기 ▼'}</span>
      </button>

      {/* 접혀 있을 때: 일차별 한 줄 요약 */}
      {!open && (
        <ul className="post-route-summary">
          {route.days.map((d) => (
            <li key={d.dayNo}>
              <b>{d.dayNo}일차</b> {d.spots.map((s) => s.poi.name).join(' → ')}
            </li>
          ))}
        </ul>
      )}

      {open && (
        <div className="post-route-body">
          <div className="chips">
            {route.days.map((d) => (
              <button
                key={d.dayNo}
                type="button"
                className={d.dayNo === dayNo ? 'chip on' : 'chip'}
                onClick={() => setDayNo(d.dayNo)}
              >
                {d.dayNo}일차 <small>{formatDate(d.date)}</small>
              </button>
            ))}
          </div>
          <KakaoMap points={points} path={points} height={280} />
          <ol className="timeline">
            {day?.spots.map((s) => (
              <li key={s.poi.poiId}>
                <span className="order small">{s.visitOrder}</span> {s.poi.name}
                <small className="muted"> · {s.poi.regionName}</small>
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  );
}