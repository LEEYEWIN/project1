import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { addDislike, fetchDislikes, fetchRecommendedPois, removeDislike } from '../api/dislikeApi.js';
import { errorMessage } from '../api/client.js';
import PoiCard from '../components/common/PoiCard.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import { formatDateTime } from '../utils/format.js';

/**
 * 관심없음 관광지 관리 (/dislikes)
 * DB: USER_POI_DISLIKE (user_id, poi_id, created_at) — 행이 있으면 관심없음, 없으면 해제
 * - 위: 지금 관심없음으로 표시한 관광지. [↺ 되돌리기]를 눌러도 카드는 이 화면에 남아 있어 [관심없음]으로 다시 표시 가능
 * - 아래: 이전에 추천받은 관광지 (내 모든 여행의 AI 추천 기록). 여기서도 [관심없음] ↔ [↺ 되돌리기]
 * - 관심없음인 관광지는 내 모든 여행의 다음 AI 추천에서 제외된다
 * - 두 목록 모두 한 쪽에 12곳(4열 × 3줄)씩 페이지로 나눠 보여 준다 (탭마다 쪽 번호 따로)
 */
const PAGE_SIZE = 12;

/** 이전 · n / 전체 · 다음 */
function Pager({ page, total, onPage }) {
  if (total <= 1) return null;
  return (
    <nav className="pager" aria-label="페이지">
      <button type="button" className="btn ghost" disabled={page === 0} onClick={() => onPage(page - 1)}>
        이전
      </button>
      <span>
        {page + 1} / {total}
      </span>
      <button type="button" className="btn ghost" disabled={page + 1 >= total} onClick={() => onPage(page + 1)}>
        다음
      </button>
    </nav>
  );
}

