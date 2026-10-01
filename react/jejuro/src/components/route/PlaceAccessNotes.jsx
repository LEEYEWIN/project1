// 섬(배편 확인)·산(주차·등산 가능 여부 확인)처럼 차로 바로 가기 어려운 관광지 안내.
// 서버 길찾기 응답(placeNotes)을 그대로 보여 준다. 경로 짜기(미리보기)와 동선 안내 상세에서 함께 쓴다.
const ICON = { ISLAND: '🚢', MOUNTAIN: '⛰' };
const LABEL = { ISLAND: '섬', MOUNTAIN: '산·오름' };

export default function PlaceAccessNotes({ notes }) {
  if (!notes?.length) return null;
  return (
    <div className="place-access-notes" role="note">
      <strong>방문 전 확인하세요</strong>
      <ul>
        {notes.map((n) => (
          <li key={`${n.order}-${n.kind}`} className={`place-access-${n.kind.toLowerCase()}`}>
            <span className="place-access-icon" aria-hidden="true">{ICON[n.kind] ?? 'ℹ'}</span>
            <div>
              <b>{n.order}. {n.name}</b> <span className="place-access-kind">{LABEL[n.kind] ?? ''}</span>
              <p>{n.message}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
