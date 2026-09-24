/** 에러 메시지 + (선택) 다시 시도 버튼 */
export default function ErrorBox({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="error-box">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn ghost small" onClick={onRetry}>
          다시 시도
        </button>
      )}
    </div>
  );
}