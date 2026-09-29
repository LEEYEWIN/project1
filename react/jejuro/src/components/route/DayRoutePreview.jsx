import { useEffect, useMemo, useState } from 'react';
import { previewDirections, previewOptimize } from '../../api/directionsApi.js';
import { errorMessage } from '../../api/client.js';
import { formatDistance, formatDuration } from '../../utils/format.js';
import KakaoMap from '../map/KakaoMap.jsx';
import LegList from '../map/LegList.jsx';
import LodgingPanel from './LodgingPanel.jsx';

/**
 * 5페이지 아래쪽: 선택한 일차의 동선 미리보기 + 주변 숙소 + 효율적인 순서 추천.
 * - 이동 정보 탭: 화면의 순서대로 지도·구간별 이동시간을 다시 계산
 * - 주변 숙소 탭(FR-26): 루트를 짜는 동안 그날 마지막 관광지 주변 숙소를 확인 → 숙소 위치를 보고 순서를 정할 수 있게
 *   (숙소는 자동 저장된 경로 기준으로 찾으므로 savedRoute를 넘겨받는다)
 * - 순서 추천: 1번 방문지는 출발점으로 고정, 선택하면 마지막도 고정. 기준·계산 방법을 화면에 그대로 설명한다
 *
 * spots: 현재 일차 방문지(순서 = 방문 순서) / onReorder(newSpots): 추천 순서 적용
 * routeId, savedRoute: 자동 저장된 경로(숙소 조회용) / saving: 저장 중이면 숙소 탭에 안내
 */
