import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import Layout from './components/common/Layout.jsx';
import AuthPage from './pages/AuthPage.jsx';
import HomePage from './pages/HomePage.jsx';
import WithdrawalPage from './pages/WithdrawalPage.jsx';
import { RequireAuth } from './auth/AuthContext.jsx';
import TravelCreatePage from './pages/TravelCreatePage.jsx';
import RecommendingPage from './pages/RecommendingPage.jsx';
import RecommendationListPage from './pages/RecommendationListPage.jsx';
import BookmarkPage from './pages/BookmarkPage.jsx';
import PoiListPage from './pages/PoiListPage.jsx';
import PoiDetailPage from './pages/PoiDetailPage.jsx';
import RoutePlannerPage from './pages/RoutePlannerPage.jsx';
import RouteMapPage from './pages/RouteMapPage.jsx';
import MyTravelsPage from './pages/MyTravelsPage.jsx';
import TravelDetailPage from './pages/TravelDetailPage.jsx';
import FeedbackPage from './pages/FeedbackPage.jsx';
import CommunityPage from './pages/CommunityPage.jsx';
import PostDetailPage from './pages/PostDetailPage.jsx';
import PostWritePage from './pages/PostWritePage.jsx';
import AdminLayout from './components/admin/AdminLayout.jsx';
import AdminKpiPage from './pages/admin/AdminKpiPage.jsx';
import AdminReportsPage from './pages/admin/AdminReportsPage.jsx';
import AdminPoisPage from './pages/admin/AdminPoisPage.jsx';
import DislikesPage from './pages/DislikesPage.jsx';

/**
 * 화면 주소(URL) 정리
 *  1 /travels/new                                   여행 만들기 + 설문
 *  2 /travels/:travelId/recommending                AI 추천 중
 *  3 /travels/:travelId/recommendations             추천 관광지 목록 (+ 추천 기준, [+ 장소 추가])
 *  3-1 /pois, /pois/:poiId                          관광지 목록·검색·상세 (둘러보기, 장소 추가 없음)
 *      /travels/:travelId/pois(/:poiId)              같은 화면 + [+ 장소 추가]·[루트에 추가]
 *  4 /travels/:travelId/bookmarks                   여행 장소 (예전 이름: 찜 목록)
 *  5 /travels/:travelId/route                       경로 짜기 (여행당 1개, 자동 저장, 숙소·순서 추천)
 *    (예전 주소 /routes/new, /routes/:routeId/edit 는 여기로 이동)
 *  6 /travels/:travelId/routes/:routeId/map         카카오맵 동선·이동시간·주변 숙소 (보기 전용)
 *  7 /travels, /travels/:travelId                   내 여행(달력+목록), 여행 상세(일정 확정)
 *  8 /travels/:travelId/feedback                    다녀온 후 후기
 *  9 /community                                     커뮤니티 목록 (?type=REVIEW|QUESTION&sort=latest|likes&page=0)
 *    /community/posts/new?type=REVIEW              글쓰기
 *    /community/posts/:postId                       글 상세 (좋아요·댓글·대댓글, 경로 공유·가져오기)
 *    /community/posts/:postId/edit                  글 수정
 * 관리자 (USER.role = ADMIN)
 *    /admin/kpi                                     AI 추천 KPI 대시보드
 *    /admin/reports                                 게시글·댓글 신고 처리
 *    /admin/pois                                    관광지 데이터 관리
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage key="login" />} />
      <Route path="/signup" element={<AuthPage key="signup" signup />} />
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route element={<RequireAuth />}>
        <Route path="/account/withdraw" element={<WithdrawalPage />} />
        <Route path="/travels" element={<MyTravelsPage />} />
        <Route path="/travels/new" element={<TravelCreatePage />} />
        <Route path="/travels/:travelId" element={<TravelDetailPage />} />
        <Route path="/travels/:travelId/recommending" element={<RecommendingPage />} />
        <Route path="/travels/:travelId/recommendations" element={<RecommendationListPage />} />
        </Route>
        <Route path="/pois" element={<PoiListPage />} />
        <Route path="/pois/:poiId" element={<PoiDetailPage />} />
        <Route element={<RequireAuth />}>
        <Route path="/travels/:travelId/pois" element={<PoiListPage />} />
        <Route path="/travels/:travelId/pois/:poiId" element={<PoiDetailPage />} />
        <Route path="/travels/:travelId/bookmarks" element={<BookmarkPage />} />
        <Route path="/travels/:travelId/route" element={<RoutePlannerPage />} />
        <Route path="/travels/:travelId/routes/new" element={<ToRoutePlanner />} />
        <Route path="/travels/:travelId/routes/:routeId/edit" element={<ToRoutePlanner />} />
        <Route path="/travels/:travelId/routes/:routeId/map" element={<RouteMapPage />} />
        <Route path="/travels/:travelId/feedback" element={<FeedbackPage />} />
        <Route path="/dislikes" element={<DislikesPage />} />
        <Route path="/community" element={<CommunityPage />} />
        <Route path="/community/posts/new" element={<PostWritePage />} />
        <Route path="/community/posts/:postId" element={<PostDetailPage />} />
        <Route path="/community/posts/:postId/edit" element={<PostWritePage />} />
        </Route>
        <Route path="*" element={<p className="page">페이지를 찾을 수 없습니다.</p>} />
      </Route>
      <Route element={<RequireAuth admin />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/kpi" replace />} />
          <Route path="kpi" element={<AdminKpiPage />} />
          <Route path="reports" element={<AdminReportsPage />} />
          <Route path="pois" element={<AdminPoisPage />} />
        </Route>
      </Route>
    </Routes>
  );
}

/** 예전 경로 편집 주소 → 여행당 경로 1개 화면으로 */
function ToRoutePlanner() {
  const { travelId } = useParams();
  return <Navigate to={`/travels/${travelId}/route`} replace />;
}
