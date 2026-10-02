import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import client, { errorMessage } from '../api/client.js';
import { SOCIAL_PROVIDERS, getSocialProviders, socialErrorMessage, startSocialLink, unlinkSocial } from '../api/authApi.js';
import useNicknameCheck from '../hooks/useNicknameCheck.js';
import '../styles/profile.css';
const providerName = code => ({ GOOGLE: 'Google', KAKAO: '카카오', NAVER: '네이버', EMAIL: '이메일·비밀번호', UNKNOWN: '확인할 수 없음' }[code] || code);

function SocialProviderIcon({ code }) {
 if (code === 'NAVER') return <svg className="profile-social-icon" viewBox="0 0 24 24" aria-hidden="true"><rect width="24" height="24" rx="5" fill="#03c75a" /><path d="M6 5h4l4.2 6V5H18v14h-4l-4.2-6V19H6Z" fill="#fff" /></svg>;
 if (code === 'KAKAO') return <svg className="profile-social-icon" viewBox="0 0 24 24" aria-hidden="true"><rect width="24" height="24" rx="5" fill="#fee500" /><path d="M12 5.2c-5 0-8.4 3.1-8.4 6.6 0 2.4 1.5 4.4 3.9 5.5l-.8 3.1 3.8-2.4c.5.1 1 .1 1.5.1 5 0 8.4-2.9 8.4-6.3S17 5.2 12 5.2Z" fill="#2d211e" /><text x="12" y="13.3" fill="#fee500" fontSize="5" fontWeight="800" textAnchor="middle">TALK</text></svg>;
 return <svg className="profile-social-icon" viewBox="0 0 24 24" aria-hidden="true"><rect width="24" height="24" rx="5" fill="#fff" stroke="#e5dfd8" /><path d="M20.4 12.2c0-.6-.1-1.1-.2-1.6H12v3.2h4.7c-.2 1.2-.9 2.2-2 2.9v2.6h3.3c1.9-1.8 3-4.3 3-7.1Z" fill="#4285f4" /><path d="M12 21c2.7 0 5-1 6.6-2.6l-3.3-2.6c-.9.6-2 .9-3.3.9-2.5 0-4.6-1.7-5.4-4H3.2v2.7A9 9 0 0 0 12 21Z" fill="#34a853" /><path d="M6.6 12.7a5.8 5.8 0 0 1 0-3.4V6.6H3.2a9 9 0 0 0 0 8.8Z" fill="#fbbc05" /><path d="M12 7.3c1.4 0 2.6.5 3.5 1.4l2.9-2.9A8.7 8.7 0 0 0 12 3a9 9 0 0 0-8.8 6.3l3.4 2.7c.8-2.8 2.9-4.7 5.4-4.7Z" fill="#ea4335" /></svg>;
}

