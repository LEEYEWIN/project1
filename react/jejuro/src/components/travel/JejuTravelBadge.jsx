export const JEJU_BADGES = [
  '돌하르방',
  '한라산',
  '감귤',
  '제주 바다',
  '해안도로',
  '폭포',
  '제주 해안 절벽',
  '제주 풍력발전기',
  '제주 말',
  '해녀',
];

export default function JejuTravelBadge({ index }) {
  return (
    <span
      className={`travel-avatar travel-avatar-${index}`}
      role="img"
      aria-label={`${JEJU_BADGES[index]} 일러스트`}
    />
  );
}
