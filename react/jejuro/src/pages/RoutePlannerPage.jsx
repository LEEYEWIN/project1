import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { ensureRoute, fetchRoute, saveRoute } from '../api/routeApi.js';
import { fetchBookmarks, removeBookmark } from '../api/bookmarkApi.js';
import { errorMessage } from '../api/client.js';
import DayTabs from '../components/route/DayTabs.jsx';
import DayEditor from '../components/route/DayEditor.jsx';
import BookmarkPicker from '../components/route/BookmarkPicker.jsx';
import DayRoutePreview from '../components/route/DayRoutePreview.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

const AUTOSAVE_DELAY_MS = 700;

// 저장하지 못한 변경을 이 탭(sessionStorage)에 잠시 보관 → 화면을 떠난 뒤 저장이 실패해도 돌아오면 복구
const draftKey = (routeId) => `routeDraft:${routeId}`;
function readDraft(routeId) {
  try { return JSON.parse(sessionStorage.getItem(draftKey(routeId)) ?? 'null'); } catch { return null; }
}
function writeDraft(routeId, draft) {
  try { sessionStorage.setItem(draftKey(routeId), JSON.stringify(draft)); } catch { /* 저장소를 못 써도 편집은 계속 */ }
}
function clearDraft(routeId) {
  try { sessionStorage.removeItem(draftKey(routeId)); } catch { /* 무시 */ }
}
const toDays = (plan) => plan.map((spots, i) => ({ dayNo: i + 1, poiIds: spots.map((p) => p.poiId) }));

/**
 * 5페이지: 여행 장소를 날짜별로 배치해 경로(일정) 짜기 — /travels/:travelId/route
 * - 여행당 경로는 1개. 들어오면 서버가 경로를 찾아 주고(없으면 만듦) 마지막 상태를 불러온다.
 * - 바꿀 때마다 0.7초 뒤 자동 저장. 페이지를 나가도(관광지 화면을 보고 와도) 마지막 경로가 그대로 남는다.
 * - 여행 장소를 "모두" 배치해야 [일정 확정하러 가기]가 열린다. (확정은 여행 상세에서)
 * - 아래 동선 패널에서 이동 시간·주변 숙소·효율적인 순서 추천을 함께 본다.
 * plan[i] = i+1일차의 방문지 배열(배열 순서 = 방문 순서)
 */