export default function DislikesPage() {
  const [dislikes, setDislikes] = useState(null); // 이 화면에 들어올 때의 관심없음 목록 (되돌려도 카드는 남김)
  const [recommended, setRecommended] = useState([]);
  const [on, setOn] = useState(() => new Set()); // 지금 관심없음인 poiId
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(null);
  const [tab, setTab] = useState('DISLIKED');
  const [pages, setPages] = useState({ DISLIKED: 0, RECOMMENDED: 0 }); // 탭별 현재 쪽 (0부터)

  useEffect(() => {
    Promise.all([fetchDislikes(), fetchRecommendedPois().catch(() => [])])
      .then(([list, recs]) => {
        setDislikes(list);
        setRecommended(recs);
        setOn(new Set(list.map((d) => d.poi.poiId)));
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  /** 관심없음 ↔ 되돌리기 (몇 번이든) */
  const toggle = async (poi) => {
    const isOn = on.has(poi.poiId);
    setBusy(poi.poiId);
    setError('');
    try {
      if (isOn) await removeDislike(poi.poiId);
      else await addDislike(poi.poiId);
      setOn((s) => {
        const next = new Set(s);
        if (isOn) next.delete(poi.poiId);
        else next.add(poi.poiId);
        return next;
      });
      // 아래 목록에서 새로 표시한 곳은 위 목록에도 보이게
      if (!isOn) {
        setDislikes((list) =>
          list.some((d) => d.poi.poiId === poi.poiId) ? list : [{ poi, createdAt: new Date().toISOString() }, ...list],
        );
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  if (!dislikes && !error) return <main className="page"><Loading /></main>;

  const button = (poi) => (
    <button
      type="button"
      className={on.has(poi.poiId) ? 'dislike-btn on' : 'dislike-btn'}
      disabled={busy === poi.poiId}
      onClick={() => toggle(poi)}
      title={on.has(poi.poiId) ? '관심없음 취소 (다시 추천될 수 있어요)' : '다음 AI 추천에서 제외'}
    >
      {busy === poi.poiId ? '저장 중…' : on.has(poi.poiId) ? '↺ 되돌리기' : '관심없음'}
    </button>
  );

  const list = dislikes ?? [];
  const totalOf = (n) => Math.max(1, Math.ceil(n / PAGE_SIZE));
  const pageOf = (key, n) => Math.min(pages[key], totalOf(n) - 1);
  const slice = (key, arr) => arr.slice(pageOf(key, arr.length) * PAGE_SIZE, (pageOf(key, arr.length) + 1) * PAGE_SIZE);
  const goPage = (key, p) => {
    setPages((s) => ({ ...s, [key]: p }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  return (
    <main className="page wide dislikes-page">
      <div className="title-row">
        <h1>관심없음 관광지 {on.size}곳</h1>
        <Link className="btn ghost" to="/travels">
          내 여행으로
        </Link>
      </div>

      <section className="dislike-guide" aria-labelledby="dislike-guide-title">
        <h2 id="dislike-guide-title">관심없음 설정은 이렇게 적용돼요</h2>
        <div className="dislike-guide-grid">
          <div><strong>AI 추천에서 제외</strong><p>관심없음으로 표시한 관광지는 내 모든 여행의 다음 AI 추천에 나오지 않아요.</p></div>
          <div><strong>언제든 되돌리기</strong><p>‘되돌리기’를 누르면 다시 추천될 수 있어요. 필요하면 다시 관심없음으로 표시할 수 있어요.</p></div>
          <div><strong>직접 선택은 가능</strong><p>관광지 목록과 검색에는 계속 보이므로, 원할 때 직접 찾아 여행에 담을 수 있어요.</p></div>
        </div>
      </section>

      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'DISLIKED'} className={tab === 'DISLIKED' ? 'tab on' : 'tab'} onClick={() => setTab('DISLIKED')}>
          관심없음 {on.size}
        </button>
        <button type="button" role="tab" aria-selected={tab === 'RECOMMENDED'} className={tab === 'RECOMMENDED' ? 'tab on' : 'tab'} onClick={() => setTab('RECOMMENDED')}>
          이전에 추천받은 관광지 {recommended.length}
        </button>
      </div>

      <ErrorBox message={error} />

      {tab === 'DISLIKED' &&
        (list.length === 0 ? (
          <div className="dislike-empty">
            <h2>아직 관심없음으로 표시한 관광지가 없어요</h2>
            <p>원하지 않는 장소를 AI 추천 목록이나 이전에 추천받은 관광지에서 ‘관심없음’으로 표시해 보세요.</p>
          </div>
        ) : (
          <>
          <div className="poi-grid">
            {slice('DISLIKED', list).map(({ poi, createdAt }) => (
              <PoiCard key={poi.poiId} poi={poi} to={`/pois/${poi.poiId}`} dimmed={on.has(poi.poiId)} left={button(poi)}>
                <p className="muted small-text">
                  {on.has(poi.poiId) ? `${formatDateTime(createdAt)} 관심없음 표시` : '되돌림 · 다음 추천에 다시 나올 수 있어요'}
                </p>
              </PoiCard>
            ))}
          </div>
          <Pager page={pageOf('DISLIKED', list.length)} total={totalOf(list.length)} onPage={(p) => goPage('DISLIKED', p)} />
          </>
        ))}

      {tab === 'RECOMMENDED' &&
        (recommended.length === 0 ? (
          <p className="empty">아직 AI 추천을 받은 기록이 없어요.</p>
        ) : (
          <>
          <div className="poi-grid">
            {slice('RECOMMENDED', recommended).map(({ poi, lastRecommendedAt, times }) => (
              <PoiCard key={poi.poiId} poi={poi} to={`/pois/${poi.poiId}`} dimmed={on.has(poi.poiId)} left={button(poi)}>
                <p className="muted small-text">
                  {formatDateTime(lastRecommendedAt)} 추천{times > 1 ? ` · ${times}번 추천받음` : ''}
                  {on.has(poi.poiId) ? ' · 관심없음' : ''}
                </p>
              </PoiCard>
            ))}
          </div>
          <Pager page={pageOf('RECOMMENDED', recommended.length)} total={totalOf(recommended.length)} onPage={(p) => goPage('RECOMMENDED', p)} />
          </>
        ))}
    </main>
  );
}
