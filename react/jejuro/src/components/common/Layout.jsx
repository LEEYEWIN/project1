import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { fetchMe } from '../../api/userApi.js';
import TestUserSwitcher from './TestUserSwitcher.jsx';

/** 모든 페이지 공통 상단 메뉴. <Outlet />자리에 각 페이지가 그려진다. 관리자 계정이면 [관리자] 메뉴가 보인다. */
export default function Layout() {
  const [admin, setAdmin] = useState(false);

  useEffect(() => {
    fetchMe()
      .then((me) => setAdmin(me.role === 'ADMIN'))
      .catch(() => setAdmin(false));
  }, []);

  return (
    <>
      <header className="topbar">
        <NavLink to="/travels" className="logo">
          제주 여행
        </NavLink>
        <nav>
          <NavLink to="/travels" end>
            내 여행
          </NavLink>
          <NavLink to="/travels/new">새 여행</NavLink>
          <NavLink to="/pois">관광지</NavLink>
          <NavLink to="/community">후기 게시판</NavLink>
          {admin && <NavLink to="/admin/kpi">관리자</NavLink>}
          <TestUserSwitcher />
        </nav>
      </header>
      <Outlet />
    </>
  );
}