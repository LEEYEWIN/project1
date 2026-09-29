import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchDislikes, removeDislike } from '../api/dislikeApi.js';
import { errorMessage } from '../api/client.js';
import PoiCard from '../components/common/PoiCard.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import { formatDateTime } from '../utils/format.js';

/**
 * 관심없음 관광지 관리 (/dislikes)
 * - AI 추천 목록에서 [관심없음]을 누른 관광지 목록 (최근 순)
 * - 여기 있는 관광지는 내 모든 여행의 다음 AI 추천에서 제외된다
 * - [되돌리기]를 누르면 다시 추천될 수 있다
 */
export default function DislikesPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(null); // 되돌리는 중인 poiId

  useEffect(() => {
    fetchDislikes()
      .then(setItems)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const undo = async (poi) => {
    setBusy(poi.poiId);
    setError('');
    try {
      await removeDislike(poi.poiId);
      setItems((list) => list.filter((d) => d.poi.poiId !== poi.poiId));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  if (!items && !error) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>관심없음 관광지 {items ? `${items.length}곳` : ''}</h1>
        <Link className="btn ghost" to="/travels">
          내 여행으로
        </Link>
      </div>

      <p className="dislike-note">
        <b>여기 있는 관광지는 내 모든 여행의 다음 AI 추천에서 제외됩니다.</b> 다시 추천받고 싶으면 [되돌리기]를 누르세요.
        <br />
        관광지 목록·검색에서는 그대로 보이므로 직접 찾아서 담을 수는 있어요.
      </p>

      <ErrorBox message={error} />

      {items && items.length === 0 ? (
        <p className="empty">관심없음으로 표시한 관광지가 없어요. AI 추천 목록에서 카드 왼쪽 위 [관심없음]으로 추가할 수 있어요.</p>
      ) : (
        <div className="poi-grid">
          {(items ?? []).map(({ poi, createdAt }) => (
            <PoiCard
              key={poi.poiId}
              poi={poi}
              to={`/pois/${poi.poiId}`}
              right={
                <button type="button" className="place-btn" disabled={busy === poi.poiId} onClick={() => undo(poi)}>
                  {busy === poi.poiId ? '되돌리는 중…' : '↺ 되돌리기'}
                </button>
              }
            >
              <p className="muted small-text">{formatDateTime(createdAt)} 관심없음 표시</p>
            </PoiCard>
          ))}
        </div>
      )}
    </main>
  );
}