export default function ProfilePage() {
 const { updateNickname, clearSession } = useAuth();
 const navigate = useNavigate();
 const location = useLocation();
 const [enabledSocial,setEnabledSocial] = useState([]);
 const [profile,setProfile] = useState(null);
 const [nickname,setNickname] = useState('');
 const [values,setValues] = useState({currentPassword:'',newPassword:'',confirmPassword:''});
 const [error,setError] = useState('');
 const [notice,setNotice] = useState('');
 const [warning,setWarning] = useState('');
 const [busy,setBusy] = useState('');
 const [attempt,setAttempt] = useState(0);
 const nickCheck = useNicknameCheck(nickname, !profile || nickname.trim()===profile.nickname);
 // 소셜 연동 후 서버가 ?social=linked 또는 ?socialError= 로 돌려보낸 결과를 한 번 보여 주고 주소를 정리한다.
 useEffect(() => {
  const params=new URLSearchParams(location.search);
  if(params.get('social')==='linked'){setNotice(`${providerName(params.get('provider'))} 계정을 연동했습니다. 이제 로그인 화면에서 바로 로그인할 수 있어요.`);}
  else if(params.get('socialError')){setWarning(socialErrorMessage(params.get('socialError'),params.get('provider')));}
  if(location.search) navigate(location.pathname,{replace:true});
 },[location.search,location.pathname,navigate]);
 // 카카오 화면에서 뒤로가기로 돌아오면 브라우저가 이전 화면을 그대로 복원하므로 '이동 중' 상태를 풀어 준다.
 useEffect(() => { const reset=()=>setBusy(b=>b.startsWith('social')?'':b);window.addEventListener('pageshow',reset);return()=>window.removeEventListener('pageshow',reset);},[]);
 useEffect(() => { let active=true;getSocialProviders().then(list=>{if(active)setEnabledSocial(list);}).catch(()=>{if(active)setEnabledSocial([]);});return()=>{active=false;};},[]);
 async function linkSocial(code) {
  if(busy)return;setError('');setNotice('');setWarning('');setBusy(`social:${code}`);
  try{await startSocialLink(code);}catch(e){setError(errorMessage(e));setBusy('');}
 }
 async function removeSocial(code) {
  if(busy || !window.confirm(`${providerName(code)} 계정 연동을 해제할까요? 해제해도 이메일로 로그인할 수 있어요.`))return;
  setError('');setNotice('');setWarning('');setBusy('social');
  try{await unlinkSocial(code);setProfile(p=>({...p,socialProviders:p.socialProviders.filter(v=>v!==code)}));setNotice(`${providerName(code)} 계정 연동을 해제했습니다.`);}
  catch(e){setError(errorMessage(e));}finally{setBusy('');}
 }
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
 return <main className="profile-page">
  <header><p>MY ACCOUNT</p><h1>내 정보 수정</h1><div>나의 계정 정보를 확인하고 변경하세요.</div></header>
  {error && <div className="profile-message profile-error" role="alert">{error}{!profile && <button onClick={()=>setAttempt(v=>v+1)}>다시 불러오기</button>}</div>}
  {warning && <div className="profile-message profile-warning" role="alert"><strong>⚠ 연동하지 못했어요</strong><span>{warning}</span><button type="button" onClick={()=>setWarning('')} aria-label="안내 닫기">닫기</button></div>}
  {notice && <div className="profile-message" role="status">{notice}</div>}
  {!profile ? !error && <p role="status">회원 정보를 불러오고 있어요.</p> : <div className="profile-grid">
   <div className="profile-column">
    <section className="profile-card profile-login-card"><h2>로그인 정보</h2><dl><dt>이메일</dt><dd>{profile.email}</dd><dt>현재 로그인 방식</dt><dd>{providerName(profile.loginMethod)}{['KAKAO','NAVER','GOOGLE'].includes(profile.loginMethod) && !profile.socialProviders.includes(profile.loginMethod) && <small className="profile-login-note"> (연동 해제됨 · 다음 로그인부터는 이메일로 로그인해 주세요)</small>}</dd><dt>연동된 소셜 계정</dt><dd>{profile.socialProviders.length ? profile.socialProviders.map(providerName).join(', ') : '연동된 소셜 계정 없음'}</dd></dl></section>
    <section className="profile-card profile-nickname-card"><h2>닉네임 변경</h2><form onSubmit={saveNickname}><label htmlFor="profile-nickname">닉네임</label><input id="profile-nickname" value={nickname} onChange={e=>setNickname(e.target.value)} required minLength={2} maxLength={30} autoComplete="nickname" />{nickCheck.message ? <small className={nickCheck.status==='taken'?'profile-nickname-taken':'profile-nickname-ok'} role={nickCheck.status==='taken'?'alert':'status'}>{nickCheck.message}</small> : <small>2~30자로 입력해 주세요.</small>}<button disabled={!!busy || nickname.trim()===profile.nickname || nickCheck.status==='taken'}>{busy==='nickname'?'저장 중…':'닉네임 저장'}</button></form></section>
   </div>
   <div className="profile-column">
    <section className="profile-card profile-social-card"><h2>소셜 계정 연동</h2><p>연동하면 로그인 화면에서 소셜 계정으로 바로 로그인할 수 있어요. 소셜 계정으로 새로 가입할 수는 없어요.</p>
     {enabledSocial.length===0 ? <p className="profile-social-empty">아직 사용할 수 있는 소셜 로그인이 없어요.</p> :
     <ul className="profile-social-list">{SOCIAL_PROVIDERS.filter(p=>enabledSocial.includes(p.code)).map(p=>{const linked=profile.socialProviders.includes(p.code);return <li key={p.code}><span className={`profile-social-name profile-social-${p.code.toLowerCase()}`}><SocialProviderIcon code={p.code} />{p.name}</span><span className={linked?'profile-social-on':'profile-social-off'}>{linked?'연동됨':'연동 안 됨'}</span>{linked
       ? <button type="button" className="profile-social-unlink" disabled={!!busy} onClick={()=>removeSocial(p.code)}>연동 해제</button>
       : <button type="button" disabled={!!busy} onClick={()=>linkSocial(p.code)}>{busy===`social:${p.code}`?'이동 중…':'연동하기'}</button>}</li>;})}</ul>}
    </section>
    <section className="profile-card profile-password-card"><h2>비밀번호 변경</h2><p>변경 후 모든 기기에서 다시 로그인해야 합니다.</p><form onSubmit={savePassword}>{[['currentPassword','현재 비밀번호'],['newPassword','새 비밀번호'],['confirmPassword','새 비밀번호 확인']].map(([key,label])=><div className="profile-field" key={key}><label htmlFor={key}>{label}</label><input id={key} type="password" required minLength={key==='currentPassword'?undefined:8} maxLength={72} autoComplete={key==='currentPassword'?'current-password':'new-password'} value={values[key]} onChange={e=>setValues(v=>({...v,[key]:e.target.value}))} />{key==='confirmPassword' && values.confirmPassword && (values.newPassword===values.confirmPassword ? <small className="profile-match-ok" role="status">비밀번호가 일치합니다.</small> : <small className="profile-match-bad" role="alert">비밀번호가 일치하지 않습니다.</small>)}</div>)}<small>새 비밀번호는 8자 이상 입력해 주세요.</small><button disabled={!!busy}>{busy==='password'?'변경 중…':'비밀번호 변경'}</button></form></section>
   </div>
  </div>}
 </main>;
}