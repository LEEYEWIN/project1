import { useEffect, useState } from 'react';
import { reportContent } from '../../api/communityApi.js';
import { errorMessage } from '../../api/client.js';
import { REPORT_REASONS } from '../../utils/report.js';

/**
 * [커뮤니티] 글·댓글 신고 버튼 + 사유 고르는 창
 * - reported: 이미 신고함 → "신고함" (누를 수 없음)
 * - onReported(hidden): 신고 완료. 신고되면 관리자 확인 전까지 "신고된 게시글입니다"로 표시 (hidden = true)
 * 같은 대상은 한 번만, 내 글은 신고 불가(버튼을 아예 안 보여 줌), 정지 회원은 서버가 막음
 */
export default function ReportButton({ targetType, targetId, reported, onReported, className = 'cm-text-btn' }) {
  const [open, setOpen] = useState(false);
  const [done, setDone] = useState(reported);

  useEffect(() => setDone(reported), [reported]);

  if (done) {
    return (
      <span className={`${className} muted`} aria-disabled="true">
        신고함
      </span>
    );
  }

  return (
    <>
      <button type="button" className={className} onClick={() => setOpen(true)}>
        신고
      </button>
      {open && (
        <ReportDialog
          targetType={targetType}
          targetId={targetId}
          onClose={() => setOpen(false)}
          onDone={(hidden) => {
            setOpen(false);
            setDone(true);
            onReported?.(hidden);
          }}
        />
      )}
    </>
  );
}

function ReportDialog({ targetType, targetId, onClose, onDone }) {
  const [reason, setReason] = useState('');
  const [detail, setDetail] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const what = targetType === 'POST' ? '글' : '댓글';

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const submit = async (e) => {
    e.preventDefault();
    if (!reason) {
      setError('신고 사유를 골라 주세요.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const { hidden } = await reportContent({ targetType, targetId, reasonCode: reason, detail: detail.trim() });
      window.alert(`신고가 접수되었어요. 관리자가 확인할 때까지 이 ${what}은(는) "신고된 ${what === '글' ? '게시글' : what}입니다"로 표시됩니다.`);
      onDone(hidden);
    } catch (err) {
      setError(errorMessage(err));
      if (err?.response?.status === 409) onDone(false); // 이미 신고한 대상
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rp-overlay" role="presentation" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <form className="rp-dialog" role="dialog" aria-modal="true" aria-labelledby="rp-title" onSubmit={submit}>
        <h2 id="rp-title">{what} 신고</h2>
        <p className="cm-muted">
          신고한 내용은 관리자만 봅니다. 같은 {what}은(는) 한 번만 신고할 수 있어요.
        </p>
        <fieldset className="rp-reasons">
          <legend className="sr-only">신고 사유</legend>
          {REPORT_REASONS.map((r) => (
            <label key={r.code} className={reason === r.code ? 'on' : ''}>
              <input
                type="radio"
                name="reason"
                value={r.code}
                checked={reason === r.code}
                onChange={() => setReason(r.code)}
              />
              <span>
                <b>{r.label}</b>
                <small>{r.hint}</small>
              </span>
            </label>
          ))}
        </fieldset>
        <label className="cm-field">
          <span>자세한 내용 (선택)</span>
          <textarea
            rows={3}
            maxLength={200}
            value={detail}
            placeholder="어떤 점이 문제인지 적어 주시면 처리에 도움이 돼요 (200자)"
            onChange={(e) => setDetail(e.target.value)}
          />
        </label>
        {error && (
          <p className="error-text" role="alert">
            {error}
          </p>
        )}
        <p className="rp-policy">
          신고가 5건 이상(음란·개인정보는 2건 이상) 쌓이면 관리자 확인 전까지 자동으로 가려지고, 허위 신고가 반복되면 신고한
          회원도 제재될 수 있어요.
        </p>
        <div className="cm-form-actions">
          <button type="button" className="cm-btn" onClick={onClose}>
            취소
          </button>
          <button type="submit" className="cm-btn dark" disabled={busy}>
            신고하기
          </button>
        </div>
      </form>
    </div>
  );
}