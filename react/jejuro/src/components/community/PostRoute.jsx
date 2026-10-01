import { useMemo, useState } from 'react';
import KakaoMap from '../map/KakaoMap.jsx';
import { formatDate } from '../../utils/format.js';

/**
 * [커뮤니티 목록] 글 카드 안의 "첨부된 최종 경로"
 * route: RouteDetailResponse { tripDays, days: [{ dayNo, date, spots: [{ visitOrder, poi }] }] }
 * - 제목 줄 오른쪽 [접기]/[펼치기]: 한 줄 요약 박스를 보이거나 숨김
 * - 요약 박스의 [지도 보기]: 일차 탭 + 카카오맵 + 방문 순서
 * (목록 카드는 버튼·링크·지도 밖을 눌렀을 때만 상세로 이동한다 → CommunityPage)
 */
export default function PostRoute({ travelName, route, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen);
  const [showMap, setShowMap] = useState(false);
  const [dayNo, setDayNo] = useState(route.days[0]?.dayNo ?? null);

  const spotCount = route.days.reduce((sum, d) => sum + d.spots.length, 0);
  const day = route.days.find((d) => d.dayNo === dayNo);
  const points = useMemo(
    () => (day ? day.spots.map((s) => ({ lat: Number(s.poi.latitude), lng: Number(s.poi.longitude), name: s.poi.name })) : []),
    [day]
  );

  if (route.days.length === 0) return null;

  return (
    <div className="cm-route">
      <div className="cm-route-head">
        <strong>첨부된 최종 경로</strong>
        <button type="button" className="cm-text-btn" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
          {open ? '접기' : '펼치기'}
        </button>
      </div>

      {open && (
        <div className="cm-route-box">
          <div className="cm-route-line">
            <div className="cm-route-summary">
              <strong>{travelName ?? '여행'}</strong>
              <span>{route.tripDays}일</span>
              <span>방문지 {spotCount}곳</span>
            </div>
            <button type="button" className="cm-text-btn" onClick={() => setShowMap((v) => !v)}>
              {showMap ? '지도 닫기' : '지도 보기'}
            </button>
          </div>

          {showMap && (
            <div className="cm-route-map">
              <div className="chips">
                {route.days.map((d) => (
                  <button
                    key={d.dayNo}
                    type="button"
                    className={d.dayNo === dayNo ? 'chip on' : 'chip'}
                    onClick={() => setDayNo(d.dayNo)}
                  >
                    {d.dayNo}일차 <small>{formatDate(d.date)}</small>
                  </button>
                ))}
              </div>
              <KakaoMap points={points} path={points} height={260} />
              <p className="cm-route-names"><b>방문 순서</b>{day?.spots.map((s) => s.poi.name).join(' → ')}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
