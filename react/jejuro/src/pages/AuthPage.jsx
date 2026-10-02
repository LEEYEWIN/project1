import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { errorMessage, showError, isBlocked } from '../api/client.js';
import {
  getSocialProviders,
  sendEmailCode,
  socialErrorMessage,
  socialLoginUrl,
  socialName,
  verifyEmailCode,
} from '../api/authApi.js';
import useNicknameCheck from '../hooks/useNicknameCheck.js';
import '../styles/auth.css';

const CODE_VALID_MS = 5 * 60 * 1000; // 서버와 같은 5분

// 남은 밀리초 → 4:59
function formatRemaining(ms) {
  const total = Math.max(0, Math.ceil(ms / 1000));
  const min = Math.floor(total / 60);
  const sec = String(total % 60).padStart(2, '0');
  return `${min}:${sec}`;
}

// 생년월일: 연·월·일을 각각 골라 YYYY-MM-DD 로 만든다 (달력보다 연도를 고르기 쉽게)
const pad2 = (n) => String(n).padStart(2, '0');

function BirthDateSelect({ value, onChange }) {
  const today = new Date();
  const thisYear = today.getFullYear();
  const [parts, setParts] = useState(() => {
    const [y = '', m = '', d = ''] = (value || '').split('-');
    return { y, m: m ? String(Number(m)) : '', d: d ? String(Number(d)) : '' };
  });

  const years = Array.from({ length: thisYear - 1920 + 1 }, (_, i) => thisYear - i);
  const lastDay = parts.y && parts.m ? new Date(Number(parts.y), Number(parts.m), 0).getDate() : 31;
  const isFuture = (y, m, d) =>
    new Date(Number(y), Number(m) - 1, Number(d)) > new Date(thisYear, today.getMonth(), today.getDate());

  function update(key, v) {
    const next = { ...parts, [key]: v };
    // 월을 바꿔 그 달에 없는 날짜가 되면(예: 2월 30일) 일을 비운다
    if (next.y && next.m && next.d) {
      const max = new Date(Number(next.y), Number(next.m), 0).getDate();
      if (Number(next.d) > max) next.d = '';
    }
    setParts(next);
    onChange(next.y && next.m && next.d ? `${next.y}-${pad2(next.m)}-${pad2(next.d)}` : '');
  }

  return (
    <div className="auth-birth-selects">
      <select id="birthYear" aria-label="태어난 연도" value={parts.y} onChange={(e) => update('y', e.target.value)} required>
        <option value="">연도</option>
        {years.map((y) => <option key={y} value={y}>{y}년</option>)}
      </select>
      <select aria-label="태어난 월" value={parts.m} onChange={(e) => update('m', e.target.value)} required>
        <option value="">월</option>
        {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
          <option key={m} value={m} disabled={parts.y && isFuture(parts.y, m, 1)}>{m}월</option>
        ))}
      </select>
      <select aria-label="태어난 일" value={parts.d} onChange={(e) => update('d', e.target.value)} required>
        <option value="">일</option>
        {Array.from({ length: lastDay }, (_, i) => i + 1).map((d) => (
          <option key={d} value={d} disabled={parts.y && parts.m && isFuture(parts.y, parts.m, d)}>{d}일</option>
        ))}
      </select>
    </div>
  );
}

function PasswordInput({
  id,
  label,
  value,
  onChange,
  autoComplete,
  hint,
  minLength,
  placeholder,
  hintClass,
  hintRole,
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="auth-field">
      <label htmlFor={id}>{label}</label>

      <div className="auth-password">
        <input
          id={id}
          name={id}
          type={visible ? 'text' : 'password'}
          required
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          autoComplete={autoComplete}
          minLength={minLength}
          maxLength={72}
          aria-describedby={hint ? `${id}-hint` : undefined}
        />

        <button
          type="button"
          onClick={() => setVisible(!visible)}
          aria-label={`${label} ${visible ? '숨기기' : '보기'}`}
          aria-pressed={visible}
        >
          {visible ? '숨기기' : '보기'}
        </button>
      </div>

      {hint && (
        <small id={`${id}-hint`} className={hintClass} role={hintRole}>
          {hint}
        </small>
      )}
    </div>
  );
}

