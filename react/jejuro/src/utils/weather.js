/**
 * WMO 날씨 코드 → 이모지·이름 (Open-Meteo weather_code)
 * 0 맑음 / 1·2 구름 조금 / 3 흐림 / 45·48 안개 / 51~57 이슬비 / 61~67 비 / 71~77 눈 / 80~82 소나기 / 85·86 눈 소나기 / 95~99 뇌우
 */
export function weatherInfo(code) {
  if (code === 0) return { emoji: '☀️', label: '맑음' };
  if (code === 1 || code === 2) return { emoji: '🌤️', label: '구름 조금' };
  if (code === 3) return { emoji: '☁️', label: '흐림' };
  if (code === 45 || code === 48) return { emoji: '🌫️', label: '안개' };
  if (code >= 51 && code <= 57) return { emoji: '🌦️', label: '이슬비' };
  if (code >= 61 && code <= 67) return { emoji: '🌧️', label: '비' };
  if (code >= 71 && code <= 77) return { emoji: '❄️', label: '눈' };
  if (code >= 80 && code <= 82) return { emoji: '🌧️', label: '소나기' };
  if (code === 85 || code === 86) return { emoji: '🌨️', label: '눈 소나기' };
  if (code >= 95) return { emoji: '⛈️', label: '뇌우' };
  return { emoji: '🌡️', label: '날씨' };
}

/** 오전/오후 날씨 한 줄 설명: "오전 맑음 · 오후 비 · 최저 18° / 최고 24°" */
export function spotWeatherText(w) {
  const am = w.amCode != null ? `오전 ${weatherInfo(w.amCode).label}` : '';
  const pm = w.pmCode != null ? `오후 ${weatherInfo(w.pmCode).label}` : '';
  const temp = w.tempMin != null && w.tempMax != null ? `최저 ${Math.round(w.tempMin)}° / 최고 ${Math.round(w.tempMax)}°` : '';
  return [am, pm, temp].filter(Boolean).join(' · ');
}