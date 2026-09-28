import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { fetchPoiCategories, searchPois } from '../api/poiApi.js';
import { errorMessage } from '../api/client.js';
import useBookmarks from '../hooks/useBookmarks.js';
import PoiCard from '../components/common/PoiCard.jsx';
import HeartButton from '../components/common/HeartButton.jsx';
import AddToRouteButton from '../components/poi/AddToRouteButton.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import { CATEGORY_LABEL, categoryLabel } from '../utils/format.js';
import '../styles/poi.css';

// REGION 테이블의 초기 데이터와 같은 값
const REGIONS = [
  { id: '', name: '전체' },
  { id: '1', name: '동부' },
  { id: '2', name: '서부' },
  { id: '3', name: '남부' },
  { id: '4', name: '북부' },
];
const PAGE_SIZE = 12;

/**
 * 관광지 목록·검색 (FR-32, FR-34) — 두 곳에서 같은 화면을 쓴다
 * ① /pois                      메인 메뉴에서: 둘러보기만 (찜 없음, 비회원 가능)
 * ② /travels/:travelId/pois    여행 찜 목록 → "전체 관광지 보기": ♡ 찜 + [루트에 추가]
 * - 권역·관광 유형·검색어는 함께 적용(AND), 이름 가나다순, 12개씩
 * - 조건을 바꾸면 1쪽부터. 조건은 주소(?region=&category=&q=&page=)에 남겨 뒤로 가기 해도 유지
 */
export default function PoiListPage() {
  const { travelId } = useParams();
  const travelMode = Boolean(travelId);
  const [params, setParams] = useSearchParams();
  const regionId = params.get('region') ?? '';
  const category = params.get('category') ?? '';
  const keyword = params.get('q') ?? '';
  const page = Math.max(Number(params.get('page')) || 0, 0);

  const [keywordInput, setKeywordInput] = useState(keyword);
  const [result, setResult] = useState(null);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState('');
  const { bookmarks, isBookmarked, toggle, ensure, pending, error: bookmarkError } = useBookmarks(travelId);

  useEffect(() => {
    fetchPoiCategories()
      .then((list) => {
        const order = Object.keys(CATEGORY_LABEL); // 팀 분류표 순서
        setCategories([...list].sort((a, b) => order.indexOf(a) - order.indexOf(b)));
      })
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => setKeywordInput(keyword), [keyword]);

  useEffect(() => {
    let cancelled = false;
    setResult(null);
    setError('');
    searchPois({ regionId, category, keyword, page, size: PAGE_SIZE })
      .then((res) => !cancelled && setResult(res))
      .catch((e) => !cancelled && setError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [regionId, category, keyword, page]);

  /** 조건 변경 → 1쪽부터 (FR-34) */
  const change = (patch) => {
    const next = { region: regionId, category, q: keyword, ...patch, page: patch.page ?? '0' };
    Object.keys(next).forEach((k) => (next[k] === '' || next[k] == null) && delete next[k]);
    setParams(next);
  };

  const submitKeyword = (e) => {
    e.preventDefault();
    change({ q: keywordInput.trim() });
  };

  const detailPath = (poiId) => (travelMode ? `/travels/${travelId}/pois/${poiId}` : `/pois/${poiId}`);
  const filtered = regionId || category || keyword;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>제주 관광지</h1>
        {travelMode && (
          <Link className="btn ghost" to={`/travels/${travelId}/bookmarks`}>
            ← 찜 목록
          </Link>
        )}
      </div>
      <p className="hint">
        {travelMode
          ? '♡로 찜하거나 [루트에 추가]로 경로의 원하는 일차에 바로 넣을 수 있어요.'
          : '제주 관광지를 권역·관광 유형·이름으로 찾아보세요. 찜과 루트 만들기는 여행을 만든 뒤 할 수 있어요.'}
      </p>

      <section className="card filter-box" aria-label="관광지 찾기">
        <div className="chips" role="group" aria-label="권역">
          {REGIONS.map((r) => (
            <button
              key={r.name}
              type="button"
              aria-pressed={regionId === r.id}
              className={regionId === r.id ? 'chip on' : 'chip'}
              onClick={() => change({ region: r.id })}
            >
              {r.name}
            </button>
          ))}
        </div>
        <div className="row">
          <label className="sr-only" htmlFor="poi-category">관광 유형</label>
          <select id="poi-category" value={category} onChange={(e) => change({ category: e.target.value })}>
            <option value="">모든 관광 유형</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {categoryLabel(c)}
              </option>
            ))}
          </select>
          <form className="search" role="search" onSubmit={submitKeyword}>
            <label className="sr-only" htmlFor="poi-keyword">관광지 이름</label>
            <input
              id="poi-keyword"
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

      {!result && !error && <Loading />}

      {result && result.items.length === 0 && (
        <div className="empty">
          <p>{keyword ? `‘${keyword}’ 검색 결과가 없어요.` : '조건에 맞는 관광지가 없어요.'}</p>
          <p className="hint">검색어를 줄이거나 권역·관광 유형을 바꿔 보세요.</p>
          {filtered && (
            <button type="button" className="btn ghost" onClick={() => setParams({})}>
              조건 모두 지우기
            </button>
          )}
        </div>
      )}

      {result && result.items.length > 0 && (
        <>
          <p className="muted">총 {result.totalCount}곳 · 이름순</p>
          <div className="poi-grid">
            {result.items.map((poi) => (
              <PoiCard
                key={poi.poiId}
                poi={poi}
                to={detailPath(poi.poiId)}
                right={
                  travelMode && (
                    <HeartButton
                      on={isBookmarked(poi.poiId)}
                      disabled={pending.has(poi.poiId)}
                      onClick={() => toggle(poi)}
                    />
                  )
                }
              >
                {travelMode && <AddToRouteButton travelId={travelId} poi={poi} ensureBookmarked={ensure} />}
              </PoiCard>
            ))}
          </div>

          {result.totalPages > 1 && (
            <nav className="pager" aria-label="페이지">
              <button type="button" className="btn ghost" disabled={page === 0} onClick={() => change({ page: String(page - 1) })}>
                이전
              </button>
              <span>
                {page + 1} / {result.totalPages}
              </span>
              <button
                type="button"
                className="btn ghost"
                disabled={page + 1 >= result.totalPages}
                onClick={() => change({ page: String(page + 1) })}
              >
                다음
              </button>
            </nav>
          )}
        </>
      )}

      {travelMode ? (
        <div className="bottom-bar">
          <span>찜 {bookmarks.length}곳</span>
          <Link className="btn primary" to={`/travels/${travelId}/bookmarks`}>
            찜 목록에서 루트 짜기 →
          </Link>
        </div>
      ) : (
        <div className="bottom-bar">
          <span>마음에 드는 곳이 있나요? 여행을 만들면 AI 추천과 찜, 루트 짜기를 할 수 있어요.</span>
          <Link className="btn primary" to="/travels/new">
            새 여행 만들기 →
          </Link>
        </div>
      )}
    </main>
  );
}
