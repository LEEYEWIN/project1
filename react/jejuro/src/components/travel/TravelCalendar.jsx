import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchJejuWeather } from '../../api/weatherApi.js';
import { weatherInfo, weatherText } from '../../utils/weather.js';

const WEEK = ['일', '월', '화', '수', '목', '금', '토'];
const COLORS = 5; // .cal-c0 ~ .cal-c4 (styles.css)

/** Date → 'YYYY-MM-DD' (내 PC 시간 기준) */
export function toIso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** 달력 첫 달: 오늘 이후 가장 가까운 여행의 달, 없으면 이번 달 */
function initialMonth(travels) {
  const today = toIso(new Date());
  const next = [...travels].filter((t) => t.endDate >= today).sort((a, b) => (a.startDate < b.startDate ? -1 : 1))[0];
  const base = next ? new Date(`${next.startDate}T00:00:00`) : new Date();
  return new Date(base.getFullYear(), base.getMonth(), 1);
}

/**
 * 내 여행 달력 (월 단위)
 * - 여행 기간을 색 막대로 표시 (여행마다 색이 다름). 막대에는 첫날·일요일에만 여행 이름
 * - 날짜를 누르면 아래에 그날 일정(N일차 방문지)을 보여 줌
 * - 날씨: 여행이 있는 날짜에만 이모지로 (제주 예보, 오늘부터 16일까지. 그 뒤 날짜는 표시 안 함)
 * travels: TravelSummaryResponse[]  { travelId, travelName, startDate, endDate, days: [{ dayNo, date, spots }] }
 */
export default function TravelCalendar({ travels }) {
  const [month, setMonth] = useState(() => initialMonth(travels));
  const [selected, setSelected] = useState(null); // 'YYYY-MM-DD'
  const [weather, setWeather] = useState(() => new Map()); // 'YYYY-MM-DD' → 예보
  const today = toIso(new Date());

  // 날씨는 실패해도 달력은 그대로 (조용히 무시)
  useEffect(() => {
    let alive = true;
    fetchJejuWeather()
      .then((res) => alive && setWeather(new Map(res.days.map((d) => [d.date, d]))))
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, []);

  // 여행마다 고정 색 (시작일 순서)
  const colorOf = useMemo(() => {
    const sorted = [...travels].sort((a, b) => (a.startDate < b.startDate ? -1 : 1));
    return new Map(sorted.map((t, i) => [t.travelId, i % COLORS]));
  }, [travels]);

  // 6주(42칸) 날짜
  const cells = useMemo(() => {
    const first = new Date(month.getFullYear(), month.getMonth(), 1);
    const start = new Date(first);
    start.setDate(1 - first.getDay());
    return Array.from({ length: 42 }, (_, i) => {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      return d;
    });
  }, [month]);

  const travelsOn = (iso) => travels.filter((t) => t.startDate <= iso && iso <= t.endDate);

  const move = (n) => {
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() + n, 1));
    setSelected(null);
  };

  const selectedTravels = selected ? travelsOn(selected) : [];

  return (
    <section className="card cal" aria-label="여행 달력">
      <div className="cal-head">
        <button type="button" className="btn small ghost" aria-label="이전 달" onClick={() => move(-1)}>
          ‹
        </button>
        <h2>
          {month.getFullYear()}년 {month.getMonth() + 1}월
        </h2>
        <button type="button" className="btn small ghost" aria-label="다음 달" onClick={() => move(1)}>
          ›
        </button>
        <button
          type="button"
          className="btn small ghost cal-today-btn"
          onClick={() => {
            const n = new Date();
            setMonth(new Date(n.getFullYear(), n.getMonth(), 1));
            setSelected(today);
          }}
        >
          오늘
        </button>
      </div>

      <div className="cal-grid" role="grid">
        {WEEK.map((w, i) => (
          <div key={w} className={`cal-week ${i === 0 ? 'sun' : i === 6 ? 'sat' : ''}`} role="columnheader">
            {w}
          </div>
        ))}
        {cells.map((d) => {
          const iso = toIso(d);
          const inMonth = d.getMonth() === month.getMonth();
          const list = travelsOn(iso);
          const cls = [
            'cal-cell',
            !inMonth && 'out',
            iso === today && 'today',
            iso === selected && 'selected',
            d.getDay() === 0 && 'sun',
            d.getDay() === 6 && 'sat',
          ]
            .filter(Boolean)
            .join(' ');
          return (
            <button key={iso} type="button" className={cls} role="gridcell" aria-label={`${iso} 여행 ${list.length}개`} onClick={() => setSelected(iso)}>
              <span className="cal-num">
                {d.getDate()}
                {list.length > 0 && weather.has(iso) && (
                  <span className="cal-weather" title={weatherText(weather.get(iso))} aria-label={weatherText(weather.get(iso))}>
                    {weatherInfo(weather.get(iso).code).emoji}
                  </span>
                )}
              </span>
              {list.slice(0, 2).map((t) => {
                const isStart = iso === t.startDate;
                const isEnd = iso === t.endDate;
                const showName = isStart || d.getDay() === 0 || d.getDate() === 1;
                return (
                  <span
                    key={t.travelId}
                    className={`cal-bar cal-c${colorOf.get(t.travelId)} ${isStart ? 'start' : ''} ${isEnd ? 'end' : ''}`}
                    title={t.travelName}
                  >
                    {showName ? t.travelName : ' '}
                  </span>
                );
              })}
              {list.length > 2 && <span className="cal-more">+{list.length - 2}</span>}
            </button>
          );
        })}
      </div>

      {selected && (
        <div className="cal-day" aria-live="polite">
          <h3>
            {Number(selected.slice(5, 7))}월 {Number(selected.slice(8, 10))}일 일정
            {selectedTravels.length > 0 && weather.has(selected) && (
              <small className="cal-weather-text">
                {' '}
                {weatherInfo(weather.get(selected).code).emoji} {weatherText(weather.get(selected))}
              </small>
            )}
          </h3>
          {selectedTravels.length === 0 ? (
            <p className="muted">이 날은 여행 일정이 없어요.</p>
          ) : (
            selectedTravels.map((t) => {
              const dayNo = Math.round((new Date(`${selected}T00:00:00`) - new Date(`${t.startDate}T00:00:00`)) / 86400000) + 1;
              const plan = t.days.find((d) => d.dayNo === dayNo);
              return (
                <div key={t.travelId} className="cal-day-item">
                  <span className={`cal-dot cal-c${colorOf.get(t.travelId)}`} aria-hidden="true" />
                  <div>
                    <Link to={`/travels/${t.travelId}`}>
                      <strong>{t.travelName}</strong>
                    </Link>{' '}
                    <small className="muted">
                      {dayNo}일차 / {t.tripDays}일
                    </small>
                    <p>{plan && plan.spots.length > 0 ? plan.spots.join(' → ') : '아직 이 날 방문지를 정하지 않았어요.'}</p>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </section>
  );
}