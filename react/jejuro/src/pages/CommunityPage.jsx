import { useEffect, useState } from 'react';
import { fetchPosts } from '../api/communityApi.js';
import { errorMessage } from '../api/client.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import PostRoute from '../components/community/PostRoute.jsx';

/** 후기 게시판 (8페이지에서 올린 REVIEW 글 목록). 여행이 첨부된 후기는 최종 경로도 함께 보여준다. */
export default function CommunityPage() {
  const [type, setType] = useState('REVIEW');
  const [posts, setPosts] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setPosts(null);
    fetchPosts(type)
      .then(setPosts)
      .catch((e) => setError(errorMessage(e)));
  }, [type]);

  return (
    <main className="page">
      <h1>커뮤니티</h1>
      <div className="chips">
        <button type="button" className={type === 'REVIEW' ? 'chip on' : 'chip'} onClick={() => setType('REVIEW')}>
          여행 후기
        </button>
        <button type="button" className={type === 'QUESTION' ? 'chip on' : 'chip'} onClick={() => setType('QUESTION')}>
          질문
        </button>
      </div>
      <ErrorBox message={error} />
      {!posts ? (
        <Loading />
      ) : posts.length === 0 ? (
        <p className="empty">아직 글이 없습니다.</p>
      ) : (
        posts.map((p) => (
          <article key={p.postId} className="card post">
            <h2>{p.title}</h2>
            <p className="muted">
              {p.authorName} · {p.createdAt?.slice(0, 10)}
            </p>
            <p className="post-content">{p.content}</p>
            {p.route && <PostRoute travelName={p.travelName} route={p.route} />}
          </article>
        ))
      )}
    </main>
  );
}