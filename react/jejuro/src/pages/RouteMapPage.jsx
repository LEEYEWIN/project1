import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchRoute } from '../api/routeApi.js';
import { fetchDirections } from '../api/directionsApi.js';
import { errorMessage } from '../api/client.js';
import { formatDate, formatDistance, formatDuration } from '../utils/format.js';
import KakaoMap from '../components/map/KakaoMap.jsx';
import LegList from '../components/map/LegList.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import LodgingPanel from '../components/route/LodgingPanel.jsx';

/**
 * 6페이지: 카카오맵으로 일차별 동선과 이동수단별 시간 보기
 * - 자동차: 서버가 카카오모빌리티 길찾기 호출(실제 도로 경로)
 * - 도보: 직선거리 기반 추정(카카오 도보 API는 제휴 전용)
 * - 순서 바꾸기·효율적인 순서 추천은 경로 짜기 화면(/travels/:id/route)에서 한다 (여기서는 보기 전용)
 * - 주변 숙소(FR-26): 오른쪽 [주변 숙소] 탭 → 기준 관광지·반경·숙소를 지도에 원·마커로 표시
 */
export default function RouteMapPage() {
  const { travelId, routeId } = useParams();
  const [route, setRoute] = useState(null);
  const [dayNo, setDayNo] = useState(null);
  const [mode, setMode] = useState('CAR');
  const [dir, setDir] = useState(null);
  const [error, setError] = useState('');
  const [panel, setPanel] = useState('route'); // 'route' 이동 정보 | 'lodging' 주변 숙소
  const [lodgingResult, setLodgingResult] = useState(null);
  const [selectedLodgingId, setSelectedLodgingId] = useState(null);

  const loadRoute = useCallback(async () => {
    const detail = await fetchRoute(routeId);
    setRoute(detail);
    setDayNo((cur) => cur ?? detail.days[0]?.dayNo ?? null);
    return detail;
  }, [routeId]);

  useEffect(() => {
    loadRoute().catch((e) => setError(errorMessage(e)));
  }, [loadRoute]);

  // 일차·이동수단이 바뀔 때마다 이동 정보 다시 조회
  useEffect(() => {
    if (!dayNo) return;
    setDir(null);
    fetchDirections(routeId, dayNo, mode)
      .then(setDir)
      .catch((e) => setError(errorMessage(e)));
  }, [routeId, dayNo, mode]);

  // 날짜·이동수단·탭이 바뀌면 지도의 숙소 표시를 지움 (새 결과가 오면 다시 그림)
  useEffect(() => {
    setLodgingResult(null);
    setSelectedLodgingId(null);
  }, [dayNo, mode, panel]);

  const lodgingOnMap = useMemo(() => {
    if (panel !== 'lodging' || !lodgingResult?.anchor) return null;
    return {
      center: { lat: lodgingResult.anchor.latitude, lng: lodgingResult.anchor.longitude, name: lodgingResult.anchor.name },
      radiusM: lodgingResult.radiusKm * 1000,
      items: lodgingResult.items,
    };
  }, [panel, lodgingResult]);

  const day = route?.days.find((d) => d.dayNo === dayNo);
  const points = useMemo(
    () => (day ? day.spots.map((s) => ({ lat: Number(s.poi.latitude), lng: Number(s.poi.longitude), name: s.poi.name })) : []),
    [day]
  );

  if (error && !route) return <main className="page"><ErrorBox message={error} /></main>;
  if (!route) return <main className="page"><Loading /></main>;
  if (route.days.length === 0) {
    return (
      <main className="page">
        <p className="empty">저장된 일정이 없습니다.</p>
        {route.locked ? (
          <Link className="btn primary" to={`/travels/${travelId}`}>여행으로 돌아가기</Link>
        ) : (
          <Link className="btn primary" to={`/travels/${travelId}/route`}>
            경로 편집하기
          </Link>
        )}
      </main>
    );
  }

  return (
    <main className="page wide">
      <div className="title-row">
        <h1>{route.routeName ?? '여행 경로'} · 동선</h1>
        {route.locked ? (
          <span className="badge">확정된 일정 · 수정 불가</span>
        ) : (
          <Link className="btn ghost" to={`/travels/${travelId}/route`}>
            경로 짜기로 돌아가기
          </Link>
        )}
      </div>

      <div className="day-tabs">
        {route.days.map((d) => (
          <button
            key={d.dayNo}
            type="button"
            className={d.dayNo === dayNo ? 'day-tab on' : 'day-tab'}
            onClick={() => setDayNo(d.dayNo)}
          >
            <strong>{d.dayNo}일차</strong>
            <small>
              {formatDate(d.date)} · {d.spots.length}곳
            </small>
          </button>
        ))}
      </div>

      <div className="chips">
        <button type="button" className={mode === 'CAR' ? 'chip on' : 'chip'} onClick={() => setMode('CAR')}>
          🚗 자동차
        </button>
        <button type="button" className={mode === 'WALK' ? 'chip on' : 'chip'} onClick={() => setMode('WALK')}>
          🚶 도보
        </button>
      </div>

      <div className="map-layout">
        <KakaoMap
          points={points}
          path={dir?.path ?? points}
          lodging={lodgingOnMap}
          selectedLodgingId={selectedLodgingId}
          onSelectLodging={setSelectedLodgingId}
        />

        <section className="card">
          <div className="panel-tabs">
            <button type="button" className={panel === 'route' ? 'on' : ''} onClick={() => setPanel('route')}>
              이동 정보
            </button>
            <button type="button" className={panel === 'lodging' ? 'on' : ''} onClick={() => setPanel('lodging')}>
              주변 숙소
            </button>
          </div>

          {panel === 'lodging' ? (
            <LodgingPanel
              key={`${dayNo}-${mode}`}
              routeId={routeId}
              route={route}
              dayNo={dayNo}
              mode={mode}
              selectedId={selectedLodgingId}
              onSelect={setSelectedLodgingId}
              onResult={setLodgingResult}
            />
          ) : (
            <>
            {!dir ? (
              <Loading text="이동 정보를 계산하는 중…" />
            ) : (
              <>
                <p className="total">
                  총 {formatDuration(dir.totalDurationSec)} · {formatDistance(dir.totalDistanceM)}
                  {dir.estimated && <span className="badge">추정값</span>}
                </p>
                {dir.notice && <p className="hint">⚠ {dir.notice}</p>}
                {dir.estimated && !dir.notice && (
                  <p className="hint">
                    {mode === 'WALK'
                      ? '도보 시간은 직선거리×1.3, 시속 4km로 계산한 값입니다.'
                      : '카카오 REST 키가 없어 직선거리로 추정했습니다.'}
                  </p>
                )}
                <LegList legs={dir.legs} mode={mode} />
              </>
            )}

            </>
          )}
        </section>
      </div>

      <ErrorBox message={error} />

      <div className="bottom-bar">
        <span>{route.locked ? '확정된 일정이에요.' : '여행 장소를 모두 배치했다면 여행 상세에서 일정을 확정하세요.'}</span>
        <Link className="btn primary" to={`/travels/${travelId}`}>
          여행 상세로 →
        </Link>
      </div>
    </main>
  );
}
