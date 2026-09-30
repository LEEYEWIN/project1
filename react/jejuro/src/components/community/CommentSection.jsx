import { useCallback, useEffect, useState } from 'react';
import { createComment, deleteComment, fetchComments, updateComment } from '../../api/communityApi.js';
import { errorMessage } from '../../api/client.js';
import { formatDateTime } from '../../utils/format.js';
import ErrorBox from '../common/ErrorBox.jsx';
import ReportButton from './ReportButton.jsx';

/**
 * [커뮤니티 상세] 댓글·대댓글
 * - 댓글 목록(오래된 순) + 각 댓글 아래 대댓글
 * - [답글] 누르면 그 댓글 아래 입력칸, 대댓글에 답글을 달아도 같은 원댓글 아래에 붙음(한 단계)
 * - 내 댓글만 [수정] [삭제]
 * - 남의 댓글에 [신고]. 신고로 가려진 댓글은 "신고로 가려진 댓글입니다." (작성자·관리자에게는 내용 + 안내)
 * - locked: 글이 신고로 가려짐 → 새 댓글·답글 입력칸 숨김
 * - onCountChange: 댓글 수가 바뀌면 상세 화면 숫자도 갱신
 */
export default function CommentSection({ postId, locked = false, onCountChange }) {
  const [comments, setComments] = useState(null);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    const list = await fetchComments(postId);
    setComments(list);
    const count = list.reduce((n, c) => n + (c.deleted ? 0 : 1) + c.replies.length, 0);
    onCountChange?.(count);
  }, [postId, onCountChange]);

  useEffect(() => {
    load().catch((e) => setError(errorMessage(e)));
  }, [load]);

  /** 쓰기·수정·삭제 공통: 실행 후 목록 다시 읽기 */
  const run = async (fn) => {
    setBusy(true);
    setError('');
    try {
      await fn();
      await load();
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!text.trim()) {
      setError('댓글 내용을 입력해 주세요.');
      return;
    }
    if (await run(() => createComment(postId, { content: text.trim() }))) setText('');
  };

  const actions = {
    busy,
    locked,
    reported: () => load().catch(() => {}), // 신고되면 "신고된 댓글입니다"로 바뀌므로 목록 다시 읽기
    reply: (parentCommentId, content) => run(() => createComment(postId, { content, parentCommentId })),
    edit: (commentId, content) => run(() => updateComment(commentId, content)),
    remove: (commentId) => {
      if (!window.confirm('댓글을 삭제할까요?')) return Promise.resolve(false);
      return run(() => deleteComment(commentId));
    },
  };

  return (
    <section className="cm-section" aria-labelledby="cm-comments-title">
      <h2 id="cm-comments-title" className="cm-h2">댓글</h2>
      <p className="cm-sub">제주 여행의 순간에 이야기를 남겨 보세요.</p>

      {comments && comments.length === 0 && <p className="cm-muted">첫 댓글을 남겨 보세요.</p>}

      {comments && comments.length > 0 && (
        <ul className="cm-comments">
          {comments.map((c) => (
            <li key={c.commentId}>
              <CommentItem comment={c} actions={actions} />
              {c.replies.length > 0 && (
                <ul className="cm-replies">
                  {c.replies.map((r) => (
                    <li key={r.commentId}>
                      <CommentItem comment={r} actions={actions} replyTo={c.commentId} />
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      )}

      <ErrorBox message={error} />

      {locked ? (
        <p className="cm-muted">신고로 가려진 글이라 댓글을 달 수 없어요.</p>
      ) : (
      <form className="cm-comment-form" onSubmit={submit}>
        <label htmlFor="cm-new-comment" className="sr-only">댓글</label>
        <input
          id="cm-new-comment"
          value={text}
          maxLength={1000}
          placeholder="댓글을 입력해 주세요"
          onChange={(e) => setText(e.target.value)}
        />
        <button type="submit" className="cm-btn yellow" disabled={busy}>
          댓글 등록
        </button>
      </form>
      )}
    </section>
  );
}

/**
 * 댓글 한 개. replyTo: 대댓글이면 원댓글 ID (답글을 달면 원댓글 아래로)
 */
function CommentItem({ comment, actions, replyTo }) {
  const [mode, setMode] = useState(null); // null | 'edit' | 'reply'
  const [value, setValue] = useState('');

  if (comment.deleted) {
    return <div className="cm-comment deleted">삭제된 댓글입니다.</div>;
  }
  if (comment.hidden && comment.content == null) {
    return (
      <div className="cm-comment deleted">
        {comment.blockReason ? `${comment.blockReason} 등의 사유로 차단된 댓글입니다.` : '신고된 댓글입니다.'}
      </div>
    );
  }

  const open = (m) => {
    setMode(m);
    setValue(m === 'edit' ? comment.content : '');
  };

  const save = async (e) => {
    e.preventDefault();
    if (!value.trim()) return;
    const ok =
      mode === 'edit'
        ? await actions.edit(comment.commentId, value.trim())
        : await actions.reply(replyTo ?? comment.commentId, value.trim());
    if (ok) setMode(null);
  };

  return (
    <div className="cm-comment">
      <div className="cm-comment-head">
        <strong>{comment.authorName}</strong>
        <span className="cm-muted">
          {formatDateTime(comment.createdAt, true)}
          {comment.updatedAt && ' · 수정됨'}
        </span>
      </div>

      {mode === 'edit' ? null : <p className="cm-comment-body">{comment.content}</p>}
      {comment.hidden && comment.blockReason && (
        <p className="cm-hidden-note small">{comment.blockReason} 등의 사유로 차단된 댓글이에요 (관리자라서 보이는 중)</p>
      )}
      {comment.hidden && !comment.blockReason && (
        <p className="cm-hidden-note small">
          신고된 댓글이에요. 다른 회원에게는 &quot;신고된 댓글입니다&quot;로 보여요 (관리자라서 보이는 중)
        </p>
      )}

      {mode === null && (
        <div className="cm-comment-actions">
          {!actions.locked && !comment.hidden && (
            <button type="button" className="cm-text-btn" onClick={() => open('reply')}>
              답글
            </button>
          )}
          {!comment.mine && (
            <ReportButton
              targetType="COMMENT"
              targetId={comment.commentId}
              reported={comment.reportedByMe}
              onReported={() => actions.reported()}
            />
          )}
          {comment.mine && (
            <>
              <button type="button" className="cm-text-btn" onClick={() => open('edit')}>
                수정
              </button>
              <button type="button" className="cm-text-btn danger" onClick={() => actions.remove(comment.commentId)}>
                삭제
              </button>
            </>
          )}
        </div>
      )}

      {mode && (
        <form className="cm-comment-form small" onSubmit={save}>
          <label className="sr-only" htmlFor={`cm-c-${comment.commentId}`}>
            {mode === 'edit' ? '댓글 수정' : '답글'}
          </label>
          <input
            id={`cm-c-${comment.commentId}`}
            value={value}
            maxLength={1000}
            // eslint-disable-next-line jsx-a11y/no-autofocus
            autoFocus
            placeholder={mode === 'edit' ? '' : `${comment.authorName}님에게 답글`}
            onChange={(e) => setValue(e.target.value)}
          />
          <button type="button" className="cm-btn" onClick={() => setMode(null)}>
            취소
          </button>
          <button type="submit" className="cm-btn yellow" disabled={actions.busy}>
            {mode === 'edit' ? '수정' : '등록'}
          </button>
        </form>
      )}
    </div>
  );
}