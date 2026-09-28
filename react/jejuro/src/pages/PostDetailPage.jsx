import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deletePost, fetchPost, increaseView, likePost, unlikePost } from '../api/communityApi.js';
import { errorMessage } from '../api/client.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import CommentSection from '../components/community/CommentSection.jsx';
import RouteDayList from '../components/community/RouteDayList.jsx';
import { formatDateTime } from '../utils/format.js';
import '../styles/community.css';

/**
 * 커뮤니티 글 상세
 * - 제목, 닉네임·작성일·글 종류, 첨부 사진 1장 + 본문(+ 만족도 별점)
 * - 조회수 / 좋아요(누르기·취소) / 댓글 수
 * - 첨부된 최종 경로(일차별, 펼치면 지도)
 * - 댓글·대댓글
 * 조회수는 브라우저 탭마다 글 하나당 한 번만 올린다(sessionStorage). 새로고침으로 늘지 않음.
 */
export default function PostDetailPage() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [error, setError] = useState('');
  const [liking, setLiking] = useState(false);
  const viewedRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setPost(null);
    fetchPost(postId)
      .then(async (p) => {
        if (cancelled) return;
        setPost(p);
        if (!viewedRef.current && markViewed(postId)) {
          viewedRef.current = true;
          const { viewCount } = await increaseView(postId);
          if (!cancelled) setPost((cur) => cur && { ...cur, viewCount });
        }
      })
      .catch((e) => !cancelled && setError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [postId]);

  const toggleLike = async () => {
    setLiking(true);
    try {
      const res = post.liked ? await unlikePost(postId) : await likePost(postId);
      setPost((cur) => ({ ...cur, liked: res.liked, likeCount: res.likeCount }));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLiking(false);
    }
  };

  const remove = async () => {
    if (!window.confirm('글을 삭제할까요?\n삭제하면 댓글도 함께 볼 수 없게 됩니다.')) return;
    try {
      await deletePost(postId);
      navigate('/community', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  /** 목록에서 들어왔으면 뒤로(탭·정렬·페이지 유지), 주소로 바로 들어왔으면 목록 첫 페이지 */
  const backToList = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/community'));

  const setCommentCount = useCallback((n) => setPost((cur) => cur && { ...cur, commentCount: n }), []);

  if (error && !post) {
    return (
      <main className="page wide cm">
        <ErrorBox message={error} />
        <Link className="cm-btn" to="/community">
          목록으로
        </Link>
      </main>
    );
  }
  if (!post) return <main className="page wide cm"><Loading /></main>;

  return (
    <main className="page wide cm">
      <div className="cm-detail-top">
        <button type="button" className="cm-text-btn" onClick={backToList}>
          ‹ 목록으로
        </button>
        {post.mine && (
          <div className="cm-owner">
            <Link className="cm-text-btn" to={`/community/posts/${post.postId}/edit`}>
              수정
            </Link>
            <button type="button" className="cm-text-btn danger" onClick={remove}>
              삭제
            </button>
          </div>
        )}
      </div>

      <h1 className="cm-title">{post.title}</h1>
      <p className="cm-meta">
        {post.authorName} · {formatDateTime(post.createdAt)} · {post.postType === 'REVIEW' ? '여행 후기' : '질문'}
        {post.updatedAt && ` · 수정 ${formatDateTime(post.updatedAt)}`}
      </p>

      <div className={post.imageUrl ? 'cm-body with-image' : 'cm-body'}>
        {post.imageUrl && <img className="cm-photo" src={post.imageUrl} alt={`${post.title} 첨부 사진`} />}
        <div className="cm-content-card">
          <p className="cm-content">{post.content}</p>
          {post.satisfaction != null && (
            <span className="cm-rating" aria-label={`만족도 5점 중 ${post.satisfaction}점`}>
              만족도 {'★'.repeat(post.satisfaction)}
              {'☆'.repeat(5 - post.satisfaction)}
            </span>
          )}
        </div>
      </div>

      <div className="cm-statbar">
        <span>조회 {post.viewCount}</span>
        <button
          type="button"
          className={post.liked ? 'cm-like on' : 'cm-like'}
          aria-pressed={post.liked}
          aria-label={post.liked ? '좋아요 취소' : '좋아요'}
          disabled={liking}
          onClick={toggleLike}
        >
          {post.liked ? '♥' : '♡'} {post.likeCount}
        </button>
        <span>댓글 {post.commentCount}</span>
      </div>

      <ErrorBox message={error} />

      {post.route && post.route.days.length > 0 && (
        <section className="cm-section" aria-labelledby="cm-route-title">
          <h2 id="cm-route-title" className="cm-h2">첨부된 최종 경로</h2>
          <p className="cm-sub">
            {post.travelName ? `${post.travelName} · ` : ''}일차별 방문지와 지도를 확인합니다.
          </p>
          <RouteDayList route={post.route} />
        </section>
      )}

      <CommentSection postId={post.postId} onCountChange={setCommentCount} />
    </main>
  );
}

/** 이 탭에서 처음 보는 글이면 true (그리고 본 것으로 표시) */
function markViewed(postId) {
  const key = `viewed-post-${postId}`;
  try {
    if (sessionStorage.getItem(key)) return false;
    sessionStorage.setItem(key, '1');
  } catch {
    // 저장소를 못 쓰는 브라우저: 매번 올림
  }
  return true;
}