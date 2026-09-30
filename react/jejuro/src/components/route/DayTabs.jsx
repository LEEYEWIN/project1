import { formatDate } from '../../utils/format.js';

/** 1일차 / 2일차 ... 탭. counts[i] = i일차 방문지 수 */
export default function DayTabs({ startDate, tripDays, current, onChange, counts = [] }) {
  const start = new Date(`${startDate}T00:00:00`);
  return (
    <div className="day-tabs" role="tablist">
      {Array.from({ length: tripDays }, (_, i) => {
        const d = new Date(start);
        d.setDate(start.getDate() + i);
        const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        return (
          <button
            key={i}
            type="button"
            role="tab"
            aria-selected={current === i}
            className={current === i ? 'day-tab on' : 'day-tab'}
            onClick={() => onChange(i)}
          >
            <strong>{i + 1}일차</strong>
            <small>
              {formatDate(iso)}
              {counts[i] ? ` · ${counts[i]}곳` : ''}
            </small>
          </button>
        );
      })}
    </div>
  );
}