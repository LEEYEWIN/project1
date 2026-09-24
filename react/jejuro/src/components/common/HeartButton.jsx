/** 찜 하트 버튼 */
export default function HeartButton({ on, onClick, disabled }) {
  return (
    <button
      type="button"
      className={on ? 'heart on' : 'heart'}
      aria-label={on ? '찜 취소' : '찜하기'}
      aria-pressed={on}
      disabled={disabled}
      onClick={onClick}
    >
      {on ? '♥' : '♡'}
    </button>
  );
}