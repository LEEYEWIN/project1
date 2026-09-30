import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import '../styles/account.css';

export default function WithdrawalPage() {
  const { withdraw } = useAuth();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    if (busy || !confirmed) return;
    setBusy(true); setError('');
    try {
      await withdraw(password, confirmed);
      navigate('/login', { replace: true, state: { withdrawn: true } });
    } catch (err) { setError(errorMessage(err)); }
    finally { setBusy(false); }
  }
  return <main className="withdraw-page">
    <div className="withdraw-content">
      <header className="withdraw-heading"><p>MY ACCOUNT</p><h1>회원 탈퇴</h1>
        <div>탈퇴 요청 후 계정 이용이 중단됩니다.</div><span className="withdraw-tangerine" aria-hidden="true">🍊</span>
      </header>
      <section className="withdraw-notice" aria-labelledby="withdraw-notice-title">
        <h2 id="withdraw-notice-title">탈퇴 전에 확인해 주세요</h2>
        <p>탈퇴 요청 즉시 로그아웃되며, 이 계정으로 다시 로그인할 수 없습니다.</p>
        <p>계정은 탈퇴 대기 상태로 전환됩니다. 여행 계획·찜·후기·게시글은 즉시 삭제되지 않습니다.</p>
      </section>
      <form onSubmit={submit} className="withdraw-form" aria-busy={busy}>
        <div className="withdraw-password"><label htmlFor="withdraw-password">현재 비밀번호</label>
          <input id="withdraw-password" type="password" autoComplete="current-password" placeholder="비밀번호 입력"
            required maxLength={72} value={password} onChange={event => setPassword(event.target.value)} disabled={busy} />
        </div>
        <label className={`withdraw-confirm ${confirmed ? 'is-checked' : ''}`}>
          <input type="checkbox" required checked={confirmed} onChange={event => setConfirmed(event.target.checked)} disabled={busy} />
          <span>탈퇴 내용을 확인했습니다</span>
        </label>
        {error && <p className="withdraw-error" role="alert">{error}</p>}
        <div className="withdraw-actions"><button type="submit" disabled={busy || !confirmed || !password}>{busy ? '처리 중…' : '탈퇴 요청'}</button></div>
      </form>
    </div>
  </main>;
}
