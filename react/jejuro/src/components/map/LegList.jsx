import { formatDistance, formatDuration } from '../../utils/format.js';

/** 대중교통은 카카오 공개 API가 없으므로 카카오맵 길찾기 화면으로 연결한다. */
function openKakaoTransit(leg) {
  const from = `${encodeURIComponent(leg.fromName)},${leg.fromLat},${leg.fromLng}`;
  const to = `${encodeURIComponent(leg.toName)},${leg.toLat},${leg.toLng}`;
  window.open(`https://map.kakao.com/link/from/${from}/to/${to}`, '_blank', 'noopener');
}

const won = (n) => `${Number(n).toLocaleString()}원`;

/**
 * 구간별 이동 정보: ① → ②  12분 · 5.3km · 🚕 약 8,700원
 * 택시비 = 제주 중형택시 거리요금으로 계산한 예상값 (구간마다 따로 탄다고 가정, 심야할증·정체 시간요금 제외)
 */
export default function LegList({ legs, mode }) {
  if (legs.length === 0) return <p className="empty">방문지가 2곳 이상이면 이동 정보가 표시됩니다.</p>;
  const car = mode === 'CAR'; // 택시비는 자동차 동선일 때만
  const total = car ? legs.reduce((sum, l) => sum + (l.taxiFare ?? 0), 0) : 0;
  return (
    <>
      <ol className="leg-list">
        {legs.map((leg) => (
          <li key={leg.fromOrder}>
            <span className="order small">{leg.fromOrder}</span>
            <span className="leg-name">{leg.fromName}</span>
            <span className="arrow">→</span>
            <span className="order small">{leg.toOrder}</span>
            <span className="leg-name">{leg.toName}</span>
            <span className="leg-time">
              {mode === 'CAR' ? '🚗' : '🚶'} {formatDuration(leg.durationSec)} · {formatDistance(leg.distanceM)}
            </span>
            {car && leg.taxiFare > 0 && <span className="leg-taxi">🚕 약 {won(leg.taxiFare)}</span>}
            <button type="button" className="leg-link" onClick={() => openKakaoTransit(leg)}>
              대중교통 보기
            </button>
          </li>
        ))}
      </ol>
      {total > 0 && (
        <p className="leg-taxi-total">
          🚕 예상 택시비 합계 <b>약 {won(total)}</b>
          <small className="muted"> · 제주 중형택시 거리요금 기준(기본 4,300원/2km, 126m당 100원), 구간마다 따로 탄다고 가정 · 심야할증 제외</small>
        </p>
      )}
    </>
  );
}