import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchTravelDetail } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import { fetchLatestRecommendations } from '../api/recommendApi.js';
import { loadPickStyle } from '../utils/recommendStorage.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import PlaceButton from '../components/common/PlaceButton.jsx';
import PlaceGuide from '../components/common/PlaceGuide.jsx';
import RegionFilter from '../components/common/RegionFilter.jsx';
import RecommendBasis from '../components/recommend/RecommendBasis.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 3페이지: 추천 관광지 목록
 * 데이터 출처: ① 2페이지가 넘겨준 state.pois ② 없으면(새로고침·다른 기기) 서버 추천 기록으로 다시 조회
 * - 위쪽 "추천 기준": 이 여행의 정보·설문 1순위로 추천했다는 것을 보여 줌
 * - 카드의 [+ 장소 추가]: 이 여행의 일정(경로)에 넣을 장소로 담기
 * - 카드를 누르면 관광지 상세(/travels/:travelId/pois/:poiId)로 이동, 뒤로 가기로 돌아온다.
 * - 관심없음 버튼은 이 화면에 두지 않는다 (관심없음 관리 화면에서만). 관심없음인 곳은 서버가 추천에서 뺀다.
 * - 제목 아래 한 줄: 설문 첫 질문(여행지 선택 성향)에 맞춘 안내 문구. 저장된 값이 없으면 숨김
 * - 수정이 잠긴 여행(확정 후 출발일부터 · 바꾸는 중인 변경 전 여행): [+ 장소 추가] 버튼 비활성화 (🔒 잠김)
 */
const PICK_STYLE_TEXT = {
  POPULAR: '회원님과 비슷한 여행 취향을 가진 사람들이 많이 고른 장소예요.',
  UNIQUE: '회원님만의 여행 취향을 바탕으로 추천한 장소예요.',
};

export default function RecommendationListPage() {
  const { travelId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [pois, setPois] = useState(location.state?.pois ?? null);
  const [travel, setTravel] = useState(null);
  const [error, setError] = useState('');
  const [region, setRegion] = useState('전체');
  const pickStyle = loadPickStyle(travelId);
  const pickStyleText = PICK_STYLE_TEXT[pickStyle];
  const { bookmarks, isBookmarked, toggle, pending, locked, lockMessage, error: bookmarkError } = useBookmarks(travelId, 'RECOMMEND'); // 여기서 담으면 'AI 추천으로 담음'으로 기록

  useEffect(() => {
    if (pois) return;
    // 새로고침·다른 탭·다른 기기: 서버의 추천 기록에서 처음 받은 목록을 그대로 다시 연다
    let active = true;
    fetchLatestRecommendations(travelId)
      .then((d) => active && setPois(d.pois))
      .catch((e) => {
        if (!active) return;
        if (e.response?.status === 404) navigate(`/travels/${travelId}/recommending`, { replace: true }); // 아직 추천 전
        else setError(errorMessage(e));
      });
    return () => {
      active = false;
    };
  }, [pois, travelId, navigate]);

  // 추천 기준(여행 정보·설문) — 실패해도 목록은 보여 준다
  useEffect(() => {
    fetchTravelDetail(travelId)
      .then(setTravel)
      .catch(() => setTravel(null));
  }, [travelId]);

  const regions = useMemo(() => [...new Set((pois ?? []).map((p) => p.regionName))], [pois]);
  const visible = (pois ?? []).filter((p) => region === '전체' || p.regionName === region);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!pois) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide recommendations-page">
      <div className="title-row">
        <h1>AI 추천 관광지 {pois.length}곳</h1>
        <div className="actions">
          {/* 추천은 여행당 한 번: 받은 뒤에는 비활성화하고, 누르면 이유를 알림으로 보여 준다 */}
          <button
            type="button"
            className="btn ghost"
            aria-disabled="true"
            title="이미 AI 추천을 받은 여행이에요"
            onClick={() => window.alert('이미 AI 추천을 받은 여행이라 다시 추천받을 수 없어요.\n다른 곳을 더 찾으려면 [전체 관광지 보기]를 이용해 주세요.')}
          >
            AI 추천 받기 완료
          </button>
        </div>
      </div>

      {pickStyleText && <p className="pick-style-note" role="note">{pickStyleText}</p>}

      <RecommendBasis travel={travel} count={pois.length} />
      <PlaceGuide travelId={travelId} count={bookmarks.length} />

      {locked && (
        <p className="locked-note" role="note">
          🔒 {lockMessage} 추천 목록은 볼 수 있어요.
        </p>
      )}

      <aside className="more-places-guide" aria-labelledby="more-places-title">
        <div>
          <h2 id="more-places-title">
            {locked ? '제주의 다른 관광지도 둘러보세요' : '추천 외의 관광지도 여행에 추가할 수 있어요'}
          </h2>
          <p id="more-places-description">
            {locked
              ? '관광지 더보기에서 다른 장소도 살펴보세요. 지금은 이 여행에 장소를 추가할 수 없어요.'
              : '다른 관광지도 추가하고 싶다면 관광지 더보기에서 찾아보세요. 마음에 드는 곳의 [+ 장소 추가]를 누르면 이 여행의 여행 장소에 함께 담겨요.'}
          </p>
        </div>
        <Link className="btn ghost" to={`/travels/${travelId}/pois?from=recommend`} aria-describedby="more-places-description">
          관광지 더보기 <span aria-hidden="true">→</span>
        </Link>
      </aside>

      <RegionFilter regions={regions} value={region} onChange={setRegion} />
      <ErrorBox message={bookmarkError} />

      {visible.length === 0 ? (
        <p className="empty">추천 결과가 없습니다. 관광지 데이터가 적재되었는지 확인하세요.</p>
      ) : (
        <div className="poi-grid">
          {visible.map((poi) => (
            <PoiCard
              key={poi.poiId}
              poi={poi}
              to={`/travels/${travelId}/pois/${poi.poiId}`}
              linkState={{ source: 'RECOMMEND' }}
              right={
                <PlaceButton
                  on={isBookmarked(poi.poiId)}
                  disabled={pending.has(poi.poiId)}
                  locked={locked}
                  onClick={() => toggle(poi)}
                />
              }
            />
          ))}
        </div>
      )}

      <div className="bottom-bar">
        <span>여행 장소 {bookmarks.length}곳 · 담은 곳으로 경로를 만들어요</span>
        <Link className="btn primary" to={`/travels/${travelId}/bookmarks`}>
          여행 장소 보기 →
        </Link>
      </div>
    </main>
  );
}