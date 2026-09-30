import { useMemo, useState } from 'react';
import KakaoMap from '../map/KakaoMap.jsx';
import { formatDate } from '../../utils/format.js';

/**
 * [커뮤니티 상세] 첨부된 최종 경로: 일차별 한 줄 + 펼치면 그날 지도
 * route: RouteDetailResponse
 */
export default function RouteDayList({ route }) {
  const [openDay, setOpenDay] = useState(null);

  if (!route || route.days.length === 0) return null;

  return (
    <ul className="cm-days">
      {route.days.map((d) => (
        <DayRow key={d.dayNo} day={d} open={openDay === d.dayNo} onToggle={() => setOpenDay(openDay === d.dayNo ? null : d.dayNo)} />
      ))}
    </ul>
  );
}

function DayRow({ day, open, onToggle }) {
  const points = useMemo(
    () => day.spots.map((s) => ({ lat: Number(s.poi.latitude), lng: Number(s.poi.longitude), name: s.poi.name })),
    [day]
  );
  return (
    <li className="cm-day">
      <button type="button" className="cm-day-head" onClick={onToggle} aria-expanded={open}>
        <strong>{day.dayNo}일차</strong>
        <span className="cm-day-date">{day.date ? formatDate(day.date) : ''}</span>
        <span className="cm-day-names">{day.spots.map((s) => s.poi.name).join(' → ')}</span>
        <span className={open ? 'cm-caret up' : 'cm-caret'} aria-hidden="true" />
      </button>
      {open && (
        <div className="cm-day-body">
          <KakaoMap points={points} path={points} height={280} />
        </div>
      )}
    </li>
  );
}