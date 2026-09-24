import { formatDistance, formatDuration } from '../../utils/format.js';

/** 대중교통은 카카오 공개 API가 없으므로 카카오맵 길찾기 화면으로 연결한다. */
function kakaoMapLink(leg) {
  const from = `${encodeURIComponent(leg.fromName)},${leg.fromLat},${leg.fromLng}`;
  const to = `${encodeURIComponent(leg.toName)},${leg.toLat},${leg.toLng}`;
  return `https://map.kakao.com/link/from/${from}/to/${to}`;
}

/** 구간별 이동 정보: ① → ②  12분 · 5.3km */
export default function LegList({ legs, mode }) {
  if (legs.length === 0) return <p className="empty">방문지가 2곳 이상이면 이동 정보가 표시됩니다.</p>;
  return (
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
          <a href={kakaoMapLink(leg)} target="_blank" rel="noreferrer" className="leg-link">
            대중교통 보기
          </a>
        </li>
      ))}
    </ol>
  );
}