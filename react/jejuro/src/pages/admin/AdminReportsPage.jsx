import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchReports, handleReport, unblockReport } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';
import Loading from '../../components/common/Loading.jsx';
import { Forbidden, Pager, dt } from '../../components/admin/AdminCommon.jsx';
import { ACTION_LABEL, REASON_LABEL, REPORT_REASONS } from '../../utils/report.js';

const TYPES = [
  { value: '', label: '전체' },
  { value: 'POST', label: '글' },
  { value: 'COMMENT', label: '댓글' },
];

/**
 * 관리자: 게시글·댓글 신고 처리 (/admin/reports)
 * - 신고 1건씩이 아니라 신고된 글·댓글 단위로 묶어서, 신고 많은 순으로 보여 준다
 * - 처리: 차단(사유 선택 → 모든 회원에게 "○○ 등의 사유로 차단되었습니다." 알림 후 목록으로) / 반려(정상 표시)
 * - 회원 제재(정지·경고)는 하지 않는다 — 글·댓글만 처리
 */
export default function AdminReportsPage() {
  const [status, setStatus] = useState('PENDING');
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [forbidden, setForbidden] = useState(false);

  const load = useCallback(() => {
    setError('');
    return fetchReports({ status, type, page })
      .then(setData)
      .catch((e) => {
        if (e?.response?.status === 403) setForbidden(true);
        else setError(errorMessage(e));
      });
  }, [status, type, page]);

  useEffect(() => {
    load();
  }, [load]);

  if (forbidden) return <Forbidden />;

  const changeStatus = (s) => {
    setStatus(s);
    setPage(0);
  };

  return (
    <div className="adm-page">
      <header className="adm-header">
        <div>
          <h1>게시글·신고</h1>
          <p className="adm-muted">신고된 글·댓글을 확인하고 차단 또는 반려합니다.</p>
        </div>
        <div className="adm-filters">
          <button type="button" className={status === 'PENDING' ? 'on' : ''} onClick={() => changeStatus('PENDING')}>
            처리 대기 {data ? <span className="num">{data.pendingCount}</span> : ''}
          </button>
          <button type="button" className={status === 'DONE' ? 'on' : ''} onClick={() => changeStatus('DONE')}>
            처리 완료
          </button>
          <label>
            대상
            <select
              value={type}
              onChange={(e) => {
                setType(e.target.value);
                setPage(0);
              }}
            >
              {TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </header>

      <PolicyCard />

      {error && <p className="adm-error">{error}</p>}
      {!data ? (
        <Loading />
      ) : data.items.length === 0 ? (
        <p className="adm-empty">{status === 'PENDING' ? '처리할 신고가 없어요.' : '처리한 신고가 없어요.'}</p>
      ) : (
        <ul className="adm-list">
          {data.items.map((t) => (
            <li key={`${t.targetType}-${t.targetId}`}>
              <ReportCard target={t} onHandled={load} />
            </li>
          ))}
        </ul>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}

/** 처리 기준 (서버 ReportPolicy와 같은 내용) */
function PolicyCard() {
  return (
    <details className="adm-card adm-policy">
      <summary>
        <b>신고 처리 기준</b> <span className="adm-muted">눌러서 펼치기</span>
      </summary>
      <table className="adm-tbl">
        <thead>
          <tr>
            <th>상태</th>
            <th>회원에게 보이는 것</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>신고됨 (처리 대기)</td>
            <td>목록·상세 제목 자리에 "신고된 게시글입니다" (내용·사진·경로 가림, 댓글은 "신고된 댓글입니다")</td>
          </tr>
          <tr>
            <td>차단</td>
            <td>글을 열면 "[선택한 사유] 등의 사유로 차단되었습니다." 알림 → 목록으로. 목록에서도 빠짐 (작성자 포함, 관리자만 내용 확인)</td>
          </tr>
          <tr>
            <td>반려</td>
            <td>원래대로 정상 표시</td>
          </tr>
        </tbody>
      </table>
      <p className="adm-note">
        <b>신고 사유</b>: 음란·불법 / 개인정보 노출 / 욕설·비방 / 스팸·광고. 차단 사유 기본값은 가장 많이 받은 사유(같으면 더 무거운 사유)이고 바꿀 수 있어요.
        <br />
        회원 제재(정지·경고)는 하지 않고 글·댓글만 처리합니다. 잘못 차단했으면 [처리 완료]에서 [차단 해제].
      </p>
    </details>
  );
}

function ReportCard({ target: t, onHandled }) {
  const [blockReason, setBlockReason] = useState(t.mainReason ?? t.reasons[0]?.code ?? 'ABUSE');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const pending = t.status === 'PENDING';
  const what = t.targetType === 'POST' ? '글' : '댓글';
  const currentStatus = t.deleted
    ? '삭제됨'
    : t.blockReason
      ? '차단됨'
      : t.hidden
        ? `신고된 ${what === '글' ? '게시글' : '댓글'}로 표시 중`
        : pending
          ? '처리 대기'
          : t.action === 'KEEP'
            ? '정상 표시'
            : '처리 완료';

  const unblock = async () => {
    if (!window.confirm('차단을 풀고 다시 모든 회원에게 보이게 할까요?')) return;
    setBusy(true);
    setError('');
    try {
      await unblockReport(t.targetType, t.targetId);
      await onHandled();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  /** action: BLOCK 차단 / KEEP 반려 */
  const submit = async (action) => {
    const msg =
      action === 'BLOCK'
        ? `이 ${what}을(를) "${REASON_LABEL[blockReason]}" 사유로 차단할까요?\n회원에게 "${REASON_LABEL[blockReason]} 등의 사유로 차단되었습니다." 알림이 나갑니다.`
        : `신고를 반려하고 이 ${what}을(를) 다시 정상으로 보이게 할까요?`;
    if (!window.confirm(msg)) return;
    setBusy(true);
    setError('');
    try {
      await handleReport(t.targetType, t.targetId, { action, blockReason: action === 'BLOCK' ? blockReason : null });
      await onHandled();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <article className="adm-card adm-report">
      <div className="adm-report-head">
        <div className="adm-report-count">
          <span>{what} 신고</span>
          <strong className="num">{t.reportCount}건</strong>
        </div>
        <span className="adm-report-date">
          접수 {dt(t.firstReportedAt)}
          {t.lastReportedAt !== t.firstReportedAt && ` ~ ${dt(t.lastReportedAt)}`}
        </span>
      </div>

      <div className="adm-report-body">
        {t.postTitle && (
          <p className="adm-report-title">
            {t.targetType === 'COMMENT' && <span className="adm-muted">글 </span>}
            {t.postId && !t.deleted ? (
              <Link to={`/community/posts/${t.postId}`} target="_blank" rel="noreferrer">
                {t.postTitle}
              </Link>
            ) : (
              t.postTitle
            )}
          </p>
        )}
        <dl className="adm-report-facts">
          <div><dt>{what} 번호</dt><dd className="num">#{t.targetId}</dd></div>
          <div className="adm-report-fact-reasons">
            <dt>신고 사유</dt>
            <dd>{t.reasons.length ? t.reasons.map((r) => `${REASON_LABEL[r.code] ?? r.label} ${r.count}건`).join(' · ') : '확인된 사유 없음'}</dd>
          </div>
          <div><dt>현재 상태</dt><dd>{currentStatus}</dd></div>
          <div><dt>이전 차단</dt><dd className="num">{t.authorPriorAccepted ?? 0}건</dd></div>
          <div><dt>작성자</dt><dd>{t.authorName}{t.authorId ? ` #${t.authorId}` : ''}</dd></div>
        </dl>
        <p className="adm-report-content-label">신고 대상 내용</p>
        <blockquote>{t.content ?? '(내용 없음)'}</blockquote>
        {t.details.length > 0 && (
          <ul className="adm-report-details">
            {t.details.map((d, i) => (
              <li key={`${i}-${d}`}>“{d}”</li>
            ))}
          </ul>
        )}
      </div>

      {pending ? (
        <div className="adm-report-form">
          <div className="adm-report-controls">
            <label>
              차단 사유
              <select value={blockReason} onChange={(e) => setBlockReason(e.target.value)}>
                {REPORT_REASONS.map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.label}
                  </option>
                ))}
              </select>
            </label>
            <button type="button" className="adm-btn dark" disabled={busy} onClick={() => submit('BLOCK')}>
              차단
            </button>
            <button type="button" className="adm-btn" disabled={busy} onClick={() => submit('KEEP')}>
              반려 (정상 표시)
            </button>
          </div>
          <p className="adm-muted small">
            차단하면 회원에게 “{REASON_LABEL[blockReason]} 등의 사유로 차단되었습니다.” 알림 후 목록으로 이동해요.
          </p>
          {error && <p className="adm-error">{error}</p>}
        </div>
      ) : (
        <p className="adm-report-result">
          <span className={t.status === 'REJECTED' ? 'adm-badge ok' : 'adm-badge bad'}>
            {ACTION_LABEL[t.action] ?? t.status}
            {t.action === 'BLOCK' && t.blockReason && ` · ${REASON_LABEL[t.blockReason] ?? t.blockReason}`}
          </span>{' '}
          {t.handledByName ?? '관리자'} · {dt(t.handledAt)}
          {t.blockReason && !t.deleted && (
            <button type="button" className="adm-link" disabled={busy} onClick={unblock}>
              차단 해제
            </button>
          )}
          {error && <span className="adm-error"> {error}</span>}
        </p>
      )}
    </article>
  );
}
