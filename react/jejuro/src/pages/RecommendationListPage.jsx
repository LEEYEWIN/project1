import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchPoisByIds } from '../api/poiApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import { loadRecommendationIds } from '../utils/recommendStorage.js';
import { addDislike, fetchDislikes, removeDislike } from '../api/dislikeApi.js';
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
 * - 카드의 [관심없음]: 카드는 목록에 남기고 흐리게 표시 + 내 모든 여행의 다음 AI 추천에서 제외
 *   → 같은 자리의 [↺ 되돌리기]로 바로 취소, 다시 [관심없음]도 몇 번이든 가능 (관심없음 관리 화면과 같은 USER_POI_DISLIKE 표)
 * - 일정을 확정한 여행: [+ 장소 추가] 버튼 비활성화 (🔒 확정됨)
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
  const [disliked, setDisliked] = useState(() => new Set()); // 관심없음 poiId (서버 USER_POI_DISLIKE 기준)
  const [dislikeBusy, setDislikeBusy] = useState(null);
  const { bookmarks, isBookmarked, toggle, pending, locked, error: bookmarkError } = useBookmarks(travelId, 'RECOMMEND'); // 여기서 담으면 'AI 추천으로 담음'으로 기록

  // 관심없음 목록은 화면에 들어올 때마다 서버에서 (관심없음 관리 화면에서 되돌린 것도 바로 반영)
  useEffect(() => {
    fetchDislikes()
      .then((list) => setDisliked(new Set(list.map((d) => d.poi.poiId))))
      .catch(() => {});
  }, []);

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

  /** 관심없음 ↔ 되돌리기 (카드는 목록에 그대로 두고 흐리게만) */
  const toggleDislike = async (poi) => {
    const on = disliked.has(poi.poiId);
    if (!on && !window.confirm(`[${poi.name}]을(를) 관심없음으로 표시할까요?\n다음 AI 추천부터 제외됩니다. (언제든 되돌릴 수 있어요)`)) return;
    setDislikeError('');
    setDislikeBusy(poi.poiId);
    try {
      if (on) await removeDislike(poi.poiId);
      else await addDislike(poi.poiId);
      setDisliked((s) => {
        const next = new Set(s);
        if (on) next.delete(poi.poiId);
        else next.add(poi.poiId);
        return next;
      });
    } catch (e) {
      setDislikeError(errorMessage(e));
    } finally {
      setDislikeBusy(null);
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
        같은 자리의 [↺ 되돌리기] 또는 <Link to="/dislikes">관심없음 관리</Link>에서 언제든 되돌릴 수 있어요.
      </p>

      {locked && (
        <p className="locked-note" role="note">
          🔒 일정을 확정한 여행이에요. 추천 목록은 볼 수 있지만 여행 장소를 추가하거나 뺄 수는 없어요.
        </p>
      )}

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
              dimmed={disliked.has(poi.poiId)}
              left={
                // 일정(여행 장소)에 담은 곳은 관심없음 비활성화 → 빼면 다시 누를 수 있음
                <button
                  type="button"
                  className={disliked.has(poi.poiId) ? 'dislike-btn on' : 'dislike-btn'}
                  disabled={dislikeBusy === poi.poiId || isBookmarked(poi.poiId)}
                  onClick={() => toggleDislike(poi)}
                  title={
                    isBookmarked(poi.poiId)
                      ? '여행 장소에 담은 곳은 관심없음으로 표시할 수 없어요'
                      : disliked.has(poi.poiId)
                        ? '관심없음 취소 (다시 추천될 수 있어요)'
                        : '다음 AI 추천에서 제외'
                  }
                >
                  {disliked.has(poi.poiId) ? '↺ 되돌리기' : '관심없음'}
                </button>
              }
              right={
                !disliked.has(poi.poiId) && (
                  <PlaceButton
                    on={isBookmarked(poi.poiId)}
                    disabled={pending.has(poi.poiId)}
                    locked={locked}
                    onClick={() => toggle(poi)}
                  />
                )
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