export default function DayRoutePreview({ dayNo, spots, onReorder, routeId, savedRoute, saving }) {
  const [mode, setMode] = useState('CAR');
  const [panel, setPanel] = useState('route'); // 'route' | 'lodging'
  const [dir, setDir] = useState(null);
  const [suggest, setSuggest] = useState(null);
  const [fixEnd, setFixEnd] = useState(false);
  const [error, setError] = useState('');
  const [lodgingResult, setLodgingResult] = useState(null);
  const [selectedLodgingId, setSelectedLodgingId] = useState(null);

  const poiIds = useMemo(() => spots.map((p) => p.poiId), [spots]);
  const key = poiIds.join(',');

  // 순서·이동수단이 바뀌면 0.4초 뒤 다시 계산 (연속으로 바꿀 때 요청이 몰리지 않게)
  useEffect(() => {
    setSuggest(null);
    setError('');
    if (poiIds.length === 0) {
      setDir(null);
      return undefined;
    }
    const timer = setTimeout(() => {
      previewDirections(poiIds, mode)
        .then(setDir)
        .catch((e) => setError(errorMessage(e)));
    }, 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, mode]);

  // 날짜·이동수단·탭이 바뀌면 지도의 숙소 표시를 지움
  useEffect(() => {
    setLodgingResult(null);
    setSelectedLodgingId(null);
  }, [dayNo, mode, panel]);

  // 고정 조건을 바꾸면 이전 추천은 버림
  useEffect(() => setSuggest(null), [fixEnd]);

  const points = useMemo(
    () => spots.map((p) => ({ lat: Number(p.latitude), lng: Number(p.longitude), name: p.name })),
    [spots]
  );

  const lodgingOnMap = useMemo(() => {
    if (panel !== 'lodging' || !lodgingResult?.anchor) return null;
    return {
      center: { lat: lodgingResult.anchor.latitude, lng: lodgingResult.anchor.longitude, name: lodgingResult.anchor.name },
      radiusM: lodgingResult.radiusKm * 1000,
      items: lodgingResult.items,
    };
  }, [panel, lodgingResult]);

  // 저장된 경로에서 이 날의 방문지 목록이 바뀌면 숙소 패널을 새로 그림
  const savedDaySig = savedRoute?.days.find((d) => d.dayNo === dayNo)?.spots.map((s) => s.poi.poiId).join(',') ?? '';

  const askOptimize = async () => {
    try {
      setSuggest(await previewOptimize(poiIds, fixEnd));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const applySuggest = () => {
    const byId = new Map(spots.map((p) => [p.poiId, p]));
    onReorder(suggest.poiIds.map((id) => byId.get(id)));
    setSuggest(null);
  };

  const nameOf = (id) => spots.find((p) => p.poiId === id)?.name ?? id;

  if (spots.length === 0) return null;

  return (
    <section className="card preview">
      <div className="title-row">
        <h2>{dayNo}일차 동선 · 숙소</h2>
        <div className="chips">
          <button type="button" className={mode === 'CAR' ? 'chip on' : 'chip'} onClick={() => setMode('CAR')}>
            🚗 자동차
          </button>
          <button type="button" className={mode === 'WALK' ? 'chip on' : 'chip'} onClick={() => setMode('WALK')}>
            🚶 도보
          </button>
        </div>
      </div>

      <div className="map-layout">
        <KakaoMap
          points={points}
          path={dir?.path ?? points}
          height={380}
          lodging={lodgingOnMap}
          selectedLodgingId={selectedLodgingId}
          onSelectLodging={setSelectedLodgingId}
        />

        <div>
          <div className="panel-tabs">
            <button type="button" className={panel === 'route' ? 'on' : ''} onClick={() => setPanel('route')}>
              이동 정보 · 순서 추천
            </button>
            <button type="button" className={panel === 'lodging' ? 'on' : ''} onClick={() => setPanel('lodging')}>
              주변 숙소
            </button>
          </div>

          {panel === 'lodging' ? (
            <>
              {saving && <p className="hint small">경로를 저장하는 중이에요. 저장이 끝나면 숙소가 다시 계산돼요.</p>}
              {routeId && savedRoute ? (
                <LodgingPanel
                  key={`${dayNo}-${mode}-${savedDaySig}`}
                  routeId={routeId}
                  route={savedRoute}
                  dayNo={dayNo}
                  mode={mode}
                  selectedId={selectedLodgingId}
                  onSelect={setSelectedLodgingId}
                  onResult={setLodgingResult}
                />
              ) : (
                <p className="muted">경로를 불러오는 중…</p>
              )}
            </>
          ) : (
            <>
              {error && <p className="error">{error}</p>}
              {dir ? (
                <>
                  <p className="total">
                    총 {formatDuration(dir.totalDurationSec)} · {formatDistance(dir.totalDistanceM)}
                    {dir.estimated && <span className="badge">추정값</span>}
                  </p>
                  {dir.notice && <p className="hint">⚠ {dir.notice}</p>}
                  <LegList legs={dir.legs} mode={mode} />
                </>
              ) : (
                !error && <p className="muted">이동 정보를 계산하는 중…</p>
              )}

              {spots.length >= 3 && (
                <div className="optimize">
                  <details className="opt-rule">
                    <summary>효율적인 순서는 이렇게 계산해요</summary>
                    <ol>
                      <li>
                        <b>1번 방문지는 출발점으로 고정</b>해요. 숙소·공항에서 가장 먼저 갈 곳을 1번에 두세요.
                      </li>
                      <li>아래에서 고르면 <b>마지막 방문지도 고정</b>해요. 숙소 근처 관광지로 하루를 끝내고 싶을 때 쓰세요.</li>
                      <li>
                        나머지 방문지의 순서를 바꿔 가며 <b>방문지 사이 직선거리의 합이 가장 짧은 순서</b>를 찾아요.
                      </li>
                      <li>
                        바꿀 수 있는 곳이 <b>8곳 이하</b>면 가능한 순서를 <b>모두 비교</b>한 최단 순서예요. 9곳 이상이면 가까운 곳부터
                        잇고 꼬인 구간을 풀어 줄이는 <b>근사</b> 순서예요.
                      </li>
                      <li>도로 사정·운영 시간·머무는 시간은 보지 않아요. 바꾼 뒤 위의 이동 시간을 보고 판단하세요.</li>
                    </ol>
                  </details>

                  <label className="check small">
                    <input type="checkbox" checked={fixEnd} onChange={(e) => setFixEnd(e.target.checked)} />
                    마지막 방문지({spots[spots.length - 1].name})도 고정
                  </label>

                  {!suggest ? (
                    <button type="button" className="btn ghost small" onClick={askOptimize}>
                      효율적인 순서 추천받기
                    </button>
                  ) : (
                    <div className="suggest">
                      <p className="opt-basis">
                        출발 고정: <b>{suggest.fixedStartName}</b>
                        {suggest.fixedEndName && (
                          <>
                            {' '}· 도착 고정: <b>{suggest.fixedEndName}</b>
                          </>
                        )}
                        <br />
                        {suggest.method === 'EXACT'
                          ? `가능한 순서 ${suggest.comparedCount.toLocaleString()}가지를 모두 비교한 최단 순서`
                          : '9곳 이상이라 근사 계산한 순서 (최단이 아닐 수 있음)'}
                      </p>
                      {suggest.afterDistanceM >= suggest.beforeDistanceM ? (
                        <p>지금 순서가 이미 가장 짧아요. ({formatDistance(suggest.beforeDistanceM)}, 직선 기준)</p>
                      ) : (
                        <>
                          <p>
                            직선거리 {formatDistance(suggest.beforeDistanceM)} → <strong>{formatDistance(suggest.afterDistanceM)}</strong>
                          </p>
                          <ol className="opt-order">
                            {suggest.poiIds.map((id) => (
                              <li key={id}>{nameOf(id)}</li>
                            ))}
                          </ol>
                        </>
                      )}
                      <div className="actions">
                        <button type="button" className="btn ghost small" onClick={() => setSuggest(null)}>
                          그대로 두기
                        </button>
                        <button
                          type="button"
                          className="btn primary small"
                          disabled={suggest.afterDistanceM >= suggest.beforeDistanceM}
                          onClick={applySuggest}
                        >
                          이 순서로 바꾸기
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
      <p className="hint small">순서를 바꾸면 동선이 바로 다시 계산되고, 경로는 자동으로 저장돼요.</p>
    </section>
  );
}
