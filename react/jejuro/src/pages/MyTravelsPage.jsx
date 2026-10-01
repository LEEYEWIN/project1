import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchMyTravels } from '../api/travelApi.js';
import { fetchMe } from '../api/userApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate } from '../utils/format.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import TravelCalendar from '../components/travel/TravelCalendar.jsx';
import MySidePanel from '../components/travel/MySidePanel.jsx';
import JejuTravelBadge, { JEJU_BADGES } from '../components/travel/JejuTravelBadge.jsx';

const PAGE_SIZE = 3;
const BADGE_STORAGE_KEY = 'jejuro:travel-badges:v1';

function assignBadges(travels) {
  let saved = {};
  try {
    saved = JSON.parse(localStorage.getItem(BADGE_STORAGE_KEY) || '{}') || {};
  } catch {
    saved = {};
  }
  const assigned = {};
  for (let offset = 0; offset < travels.length; offset += JEJU_BADGES.length) {
    const group = travels.slice(offset, offset + JEJU_BADGES.length);
    const used = new Set();
    group.forEach(({ travelId }) => {
      const badge = saved[travelId];
      if (Number.isInteger(badge) && badge >= 0 && badge < JEJU_BADGES.length && !used.has(badge)) {
        assigned[travelId] = badge;
        used.add(badge);
      }
    });
    group.forEach(({ travelId }) => {
      if (assigned[travelId] !== undefined) return;
      const available = JEJU_BADGES.map((_, index) => index).filter((index) => !used.has(index));
      const badge = available[Math.floor(Math.random() * available.length)];
      assigned[travelId] = badge;
      used.add(badge);
    });
  }
  try {
    localStorage.setItem(BADGE_STORAGE_KEY, JSON.stringify({ ...saved, ...assigned }));
  } catch {
    // Storage may be disabled; the current page still keeps its assignments.
  }
  return assigned;
}
/** 오늘(내 PC 시간 기준)부터 dateStr('2026-09-24')까지 남은 날 수 */
function daysUntil(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const target = new Date(y, m - 1, d);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((target - today) / 86400000);
}

/**
 * 왼쪽 상태 뱃지
 * - 여행 전 + 일정 확정 → 여행 D-N
 * - 여행 전 + 아직 확정 안 함 → 여행 계획 중
 * - 여행 기간 중            → 여행 중
 * - 여행이 끝났거나 후기를 남김 → 뱃지 없음(오른쪽 태그만)
 */
function phaseBadge(t) {
  if (t.phase === 'AFTER' || t.hasFeedback) return null;
  if (t.phase === 'DURING') return { cls: 'during', text: '여행 중' };
  if (t.adoptedRouteId) return { cls: 'dday', text: `여행 D-${daysUntil(t.startDate)}` };
  return { cls: 'before', text: '여행 계획 중' };
}

/**
 * 7페이지(목록): 내 여행
 * - 위: 달력(여행 기간·날짜별 일정) + 오른쪽 상자(프로필·다가오는 여행·할 일)
 * - 아래: 여행 카드 목록
 */
export default function MyTravelsPage() {
  const [travels, setTravels] = useState(null);
  const [me, setMe] = useState(null);
  const [error, setError] = useState('');
  const [page, setPage] = useState(1);
  const [badgeAssignments, setBadgeAssignments] = useState({});

  useEffect(() => {
    fetchMyTravels()
      .then((data) => {
        setBadgeAssignments(assignBadges(data));
        setTravels(data);
      })
      .catch((e) => setError(errorMessage(e)));
    fetchMe()
      .then(setMe)
      .catch(() => setMe(null)); // 닉네임을 못 불러와도 화면은 보여 줌
  }, []);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!travels) return <main className="page"><Loading /></main>;

  const pageCount = Math.ceil(travels.length / PAGE_SIZE);
  const currentPage = Math.min(page, Math.max(pageCount, 1));
  const visibleTravels = travels.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  return (
    <main className="page wide my-travels-list-page">
      <div className="title-row">
        <h1>내 여행</h1>
      </div>

      <div className="my-top">
        <TravelCalendar travels={travels} />
        <MySidePanel me={me} travels={travels} />
      </div>

      <section className="my-travel-results" aria-labelledby="my-travel-results-title">
        <div className="my-travel-results-heading">
          <h2 id="my-travel-results-title" className="list-title">전체 여행 {travels.length}개</h2>
        </div>
        {travels.length === 0 && <p className="empty">아직 만든 여행이 없습니다.</p>}

        <div className="travel-list">
          {visibleTravels.map((t) => {
          const b = phaseBadge(t);
          return (
            <Link key={t.travelId} to={`/travels/${t.travelId}`} className="travel-item">
              <div className="travel-item-main">
                <JejuTravelBadge index={badgeAssignments[t.travelId] ?? 0} />
                <div className="travel-item-copy">
                  <h2>{t.travelName}</h2>
                  <p className="muted">
                    {formatDate(t.startDate)} ~ {formatDate(t.endDate)} · {t.tripDays}일 · {t.regionNames.join(', ')}
                    {t.companionCount > 0 && ` · 동반 ${t.companionCount}명`}
                  </p>
                </div>
              </div>
              <div className="travel-status">
                {b && <span className={`phase ${b.cls}`}>{b.text}</span>}
                {t.imported && <span className="tag">가져온 경로</span>}
                {t.adoptedRouteId ? (
                  <span className="tag on">일정 확정</span>
                ) : (
                  <span className="tag">
                    {t.placeCount === 0 ? '장소 고르는 중' : `배치 ${t.spotCount}/${t.placeCount}곳`}
                  </span>
                )}
                {t.hasFeedback && <span className="tag on">후기 작성</span>}
              </div>
            </Link>
          );
          })}
        </div>

        {pageCount > 1 && (
          <nav className="travel-pagination" aria-label="여행 목록 페이지">
            <button type="button" onClick={() => setPage(currentPage - 1)} disabled={currentPage === 1}>
              이전
            </button>
            {Array.from({ length: pageCount }, (_, index) => index + 1).map((pageNumber) => (
              <button
                key={pageNumber}
                type="button"
                className={pageNumber === currentPage ? 'on' : ''}
                aria-current={pageNumber === currentPage ? 'page' : undefined}
                aria-label={`${pageNumber}페이지`}
                onClick={() => setPage(pageNumber)}
              >
                {pageNumber}
              </button>
            ))}
            <button type="button" onClick={() => setPage(currentPage + 1)} disabled={currentPage === pageCount}>
              다음
            </button>
          </nav>
        )}
      </section>
    </main>
  );
}
