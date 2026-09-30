import { useEffect, useState } from 'react';
import { fetchSpotWeather } from '../../api/weatherApi.js';
import { spotWeatherText, weatherInfo } from '../../utils/weather.js';

/**
 * 확정한 일정·지금 경로의 관광지별 날씨
 * - 관광지 좌표 + 그 관광지를 가는 날짜(일차 날짜)로 서버에 한 번에 요청 (Open-Meteo, 지난 31일 ~ 앞으로 16일)
 * - 범위 밖 날짜(32일 전·16일 뒤)나 실패하면 아무것도 표시하지 않는다
 * route: RouteDetailResponse { days: [{ dayNo, date, spots: [{ poi }] }] } (null이면 요청 안 함)
 * → Map('일차-관광지번호' → { amCode, pmCode, tempMin, tempMax })
 */
export function useRouteWeather(route) {
  const [weather, setWeather] = useState(() => new Map());

  useEffect(() => {
    if (!route) return undefined;
    const keys = [];
    const spots = [];
    route.days.forEach((d) =>
      d.spots.forEach((s) => {
        if (s.poi.unavailable || s.poi.latitude == null || s.poi.longitude == null || !d.date) return;
        keys.push(`${d.dayNo}-${s.poi.poiId}`);
        spots.push({ latitude: Number(s.poi.latitude), longitude: Number(s.poi.longitude), date: d.date });
      }),
    );
    if (spots.length === 0) return undefined;
    let alive = true;
    fetchSpotWeather(spots.slice(0, 80))
      .then((res) => {
        if (!alive) return;
        const map = new Map();
        res.items.forEach((w, i) => w && map.set(keys[i], w));
        setWeather(map);
      })
      .catch(() => {}); // 날씨는 부가 정보 → 실패해도 일정은 그대로
    return () => {
      alive = false;
    };
  }, [route]);

  return weather;
}

/** 관광지 이름 옆: "오전 ☀️ 오후 🌧️ 18°/24°" */
export default function SpotWeather({ weather: w }) {
  if (!w) return null;
  const text = spotWeatherText(w);
  return (
    <span className="spot-weather" title={text} aria-label={`날씨: ${text}`}>
      {w.amCode != null && (
        <span className="sw-part">
          <small>오전</small>
          {weatherInfo(w.amCode).emoji}
        </span>
      )}
      {w.pmCode != null && (
        <span className="sw-part">
          <small>오후</small>
          {weatherInfo(w.pmCode).emoji}
        </span>
      )}
      {w.tempMin != null && w.tempMax != null && (
        <span className="sw-temp">
          <span className="lo">{Math.round(w.tempMin)}°</span>/<span className="hi">{Math.round(w.tempMax)}°</span>
        </span>
      )}
    </span>
  );
}