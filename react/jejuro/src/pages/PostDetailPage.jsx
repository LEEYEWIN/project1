import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deletePost, fetchPost, increaseView, likePost, unlikePost } from '../api/communityApi.js';
import { errorMessage } from '../api/client.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import CommentSection from '../components/community/CommentSection.jsx';
import RouteDayList from '../components/community/RouteDayList.jsx';
import RouteShareActions from '../components/community/RouteShareActions.jsx';
import ReportButton from '../components/community/ReportButton.jsx';
import { formatDateTime } from '../utils/format.js';
import '../styles/community.css';

/**
 * 커뮤니티 글 상세
 * - 제목, 닉네임·작성일·글 종류, 첨부 사진 1장 + 본문(+ 만족도 별점)
 * - 조회수 / 좋아요(누르기·취소) / 댓글 수
 * - 첨부된 최종 경로(일차별, 펼치면 지도) + [경로 링크 공유]·[내 여행으로 가져오기]
 * - 댓글·대댓글
 * - 신고: 남의 글·댓글에 [신고] (같은 대상은 한 번). 신고되면 관리자 확인 전까지 "신고된 게시글입니다" (관리자만 내용 확인)
 * - 관리자가 차단한 글: 작성자 포함 모두 서버가 410 → "욕설·비방 등의 사유로 차단되었습니다." 알림 후 목록으로
 * - 관리자가 반려한 글: 원래대로 정상 표시
 * 조회수는 글에 들어올 때마다 +1 (목록에서 다시 들어오거나 새로고침해도 올라감).
 */
export default function PostDetailPage() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [error, setError] = useState('');
  const [liking, setLiking] = useState(false);
  const [version, setVersion] = useState(0); // 신고 뒤 다시 읽기
  const viewedRef = useRef(null); // 이 화면에서 조회수를 올린 글 번호 (StrictMode 두 번 실행·신고 후 다시 읽기에서 중복 방지)

  useEffect(() => {
    let cancelled = false;
    setPost(null);
    fetchPost(postId)
      .then(async (p) => {
        if (cancelled) return;
        setPost(p);
        // 글에 들어올 때마다 조회수 +1 (같은 회원이 다시 들어와도 올라감)
        if (viewedRef.current !== postId) {
          viewedRef.current = postId;
          const { viewCount } = await increaseView(postId);
          if (!cancelled) setPost((cur) => cur && { ...cur, viewCount });
        }
      })
      .catch((e) => {
        if (cancelled) return;
        if (e?.response?.status === 410) {
          // 관리자가 차단한 글: 사유 알림 → 목록으로
          window.alert(errorMessage(e));
          navigate('/community', { replace: true });
          return;
        }
        setError(errorMessage(e));
      });
    return () => {
      cancelled = true;
    };
  }, [postId, navigate, version]);

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
        {post.mine ? (
          <div className="cm-owner">
            {/* 신고 처리 전에는 수정 불가 (삭제는 가능) */}
            {!post.hidden && (
              <Link className="cm-text-btn" to={`/community/posts/${post.postId}/edit`}>
                수정
              </Link>
            )}
            <button type="button" className="cm-text-btn danger" onClick={remove}>
              삭제
            </button>
          </div>
        ) : (
          <div className="cm-owner">
            <ReportButton
              targetType="POST"
              targetId={post.postId}
              reported={post.reportedByMe}
              onReported={() => setVersion((v) => v + 1)}
            />
          </div>
        )}
      </div>

      {post.blockReason && (
        <p className="cm-hidden-note" role="status">
          {post.blockReason} 등의 사유로 차단된 글이에요. 관리자만 볼 수 있고, 다른 회원에게는 차단 안내 후 목록으로 이동합니다.
        </p>
      )}
      {post.hidden && !post.blockReason && post.content != null && (
        <p className="cm-hidden-note" role="status">
          신고된 게시글이에요. 다른 회원에게는 "신고된 게시글입니다"로 보여요. [게시글·신고]에서 차단 또는 반려해 주세요. (관리자라서 보이는 중)
        </p>
      )}

      <h1 className="cm-title">{post.title}</h1>
      <p className="cm-meta">
        {post.authorName} · {formatDateTime(post.createdAt)} · {post.postType === 'REVIEW' ? '여행 후기' : '질문'}
        {post.updatedAt && ` · 수정 ${formatDateTime(post.updatedAt)}`}
      </p>

      {post.hidden && post.content == null ? (
        <div className="cm-reported" role="status">
          <b>신고된 게시글입니다.</b>
          <span>관리자가 확인 중이에요. 문제가 없으면 다시 정상으로 보여요.</span>
        </div>
      ) : (
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
      )}

      <div className="cm-statbar">
        <span>조회 {post.viewCount}</span>
        <button
          type="button"
          className={post.liked ? 'cm-like on' : 'cm-like'}
          aria-pressed={post.liked}
          aria-label={post.liked ? '좋아요 취소' : '좋아요'}
          disabled={liking || (post.hidden && !post.liked)}
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
          <RouteShareActions post={post} />
        </section>
      )}

      <CommentSection postId={post.postId} locked={post.hidden} onCountChange={setCommentCount} />
    </main>
  );
}