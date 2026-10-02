import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import client, { errorMessage } from '../api/client.js';

const AuthContext = createContext(null);
function clearAccountCache() {
  try {
    Object.keys(sessionStorage).filter((key) => key.startsWith('recommend:') || key.startsWith('routeDraft:')).forEach((key) => sessionStorage.removeItem(key));
  } catch { /* 저장소를 사용할 수 없어도 로그인은 동작한다. */ }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expiredNotice, setExpiredNotice] = useState('');
  const userRef = useRef(null);
  userRef.current = user;
  const refresh = useCallback(async () => {
    setLoading(true);
    setError('');
    try { setUser((await client.get('/me')).data); }
    catch (err) {
      setUser(null);
      if (err.response?.status !== 401) setError(errorMessage(err));
    } finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => {
    const expired = (event) => {
      // 로그인된 화면에서 세션이 끊긴 경우만 로그인 화면에 안내를 남긴다
      if (userRef.current) setExpiredNotice(event.detail?.message || '로그인이 만료되었습니다. 다시 로그인해 주세요.');
      setUser(null);
      clearAccountCache();
    };
    window.addEventListener('auth:expired', expired);
    return () => window.removeEventListener('auth:expired', expired);
  }, []);
  async function authenticate(kind, values) {
    const { data } = await client.post(`/auth/${kind}`, values);
    clearAccountCache();
    setUser(data);
    setError('');
    setExpiredNotice('');
    return data;
  }
  async function withdraw(password, confirmed) {
    await client.post('/auth/withdraw', { password, confirmed });
    setUser(null);
    clearAccountCache();
  }
  async function updateNickname(nickname) {
    await client.patch('/me/profile/nickname', { nickname });
    setUser(user => user ? { ...user, nickname } : user);
  }
  function clearSession() { setUser(null); clearAccountCache(); }
  async function logout() {
    await client.post('/auth/logout');
    setUser(null);
    clearAccountCache();
  }
  return <AuthContext.Provider value={{ user, loading, error, expiredNotice, refresh, authenticate, logout, withdraw, clearSession, updateNickname }}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);

export function RequireAuth({ admin = false }) {
  const { user, loading, error, refresh } = useAuth();
  const location = useLocation();
  if (loading) return <main className="page center" role="status">로그인 정보를 확인하고 있어요.</main>;
  if (error) return <main className="page center"><div><p role="alert">{error}</p><button className="btn primary" onClick={refresh}>다시 연결</button></div></main>;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  if (admin && user.role !== 'ADMIN') return <main className="page"><h1>관리자만 볼 수 있어요</h1><p>관리자 권한이 있는 계정으로 로그인해 주세요.</p></main>;
  return <Outlet />;
}
