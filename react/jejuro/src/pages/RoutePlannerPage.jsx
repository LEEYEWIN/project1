import { useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { createRoute, fetchRoute, saveRoute } from '../api/routeApi.js';
import { fetchBookmarks } from '../api/bookmarkApi.js';
import { errorMessage } from '../api/client.js';
import DayTabs from '../components/route/DayTabs.jsx';
import DayEditor from '../components/route/DayEditor.jsx';
import BookmarkPicker from '../components/route/BookmarkPicker.jsx';
import DayRoutePreview from '../components/route/DayRoutePreview.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

/**
 * 5페이지: 찜한 관광지로 일차·방문 순서 짜기
 * - /routes/new 로 들어오면 빈 경로를 만들고 /routes/{id}/edit 로 바꾼다.
 * - plan[i] = i+1일차의 방문지 배열(배열 순서 = 방문 순서)
 * - 아래 "동선 미리보기"에서 저장 전 순서로 지도·이동시간을 확인한 뒤
 * - 저장: PUT /api/routes/{routeId}  { routeName, days: [{ dayNo, poiIds }] }
 */
export default function RoutePlannerPage() {
  const { travelId, routeId } = useParams();
  const navigate = useNavigate();
  const creating = useRef(false);

  const [route, setRoute] = useState(null);
  const [bookmarkPois, setBookmarkPois] = useState([]);
  const [plan, setPlan] = useState([]);
  const [routeName, setRouteName] = useState('');
  const [dayIndex, setDayIndex] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState(null);
  const [dirty, setDirty] = useState(false);

  // ① 새 경로면 만들고 주소 교체
  useEffect(() => {
    if (routeId || creating.current) return;
    creating.current = true;
    createRoute(travelId)
      .then(({ routeId: id }) => navigate(`/travels/${travelId}/routes/${id}/edit`, { replace: true }))
      .catch((e) => setError(errorMessage(e)));
  }, [routeId, travelId, navigate]);

  // ② 경로 상세 + 찜 목록 불러오기
  useEffect(() => {
    if (!routeId) return;
    Promise.all([fetchRoute(routeId), fetchBookmarks(travelId)])
      .then(([detail, bookmarks]) => {
        applyDetail(detail);
        setBookmarkPois(bookmarks.map((b) => b.poi));
      })
      .catch((e) => setError(errorMessage(e)));
  }, [routeId, travelId]);

  /** 서버 응답(days) → 화면 상태(plan) */
  const applyDetail = (detail) => {
    const next = Array.from({ length: detail.tripDays }, () => []);
    detail.days.forEach((d) => {
      next[d.dayNo - 1] = d.spots.map((s) => s.poi);
    });
    setRoute(detail);
    setPlan(next);
    setRouteName(detail.routeName ?? '');
    setDirty(false);
  };

  const updateDay = (index, spots) => {
    setPlan((prev) => prev.map((s, i) => (i === index ? spots : s)));
    setDirty(true);
  };

  /** 찜한 곳을 (day)일차의 (pos)번째 자리에 넣기. pos는 0부터 */
  const placeAt = (poi, day, pos) => {
    setPlan((prev) =>
      prev.map((spots, i) => {
        if (i !== day) return spots;
        const next = [...spots];
        next.splice(pos, 0, poi);
        return next;
      })
    );
    setDayIndex(day); // 넣은 일차 탭을 보여줌
    setDirty(true);
  };

  /** 배치한 곳 빼기 (어느 일차에 있든) */
  const removePoi = (poiId) => {
    setPlan((prev) => prev.map((spots) => spots.filter((p) => p.poiId !== poiId)));
    setDirty(true);
  };

  /** 현재 일차의 index번째 방문지를 다른 일차의 맨 뒤로 옮기기 */
  const moveToDay = (index, targetDay) => {
    if (targetDay === dayIndex) return;
    const poi = plan[dayIndex][index];
    setPlan((prev) =>
      prev.map((spots, i) => {
        if (i === dayIndex) return spots.filter((_, n) => n !== index);
        if (i === targetDay) return [...spots, poi];
        return spots;
      })
    );
    setDirty(true);
  };

  /** 저장. 성공하면 true */
  const save = async () => {
    setSaving(true);
    setError('');
    try {
      const detail = await saveRoute(routeId, {
        routeName: routeName.trim(),
        days: plan.map((spots, i) => ({ dayNo: i + 1, poiIds: spots.map((p) => p.poiId) })),
      });
      applyDetail(detail);
      setSavedAt(new Date());
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    } finally {
      setSaving(false);
    }
  };

  /** 저장 후 여행 상세(7페이지)로 이동 → 거기서 경로를 골라 지도 보기·최종 채택 */
  const saveAndExit = async () => {
    if (dirty && !(await save())) return; // 저장 실패하면 이동하지 않고 오류를 보여줌
    navigate(`/travels/${travelId}`);
  };

  /** 지도 보기: 저장 안 된 변경이 있으면 먼저 저장하고 이동 */
  const openMap = async () => {
    if (dirty && !(await save())) return;
    navigate(`/travels/${travelId}/routes/${routeId}/map`);
  };

  if (error && !route) return <main className="page"><ErrorBox message={error} /></main>;
  if (!route) return <main className="page"><Loading text="경로를 준비하는 중…" /></main>;
  // 최종 경로가 채택된 여행은 수정 불가 → 지도(읽기 전용)로 보낸다
  if (route.locked) return <Navigate to={`/travels/${travelId}/routes/${route.routeId}/map`} replace />;

  const totalSpots = plan.reduce((sum, s) => sum + s.length, 0);

  return (
    <main className="page wide">
      <div className="title-row">
        <input
          className="title-input"
          value={routeName}
          maxLength={100}
          placeholder="경로 이름 (예: 동부 2박3일)"
          onChange={(e) => {
            setRouteName(e.target.value);
            setDirty(true);
          }}
        />
        <Link className="btn ghost" to={`/travels/${travelId}/bookmarks`}>
          ← 찜 목록
        </Link>
      </div>

      <DayTabs
        startDate={route.startDate}
        tripDays={route.tripDays}
        current={dayIndex}
        onChange={setDayIndex}
        counts={plan.map((s) => s.length)}
      />

      <div className="planner">
        <aside className="card">
          <h2>찜한 관광지</h2>
          <p className="hint small">일차와 순번을 고르고 "넣기"를 누르세요.</p>
          <BookmarkPicker
            pois={bookmarkPois}
            plan={plan}
            currentDay={dayIndex + 1}
            onPlace={placeAt}
            onRemove={removePoi}
          />
        </aside>
        <section className="card">
          <h2>{dayIndex + 1}일차 방문 순서</h2>
          <DayEditor
            dayNo={dayIndex + 1}
            tripDays={route.tripDays}
            spots={plan[dayIndex]}
            onChange={(s) => updateDay(dayIndex, s)}
            onMoveToDay={moveToDay}
          />
        </section>
      </div>

      {/* 저장 전에 지도·이동시간 확인 */}
      <DayRoutePreview
        dayNo={dayIndex + 1}
        spots={plan[dayIndex]}
        onReorder={(s) => updateDay(dayIndex, s)}
      />

      <ErrorBox message={error} />

      <div className="bottom-bar">
        <span>
          총 {totalSpots}곳 {dirty ? '· 저장 안 됨' : savedAt ? '· 저장됨' : ''}
        </span>
        <div className="actions">
          <button
            type="button"
            className="btn ghost"
            disabled={saving || totalSpots === 0}
            title={totalSpots === 0 ? '방문지를 먼저 넣으세요' : undefined}
            onClick={openMap}
          >
            {dirty ? '저장하고 지도 크게 보기' : '지도 크게 보기'}
          </button>
          <button type="button" className="btn primary" onClick={saveAndExit} disabled={saving}>
            {saving ? '저장 중…' : dirty ? '저장하고 여행으로' : '여행으로 돌아가기'}
          </button>
        </div>
      </div>
    </main>
  );
}