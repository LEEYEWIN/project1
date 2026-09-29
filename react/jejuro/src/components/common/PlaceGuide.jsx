import { Link } from 'react-router-dom';

/**
 * "여행 장소" 안내 상자. 장소 추가가 "참고용 저장"이 아니라 "이 여행의 일정에 넣는 것"임을 알려 준다.
 * travelId: 여행 번호, count: 지금 담긴 장소 수, showLink: [여행 장소 보기] 링크 표시
 */
export default function PlaceGuide({ travelId, count, showLink = true }) {
  return (
    <div className="place-guide" role="note">
      <p>
        <strong>선택한 관광지를 기준으로 여행 일정(경로)이 만들어집니다.</strong>
        <br />
        [+ 장소 추가]로 담은 곳은 이 여행의 <b>여행 장소</b>가 되고, 경로 짜기에서 모두 날짜별로 배치해야 일정을 확정할 수 있어요.
      </p>
      {showLink && (
        <Link className="btn small ghost" to={`/travels/${travelId}/bookmarks`}>
          여행 장소 {count ?? 0}곳 보기
        </Link>
      )}
    </div>
  );
}
