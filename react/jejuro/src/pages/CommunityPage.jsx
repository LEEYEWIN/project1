import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { fetchPosts } from '../api/communityApi.js';
import { errorMessage } from '../api/client.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import PostRoute from '../components/community/PostRoute.jsx';
import { formatDateTime } from '../utils/format.js';
import '../styles/community.css';

const TABS = [
  { value: 'REVIEW', label: '여행 후기' },
  { value: 'QUESTION', label: '질문' },
];
const PAGE_SIZE = 3;

/**
 * 커뮤니티 게시판 목록
 * - 상단 탭: 여행 후기 / 질문,  오른쪽: 최신순 / 좋아요순
 * - 한 페이지 3개, 좌·우 화살표로 페이지 이동
 * - 카드: 제목, 닉네임·작성일, 조회수·좋아요·댓글 수, 첨부된 최종 경로(후기)
 * - 탭·정렬·페이지는 주소(?type=&sort=&page=)에 남겨서 상세에서 뒤로 가기 하면 같은 페이지로 돌아온다
 */
export default function CommunityPage() {
  const [params, setParams] = useSearchParams();
  const type = params.get('type') === 'QUESTION' ? 'QUESTION' : 'REVIEW';
  const sort = params.get('sort') === 'likes' ? 'likes' : 'latest';
  const page = Math.max(Number(params.get('page')) || 0, 0);

  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    setData(null);
    setError('');
    fetchPosts({ type, sort, page, size: PAGE_SIZE })
      .then((res) => !cancelled && setData(res))
      .catch((e) => !cancelled && setError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [type, sort, page]);

  const go = (next) => setParams({ type, sort, page: String(page), ...next });

  /** 카드의 빈 곳을 누르면 상세로 (버튼·링크·지도를 누른 경우는 제외) */
  const openCard = (e, postId) => {
    if (e.target.closest('button, a, .cm-route-map')) return;
    navigate(`/community/posts/${postId}`);
  };

  return (
    <main className="page wide cm">
      <p className="cm-eyebrow">COMMUNITY</p>
      <div className="cm-title-row">
        <div>
          <h1 className="cm-title">여행 후기 게시판</h1>
          <p className="cm-sub">다른 여행자의 후기와 공개된 최종 경로를 확인하세요.</p>
        </div>
        <Link className="cm-btn dark" to={`/community/posts/new?type=${type}`}>
          글쓰기
        </Link>
      </div>

      <div className="cm-toolbar">
        <div className="cm-tabs" role="tablist" aria-label="게시판 종류">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              role="tab"
              aria-selected={type === t.value}
              className={type === t.value ? 'cm-pill on' : 'cm-pill'}
              onClick={() => go({ type: t.value, page: '0' })}
            >
              {t.label}
            </button>
          ))}
        </div>
        <label className="cm-sort">
          <span className="sr-only">정렬</span>
          <select value={sort} onChange={(e) => go({ sort: e.target.value, page: '0' })}>
            <option value="latest">최신순</option>
            <option value="likes">좋아요순</option>
          </select>
        </label>
      </div>

      <ErrorBox message={error} />

      {!data && !error && <Loading />}

      {data && data.items.length === 0 && (
        <div className="cm-empty">
          <p>아직 {type === 'REVIEW' ? '여행 후기' : '질문'}가 없어요. 첫 글을 남겨 보세요.</p>
          <Link className="cm-btn" to={`/community/posts/new?type=${type}`}>
            글쓰기
          </Link>
        </div>
      )}

      {data && data.items.length > 0 && (
        <>
          <ul className="cm-list">
            {data.items.map((p) => (
              // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions
              <li key={p.postId} className="cm-card clickable" onClick={(e) => openCard(e, p.postId)}>
                <h2 className="cm-card-title">
                  <Link to={`/community/posts/${p.postId}`}>{p.title}</Link>
                  {p.hasImage && <span className="cm-badge">사진</span>}
                </h2>
                <p className="cm-meta">
                  {p.authorName} · {formatDateTime(p.createdAt)}
                </p>
                <p className="cm-stats">
                  <span>조회 {p.viewCount}</span>
                  <span>좋아요 {p.likeCount}</span>
                  <span>댓글 {p.commentCount}</span>
                </p>
                {p.route && <PostRoute travelName={p.travelName} route={p.route} />}
              </li>
            ))}
          </ul>

          <nav className="cm-pager" aria-label="페이지">
            <button
              type="button"
              className="cm-arrow"
              aria-label="이전 페이지"
              disabled={page === 0}
              onClick={() => go({ page: String(page - 1) })}
            >
              ‹
            </button>
            <span className="cm-page-no">
              {data.page + 1} / {Math.max(data.totalPages, 1)}
            </span>
            <button
              type="button"
              className="cm-arrow"
              aria-label="다음 페이지"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => go({ page: String(page + 1) })}
            >
              ›
            </button>
          </nav>
        </>
      )}
    </main>
  );
}