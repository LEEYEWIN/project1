import { useEffect, useState } from 'react';

/**
 * 왼쪽 패널: 찜한 관광지 목록.
 * 아직 배치하지 않은 곳은 [일차 ▼] [순번 ▼] 를 골라 "넣기" → 그 일차의 그 순번에 들어간다.
 * 이미 배치한 곳은 "N일차 M번" 으로 표시되고 "빼기"로 목록에서 뺄 수 있다.
 *
 * plan: [[poi, poi], [poi], ...]  plan[i] = (i+1)일차 방문지(배열 순서 = 방문 순서)
 */
export default function BookmarkPicker({ pois, plan, currentDay, onPlace, onRemove }) {
  if (pois.length === 0) {
    return <p className="empty">찜한 관광지가 없습니다.</p>;
  }

  // poiId → { day, order } (이미 배치된 위치)
  const placed = new Map();
  plan.forEach((spots, d) => spots.forEach((p, i) => placed.set(p.poiId, { day: d + 1, order: i + 1 })));

  return (
    <ul className="picker">
      {pois.map((poi) => (
        <PickerRow
          key={poi.poiId}
          poi={poi}
          placed={placed.get(poi.poiId)}
          plan={plan}
          currentDay={currentDay}
          onPlace={onPlace}
          onRemove={onRemove}
        />
      ))}
    </ul>
  );
}

function PickerRow({ poi, placed, plan, currentDay, onPlace, onRemove }) {
  const [day, setDay] = useState(currentDay);          // 1부터
  const [order, setOrder] = useState(0);               // 0이면 "맨 뒤"

  // 위쪽 일차 탭을 바꾸면 기본 선택 일차도 따라 바뀜
  useEffect(() => {
    setDay(currentDay);
    setOrder(0);
  }, [currentDay]);

  const dayIndex = Math.min(day, plan.length) - 1;
  const maxOrder = plan[dayIndex].length + 1;          // 새로 넣으면 최대 (현재 개수 + 1)번

  if (placed) {
    return (
      <li className="placed">
        <div>
          <strong>{poi.name}</strong>
          <small>{poi.regionName}</small>
        </div>
        <span className="tag on">
          {placed.day}일차 {placed.order}번
        </span>
        <button type="button" className="btn small ghost" onClick={() => onRemove(poi.poiId)}>
          빼기
        </button>
      </li>
    );
  }

  return (
    <li>
      <div>
        <strong>{poi.name}</strong>
        <small>{poi.regionName}</small>
      </div>
      <div className="place-select">
        <select
          aria-label="일차"
          value={day}
          onChange={(e) => {
            setDay(Number(e.target.value));
            setOrder(0);
          }}
        >
          {plan.map((_, i) => (
            <option key={i} value={i + 1}>
              {i + 1}일차
            </option>
          ))}
        </select>
        <select aria-label="순번" value={order} onChange={(e) => setOrder(Number(e.target.value))}>
          <option value={0}>{maxOrder}번 (맨 뒤)</option>
          {Array.from({ length: maxOrder - 1 }, (_, i) => (
            <option key={i + 1} value={i + 1}>
              {i + 1}번
            </option>
          ))}
        </select>
        <button
          type="button"
          className="btn small primary"
          onClick={() => onPlace(poi, dayIndex, (order || maxOrder) - 1)}
        >
          넣기
        </button>
      </div>
    </li>
  );
}