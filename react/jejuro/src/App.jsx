import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/common/Layout.jsx';
import TravelCreatePage from './pages/TravelCreatePage.jsx';
import RecommendingPage from './pages/RecommendingPage.jsx';
import RecommendationListPage from './pages/RecommendationListPage.jsx';
import BookmarkPage from './pages/BookmarkPage.jsx';
import PoiBrowsePage from './pages/PoiBrowsePage.jsx';
import RoutePlannerPage from './pages/RoutePlannerPage.jsx';
import RouteMapPage from './pages/RouteMapPage.jsx';
import MyTravelsPage from './pages/MyTravelsPage.jsx';
import TravelDetailPage from './pages/TravelDetailPage.jsx';
import FeedbackPage from './pages/FeedbackPage.jsx';
import CommunityPage from './pages/CommunityPage.jsx';

/**
 * 화면 주소(URL) 정리
 *  1 /travels/new                                   여행 만들기 + 설문
 *  2 /travels/:travelId/recommending                AI 추천 중
 *  3 /travels/:travelId/recommendations             추천 관광지 목록 (+ 찜 버튼)
 *  3-1 /travels/:travelId/pois                      전체 관광지 목록 (+ 찜 버튼)
 *  4 /travels/:travelId/bookmarks                   찜 목록
 *  5 /travels/:travelId/routes/new                  새 경로 만들기 → 편집으로 이동
 *    /travels/:travelId/routes/:routeId/edit        일차·방문 순서 편집
 *  6 /travels/:travelId/routes/:routeId/map         카카오맵 동선·이동시간
 *  7 /travels, /travels/:travelId                   내 여행 목록, 여행 상세(최종 경로 채택)
 *  8 /travels/:travelId/feedback                    다녀온 후 후기
 */
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/travels" replace />} />
        <Route path="/travels" element={<MyTravelsPage />} />
        <Route path="/travels/new" element={<TravelCreatePage />} />
        <Route path="/travels/:travelId" element={<TravelDetailPage />} />
        <Route path="/travels/:travelId/recommending" element={<RecommendingPage />} />
        <Route path="/travels/:travelId/recommendations" element={<RecommendationListPage />} />
        <Route path="/travels/:travelId/pois" element={<PoiBrowsePage />} />
        <Route path="/travels/:travelId/bookmarks" element={<BookmarkPage />} />
        <Route path="/travels/:travelId/routes/new" element={<RoutePlannerPage />} />
        <Route path="/travels/:travelId/routes/:routeId/edit" element={<RoutePlannerPage />} />
        <Route path="/travels/:travelId/routes/:routeId/map" element={<RouteMapPage />} />
        <Route path="/travels/:travelId/feedback" element={<FeedbackPage />} />
        <Route path="/community" element={<CommunityPage />} />
        <Route path="*" element={<p className="page">페이지를 찾을 수 없습니다.</p>} />
      </Route>
    </Routes>
  );
}