export default function AuthPage({ signup = false }) {
  const loginSea =
    'https://images.unsplash.com/photo-1579169825453-8d4b4653cc2c?q=80&w=3540&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D';

  const { user, loading, authenticate, expiredNotice } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();

  const [values, setValues] = useState({
    email: '',
    password: '',
    confirm: '',
    nickname: '',
    birthDate: '',
    genderCode: '',
  });

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  // 인증번호 받기·확인 오류는 입력 칸 바로 아래에 보여 준다 (맨 아래 오류 칸은 눈에 잘 안 띔)
  const [emailError, setEmailError] = useState('');
  const nicknameCheck = useNicknameCheck(signup ? values.nickname : '');
  const confirmMatches = values.confirm !== '' && values.password === values.confirm;
  const [codeError, setCodeError] = useState('');

  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [sending, setSending] = useState(false);
  const [notice, setNotice] = useState('');
  const [expiresAt, setExpiresAt] = useState(null);
  const [now, setNow] = useState(Date.now());
  const [socialProviders, setSocialProviders] = useState([]);

  const remaining = expiresAt ? expiresAt - now : 0;
  // 인증을 마친 뒤에는 시간 제한이 없다(인증번호만 5분 유효). 가입은 같은 브라우저 세션이 유지되는 동안 가능
  const codeExpired = codeSent && !verified && remaining <= 0;

  // 인증번호를 보낸 뒤 1초마다 남은 시간을 다시 그린다.
  useEffect(() => {
    if (!expiresAt) return undefined;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [expiresAt]);

  // 인증 전에 5분이 지나면 인증번호가 무효가 되므로 화면도 처음 상태로 돌린다.
  useEffect(() => {
    if (!codeExpired) return;
    setVerified(false);
    setCode('');
    setNotice('');
  }, [codeExpired]);

  // 로그인 화면: 키가 설정된 소셜 로그인 버튼만 보여 준다.
  useEffect(() => {
    if (signup) return undefined;
    let active = true;
    getSocialProviders()
      .then((list) => active && setSocialProviders(list))
      .catch(() => active && setSocialProviders([]));
    return () => {
      active = false;
    };
  }, [signup]);

  // 소셜 로그인 후 서버가 ?socialError= 로 돌려보낸 경우
  const params = new URLSearchParams(location.search);
  const socialError = params.get('socialError');
  const socialErrorText = socialError
    ? socialErrorMessage(socialError, params.get('provider'))
    : '';

  const change = (key) => (event) => {
    setValues((old) => ({
      ...old,
      [key]: event.target.value,
    }));
  };

  const changeEmail = (event) => {
    setValues((old) => ({
      ...old,
      email: event.target.value,
    }));

    setVerified(false);
    setCodeSent(false);
    setCode('');
    setError('');
    setEmailError('');
    setCodeError('');
    setNotice('');
    setExpiresAt(null);
  };

  const target =
    typeof location.state?.from === 'string' &&
    location.state.from.startsWith('/') &&
    !location.state.from.startsWith('//') &&
    !['/login', '/signup'].includes(location.state.from)
      ? location.state.from
      : '/travels';

  if (!loading && user) {
    return <Navigate to={target} replace />;
  }

  async function sendCode() {
    const email = values.email.trim();

    if (!email) {
      setEmailError('이메일을 입력해 주세요.');
      return;
    }

    if (sending) return;

    setEmailError('');
    setCodeError('');
    setNotice('');
    setSending(true);

    try {
      await sendEmailCode(email);

      const sentAt = Date.now();
      setCodeSent(true);
      setVerified(false);
      setCode('');
      setNow(sentAt);
      setExpiresAt(sentAt + CODE_VALID_MS);
      setNotice(
        `${email}로 인증번호를 보냈어요. 메일함(스팸함 포함)을 확인해 주세요.`
      );
    } catch (err) {
      // 이미 가입된 이메일(409)은 이메일 칸을 고치면 되므로 칸 아래 메시지, 재전송 대기·횟수 초과(429)는 알림창
      if (err?.response?.status === 409) setEmailError(errorMessage(err));
      else showError(err, setEmailError);
    } finally {
      setSending(false);
    }
  }

  async function checkCode() {
    const email = values.email.trim();
    const verificationCode = code.trim();

    if (!verificationCode) {
      setCodeError('인증번호를 입력해 주세요.');
      return;
    }

    if (codeExpired) {
      setCodeError('인증 시간이 지났어요. 인증번호를 다시 받아 주세요.');
      return;
    }

    setCodeError('');

    try {
      const result = await verifyEmailCode(
        email,
        verificationCode
      );

      if (result?.verified) {
        setVerified(true);
        setNotice('');
      } else {
        setVerified(false);
        setCodeError('인증번호가 올바르지 않습니다.');
      }
    } catch (err) {
      setVerified(false);
      // 틀린 번호(400)는 칸 아래 메시지, 시간 지남(410)·5번 틀림(429)은 알림창 후 번호 칸을 비운다
      showError(err, setCodeError);
      if (isBlocked(err)) setCode('');
    }
  }

  async function submit(event) {
    event.preventDefault();

    if (busy) return;

    setError('');

    if (signup && nicknameCheck.status === 'taken') {
      setError(nicknameCheck.message);
      return;
    }

    if (signup && !verified) {
      setError('이메일 인증을 완료해 주세요.');
      return;
    }

    if (
      signup &&
      values.password !== values.confirm
    ) {
      setError(
        '비밀번호가 서로 다릅니다. 다시 확인해 주세요.'
      );
      return;
    }

    if (
      new TextEncoder().encode(values.password).length >
      72
    ) {
      setError(
        '비밀번호가 너무 깁니다. 영문·숫자 72자, 한글 24자 이내로 입력해 주세요.'
      );
      return;
    }

    setBusy(true);

    try {
      const body = {
        email: values.email.trim(),
        password: values.password,
      };

      if (signup) {
        Object.assign(body, {
          nickname: values.nickname.trim(),
          birthDate: values.birthDate,
          genderCode: Number(values.genderCode),
        });
      }

      await authenticate(
        signup ? 'signup' : 'login',
        body
      );

      navigate(target, {
        replace: true,
      });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main
      className={`auth-page auth-login ${
        signup ? 'auth-signup' : ''
      }`}
    >
      <aside className="auth-login-photo">
        <img
          src={loginSea}
          alt="제주 바다와 성산일출봉"
        />
      </aside>

      <section className="auth-main">
        <Link className="auth-login-logo" to="/" title="메인 화면으로">
          JEJURO
        </Link>

        <div
          className={`auth-form-wrap ${
            signup ? 'auth-form-signup' : ''
          }`}
        >
          <h2>
            {signup ? '회원가입' : '로그인'}
          </h2>

          <p className="auth-intro">
            {signup
              ? '이메일 인증 후 계정을 만들 수 있습니다.'
              : '저장한 여행과 새로운 발견이 기다리고 있어요.'}
          </p>

          {!signup &&
            location.state?.withdrawn && (
              <p
                className="auth-withdrawn"
                role="status"
              >
                탈퇴 요청이 완료되었습니다.
                계정 이용이 중단되었어요.
              </p>
            )}

          {!signup && expiredNotice && !location.state?.passwordChanged && <p className="auth-withdrawn" role="status">{expiredNotice}</p>}
          {!signup && location.state?.passwordChanged && <p className="auth-withdrawn" role="status">비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.</p>}
          {!signup && socialErrorText && (
            <div className="auth-error" role="alert">
              {socialErrorText}
            </div>
          )}
          <form
            onSubmit={submit}
            className="auth-form"
            aria-busy={busy}
          >
            {signup ? (
              <>
                <div className="auth-field">
                  <label htmlFor="email">
                    이메일
                  </label>

                  <div className="auth-inline">
                    <input
                      id="email"
                      name="email"
                      type="email"
                      value={values.email}
                      onChange={changeEmail}
                      placeholder="이메일 주소를 입력해주세요"
                      required
                      maxLength={255}
                      disabled={verified}
                    />

                    <button
                      type="button"
                      className="auth-mini-btn"
                      onClick={sendCode}
                      disabled={
                        !values.email || verified || sending
                      }
                    >
                      {sending
                        ? '전송 중…'
                        : codeSent
                          ? '재전송'
                          : '인증번호 받기'}
                    </button>
                  </div>

                  {emailError && (
                    <p className="auth-field-error" role="alert">
                      {emailError}
                    </p>
                  )}
                </div>

                <div className="auth-field">
                  <label htmlFor="code">
                    인증번호
                  </label>

                  <div className="auth-inline">
                    <input
                      id="code"
                      value={code}
                      onChange={(e) =>
                        setCode(
                          e.target.value.replace(
                            /\D/g,
                            ''
                          )
                        )
                      }
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="인증번호를 입력해주세요"
                      disabled={
                        !codeSent || verified || codeExpired
                      }
                    />

                    <button
                      type="button"
                      className="auth-mini-btn"
                      onClick={checkCode}
                      disabled={
                        !codeSent ||
                        verified ||
                        codeExpired ||
                        !code
                      }
                    >
                      {verified ? '완료' : '확인'}
                    </button>
                  </div>

                  {codeError && (
                    <p className="auth-field-error" role="alert">
                      {codeError}
                    </p>
                  )}

                  {notice && (
                    <p
                      className="auth-notice"
                      role="status"
                    >
                      {notice}
                    </p>
                  )}

                  {verified ? (
                    <small className="auth-timer">
                      이메일 인증이 완료되었습니다. 이어서 가입 정보를 입력해 주세요.
                    </small>
                  ) : codeExpired ? (
                    <small className="auth-timer auth-timer-expired">
                      인증 시간이 지났어요.
                      인증번호를 다시 받아 주세요.
                    </small>
                  ) : codeSent ? (
                    <small className="auth-timer">
                      남은 시간{' '}
                      <strong>
                        {formatRemaining(remaining)}
                      </strong>{' '}
                      · 인증번호는 5분간 유효합니다.
                    </small>
                  ) : (
                    <small>
                      인증번호는 5분간
                      유효합니다. (재전송은
                      1분 뒤부터, 5번 틀리면
                      다시 받아야 해요)
                    </small>
                  )}
                </div>
              </>
            ) : (
              <div className="auth-field">
                <label htmlFor="email">
                  이메일 주소
                </label>

                <input
                  id="email"
                  name="email"
                  type="email"
                  value={values.email}
                  onChange={change('email')}
                  autoComplete="username"
                  placeholder="이메일 주소를 입력해주세요"
                  required
                  maxLength={255}
                />
              </div>
            )}

            <PasswordInput
              id="password"
              label="비밀번호"
              placeholder="비밀번호를 입력해주세요"
              value={values.password}
              onChange={change('password')}
              autoComplete={
                signup
                  ? 'new-password'
                  : 'current-password'
              }
              minLength={
                signup ? 8 : undefined
              }
              hint={
                signup
                  ? '8자 이상 입력해 주세요.'
                  : undefined
              }
            />

            {signup && (
              <>
                <PasswordInput
                  id="confirm"
                  label="비밀번호 확인"
                  placeholder="비밀번호를 다시 한 번 입력해주세요"
                  value={values.confirm}
                  onChange={change('confirm')}
                  autoComplete="new-password"
                  hint={
                    values.confirm
                      ? confirmMatches
                        ? '비밀번호가 일치합니다.'
                        : '비밀번호가 일치하지 않습니다.'
                      : undefined
                  }
                  hintClass={confirmMatches ? 'auth-match-ok' : 'auth-match-bad'}
                  hintRole={confirmMatches ? 'status' : 'alert'}
                />

                <div className="auth-profile-row">
                  <div className="auth-field">
                    <label htmlFor="nickname">
                      닉네임
                    </label>

                    <input
                      id="nickname"
                      name="nickname"
                      value={values.nickname}
                      onChange={change(
                        'nickname'
                      )}
                      autoComplete="nickname"
                      placeholder="닉네임을 입력해주세요"
                      required
                      minLength={2}
                      maxLength={30}
                      aria-describedby="nickname-check"
                    />

                    {nicknameCheck.message && (
                      <small
                        id="nickname-check"
                        className={nicknameCheck.status === 'taken' ? 'auth-nickname-taken' : 'auth-nickname-ok'}
                        role={nicknameCheck.status === 'taken' ? 'alert' : 'status'}
                      >
                        {nicknameCheck.message}
                      </small>
                    )}
                  </div>

                  <div className="auth-field auth-birth">
                    <label htmlFor="birthYear">
                      생년월일
                    </label>

                    <BirthDateSelect
                      value={values.birthDate}
                      onChange={(birthDate) =>
                        setValues((old) => ({ ...old, birthDate }))
                      }
                    />
                  </div>
                </div>

                <fieldset className="auth-gender">
                  <legend>성별</legend>

                  <div>
                    {[
                      ['1', '남성'],
                      ['2', '여성'],
                    ].map(
                      ([value, label]) => (
                        <label key={value}>
                          <input
                            type="radio"
                            name="genderCode"
                            value={value}
                            checked={
                              values.genderCode ===
                              value
                            }
                            onChange={change(
                              'genderCode'
                            )}
                            required
                          />

                          <span>
                            {label}
                          </span>
                        </label>
                      )
                    )}
                  </div>
                </fieldset>

                <p className="auth-profile-note">
                  생년월일과 성별은 여행
                  추천에 사용됩니다.
                </p>
              </>
            )}

            {error && (
              <div
                className="auth-error"
                role="alert"
              >
                {error}
              </div>
            )}

            <button
              className="auth-submit"
              type="submit"
              disabled={
                busy ||
                loading ||
                (signup && !verified)
              }
            >
              {busy
                ? signup
                  ? '계정을 만들고 있어요…'
                  : '로그인 중…'
                : signup
                  ? '회원가입'
                  : '로그인'}
            </button>
          </form>

          {!signup && socialProviders.length > 0 && (
            <div className="auth-social">
              <p className="auth-social-title">
                연동한 소셜 계정으로 로그인
              </p>

              <div className="auth-social-buttons">
                {socialProviders.map((provider) => (
                  <a
                    key={provider}
                    className={`auth-social-btn auth-social-${provider.toLowerCase()}`}
                    href={socialLoginUrl(provider)}
                  >
                    {socialName(provider)}로 로그인
                  </a>
                ))}
              </div>

              <small>
                소셜 계정은 회원가입 후 내 정보 수정에서
                연동하면 사용할 수 있어요.
              </small>
            </div>
          )}

          {!signup && (
            <Link
              className="auth-signup-button"
              to="/signup"
              state={location.state}
            >
              회원가입
            </Link>
          )}

          {signup && (
            <p className="auth-switch">
              <Link
                to="/login"
                state={location.state}
              >
                로그인으로 돌아가기
              </Link>
            </p>
          )}
        </div>

        <footer className="auth-footer">
          JEJURO · 나의 취향으로 완성하는 제주
        </footer>
      </section>
    </main>
  );
}