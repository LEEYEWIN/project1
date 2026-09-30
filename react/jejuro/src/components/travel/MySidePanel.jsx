import { Link } from 'react-router-dom';
import { formatDate } from '../../utils/format.js';
import { toIso } from './TravelCalendar.jsx';

/** 오늘부터 dateStr까지 남은 날 수 */
function daysUntil(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const now = new Date();
  return Math.round((new Date(y, m - 1, d) - new Date(now.getFullYear(), now.getMonth(), now.getDate())) / 86400000);
}

/** 여행의 다음 할 일 { text, to, button } (없으면 null) */
export function nextAction(t) {
  if (t.phase === 'AFTER') {
    if (t.adoptedRouteId && !t.hasFeedback) return { text: '다녀온 후기를 남겨 주세요', to: `/travels/${t.travelId}/feedback`, button: '후기 쓰기' };
    return null;
  }
  if (t.adoptedRouteId) return null;
  if (t.placeCount === 0) {
    return t.imported
      ? { text: '관광지를 여행 장소에 추가해 주세요', to: `/travels/${t.travelId}/pois`, button: '관광지 찾기' }
      : { text: 'AI 추천에서 여행 장소를 골라 주세요', to: `/travels/${t.travelId}/recommendations`, button: '추천 보기' };
  }
  if (t.spotCount < t.placeCount) {
    return { text: `여행 장소 ${t.placeCount - t.spotCount}곳을 날짜별로 배치해 주세요`, to: `/travels/${t.travelId}/route`, button: '경로 짜기' };
  }
  return { text: '경로를 확인하고 일정을 확정해 주세요', to: `/travels/${t.travelId}`, button: '일정 확정' };
}

/**
 * 내 여행 달력 옆 상자
 * ① 프로필: 닉네임 + 여행 통계(계획 중·다녀옴·후기)
 * ② 다가오는 여행: D-day, 기간, 진행 단계(장소 → 배치 → 확정), 다음 할 일 버튼
 * ③ 할 일: 여행마다 다음 단계(장소 고르기·경로 배치·일정 확정·후기 쓰기)
 */
export default function MySidePanel({ me, travels }) {
  const today = toIso(new Date());
  const upcoming = [...travels]
    .filter((t) => t.endDate >= today)
    .sort((a, b) => (a.startDate < b.startDate ? -1 : 1))[0];

  const planning = travels.filter((t) => t.phase !== 'AFTER').length;
  const done = travels.filter((t) => t.phase === 'AFTER').length;
  const reviews = travels.filter((t) => t.hasFeedback).length;

  const todos = travels
    .map((t) => ({ t, a: nextAction(t) }))
    .filter((x) => x.a && x.t.travelId !== upcoming?.travelId)
    .slice(0, 4);

  return (
    <aside className="side">
      <section className="side-box profile">
        <p className="side-hello">
          <b>{me?.nickname ?? '여행자'}</b> 님의 제주
        </p>
        <dl className="side-stats">
          <div>
            <dt>계획·진행 중</dt>
            <dd>{planning}</dd>
          </div>
          <div>
            <dt>다녀온 여행</dt>
            <dd>{done}</dd>
          </div>
          <div>
            <dt>남긴 후기</dt>
            <dd>{reviews}</dd>
          </div>
        </dl>
        <Link className="btn primary small side-new" to="/travels/new">
          + 새 여행 만들기
        </Link>
      </section>

      <section className="side-box">
        <h3>다가오는 여행</h3>
        {upcoming ? (
          <UpcomingCard t={upcoming} />
        ) : (
          <p className="muted small-text">예정된 여행이 없어요. 새 여행을 만들어 보세요.</p>
        )}
      </section>

      <section className="side-box">
        <h3>할 일</h3>
        {todos.length === 0 ? (
          <p className="muted small-text">지금은 할 일이 없어요.</p>
        ) : (
          <ul className="todo-list">
            {todos.map(({ t, a }) => (
              <li key={t.travelId}>
                <span>
                  <b>{t.travelName}</b>
                  <small>{a.text}</small>
                </span>
                <Link className="btn small ghost" to={a.to}>
                  {a.button}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </aside>
  );
}

function UpcomingCard({ t }) {
  const d = daysUntil(t.startDate);
  const action = nextAction(t);
  const steps = [
    { label: '여행 장소', done: t.placeCount > 0, note: `${t.placeCount}곳` },
    { label: '날짜별 배치', done: t.placeCount > 0 && t.spotCount >= t.placeCount, note: `${t.spotCount}/${t.placeCount}` },
    { label: '일정 확정', done: Boolean(t.adoptedRouteId) },
  ];
  return (
    <div className="upcoming">
      <p className="upcoming-dday">{t.phase === 'DURING' ? '여행 중' : d === 0 ? 'D-DAY' : `D-${d}`}</p>
      <Link to={`/travels/${t.travelId}`} className="upcoming-name">
        {t.travelName}
      </Link>
      <p className="muted small-text">
        {formatDate(t.startDate)} ~ {formatDate(t.endDate)} · {t.tripDays}일 · {t.regionNames.join(', ')}
      </p>
      <ul className="mini-steps">
        {steps.map((s) => (
          <li key={s.label} className={s.done ? 'done' : ''}>
            {s.done ? '✓' : '○'} {s.label}
            {s.note && <small> {s.note}</small>}
          </li>
        ))}
      </ul>
      {action && (
        <Link className="btn small primary" to={action.to}>
          {action.button} →
        </Link>
      )}
    </div>
  );
}
