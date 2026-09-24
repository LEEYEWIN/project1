import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchMyTravels } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate } from '../utils/format.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

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
 * - 여행 전 + 최종 경로 채택 → 여행 D-N
 * - 여행 전 + 아직 채택 안 함 → 여행 계획 중
 * - 여행 기간 중            → 여행 중
 * - 여행이 끝났거나 후기를 남김 → 뱃지 없음(오른쪽 태그만)
 */
function phaseBadge(t) {
  if (t.phase === 'AFTER' || t.hasFeedback) return null;
  if (t.phase === 'DURING') return { cls: 'during', text: '여행 중' };
  if (t.adoptedRouteId) return { cls: 'dday', text: `여행 D-${daysUntil(t.startDate)}` };
  return { cls: 'before', text: '여행 계획 중' };
}

/** 7페이지(목록): 내 여행 기록 */
export default function MyTravelsPage() {
  const [travels, setTravels] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchMyTravels()
      .then(setTravels)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <main className="page"><ErrorBox message={error} /></main>;
  if (!travels) return <main className="page"><Loading /></main>;

  return (
    <main className="page">
      <div className="title-row">
        <h1>내 여행</h1>
        <Link className="btn primary" to="/travels/new">
          + 새 여행
        </Link>
      </div>

      {travels.length === 0 && <p className="empty">아직 만든 여행이 없습니다.</p>}

      <div className="travel-list">
        {travels.map((t) => (
          <Link key={t.travelId} to={`/travels/${t.travelId}`} className="travel-item">
            <div>
              {(() => {
                const b = phaseBadge(t);
                return b && <span className={`phase ${b.cls}`}>{b.text}</span>;
              })()}
              <h2>{t.travelName}</h2>
              <p className="muted">
                {formatDate(t.startDate)} ~ {formatDate(t.endDate)} · {t.tripDays}일 · {t.regionNames.join(', ')}
                {t.companionCount > 0 && ` · 동반 ${t.companionCount}명`}
              </p>
            </div>
            <div className="travel-status">
              {t.adoptedRouteId ? <span className="tag on">최종 경로 확정</span> : <span className="tag">경로 {t.routeCount}개</span>}
              {t.hasFeedback && <span className="tag on">후기 작성</span>}
            </div>
          </Link>
        ))}
      </div>
    </main>
  );
}