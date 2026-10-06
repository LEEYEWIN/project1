import { useState } from 'react';
import { addDays, busyBetween, busyOn, todayIso, toIso } from '../../utils/travelDates.js';

const WEEK = ['일', '월', '화', '수', '목', '금', '토'];

/**
 * 여행 날짜 달력 (여행 만들기 · 커뮤니티 일정 가져오기 공용)
 * - 지난 날짜와 이미 여행이 있는 날은 누를 수 없다 (같은 기간에는 여행을 하나만 만들 수 있음)
 * - 기간 선택: 시작일 → 종료일 순서로 누른다. 사이에 다른 여행이 있으면 그 기간은 고를 수 없다
 * - fixedDays: 일수가 정해진 경우(가져오기) 시작일만 누르면 종료일이 자동으로 정해진다
 * props: start, end ('YYYY-MM-DD' 또는 ''), onChange({ start, end }), busy: [{ start, end, name }]
 */
export default function DateRangePicker({ start, end, onChange, busy = [], fixedDays }) {
  const today = todayIso();
  const [month, setMonth] = useState(() => {
    const base = new Date(`${start || today}T00:00:00`);
    return new Date(base.getFullYear(), base.getMonth(), 1);
  });
  const [hint, setHint] = useState('');

  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const cells = [...Array(first.getDay()).fill(null), ...Array.from({ length: days }, (_, i) => toIso(new Date(month.getFullYear(), month.getMonth(), i + 1)))];
  const canPrev = toIso(first) > today;

  const pick = (iso) => {
    setHint('');
    if (fixedDays) {
      const last = addDays(iso, fixedDays - 1);
      const hit = busyBetween(iso, last, busy);
      if (hit) {
        setHint(`이 날짜로 시작하면 '${hit.name}' 여행(${hit.start} ~ ${hit.end})과 겹쳐요. 다른 시작일을 골라 주세요.`);
        return;
      }
      onChange({ start: iso, end: last });
      return;
    }
    if (!start || end || iso < start) {
      onChange({ start: iso, end: '' }); // 새로 시작일 고르기
      return;
    }
    const hit = busyBetween(start, iso, busy);
    if (hit) {
      setHint(`사이에 '${hit.name}' 여행(${hit.start} ~ ${hit.end})이 있어 이 기간은 고를 수 없어요.`);
      return;
    }
    onChange({ start, end: iso });
  };

  const inRange = (iso) => start && (end ? start <= iso && iso <= end : iso === start);

  return (
    <div className="date-range-picker">
      <div className="drp-head">
        <button type="button" className="drp-nav" disabled={!canPrev} aria-label="이전 달"
          onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}>‹</button>
        <strong>{month.getFullYear()}년 {month.getMonth() + 1}월</strong>
        <button type="button" className="drp-nav" aria-label="다음 달"
          onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}>›</button>
      </div>
      <div className="drp-grid" role="grid">
        {WEEK.map((w) => <span key={w} className="drp-week">{w}</span>)}
        {cells.map((iso, i) => {
          if (!iso) return <span key={`b${i}`} />;
          const trip = busyOn(iso, busy);
          const past = iso < today;
          const cls = ['drp-day', past && 'past', trip && 'busy', inRange(iso) && 'on', iso === start && 'start', iso === end && 'end']
            .filter(Boolean).join(' ');
          return (
            <button key={iso} type="button" className={cls} disabled={past || Boolean(trip)} onClick={() => pick(iso)}
              title={trip ? `'${trip.name}' 여행이 있는 날` : past ? '지난 날짜' : undefined} aria-pressed={Boolean(inRange(iso))}>
              {Number(iso.slice(8))}
            </button>
          );
        })}
      </div>
      <p className="drp-legend">
        <span className="drp-key busy" /> 이미 여행이 있는 날 <span className="drp-key past" /> 지난 날짜 · 같은 기간에는 여행을 하나만 만들 수 있어요
      </p>
      {!fixedDays && start && !end && <p className="drp-guide">종료일을 눌러 주세요. (당일 여행은 시작일을 한 번 더)</p>}
      {hint && <p className="drp-hint" role="alert">{hint}</p>}
    </div>
  );
}
