import { formatDate } from '../../utils/format.js';

/**
 * 후기: 확정 일정의 관광지별 결과 (쿠팡이츠 메뉴별 평가처럼 한 줄씩)
 * - 일부만 다녀왔어요: 처음엔 모두 [✓ 갔어요]. 못 간 곳만 눌러서 [✕ 못 갔어요]로 바꾼다
 * - 계획대로 다녀왔어요: 방문 여부는 고정(모두 갔어요), [좋았어요]/[아쉬워요]만 선택
 * - 간 곳에만 [좋았어요]/[아쉬워요] (선택, 다시 누르면 해제)
 *
 * route: 확정 경로 { days: [{ dayNo, date, spots: [{ visitOrder, poi }] }] }
 * value: { [poiId]: { visited: boolean, reaction: 'LIKE'|'DISLIKE'|null } }
 * canMiss: 못 갔어요로 바꿀 수 있는지 (일부만 다녀왔어요일 때 true)
 */
export default function SpotChecklist({ route, value, onChange, canMiss }) {
  const all = route.days.flatMap((d) => d.spots);
  const visitedCount = all.filter((s) => value[s.poi.poiId]?.visited !== false).length;

  const set = (poiId, patch) => onChange({ ...value, [poiId]: { ...value[poiId], ...patch } });
  const toggleVisit = (poiId) => {
    const visited = value[poiId]?.visited === false; // 못 갔어요 → 갔어요
    set(poiId, { visited, reaction: visited ? value[poiId]?.reaction ?? null : null });
  };
  const react = (poiId, reaction) => set(poiId, { reaction: value[poiId]?.reaction === reaction ? null : reaction });

  return (
    <div className="spot-check">
      {route.days.map((d) => (
        <div key={d.dayNo} className="spot-check-day">
          <h3>
            {d.dayNo}일차 <small>{formatDate(d.date)}</small>
          </h3>
          <ul>
            {d.spots.map((s) => {
              const v = value[s.poi.poiId] ?? { visited: true, reaction: null };
              const missed = v.visited === false;
              return (
                <li key={s.poi.poiId} className={missed ? 'missed' : ''}>
                  <span className="order small">{s.visitOrder}</span>
                  <span className="spot-check-name">{s.poi.name}</span>
                  {canMiss && (
                    <button
                      type="button"
                      className={missed ? 'visit-btn off' : 'visit-btn'}
                      aria-pressed={!missed}
                      onClick={() => toggleVisit(s.poi.poiId)}
                    >
                      {missed ? '✕ 못 갔어요' : '✓ 갔어요'}
                    </button>
                  )}
                  {!missed && (
                    <span className="react-btns" role="group" aria-label={`${s.poi.name} 평가`}>
                      <button
                        type="button"
                        className={v.reaction === 'LIKE' ? 'react-btn on' : 'react-btn'}
                        aria-pressed={v.reaction === 'LIKE'}
                        onClick={() => react(s.poi.poiId, 'LIKE')}
                      >
                        좋았어요
                      </button>
                      <button
                        type="button"
                        className={v.reaction === 'DISLIKE' ? 'react-btn on bad' : 'react-btn'}
                        aria-pressed={v.reaction === 'DISLIKE'}
                        onClick={() => react(s.poi.poiId, 'DISLIKE')}
                      >
                        아쉬워요
                      </button>
                    </span>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      ))}
      <p className="spot-check-sum">
        {all.length}곳 중 <b>{visitedCount}곳 다녀옴</b>
        {all.length - visitedCount > 0 && ` · ${all.length - visitedCount}곳 못 감`}
      </p>
    </div>
  );
}