import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { fetchReports } from '../../api/adminApi.js';
import TestUserSwitcher from '../common/TestUserSwitcher.jsx';
import '../../styles/admin.css';

/**
 * 관리자 화면 공통 틀: 왼쪽 메뉴 + 오른쪽 내용 (<Outlet />)
 * 게시글·신고 옆 숫자 = 처리 대기 중인 신고 대상 수 (화면을 옮길 때마다 다시 셈)
 */
export default function AdminLayout() {
  const { pathname } = useLocation();
  const [pending, setPending] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetchReports({ status: 'PENDING', page: 0 })
      .then((d) => !cancelled && setPending(d.pendingCount))
      .catch(() => {}); // 관리자가 아니면 403 → 숫자 없이
    return () => {
      cancelled = true;
    };
  }, [pathname]);

  return (
    <div className="adm">
      <nav className="adm-nav" aria-label="관리자 메뉴">
        <div className="adm-logo">
          제주로 <span>ADMIN</span>
        </div>
        <NavLink to="/admin/kpi">AI 추천 KPI</NavLink>
        <NavLink to="/admin/reports">
          게시글·신고 {pending > 0 && <span className="adm-nav-count num">{pending}</span>}
        </NavLink>
        <NavLink to="/admin/users">회원 관리</NavLink>
        <NavLink to="/admin/pois">관광지 데이터</NavLink>
        <div className="adm-user">
          <TestUserSwitcher />
        </div>
        <NavLink to="/travels" className="adm-back">
          ← 서비스 화면으로
        </NavLink>
      </nav>
      <main className="adm-main">
        <Outlet />
      </main>
    </div>
  );
}