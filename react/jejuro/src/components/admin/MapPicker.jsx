import { useEffect, useRef } from 'react';
import useKakaoLoader from '../../hooks/useKakaoLoader.js';

/**
 * 관리자 관광지 위치 고르기: 지도를 누르거나 마커를 끌면 위도·경도가 바뀐다.
 * lat, lng: 지금 좌표(문자열/숫자, 비어 있으면 제주 중심), onPick(lat, lng)
 * 카카오맵 키가 없으면 안내만 보여 주고, 위도·경도 칸에 직접 입력하면 된다.
 */
export default function MapPicker({ lat, lng, onPick }) {
  const { ready, error } = useKakaoLoader();
  const boxRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const onPickRef = useRef(onPick);
  onPickRef.current = onPick;

  const nLat = Number(lat);
  const nLng = Number(lng);
  const valid = Number.isFinite(nLat) && Number.isFinite(nLng) && nLat !== 0 && nLng !== 0;

  // 지도·마커 만들기 (한 번)
  useEffect(() => {
    if (!ready || mapRef.current) return;
    const { kakao } = window;
    const center = valid ? new kakao.maps.LatLng(nLat, nLng) : new kakao.maps.LatLng(33.38, 126.55);
    const map = new kakao.maps.Map(boxRef.current, { center, level: valid ? 5 : 10 });
    const marker = new kakao.maps.Marker({ position: center, draggable: true });
    marker.setMap(map);
    const pick = (pos) => onPickRef.current(pos.getLat().toFixed(7), pos.getLng().toFixed(7));
    kakao.maps.event.addListener(map, 'click', (e) => {
      marker.setPosition(e.latLng);
      pick(e.latLng);
    });
    kakao.maps.event.addListener(marker, 'dragend', () => pick(marker.getPosition()));
    mapRef.current = map;
    markerRef.current = marker;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready]);

  // 위도·경도 칸을 직접 고치면 마커도 따라감
  useEffect(() => {
    if (!mapRef.current || !valid) return;
    const pos = new window.kakao.maps.LatLng(nLat, nLng);
    markerRef.current.setPosition(pos);
    mapRef.current.panTo(pos);
  }, [nLat, nLng, valid]);

  if (error) {
    return <p className="adm-muted">지도를 불러오지 못했어요({error}). 위도·경도를 직접 입력하세요.</p>;
  }
  return (
    <div>
      <div ref={boxRef} className="adm-map-picker" role="application" aria-label="지도를 눌러 관광지 위치 고르기" />
      <p className="adm-muted">지도를 누르거나 마커를 끌어서 위치를 정하면 위도·경도가 자동으로 바뀌어요.</p>
    </div>
  );
}