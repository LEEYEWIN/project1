export default function Loading({ text = '불러오는 중…' }) {
  return (
    <div className="loading">
      <span className="spinner" aria-hidden="true" />
      {text}
    </div>
  );
}