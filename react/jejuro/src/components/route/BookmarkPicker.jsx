import { useEffect, useState } from 'react';

/**
 * 왼쪽 패널: 여행 장소 목록 (아직 배치 안 한 곳이 위로).
 * 아직 배치하지 않은 곳은 [일차 ▼] [순번 ▼] 를 골라 "넣기" → 그 일차의 그 순번에 들어간다.
 * 이미 배치한 곳은 "N일차 M번" 으로 표시되고 "빼기"로 경로에서만 뺄 수 있다(장소에는 남음).
 * onDeletePlace가 있으면 배치 전 장소에 [장소 삭제] (이 여행에 안 갈 곳을 여행 장소에서 아예 뺌)
 *
 * plan: [[poi, poi], [poi], ...]  plan[i] = (i+1)일차 방문지(배열 순서 = 방문 순서)
 */
export default function BookmarkPicker({ pois, plan, currentDay, onPlace, onRemove, onDeletePlace }) {
  if (pois.length === 0) {
    return <p className="empty">여행 장소가 없습니다. 추천 목록·관광지 목록에서 [+ 장소 추가]를 눌러 주세요.</p>;
  }

  // poiId → { day, order } (이미 배치된 위치)
  const placed = new Map();
  plan.forEach((spots, d) => spots.forEach((p, i) => placed.set(p.poiId, { day: d + 1, order: i + 1 })));

  // 배치 전 장소를 위로
  const sorted = [...pois].sort((a, b) => Number(placed.has(a.poiId)) - Number(placed.has(b.poiId)));

  return (
    <ul className="picker">
      {sorted.map((poi) => (
        <PickerRow
          key={poi.poiId}
          poi={poi}
          placed={placed.get(poi.poiId)}
          plan={plan}
          currentDay={currentDay}
          onPlace={onPlace}
          onRemove={onRemove}
          onDeletePlace={onDeletePlace}
        />
      ))}
    </ul>
  );
}

function PickerRow({ poi, placed, plan, currentDay, onPlace, onRemove, onDeletePlace }) {
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
        <button type="button" className="btn small ghost" title="경로에서만 빼요(여행 장소에는 남음)" onClick={() => onRemove(poi.poiId)}>
          빼기
        </button>
      </li>
    );
  }

  return (
    <li className="unplaced">
      <div>
        <strong>{poi.name}</strong>
        <small>{poi.regionName} · 아직 배치 전</small>
      </div>
      <div className="place-select">
        <label className="place-field">
          <span>일차</span>
          <select
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
        </label>
        <label className="place-field">
          <span>방문 순서</span>
          <select value={order} onChange={(e) => setOrder(Number(e.target.value))}>
            <option value={0}>{maxOrder}번 (맨 뒤)</option>
            {Array.from({ length: maxOrder - 1 }, (_, i) => (
              <option key={i + 1} value={i + 1}>
                {i + 1}번
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="btn small primary place-submit"
          onClick={() => onPlace(poi, dayIndex, (order || maxOrder) - 1)}
        >
          넣기
        </button>
        {onDeletePlace && (
          <button type="button" className="btn small ghost danger place-delete" title="이 여행에 가지 않을 곳이면 여행 장소에서 빼요" onClick={() => onDeletePlace(poi)}>
            장소 삭제
          </button>
        )}
      </div>
    </li>
  );
}
