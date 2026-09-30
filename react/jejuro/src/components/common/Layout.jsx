import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext.jsx';
import '../../styles/account.css';
import { errorMessage } from '../../api/client.js';
import logoImg from '../../assets/jejuro-logo.jpg';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  async function signOut() {
    setBusy(true); setError('');
    try { await logout(); navigate('/login', { replace: true }); }
    catch (err) { setError(errorMessage(err)); }
    finally { setBusy(false); }
  }
  return <>
    <header className="topbar">
        <NavLink to="/" className="logo">
          <img src={logoImg} alt="제주로" />
        </NavLink>      <nav aria-label="주 메뉴">
        <NavLink to="/" end>홈</NavLink>
        <NavLink to="/travels" end>내 여행</NavLink>
        <NavLink to="/travels/new">새 여행</NavLink>
        <NavLink to="/pois">관광지</NavLink>
        <NavLink to="/community">후기 게시판</NavLink>
        {user?.role === 'ADMIN' && <NavLink to="/admin/kpi">관리자</NavLink>}
        {user ? <><div className="account-menu" onMouseLeave={() => setAccountOpen(false)} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setAccountOpen(false); }} onKeyDown={(event) => { if (event.key === 'Escape') { event.currentTarget.querySelector('button').focus(); setAccountOpen(false); } }}>
          <button type="button" className="account-name account-trigger" aria-expanded={accountOpen} aria-controls="account-dropdown"
            onClick={() => setAccountOpen(!accountOpen)} onMouseEnter={() => setAccountOpen(true)}> {user.nickname}님 <span aria-hidden="true">⌄</span></button>
          <div id="account-dropdown" className={`account-dropdown ${accountOpen ? 'is-open' : ''}`} onMouseLeave={() => setAccountOpen(false)}>
            <NavLink to="/account/withdraw" onClick={() => setAccountOpen(false)}>탈퇴하기</NavLink>
          </div>
        </div><button className="btn small" onClick={signOut} disabled={busy}>{busy ? '로그아웃 중…' : '로그아웃'}</button></>
          : <><NavLink to="/login">로그인</NavLink><NavLink to="/signup">회원가입</NavLink></>}
      </nav>
    </header>
    {error && <p className="page error" role="alert">{error}</p>}
    <Outlet />
  </>;
}
