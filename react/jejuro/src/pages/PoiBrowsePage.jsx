import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchPoiCategories, searchPois } from '../api/poiApi.js';
import { errorMessage } from '../api/client.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import HeartButton from '../components/common/HeartButton.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

// REGION 테이블의 초기 데이터와 같은 값
const REGIONS = [
  { id: '', name: '전체' },
  { id: 1, name: '동부' },
  { id: 2, name: '서부' },
  { id: 3, name: '남부' },
  { id: 4, name: '북부' },
];

/**
 * 3-1페이지: 전체 관광지 목록
 * AI 추천 외의 관광지도 권역·분류·이름으로 찾아서 찜할 수 있다.
 * 찜한 관광지는 AI 추천에서 찜한 것과 같은 찜 목록(TRAVEL_BOOKMARK)에 들어간다.
 */
export default function PoiBrowsePage() {
  const { travelId } = useParams();
  const [filter, setFilter] = useState({ regionId: '', category: '', keyword: '' });
  const [keywordInput, setKeywordInput] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const { bookmarks, isBookmarked, toggle, pending, error: bookmarkError } = useBookmarks(travelId);

  useEffect(() => {
    fetchPoiCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  // 필터나 페이지가 바뀔 때마다 다시 검색
  useEffect(() => {
    setResult(null);
    searchPois({ ...filter, page })
      .then(setResult)
      .catch((e) => setError(errorMessage(e)));
  }, [filter, page]);

  const changeFilter = (patch) => {
    setPage(0);
    setFilter((f) => ({ ...f, ...patch }));
  };

  const submitKeyword = (e) => {
    e.preventDefault();
    changeFilter({ keyword: keywordInput.trim() });
  };

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>전체 관광지</h1>
        <Link className="btn ghost" to={`/travels/${travelId}/recommendations`}>
          ← AI 추천 목록
        </Link>
      </div>
      <p className="hint">AI 추천에 없는 곳도 찾아서 ♡로 찜할 수 있어요. 찜한 곳으로 일차·방문 순서를 직접 짭니다.</p>

      <section className="card filter-box">
        <div className="chips">
          {REGIONS.map((r) => (
            <button
              key={r.name}
              type="button"
              className={String(filter.regionId) === String(r.id) ? 'chip on' : 'chip'}
              onClick={() => changeFilter({ regionId: r.id })}
            >
              {r.name}
            </button>
          ))}
        </div>
        <div className="row">
          <select value={filter.category} onChange={(e) => changeFilter({ category: e.target.value })}>
            <option value="">모든 분류</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <form className="search" onSubmit={submitKeyword}>
            <input
              type="search"
              placeholder="관광지 이름 검색"
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
            />
            <button type="submit" className="btn primary">
              검색
            </button>
          </form>
        </div>
      </section>

      <ErrorBox message={error || bookmarkError} />

      {!result ? (
        <Loading />
      ) : result.items.length === 0 ? (
        <p className="empty">조건에 맞는 관광지가 없습니다.</p>
      ) : (
        <>
          <p className="muted">총 {result.totalCount}곳</p>
          <div className="poi-grid">
            {result.items.map((poi) => (
              <PoiCard
                key={poi.poiId}
                poi={poi}
                right={
                  <HeartButton
                    on={isBookmarked(poi.poiId)}
                    disabled={pending.has(poi.poiId)}
                    onClick={() => toggle(poi)}
                  />
                }
              />
            ))}
          </div>

          {result.totalPages > 1 && (
            <div className="pager">
              <button type="button" className="btn ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>
                이전
              </button>
              <span>
                {page + 1} / {result.totalPages}
              </span>
              <button
                type="button"
                className="btn ghost"
                disabled={page + 1 >= result.totalPages}
                onClick={() => setPage(page + 1)}
              >
                다음
              </button>
            </div>
          )}
        </>
      )}

      <div className="bottom-bar">
        <span>찜 {bookmarks.length}곳</span>
        <Link className="btn primary" to={`/travels/${travelId}/bookmarks`}>
          찜 목록에서 루트 짜기 →
        </Link>
      </div>
    </main>
  );
}