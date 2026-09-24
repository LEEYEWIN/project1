import { NavLink, Outlet } from 'react-router-dom';
import TestUserSwitcher from './TestUserSwitcher.jsx';

/** 모든 페이지 공통 상단 메뉴. <Outlet />자리에 각 페이지가 그려진다. */
export default function Layout() {
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
          <NavLink to="/community">후기 게시판</NavLink>
          <TestUserSwitcher />
        </nav>
      </header>
      <Outlet />
    </>
  );
}