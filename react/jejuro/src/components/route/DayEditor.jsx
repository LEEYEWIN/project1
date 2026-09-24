/**
 * 선택한 일차의 방문 순서표.
 * - 순번 칸(1번 ▼)에서 번호를 고르면 그 순번으로 이동
 * - 일차 칸(1일차 ▼)에서 다른 일차를 고르면 그 일차의 맨 뒤로 이동
 * - ▲▼ 로 한 칸씩, ✕ 로 빼기
 * 화면의 순번이 그대로 ROUTE_SPOT.visit_order 로 저장된다.
 */
export default function DayEditor({ dayNo, tripDays, spots, onChange, onMoveToDay }) {
  const moveTo = (from, to) => {
    if (to < 0 || to >= spots.length || from === to) return;
    const next = [...spots];
    const [item] = next.splice(from, 1);
    next.splice(to, 0, item);
    onChange(next);
  };

  const remove = (index) => onChange(spots.filter((_, i) => i !== index));

  if (spots.length === 0) {
    return <p className="empty">{dayNo}일차에 방문할 곳을 왼쪽 찜 목록에서 넣어 주세요.</p>;
  }

  return (
    <ol className="spot-list">
      {spots.map((poi, i) => (
        <li key={poi.poiId}>
          <select
            className="order-select"
            aria-label="방문 순번"
            value={i}
            onChange={(e) => moveTo(i, Number(e.target.value))}
          >
            {spots.map((_, n) => (
              <option key={n} value={n}>
                {n + 1}번
              </option>
            ))}
          </select>
          <div className="spot-info">
            <strong>{poi.name}</strong>
            <small>
              {poi.regionName} · {poi.address}
            </small>
          </div>
          <select
            aria-label="일차 이동"
            value={dayNo}
            onChange={(e) => onMoveToDay(i, Number(e.target.value) - 1)}
          >
            {Array.from({ length: tripDays }, (_, d) => (
              <option key={d} value={d + 1}>
                {d + 1}일차
              </option>
            ))}
          </select>
          <div className="spot-actions">
            <button type="button" aria-label="위로" onClick={() => moveTo(i, i - 1)} disabled={i === 0}>
              ▲
            </button>
            <button type="button" aria-label="아래로" onClick={() => moveTo(i, i + 1)} disabled={i === spots.length - 1}>
              ▼
            </button>
            <button type="button" aria-label="빼기" onClick={() => remove(i)}>
              ✕
            </button>
          </div>
        </li>
      ))}
    </ol>
  );
}