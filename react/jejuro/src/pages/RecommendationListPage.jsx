import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchPoisByIds } from '../api/poiApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import { loadRecommendationIds, saveRecommendation } from '../utils/recommendStorage.js';
import { addDislike } from '../api/dislikeApi.js';
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
 * 데이터 출처: ① 2페이지가 넘겨준 state.pois ② 없으면(새로고침) sessionStorage의 ID로 다시 조회
 * - 위쪽 "추천 기준": 이 여행의 정보·설문 1순위로 추천했다는 것을 보여 줌
 * - 카드의 [+ 장소 추가]: 이 여행의 일정(경로)에 넣을 장소로 담기
 * - 카드를 누르면 관광지 상세(/travels/:travelId/pois/:poiId)로 이동, 뒤로 가기로 돌아온다.
 * - 카드의 [관심없음]: 이 목록에서 빼고, 내 모든 여행의 다음 AI 추천에서도 제외 (관심없음 관리 화면에서 되돌리기)
 */
export default function RecommendationListPage() {
  const { travelId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [pois, setPois] = useState(location.state?.pois ?? null);
  const [travel, setTravel] = useState(null);
  const [error, setError] = useState('');
  const [region, setRegion] = useState('전체');
  const [dislikeError, setDislikeError] = useState('');
  const { bookmarks, isBookmarked, toggle, pending, error: bookmarkError } = useBookmarks(travelId, 'RECOMMEND'); // 여기서 담으면 'AI 추천으로 담음'으로 기록

  useEffect(() => {
    if (pois) return;
    const ids = loadRecommendationIds(travelId);
    if (!ids) {
      navigate(`/travels/${travelId}/recommending`, { replace: true }); // 추천 기록 없음 → 다시 추천
      return;
    }
    fetchPoisByIds(ids)
      .then(setPois)
      .catch((e) => setError(errorMessage(e)));
  }, [pois, travelId, navigate]);

  // 추천 기준(여행 정보·설문) — 실패해도 목록은 보여 준다
  useEffect(() => {
    fetchTravelDetail(travelId)
      .then(setTravel)
      .catch(() => setTravel(null));
  }, [travelId]);

  /** 관심없음 → 목록에서 빼고 새로고침해도 빠지도록 저장된 추천 목록도 고침 */
  const dislike = async (poi) => {
    if (!window.confirm(`[${poi.name}]을(를) 관심없음으로 표시할까요?\n이 목록에서 빠지고, 다음 AI 추천부터 제외됩니다. (관심없음 관리에서 되돌릴 수 있어요)`)) return;
    setDislikeError('');
    try {
      await addDislike(poi.poiId);
      setPois((list) => {
        const next = list.filter((p) => p.poiId !== poi.poiId);
        saveRecommendation(travelId, next);
        return next;
      });
    } catch (e) {
      setDislikeError(errorMessage(e));
    }
  };

  const regions = useMemo(() => [...new Set((pois ?? []).map((p) => p.regionName))], [pois]);
  const visible = (pois ?? []).filter((p) => region === '전체' || p.regionName === region);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!pois) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>AI 추천 관광지 {pois.length}곳</h1>
        <div className="actions">
          <button type="button" className="btn ghost" onClick={() => navigate(`/travels/${travelId}/recommending`)}>
            다시 추천 받기
          </button>
          <Link className="btn ghost" to={`/travels/${travelId}/pois`}>
            전체 관광지 보기
          </Link>
        </div>
      </div>

      <RecommendBasis travel={travel} count={pois.length} />
      <PlaceGuide travelId={travelId} count={bookmarks.length} />

      <p className="dislike-note">
        🙅 가고 싶지 않은 곳은 카드 왼쪽 위 <b>[관심없음]</b>을 누르세요. <b>관심없음으로 표시한 관광지는 다음 AI 추천에서 제외됩니다.</b>{' '}
        <Link to="/dislikes">관심없음 관리</Link>에서 언제든 되돌릴 수 있어요.
      </p>

      <RegionFilter regions={regions} value={region} onChange={setRegion} />
      <ErrorBox message={bookmarkError || dislikeError} />

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
              left={
                !isBookmarked(poi.poiId) && (
                  <button type="button" className="dislike-btn" onClick={() => dislike(poi)} title="다음 AI 추천에서 제외">
                    관심없음
                  </button>
                )
              }
              right={
                <PlaceButton
                  on={isBookmarked(poi.poiId)}
                  disabled={pending.has(poi.poiId)}
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