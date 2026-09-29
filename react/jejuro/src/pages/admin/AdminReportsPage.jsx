import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchReports, handleReport, unblockReport } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';
import Loading from '../../components/common/Loading.jsx';
import { Forbidden, Pager, dt } from '../../components/admin/AdminCommon.jsx';
import { ACTION_LABEL, REASON_LABEL, REPORT_REASONS, SANCTION_LABEL, suspendText } from '../../utils/report.js';

const TYPES = [
  { value: '', label: '전체' },
  { value: 'POST', label: '글' },
  { value: 'COMMENT', label: '댓글' },
];

/**
 * 관리자: 게시글·댓글 신고 처리 (/admin/reports)
 * - 신고 1건씩이 아니라 신고된 글·댓글 단위로 묶어서, 신고 많은 순으로 보여 준다
 * - 처리: 유지(다시 보이기) / 차단(사유 선택 → 모든 회원에게 차단 안내) / 삭제 + 작성자 제재(경고·7일·30일·영구)
 * - 사유별 권장 처리(처리 기준)를 함께 보여 주고 [권장대로]로 한 번에 채울 수 있다
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
          <p className="adm-muted">신고된 글·댓글을 확인하고 유지·차단·삭제와 작성자 제재를 정합니다.</p>
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
            <th>대표 사유</th>
            <th>권장 처리</th>
            <th>작성자 제재</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>음란·불법</td>
            <td>차단</td>
            <td>30일 정지 · 이전에 조치받은 적 있으면 영구 정지</td>
          </tr>
          <tr>
            <td>개인정보 노출</td>
            <td>차단</td>
            <td>경고</td>
          </tr>
          <tr>
            <td>욕설·비방</td>
            <td>차단</td>
            <td>경고 (경고 3회마다 자동 7일 정지)</td>
          </tr>
          <tr>
            <td>스팸·광고</td>
            <td>차단</td>
            <td>경고 · 이전 조치 2회 이상이면 7일 정지</td>
          </tr>
          <tr>
            <td>기타</td>
            <td>내용 보고 판단</td>
            <td>없음</td>
          </tr>
        </tbody>
      </table>
      <p className="adm-note">
        <b>자동 가림</b>: 처리 대기 신고가 5명 이상(음란·개인정보는 2명 이상)이면 관리자 확인 전까지 다른 회원에게 가려집니다.
        <br />
        <b>대표 사유</b>: 가장 많이 받은 사유, 같으면 더 무거운 사유. 권장은 참고용이고 최종 결정은 관리자가 합니다.
        <br />
        <b>유지</b> = 신고 반려 + 가림 해제, <b>차단</b> = 작성자 포함 모든 회원에게 "'사유' 등의 사유로 게시글이 차단되었습니다." 알림 후 목록으로(관리자만 내용 확인),{' '}
        <b>삭제</b> = 삭제 표시(복구 불가).
        정지된 회원은 글·댓글 쓰기와 신고를 할 수 없고, 읽기·여행 기능은 그대로 씁니다.
      </p>
    </details>
  );
}

function ReportCard({ target: t, onHandled }) {
  const [action, setAction] = useState(t.recommend.action);
  const mainReason = t.reasons[0]?.code ?? 'OTHER'; // 가장 많이 받은 사유
  const [blockReason, setBlockReason] = useState(mainReason);
  const [sanction, setSanction] = useState(t.recommend.sanction);
  const [memo, setMemo] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const pending = t.status === 'PENDING';
  const canSanction = action !== 'KEEP' && t.authorId != null;

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

  const applyRecommend = () => {
    setAction(t.recommend.action);
    setSanction(t.recommend.sanction);
    setBlockReason(mainReason);
  };

  const submit = async () => {
    const s = canSanction ? sanction : 'NONE';
    const how = action === 'BLOCK' ? `"${REASON_LABEL[blockReason]}" 사유로 차단` : `"${ACTION_LABEL[action]}"(으)로 처리`;
    const msg = `${t.targetType === 'POST' ? '글' : '댓글'}을(를) ${how}${
      s !== 'NONE' ? `하고 ${t.authorName}님에게 "${SANCTION_LABEL[s]}"을(를) 줄까요?` : '할까요?'
    }`;
    if (!window.confirm(msg)) return;
    setBusy(true);
    setError('');
    try {
      await handleReport(t.targetType, t.targetId, {
        action,
        blockReason: action === 'BLOCK' ? blockReason : null,
        sanction: s,
        memo: memo.trim(),
      });
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
        <span className="adm-badge">{t.targetType === 'POST' ? '글' : '댓글'} #{t.targetId}</span>
        <b className="num">신고 {t.reportCount}건</b>
        {t.reasons.map((r) => (
          <span key={r.code} className={r.code === 'SEXUAL' || r.code === 'PRIVACY' ? 'adm-badge bad' : 'adm-badge warn'}>
            {r.label} {r.count}
          </span>
        ))}
        {t.deleted && <span className="adm-badge">삭제됨</span>}
        {!t.deleted && t.blockReason && <span className="adm-badge bad">차단됨 · {REASON_LABEL[t.blockReason] ?? t.blockReason}</span>}
        {!t.deleted && t.hidden && !t.blockReason && <span className="adm-badge bad">자동 가림</span>}
        <span className="adm-muted">
          {dt(t.firstReportedAt)}
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
        <blockquote>{t.content ?? '(내용 없음)'}</blockquote>
        {t.details.length > 0 && (
          <ul className="adm-report-details">
            {t.details.map((d, i) => (
              <li key={`${i}-${d}`}>“{d}”</li>
            ))}
          </ul>
        )}
      </div>

      <p className="adm-report-author">
        작성자 <b>{t.authorName}</b>
        {t.authorId && <span className="adm-muted"> #{t.authorId}</span>} · 경고 {t.authorWarningCount}회 · 이전 조치{' '}
        {t.authorPriorAccepted}건
        {t.authorSuspendedUntil && <span className="adm-badge bad">{suspendText(t.authorSuspendedUntil)}</span>}
      </p>

      {pending ? (
        <div className="adm-report-form">
          <p className="adm-recommend">
            권장: <b>{ACTION_LABEL[t.recommend.action]}</b>
            {t.recommend.sanction !== 'NONE' && (
              <>
                {' '}
                + <b>{SANCTION_LABEL[t.recommend.sanction]}</b>
              </>
            )}{' '}
            <span className="adm-muted">— {t.recommend.note}</span>{' '}
            <button type="button" className="adm-link" onClick={applyRecommend}>
              권장대로
            </button>
          </p>
          <div className="adm-report-controls">
            <div className="adm-tabs" role="radiogroup" aria-label="처리">
              {['KEEP', 'BLOCK', 'DELETE'].map((a) => (
                <button
                  key={a}
                  type="button"
                  role="radio"
                  aria-checked={action === a}
                  className={action === a ? 'on' : ''}
                  onClick={() => setAction(a)}
                >
                  {ACTION_LABEL[a]}
                </button>
              ))}
            </div>
            {action === 'BLOCK' && (
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
            )}
            <label>
              작성자 제재
              <select value={canSanction ? sanction : 'NONE'} disabled={!canSanction} onChange={(e) => setSanction(e.target.value)}>
                {['NONE', 'WARNING', 'SUSPEND_7D', 'SUSPEND_30D', 'BAN'].map((s) => (
                  <option key={s} value={s}>
                    {SANCTION_LABEL[s]}
                  </option>
                ))}
              </select>
            </label>
            <input
              className="adm-input"
              value={memo}
              maxLength={150}
              placeholder="제재 사유 메모 (비우면 신고 사유)"
              onChange={(e) => setMemo(e.target.value)}
            />
            <button type="button" className="adm-btn dark" disabled={busy} onClick={submit}>
              처리
            </button>
          </div>
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