import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/common/Layout.jsx';
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

/**
 * 화면 주소(URL) 정리
 *  1 /travels/new                                   여행 만들기 + 설문
 *  2 /travels/:travelId/recommending                AI 추천 중
 *  3 /travels/:travelId/recommendations             추천 관광지 목록 (+ 찜 버튼)
 *  3-1 /pois, /pois/:poiId                          관광지 목록·검색·상세 (둘러보기, 찜 없음)
 *      /travels/:travelId/pois(/:poiId)              같은 화면 + 찜·루트에 추가 (찜 목록 → 전체 관광지 보기)
 *  4 /travels/:travelId/bookmarks                   찜 목록
 *  5 /travels/:travelId/routes/new                  새 경로 만들기 → 편집으로 이동
 *    /travels/:travelId/routes/:routeId/edit        일차·방문 순서 편집
 *  6 /travels/:travelId/routes/:routeId/map         카카오맵 동선·이동시간
 *  7 /travels, /travels/:travelId                   내 여행 목록, 여행 상세(최종 경로 채택)
 *  8 /travels/:travelId/feedback                    다녀온 후 후기
 *  9 /community                                     커뮤니티 목록 (?type=REVIEW|QUESTION&sort=latest|likes&page=0)
 *    /community/posts/new?type=REVIEW              글쓰기
 *    /community/posts/:postId                       글 상세 (좋아요·댓글·대댓글)
 *    /community/posts/:postId/edit                  글 수정
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
        <Route path="/pois" element={<PoiListPage />} />
        <Route path="/pois/:poiId" element={<PoiDetailPage />} />
        <Route path="/travels/:travelId/pois" element={<PoiListPage />} />
        <Route path="/travels/:travelId/pois/:poiId" element={<PoiDetailPage />} />
        <Route path="/travels/:travelId/bookmarks" element={<BookmarkPage />} />
        <Route path="/travels/:travelId/routes/new" element={<RoutePlannerPage />} />
        <Route path="/travels/:travelId/routes/:routeId/edit" element={<RoutePlannerPage />} />
        <Route path="/travels/:travelId/routes/:routeId/map" element={<RouteMapPage />} />
        <Route path="/travels/:travelId/feedback" element={<FeedbackPage />} />
        <Route path="/community" element={<CommunityPage />} />
        <Route path="/community/posts/new" element={<PostWritePage />} />
        <Route path="/community/posts/:postId" element={<PostDetailPage />} />
        <Route path="/community/posts/:postId/edit" element={<PostWritePage />} />
        <Route path="*" element={<p className="page">페이지를 찾을 수 없습니다.</p>} />
      </Route>
    </Routes>
  );
}