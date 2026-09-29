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

  useEffect(() => {
    fetchMyTravels()
      .then(setTravels)
      .catch((e) => setError(errorMessage(e)));
    fetchMe()
      .then(setMe)
      .catch(() => setMe(null)); // 닉네임을 못 불러와도 화면은 보여 줌
  }, []);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!travels) return <main className="page"><Loading /></main>;

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>내 여행</h1>
      </div>

      <div className="my-top">
        <TravelCalendar travels={travels} />
        <MySidePanel me={me} travels={travels} />
      </div>

      <h2 className="list-title">전체 여행 {travels.length}개</h2>
      {travels.length === 0 && <p className="empty">아직 만든 여행이 없습니다.</p>}

      <div className="travel-list">
        {travels.map((t) => {
          const b = phaseBadge(t);
          return (
            <Link key={t.travelId} to={`/travels/${t.travelId}`} className="travel-item">
              <div>
                {b && <span className={`phase ${b.cls}`}>{b.text}</span>}
                <h2>{t.travelName}</h2>
                <p className="muted">
                  {formatDate(t.startDate)} ~ {formatDate(t.endDate)} · {t.tripDays}일 · {t.regionNames.join(', ')}
                  {t.companionCount > 0 && ` · 동반 ${t.companionCount}명`}
                </p>
              </div>
              <div className="travel-status">
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
    </main>
  );
}