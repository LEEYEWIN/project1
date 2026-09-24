import { useEffect, useRef } from 'react';
import useKakaoLoader from '../../hooks/useKakaoLoader.js';

/**
 * 카카오 지도: 번호 마커 + 경로 선
 * points: [{ lat, lng, name }]  (배열 순서 = 방문 순서, 마커에 1,2,3… 표시)
 * path:   [{ lat, lng }]        (자동차: 도로 좌표 / 도보·추정: 방문지를 잇는 직선)
 */
export default function KakaoMap({ points, path, height = 460 }) {
  const { ready, error } = useKakaoLoader();
  const boxRef = useRef(null);
  const mapRef = useRef(null);
  const drawnRef = useRef([]); // 지우기 위해 그린 객체 보관

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

    map.setBounds(bounds, 60, 60, 60, 60); // 모든 방문지가 보이게 확대/이동
  }, [ready, points, path]);

  if (error) return <div className="map-box error-box">{error}</div>;
  return <div ref={boxRef} className="map-box" style={{ height }} />;
}