import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import client, { errorMessage } from '../api/client.js';
import '../styles/profile.css';
const providerName = code => ({ GOOGLE: 'Google', KAKAO: '카카오', NAVER: '네이버', EMAIL: '이메일·비밀번호', UNKNOWN: '확인할 수 없음' }[code] || code);
export default function ProfilePage() {
 const { updateNickname, clearSession } = useAuth();
 const navigate = useNavigate();
 const [profile,setProfile] = useState(null);
 const [nickname,setNickname] = useState('');
 const [values,setValues] = useState({currentPassword:'',newPassword:'',confirmPassword:''});
 const [error,setError] = useState('');
 const [notice,setNotice] = useState('');
 const [busy,setBusy] = useState('');
 const [attempt,setAttempt] = useState(0);
 useEffect(() => { let active=true;setError('');client.get('/me/profile').then(({data}) => {if(active){setProfile(data);setNickname(data.nickname);}}).catch(e=>{if(active)setError(errorMessage(e));});return()=>{active=false;};},[attempt]);
 async function saveNickname(e) {
  e.preventDefault();if(busy)return;setError('');setNotice('');setBusy('nickname');
  try {await updateNickname(nickname.trim());setProfile(p=>({...p,nickname:nickname.trim()}));setNotice('닉네임을 변경했습니다.');}
  catch(e){setError(errorMessage(e));}finally{setBusy('');}
 }
 async function savePassword(e) {
  e.preventDefault();if(busy)return;setError('');setNotice('');
  if(values.newPassword!==values.confirmPassword){setError('새 비밀번호가 서로 다릅니다.');return;}
  if(new TextEncoder().encode(values.newPassword).length>72){setError('새 비밀번호는 UTF-8 기준 72바이트 이내로 입력해 주세요.');return;}
  setBusy('password');try{await client.post('/me/profile/password',values);clearSession();navigate('/login',{replace:true,state:{passwordChanged:true}});}
  catch(e){setError(errorMessage(e));}finally{setBusy('');}
 }
 return <main className="profile-page"><header><p>MY ACCOUNT</p><h1>내 정보 수정</h1><div>나의 계정 정보를 확인하고 변경하세요.</div></header>
  {error && <div className="profile-message profile-error" role="alert">{error}{!profile && <button onClick={()=>setAttempt(v=>v+1)}>다시 불러오기</button>}</div>}
  {notice && <div className="profile-message" role="status">{notice}</div>}
  {!profile ? !error && <p role="status">회원 정보를 불러오고 있어요.</p> : <div className="profile-grid">
   <section className="profile-card"><h2>로그인 정보</h2><dl><dt>이메일</dt><dd>{profile.email}</dd><dt>현재 로그인 방식</dt><dd>{providerName(profile.loginMethod)}</dd><dt>연결된 소셜 계정</dt><dd>{profile.socialProviders.length ? profile.socialProviders.map(providerName).join(', ') : '연결된 소셜 계정 없음'}</dd></dl></section>
   <section className="profile-card"><h2>닉네임 변경</h2><form onSubmit={saveNickname}><label htmlFor="profile-nickname">닉네임</label><input id="profile-nickname" value={nickname} onChange={e=>setNickname(e.target.value)} required minLength={2} maxLength={30} autoComplete="nickname" /><small>2~30자로 입력해 주세요.</small><button disabled={!!busy || nickname.trim()===profile.nickname}>{busy==='nickname'?'저장 중…':'닉네임 저장'}</button></form></section>
   <section className="profile-card profile-password-card"><h2>비밀번호 변경</h2><p>변경 후 모든 기기에서 다시 로그인해야 합니다.</p><form onSubmit={savePassword}>{[['currentPassword','현재 비밀번호'],['newPassword','새 비밀번호'],['confirmPassword','새 비밀번호 확인']].map(([key,label])=><div className="profile-field" key={key}><label htmlFor={key}>{label}</label><input id={key} type="password" required minLength={key==='currentPassword'?undefined:8} maxLength={72} autoComplete={key==='currentPassword'?'current-password':'new-password'} value={values[key]} onChange={e=>setValues(v=>({...v,[key]:e.target.value}))} /></div>)}<small>새 비밀번호는 8자 이상 입력해 주세요.</small><button disabled={!!busy}>{busy==='password'?'변경 중…':'비밀번호 변경'}</button></form></section>
  </div>}
 </main>;
}
