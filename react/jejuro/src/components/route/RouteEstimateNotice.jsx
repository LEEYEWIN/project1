/** 길찾기 실패 사유를 문장별로 나눠 보여 준다. */
export default function RouteEstimateNotice({ notice }) {
  if (!notice) return null;
  const sentences = String(notice).trim().split(/(?<=[.!?])\s+/).filter(Boolean);

  return (
    <div className="route-estimate-notice" role="note">
      <strong>이동 정보 안내</strong>
      {sentences.map((sentence, index) => (
        <p key={`${index}-${sentence}`} className={index === 0 ? 'route-estimate-lead' : undefined}>{sentence}</p>
      ))}
    </div>
  );
}
