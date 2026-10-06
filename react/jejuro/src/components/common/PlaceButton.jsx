/**
 * [장소 추가] 버튼 (예전 찜 하트 버튼)
 * 여행 장소 = 이 여행의 경로(일정)에 넣을 관광지. 여기 담은 곳으로 일차별 경로를 만든다.
 * on=false → "+ 장소 추가" / on=true → "✓ 추가됨" (다시 누르면 장소에서 빠짐)
 * locked=true → 수정이 잠긴 여행(확정 후 출발일부터 · 바꾸는 중인 변경 전 여행): 누를 수 없음 ("✓ 추가됨" / "🔒 잠김")
 */
export default function PlaceButton({ on, onClick, disabled, locked = false, compact = false }) {
  const label = on ? '✓ 추가됨' : locked ? '🔒 잠김' : compact ? '+ 추가' : '+ 장소 추가';
  return (
    <button
      type="button"
      className={on ? 'place-btn on' : 'place-btn'}
      aria-pressed={on}
      aria-label={locked ? '지금은 이 여행의 장소를 바꿀 수 없어요' : on ? '여행 장소에서 빼기' : '여행 장소에 추가'}
      title={
        locked
          ? '일정을 확정한 여행은 장소를 추가하거나 뺄 수 없어요'
          : on
            ? '누르면 여행 장소에서 빠져요'
            : '이 여행의 일정(경로)에 넣을 장소로 추가해요'
      }
      disabled={disabled || locked}
      onClick={onClick}
    >
      {label}
    </button>
  );
}