export default function RoutePlannerPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();

  const [routeId, setRouteId] = useState(null);
  const [savedRoute, setSavedRoute] = useState(null); // 서버에 저장된 경로 (숙소 조회 기준)
  const [places, setPlaces] = useState([]); // 여행 장소
  const [plan, setPlan] = useState([]);
  const [routeName, setRouteName] = useState('');
  const [dayIndex, setDayIndex] = useState(0);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('saved'); // 'saved' | 'dirty' | 'saving' | 'error' | 'conflict'
  const [savedAt, setSavedAt] = useState(null);

  // 자동 저장용: 최신 값·변경 번호·진행 중 저장
  const latest = useRef({ plan: [], routeName: '' });
  const version = useRef(0);
  const savedVersion = useRef(0);
  const chain = useRef(Promise.resolve());
  const timer = useRef(null);
  const baseVersion = useRef(null); // 서버에서 마지막으로 받은 경로 버전 (다른 탭의 변경 감지용)
  const conflict = useRef(false); // 다른 탭과 충돌 → 자동 저장 멈춤
  const saveRestored = useRef(false); // 복구한 변경을 화면 준비 후 바로 저장
  latest.current = { plan, routeName };

  // ① 경로 찾기(없으면 생성) + 경로 상세 + 여행 장소
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { routeId: id } = await ensureRoute(travelId);
        let [detail, bookmarks] = await Promise.all([fetchRoute(id), fetchBookmarks(travelId)]);
        if (cancelled) return;

        // 예전 규칙으로 저장된 경로 정리: 여행 장소에 없는 곳·여러 날에 겹친 곳은 빼고 바로 저장
        const placeIds = new Set(bookmarks.map((b) => b.poi.poiId));
        const seen = new Set();
        const next = Array.from({ length: detail.tripDays }, () => []);
        let cleaned = false;
        detail.days.forEach((d) => {
          next[d.dayNo - 1] = d.spots
            .map((s) => s.poi)
            .filter((p) => {
              const keep = placeIds.has(p.poiId) && !seen.has(p.poiId);
              seen.add(p.poiId);
              if (!keep) cleaned = true;
              return keep;
            });
        });
        if (cleaned && !detail.locked) {
          detail = await saveRoute(id, {
            routeName: detail.routeName ?? '',
            days: toDays(next),
            baseVersion: detail.version,
          });
          if (cancelled) return;
        }
        baseVersion.current = detail.version;

        // 지난번에 저장하지 못하고 떠난 변경이 있으면 복구
        let plan0 = next;
        let name0 = detail.routeName ?? '';
        let restored = false;
        const draft = detail.locked ? null : readDraft(id);
        if (draft) {
          if (draft.baseVersion === detail.version) {
            const byId = new Map(bookmarks.map((b) => [b.poi.poiId, b.poi]));
            const used = new Set();
            plan0 = Array.from({ length: detail.tripDays }, (_, i) =>
              (draft.days[i] ?? [])
                .map((pid) => byId.get(pid))
                .filter((p) => p && !used.has(p.poiId) && used.add(p.poiId))
            );
            name0 = draft.routeName ?? name0;
            restored = true;
          } else {
            clearDraft(id);
            window.alert('저장하지 못한 경로 변경이 있었지만, 그 사이 다른 화면에서 경로가 바뀌어 복구하지 못했어요. 최신 경로를 보여 드릴게요.');
          }
        }
        setRouteId(id);
        setSavedRoute(detail);
        setPlan(plan0);
        setRouteName(name0);
        if (restored) {
          latest.current = { plan: plan0, routeName: name0 };
          version.current += 1;
          saveRestored.current = true;
          setStatus('dirty');
          window.alert('지난번에 저장하지 못한 경로 변경을 복구했어요. 바로 저장할게요.');
        }
        setPlaces(bookmarks.map((b) => b.poi));
        // 처음 열 때: 아직 배치 안 한 장소가 있으면 방문지가 가장 적은 날을 보여 줌
        const emptiest = next.reduce((best, s, i) => (s.length < next[best].length ? i : best), 0);
        setDayIndex(detail.days.length === 0 ? 0 : emptiest);
      } catch (e) {
        if (!cancelled) setError(errorMessage(e));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [travelId]);

  /** 지금 화면 상태를 서버에 저장 (저장은 한 번에 하나씩 순서대로) */
  const flush = useCallback(() => {
    if (!routeId) return chain.current;
    if (conflict.current) return Promise.reject(new Error('conflict'));
    clearTimeout(timer.current);
    const target = version.current;
    if (target === savedVersion.current) return chain.current;
    chain.current = chain.current
      .catch(() => {})
      .then(async () => {
        if (target <= savedVersion.current) return;
        const { plan: p, routeName: n } = latest.current;
        setStatus('saving');
        const detail = await saveRoute(routeId, {
          routeName: n.trim(),
          days: toDays(p),
          baseVersion: baseVersion.current,
        });
        baseVersion.current = detail.version;
        savedVersion.current = Math.max(savedVersion.current, target);
        if (version.current === savedVersion.current) clearDraft(routeId);
        setSavedRoute(detail);
        setSavedAt(new Date());
        setError('');
        setStatus(version.current === savedVersion.current ? 'saved' : 'dirty');
      })
      .catch((e) => {
        if (e?.response?.status === 409) {
          // 다른 탭에서 먼저 바뀜(또는 일정 확정) → 덮어쓰지 않고 멈춘 뒤 알린다
          if (!conflict.current) {
            conflict.current = true;
            clearDraft(routeId);
            setStatus('conflict');
            const msg = errorMessage(e);
            setError(msg);
            window.alert(`${msg}\n[최신 경로 불러오기]를 눌러 주세요. 이 화면의 마지막 변경은 저장되지 않았어요.`);
          }
        } else if (!conflict.current) {
          setStatus('error');
          setError(`자동 저장에 실패했어요. ${errorMessage(e)}`);
        }
        throw e;
      });
    return chain.current;
  }, [routeId]);

  // 복구한 변경은 경로 번호가 준비되면 바로 저장
  useEffect(() => {
    if (routeId && saveRestored.current) {
      saveRestored.current = false;
      flush().catch(() => {});
    }
  }, [routeId, flush]);

  /** 화면이 바뀌었음을 표시하고 잠시 뒤 자동 저장 */
  const touch = () => {
    if (conflict.current) {
      window.alert('다른 화면에서 이 경로가 바뀌어 지금 화면의 변경은 저장할 수 없어요. [최신 경로 불러오기]를 눌러 주세요.');
      return;
    }
    version.current += 1;
    setStatus('dirty');
    clearTimeout(timer.current);
    timer.current = setTimeout(() => flush().catch(() => {}), AUTOSAVE_DELAY_MS);
  };

  // 바뀐 내용은 저장이 끝날 때까지 이 탭에도 보관 (화면을 떠난 뒤 저장이 실패해도 돌아오면 복구)
  useEffect(() => {
    if (!routeId || conflict.current || version.current === savedVersion.current) return;
    writeDraft(routeId, { baseVersion: baseVersion.current, routeName, days: plan.map((spots) => spots.map((p) => p.poiId)) });
  }, [plan, routeName, routeId]);

  // 페이지를 나갈 때(다른 화면으로 이동) 저장 안 된 변경이 있으면 바로 저장
  // (실패하면 보관해 둔 변경이 남아 있어 다시 들어올 때 복구한다)
  const flushRef = useRef(flush);
  flushRef.current = flush;
  useEffect(
    () => () => {
      flushRef.current().catch(() => {});
    },
    []
  );

  // 브라우저를 닫거나 새로고침할 때 저장 중이면 경고
  useEffect(() => {
    const onBeforeUnload = (e) => {
      if (version.current !== savedVersion.current) {
        flushRef.current().catch(() => {});
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, []);

  const updateDay = (index, spots) => {
    setPlan((prev) => prev.map((s, i) => (i === index ? spots : s)));
    touch();
  };

  /** 장소를 (day)일차의 (pos)번째 자리에 넣기. pos는 0부터 */
  const placeAt = (poi, day, pos) => {
    setPlan((prev) =>
      prev.map((spots, i) => {
        if (i !== day) return spots;
        const next = [...spots];
        next.splice(pos, 0, poi);
        return next;
      })
    );
    setDayIndex(day);
    touch();
  };

  /** 경로에서 빼기 (여행 장소에는 남음) */
  const removePoi = (poiId) => {
    setPlan((prev) => prev.map((spots) => spots.filter((p) => p.poiId !== poiId)));
    touch();
  };

  /** 여행 장소에서 아예 삭제 (배치 전 장소만) */
  const deletePlace = async (poi) => {
    if (!window.confirm(`${poi.name}을(를) 여행 장소에서 뺄까요?\n이 여행 일정에 넣지 않을 곳일 때 사용하세요.`)) return;
    try {
      await flush();
      await removeBookmark(travelId, poi.poiId);
      setPlaces((list) => list.filter((p) => p.poiId !== poi.poiId));
      // 장소 빼기는 서버에서 경로도 정리할 수 있으므로 최신 버전을 다시 받아 둔다
      baseVersion.current = (await fetchRoute(routeId)).version;
    } catch (e) {
      setError(errorMessage(e));
    }
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
    touch();
  };

  /** 저장이 끝난 뒤 이동 (실패하면 이동하지 않고 오류 표시) */
  const saveThen = async (path) => {
    try {
      await flush();
      navigate(path);
    } catch (e) {
      // 충돌은 flush가 이미 알림을 띄움. 그 밖의 저장 실패는 이동하지 않고 알림
      if (!conflict.current) window.alert(`변경 내용을 저장하지 못해 이동하지 않았어요. ${errorMessage(e)}`);
    }
  };

  if (error && !savedRoute) return <main className="page"><ErrorBox message={error} /></main>;
  if (!savedRoute) return <main className="page"><Loading text="경로를 불러오는 중…" /></main>;
  // 일정을 확정한 여행은 수정 불가 → 지도(읽기 전용)로 보낸다
  if (savedRoute.locked) return <Navigate to={`/travels/${travelId}/routes/${savedRoute.routeId}/map`} replace />;

  const placedIds = new Set(plan.flat().map((p) => p.poiId));
  const unplaced = places.filter((p) => !placedIds.has(p.poiId));
  const totalSpots = placedIds.size;
  const ready = places.length > 0 && unplaced.length === 0;

  const statusText =
    status === 'saving'
      ? '저장 중…'
      : status === 'dirty'
        ? '변경됨 · 곧 자동 저장'
        : status === 'error'
          ? '저장 실패'
          : status === 'conflict'
            ? '다른 화면에서 변경됨 · 저장 멈춤'
          : savedAt
            ? `자동 저장됨 ${String(savedAt.getHours()).padStart(2, '0')}:${String(savedAt.getMinutes()).padStart(2, '0')}`
            : '저장됨';

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>나에게 맞는 경로를 짜세요</h1>
        <div className="actions">
          <Link
            className="btn ghost"
            to={`/travels/${travelId}/bookmarks`}
            onClick={(e) => { e.preventDefault(); saveThen(`/travels/${travelId}/bookmarks`); }}
          >
            ← 이전
          </Link>
        </div>
      </div>

      <div className="planner-guide">
        <section className="planner-guide-card" aria-labelledby="planner-save-title">
          <span className="planner-guide-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 3h12l4 4v13a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
              <path d="M7 3v6h10V3M7 21v-8h10v8M14 5v2" />
            </svg>
          </span>
          <div>
            <span className="planner-guide-label">자동 저장 안내</span>
            <h2 id="planner-save-title">이 여행의 경로는 하나예요</h2>
            <p>변경한 경로는 <strong>자동으로 저장돼요.</strong> 관광지를 더 찾아보고 돌아와도 마지막 상태에서 이어서 짤 수 있어요.</p>
          </div>
        </section>
        <section className="planner-guide-card planner-guide-stay" aria-labelledby="planner-stay-title">
          <span className="planner-guide-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 10 12 3l9 7M5 9v12h14V9M9 21v-8h6v8" />
            </svg>
          </span>
          <div>
            <span className="planner-guide-label">동선 계획 팁</span>
            <h2 id="planner-stay-title">숙소도 함께 살펴보세요</h2>
            <p>아래 <strong className="planner-guide-tab">주변 숙소</strong> 탭에서 그날 마지막 관광지 근처의 숙소를 확인해 보세요. 숙소와 가까운 곳에서 하루를 마무리하면 이동을 줄일 수 있어요.</p>
          </div>
        </section>
      </div>

      <DayTabs
        startDate={savedRoute.startDate}
        tripDays={savedRoute.tripDays}
        current={dayIndex}
        onChange={setDayIndex}
        counts={plan.map((s) => s.length)}
      />

      <div className="planner">
        <aside className="card">
          <h2>
            여행 장소 <small>{totalSpots}/{places.length}곳 배치</small>
          </h2>
          {unplaced.length > 0 ? (
            <p className="placement-status">아직 배치 전 {unplaced.length}곳 — 모두 배치해야 일정을 확정할 수 있어요.</p>
          ) : (
            places.length > 0 && <p className="placement-status done">모든 장소를 배치했어요.</p>
          )}
          <p className="hint small">날짜와 순번을 고르고 "넣기"를 누르세요.</p>
          <BookmarkPicker
            pois={places}
            plan={plan}
            currentDay={dayIndex + 1}
            onPlace={placeAt}
            onRemove={removePoi}
            onDeletePlace={deletePlace}
          />
        </aside>
        <section className="card">
          <h2>{dayIndex + 1}일차 방문 순서</h2>
          <DayEditor
            dayNo={dayIndex + 1}
            tripDays={savedRoute.tripDays}
            spots={plan[dayIndex]}
            onChange={(s) => updateDay(dayIndex, s)}
            onMoveToDay={moveToDay}
          />
        </section>
      </div>

      <DayRoutePreview
        dayNo={dayIndex + 1}
        spots={plan[dayIndex]}
        onReorder={(s) => updateDay(dayIndex, s)}
        routeId={routeId}
        savedRoute={savedRoute}
        saving={status === 'saving' || status === 'dirty'}
      />

      <ErrorBox message={error} />

      <div className="bottom-bar">
        <span className={`save-state ${status}`}>
          {statusText} · {totalSpots}/{places.length}곳 배치
        </span>
        <div className="actions">
          {status === 'error' && (
            <button type="button" className="btn ghost" onClick={() => flush().catch(() => {})}>
              다시 저장
            </button>
          )}
          {status === 'conflict' && (
            <button type="button" className="btn primary" onClick={() => window.location.reload()}>
              최신 경로 불러오기
            </button>
          )}
          <button
            type="button"
            className="btn ghost"
            disabled={totalSpots === 0}
            onClick={() => saveThen(`/travels/${travelId}/routes/${routeId}/map`)}
          >
            지도 크게 보기
          </button>
          <button
            type="button"
            className="btn primary"
            disabled={!ready}
            title={ready ? undefined : places.length === 0 ? '여행 장소를 먼저 추가해 주세요' : `여행 장소 ${unplaced.length}곳을 먼저 배치해 주세요`}
            onClick={() => saveThen(`/travels/${travelId}`)}
          >
            {ready
              ? '일정 확정하러 가기 →'
              : places.length === 0
                ? '장소를 먼저 추가해 주세요'
                : `${unplaced.length}곳 더 배치하면 확정 가능`}
          </button>
        </div>
      </div>
    </main>
  );
}
