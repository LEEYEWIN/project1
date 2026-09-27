import { useEffect, useRef } from 'react';
import useKakaoLoader from '../../hooks/useKakaoLoader.js';

/**
 * 카카오 지도: 번호 마커 + 경로 선 (+ 선택: 주변 숙소)
 * points: [{ lat, lng, name }]  (배열 순서 = 방문 순서, 마커에 1,2,3… 표시)
 * path:   [{ lat, lng }]        (자동차: 도로 좌표 / 도보·추정: 방문지를 잇는 직선)
 * lodging (선택, 6페이지 주변 숙소):
 *   { center: { lat, lng, name }, radiusM, items: [{ accommodationId, name, latitude, longitude }] }
 *   → 기준 지점 표시 + 검색 반경 원 + 숙소 마커. null이면 숙소 표시를 지우고 동선 화면으로 돌아감
 * selectedLodgingId / onSelectLodging: 목록·마커에서 고른 숙소 강조
 */
export default function KakaoMap({ points, path, height = 460, lodging = null, selectedLodgingId = null, onSelectLodging }) {
  const { ready, error } = useKakaoLoader();
  const boxRef = useRef(null);
  const mapRef = useRef(null);
  const drawnRef = useRef([]); // 지우기 위해 그린 객체 보관 (동선)
  const boundsRef = useRef(null); // 동선 전체가 보이는 범위 (숙소 표시를 끌 때 되돌아감)
  const lodgingDrawnRef = useRef([]); // 숙소 표시용 객체
  const lodgingElsRef = useRef(new Map()); // accommodationId → 마커 요소
  const onSelectRef = useRef(onSelectLodging);
  onSelectRef.current = onSelectLodging;

  // 지도 생성 (한 번)
  useEffect(() => {
    if (!ready || mapRef.current) return;
    const { kakao } = window;
    mapRef.current = new kakao.maps.Map(boxRef.current, {
      center: new kakao.maps.LatLng(33.38, 126.55), // 제주 중심
      level: 9,
    });
  }, [ready]);

  // 마커·선 다시 그리기
  useEffect(() => {
    const map = mapRef.current;
    if (!ready || !map) return;
    const { kakao } = window;

    drawnRef.current.forEach((o) => o.setMap(null));
    drawnRef.current = [];
    boundsRef.current = null;
    if (points.length === 0) return;

    const bounds = new kakao.maps.LatLngBounds();

    points.forEach((p, i) => {
      const pos = new kakao.maps.LatLng(p.lat, p.lng);
      bounds.extend(pos);
      const el = document.createElement('div');
      el.className = 'map-marker';
      el.textContent = String(i + 1);
      el.title = p.name;
      const overlay = new kakao.maps.CustomOverlay({ position: pos, content: el, yAnchor: 0.5 });
      overlay.setMap(map);
      drawnRef.current.push(overlay);
    });

    if (path.length > 1) {
      const line = new kakao.maps.Polyline({
        path: path.map((c) => new kakao.maps.LatLng(c.lat, c.lng)),
        strokeWeight: 5,
        strokeColor: '#1f9a74',
        strokeOpacity: 0.85,
      });
      line.setMap(map);
      drawnRef.current.push(line);
    }

    boundsRef.current = bounds;
    map.setBounds(bounds, 60, 60, 60, 60); // 모든 방문지가 보이게 확대/이동
  }, [ready, points, path]);

  // 주변 숙소: 기준 지점 + 반경 원 + 숙소 마커
  useEffect(() => {
    const map = mapRef.current;
    if (!ready || !map) return;
    const { kakao } = window;

    lodgingDrawnRef.current.forEach((o) => o.setMap(null));
    lodgingDrawnRef.current = [];
    lodgingElsRef.current = new Map();

    if (!lodging || !lodging.center) {
      if (boundsRef.current) map.setBounds(boundsRef.current, 60, 60, 60, 60);
      return;
    }

    const center = new kakao.maps.LatLng(lodging.center.lat, lodging.center.lng);
    const circle = new kakao.maps.Circle({
      center,
      radius: lodging.radiusM,
      strokeWeight: 2,
      strokeColor: '#2f6fdb',
      strokeOpacity: 0.9,
      strokeStyle: 'dashed',
      fillColor: '#2f6fdb',
      fillOpacity: 0.07,
    });
    circle.setMap(map);
    lodgingDrawnRef.current.push(circle);

    const anchorEl = document.createElement('div');
    anchorEl.className = 'anchor-marker';
    anchorEl.textContent = `기준 · ${lodging.center.name}`;
    const anchor = new kakao.maps.CustomOverlay({ position: center, content: anchorEl, yAnchor: 1.6, zIndex: 4 });
    anchor.setMap(map);
    lodgingDrawnRef.current.push(anchor);

    lodging.items.forEach((it) => {
      const el = document.createElement('button');
      el.type = 'button';
      el.className = 'lodging-marker';
      el.title = it.name;
      el.textContent = '숙';
      el.addEventListener('click', () => onSelectRef.current?.(it.accommodationId));
      const overlay = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(it.latitude, it.longitude),
        content: el,
        yAnchor: 0.5,
        zIndex: 3,
        clickable: true,
      });
      overlay.setMap(map);
      lodgingDrawnRef.current.push(overlay);
      lodgingElsRef.current.set(it.accommodationId, { el, overlay, name: it.name });
    });

    map.setBounds(circle.getBounds(), 30, 30, 30, 30); // 검색 반경 전체가 보이게
  }, [ready, lodging]);

  // 고른 숙소 강조 + 지도 이동
  useEffect(() => {
    const map = mapRef.current;
    if (!ready || !map) return;
    lodgingElsRef.current.forEach(({ el, overlay, name }, id) => {
      const on = id === selectedLodgingId;
      el.className = on ? 'lodging-marker on' : 'lodging-marker';
      el.textContent = on ? name : '숙';
      overlay.setZIndex(on ? 5 : 3);
      if (on) map.panTo(overlay.getPosition());
    });
  }, [ready, lodging, selectedLodgingId]);

  if (error) return <div className="map-box error-box">{error}</div>;
  return <div ref={boxRef} className="map-box" style={{ height }} />;
}