import { useEffect, useMemo, useState } from 'react';
import { fetchLodgings } from '../../api/lodgingApi.js';
import { errorMessage } from '../../api/client.js';
import { formatDistance } from '../../utils/format.js';
import Loading from '../common/Loading.jsx';

const RADIUS_OPTIONS = [1, 3, 5, 10];
const SOURCE_NAME = { TOUR: '한국관광공사', VJ: '비짓제주', KAKAO: '카카오' };

/**
 * 6페이지: 주변 숙소 안내 (FR-26). 예약·결제는 제공하지 않는다.
 * - 기준: 기본은 선택한 날짜의 마지막 관광지. [내일 첫 관광지]·목록에서 다른 관광지로 바꿀 수 있음
 * - 반경: 기본 자동차 3km / 도보 1km (5곳 미만이면 서버가 5km → 10km로 넓힘). 직접 고르면 넓히지 않음
 * - 결과(기준·반경·숙소)는 onResult로 올려서 지도에 원·마커로 그린다.
 * - 부모가 key={`${dayNo}-${mode}`}로 그려서, 날짜·이동수단이 바뀌면 기준·반경이 기본값으로 돌아간다.
 */
export default function LodgingPanel({ routeId, route, dayNo, mode, selectedId, onSelect, onResult }) {
  const [anchorPoiId, setAnchorPoiId] = useState(null); // null = 서버 기본(그날 마지막 관광지)
  const [radiusKm, setRadiusKm] = useState(null); // null = 이동수단 기본값 + 자동 확장
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (!dayNo) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');
    onSelect(null);
    fetchLodgings(routeId, dayNo, { anchorPoiId, radiusKm, mode, expand: radiusKm == null })
      .then((res) => {
        if (cancelled) return;
        setData(res);
        onResult(res);
      })
      .catch((e) => {
        if (cancelled) return;
        setData(null);
        onResult(null);
        setError(errorMessage(e));
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // onResult·onSelect는 부모의 setState라 바뀌지 않음
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeId, dayNo, mode, anchorPoiId, radiusKm, retry]);

  // 기준으로 고를 수 있는 관광지: 경로 전체 방문지
  const spotOptions = useMemo(
    () =>
      route.days.flatMap((d) =>
        d.spots.map((s) => ({ key: `${d.dayNo}-${s.poi.poiId}`, poiId: s.poi.poiId, label: `${d.dayNo}일차 ${s.visitOrder}. ${s.poi.name}` }))
      ),
    [route]
  );
  const nextDayFirst = route.days.find((d) => d.dayNo === dayNo + 1)?.spots[0]?.poi;
  const shownRadius = data?.radiusKm ?? radiusKm ?? (mode === 'WALK' ? 1 : 3);

  return (
    <div className="lodging">
      <p className="hint small">숙소 정보만 안내해요. 예약·결제는 각 숙소나 예약 사이트에서 해 주세요.</p>

      <div className="lodging-controls">
        <label className="field">
          기준 관광지
          <select
            value={anchorPoiId ?? ''}
            onChange={(e) => setAnchorPoiId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">{dayNo}일차 마지막 관광지 (기본)</option>
            {spotOptions.map((o) => (
              <option key={o.key} value={o.poiId}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        {nextDayFirst && (
          <button
            type="button"
            className={anchorPoiId === nextDayFirst.poiId ? 'chip on' : 'chip'}
            onClick={() => setAnchorPoiId(nextDayFirst.poiId)}
          >
            내일 첫 관광지({nextDayFirst.name}) 기준
          </button>
        )}

        <div className="chips">
          {RADIUS_OPTIONS.map((km) => (
            <button
              key={km}
              type="button"
              className={shownRadius === km ? 'chip on' : 'chip'}
              onClick={() => setRadiusKm(km)}
            >
              {km}km
            </button>
          ))}
        </div>
      </div>

      {loading && <Loading text="주변 숙소를 찾는 중…" />}

      {!loading && error && (
        <div className="lodging-state">
          <p>숙소 정보를 불러오지 못했어요. {error}</p>
          <button type="button" className="btn ghost" onClick={() => setRetry((n) => n + 1)}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && !error && data && (
        <>
          {data.anchor ? (
            <p className="lodging-anchor">
              기준 <strong>{data.anchor.name}</strong> ({data.anchorLabel}) · 반경 <strong>{data.radiusKm}km</strong>
            </p>
          ) : (
            <p className="lodging-state">{data.message}</p>
          )}
          {data.anchor && data.items.length === 0 && (
            <p className="lodging-state">🏨 {data.message ?? '주변에 숙소가 없어요.'}</p>
          )}
          {data.anchor && data.items.length > 0 && data.message && <p className="hint small">ℹ {data.message}</p>}

          {data.items.length > 0 && (
            <>
              <p className="hint small">가까운 순 {data.items.length}곳 (직선거리)</p>
              <ul className="lodging-list">
                {data.items.map((it) => (
                  <li key={it.accommodationId}>
                    <button
                      type="button"
                      className={selectedId === it.accommodationId ? 'lodging-item on' : 'lodging-item'}
                      onClick={() => onSelect(it.accommodationId)}
                    >
                      {it.imageUrl && <img src={it.imageUrl} alt="" loading="lazy" />}
                      <span className="lodging-text">
                        <strong>{it.name}</strong>
                        <span className="lodging-meta">
                          {it.typeName} · {formatDistance(it.distanceM)}
                        </span>
                        <span className="lodging-addr">{it.address}</span>
                        {it.phone && <span className="lodging-addr">☎ {it.phone}</span>}
                        <span className="lodging-src">
                          정보: {SOURCE_NAME[it.source] ?? '기타'}
                          {it.imageUrl && ` · 사진: ${SOURCE_NAME[it.source] ?? '기타'}`}
                        </span>
                      </span>
                    </button>
                    <a
                      className="lodging-link"
                      href={`https://map.kakao.com/link/map/${encodeURIComponent(it.name)},${it.latitude},${it.longitude}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      카카오맵에서 보기
                    </a>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}
    </div>
  );
}