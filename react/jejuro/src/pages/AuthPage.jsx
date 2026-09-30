import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { errorMessage } from '../api/client.js';
import '../styles/auth.css';

function PasswordInput({ id, label, value, onChange, autoComplete, hint, minLength, placeholder }) {
  const [visible, setVisible] = useState(false);
  return <div className="auth-field">
    <label htmlFor={id}>{label}</label>
    <div className="auth-password">
      <input id={id} name={id} type={visible ? 'text' : 'password'} required value={value} onChange={onChange}
        placeholder={placeholder} autoComplete={autoComplete} minLength={minLength} maxLength={72} aria-describedby={hint ? `${id}-hint` : undefined} />
      <button type="button" onClick={() => setVisible(!visible)} aria-label={`${label} ${visible ? '숨기기' : '보기'}`} aria-pressed={visible}>{visible ? '숨기기' : '보기'}</button>
    </div>
    {hint && <small id={`${id}-hint`}>{hint}</small>}
  </div>;
}

export default function AuthPage({ signup = false }) {
  const loginSea = 'https://images.unsplash.com/photo-1579169825453-8d4b4653cc2c?q=80&w=3540&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D';
  const { user, loading, authenticate } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({ email: '', password: '', confirm: '', nickname: '', birthDate: '', genderCode: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const change = (key) => (event) => setValues((old) => ({ ...old, [key]: event.target.value }));
  const target = typeof location.state?.from === 'string' && location.state.from.startsWith('/') && !location.state.from.startsWith('//')
    && !['/login', '/signup'].includes(location.state.from) ? location.state.from : '/travels';
  if (!loading && user) return <Navigate to={target} replace />;

  // 임시 버전: 백엔드 연결 전까지 화면만 동작
  async function sendCode() { setError(''); setCodeSent(true); }
  async function checkCode() { setError(''); setVerified(true); }

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setError('');
    if (signup && !verified) { setError('이메일 인증을 완료해 주세요.'); return; }
    if (signup && values.password !== values.confirm) { setError('비밀번호가 서로 다릅니다. 다시 확인해 주세요.'); return; }
    if (new TextEncoder().encode(values.password).length > 72) { setError('비밀번호가 너무 깁니다. 영문·숫자 72자, 한글 24자 이내로 입력해 주세요.'); return; }
    setBusy(true);
    try {
      const body = { email: values.email.trim(), password: values.password };
      if (signup) Object.assign(body, { nickname: values.nickname.trim(), birthDate: values.birthDate, genderCode: Number(values.genderCode) });
      await authenticate(signup ? 'signup' : 'login', body);
      navigate(target, { replace: true });
    } catch (err) { setError(errorMessage(err)); }
    finally { setBusy(false); }
  }

  return <main className={`auth-page auth-login ${signup ? 'auth-signup' : ''}`}>
    <aside className="auth-login-photo">
      <img src={loginSea} alt="제주 바다와 성산일출봉" />
    </aside>
    <section className="auth-main">
      <Link className="auth-login-logo" to="/" title="메인 화면으로">JEJURO</Link>
      <div className={`auth-form-wrap ${signup ? 'auth-form-signup' : ''}`}>
        <h2>{signup ? '회원가입' : '로그인'}</h2>
        <p className="auth-intro">{signup ? '이메일 인증 후 계정을 만들 수 있습니다.' : '저장한 여행과 새로운 발견이 기다리고 있어요.'}</p>
        {!signup && location.state?.withdrawn && <p className="auth-withdrawn" role="status">탈퇴 요청이 완료되었습니다. 계정 이용이 중단되었어요.</p>}
        {!signup && location.state?.passwordChanged && <p className="auth-withdrawn" role="status">비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.</p>}
        <form onSubmit={submit} className="auth-form" aria-busy={busy}>
          {signup ? <>
            <div className="auth-field">
              <label htmlFor="email">이메일</label>
              <div className="auth-inline">
                <input id="email" name="email" type="email" value={values.email} onChange={change('email')}
                  placeholder="이메일 주소를 입력해주세요" required maxLength={255} disabled={verified} />
                <button type="button" className="auth-mini-btn" onClick={sendCode}
                  disabled={!values.email || verified}>{codeSent ? '재전송' : '인증번호 받기'}</button>
              </div>
            </div>
            <div className="auth-field">
              <label htmlFor="code">인증번호</label>
              <div className="auth-inline">
                <input id="code" value={code} onChange={(e) => setCode(e.target.value)}
                  inputMode="numeric" maxLength={6} placeholder="인증번호를 입력해주세요"
                  disabled={!codeSent || verified} />
                <button type="button" className="auth-mini-btn" onClick={checkCode}
                  disabled={!codeSent || verified || !code}>{verified ? '완료' : '확인'}</button>
              </div>
              <small>인증번호는 5분간 유효합니다.</small>
            </div>
          </> : (
            <div className="auth-field"><label htmlFor="email">이메일 주소</label><input id="email" name="email" type="email" value={values.email} onChange={change('email')} autoComplete="username" placeholder="이메일 주소를 입력해주세요" required maxLength={255} /></div>
          )}

          <PasswordInput id="password" label="비밀번호" placeholder="비밀번호를 입력해주세요" value={values.password} onChange={change('password')} autoComplete={signup ? 'new-password' : 'current-password'} minLength={signup ? 8 : undefined} hint={signup ? '8자 이상 입력해 주세요.' : undefined} />

          {signup && <>
            <PasswordInput id="confirm" label="비밀번호 확인" placeholder="비밀번호를 다시 한 번 입력해주세요" value={values.confirm} onChange={change('confirm')} autoComplete="new-password" />
            <div className="auth-profile-row">
              <div className="auth-field"><label htmlFor="nickname">닉네임</label><input id="nickname" name="nickname" value={values.nickname} onChange={change('nickname')} autoComplete="nickname" placeholder="닉네임을 입력해주세요" required minLength={2} maxLength={30} /></div>
              <div className="auth-field"><label htmlFor="birthDate">생년월일</label><input id="birthDate" name="birthDate" type="date" autoComplete="bday" value={values.birthDate} onChange={change('birthDate')} required max={new Date().toLocaleDateString('en-CA')} /></div>
            </div>
            <fieldset className="auth-gender"><legend>성별</legend><div>{[['1', '남성'], ['2', '여성']].map(([value, label]) => <label key={value}><input type="radio" name="genderCode" value={value} checked={values.genderCode === value} onChange={change('genderCode')} required /><span>{label}</span></label>)}</div></fieldset>
            <p className="auth-profile-note">생년월일과 성별은 여행 추천에 사용됩니다.</p>
          </>}

          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="auth-submit" type="submit" disabled={busy || loading}>{busy ? (signup ? '계정을 만들고 있어요…' : '로그인 중…') : (signup ? '회원가입' : '로그인')}</button>
        </form>
        {!signup && <Link className="auth-signup-button" to="/signup" state={location.state}>회원가입</Link>}
        {signup && <p className="auth-switch"><Link to="/login" state={location.state}>로그인으로 돌아가기</Link></p>}
      </div>
      <footer className="auth-footer">JEJURO · 나의 취향으로 완성하는 제주</footer>
    </section>
  </main>;
}