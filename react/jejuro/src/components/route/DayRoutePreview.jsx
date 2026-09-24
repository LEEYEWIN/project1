import { useEffect, useMemo, useState } from 'react';
import { previewDirections, previewOptimize } from '../../api/directionsApi.js';
import { errorMessage } from '../../api/client.js';
import { formatDistance, formatDuration } from '../../utils/format.js';
import KakaoMap from '../map/KakaoMap.jsx';
import LegList from '../map/LegList.jsx';

/**
 * 5페이지 아래쪽: 선택한 일차의 "저장 전" 동선 미리보기.
 * 화면에서 순번을 바꿀 때마다 그 순서대로 지도·구간별 이동시간을 다시 계산한다.
 * 확인한 뒤 [저장]을 누르면 이 순서가 그대로 DB(visit_order)에 들어간다.
 *
 * spots: 현재 일차 방문지 배열(순서 = 방문 순서) / onReorder(newSpots): 추천 순서 적용
 */
export default function DayRoutePreview({ dayNo, spots, onReorder }) {
  const [mode, setMode] = useState('CAR');
  const [dir, setDir] = useState(null);
  const [suggest, setSuggest] = useState(null);
  const [error, setError] = useState('');

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

  const points = useMemo(
    () => spots.map((p) => ({ lat: Number(p.latitude), lng: Number(p.longitude), name: p.name })),
    [spots]
  );

  const askOptimize = async () => {
    try {
      setSuggest(await previewOptimize(poiIds));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const applySuggest = () => {
    const byId = new Map(spots.map((p) => [p.poiId, p]));
    onReorder(suggest.poiIds.map((id) => byId.get(id)));
    setSuggest(null);
  };

  if (spots.length === 0) return null;

  return (
    <section className="card preview">
      <div className="title-row">
        <h2>{dayNo}일차 동선 미리보기</h2>
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
        <KakaoMap points={points} path={dir?.path ?? points} height={360} />
        <div>
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
              {!suggest ? (
                <button type="button" className="btn ghost small" onClick={askOptimize}>
                  효율적인 순서 추천받기
                </button>
              ) : (
                <div className="suggest">
                  <p>
                    이동 거리 {formatDistance(suggest.beforeDistanceM)} → <strong>{formatDistance(suggest.afterDistanceM)}</strong>
                  </p>
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
        </div>
      </div>
      <p className="hint small">순번을 바꾸면 동선이 바로 다시 계산됩니다. 마음에 들면 아래 [저장하고 여행으로]를 누르세요.</p>
    </section>
  );
}