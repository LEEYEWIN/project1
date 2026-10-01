import { useEffect, useState } from 'react';
import { fetchSpotWeather } from '../../api/weatherApi.js';
import { spotWeatherText, weatherInfo } from '../../utils/weather.js';

/**
 * 확정한 일정·지금 경로의 관광지별 날씨
 * - 관광지 좌표 + 그 관광지를 가는 날짜(일차 날짜)로 서버에 한 번에 요청 (Open-Meteo, 지난 31일 ~ 앞으로 16일)
 * - 범위 밖 날짜는 일차 제목 옆 DayWeatherNote가 안내한다 (16일 뒤: "M/D부터 날씨를 볼 수 있어요", 31일 전: "지난 날씨 정보가 없어요")
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

/** 날씨를 받을 수 있는 범위: 오늘 기준 지난 31일 ~ 앞으로 15일(오늘 포함 16일) — 서버 WeatherService와 같게 */
const PAST_DAYS = 31;
const FORECAST_DAYS = 16;

/** 'YYYY-MM-DD' → 그날 0시(내 PC 시간) */
function toDay(ymd) {
  const [y, m, d] = String(ymd).slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

function addDays(day, n) {
  const x = new Date(day);
  x.setDate(x.getDate() + n);
  return x;
}

/**
 * 일차 제목 옆 안내 (범위 안이면 아무것도 표시하지 않음 → 관광지 옆 SpotWeather가 날씨를 보여 줌)
 * - 16일 뒤 일정: "🌤 10/2부터 날씨를 볼 수 있어요" (그 날짜 = 일차 날짜 − 15일)
 * - 31일보다 전에 다녀온 일정: "지난 날씨 정보가 없어요"
 */
export function DayWeatherNote({ date }) {
  if (!date) return null;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const day = toDay(date);
  if (day > addDays(today, FORECAST_DAYS - 1)) {
    const from = addDays(day, -(FORECAST_DAYS - 1));
    return <span className="day-weather-note">🌤 {from.getMonth() + 1}/{from.getDate()}부터 날씨를 볼 수 있어요</span>;
  }
  if (day < addDays(today, -PAST_DAYS)) {
    return <span className="day-weather-note">지난 날씨 정보가 없어요</span>;
  }
  return null;
}