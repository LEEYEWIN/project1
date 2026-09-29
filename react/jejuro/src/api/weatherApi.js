import client from './client.js';

/**
 * 제주 날씨 예보 (오늘부터 16일, 서버가 Open-Meteo에서 받아 1시간 보관)
 * → { days: [{ date: 'YYYY-MM-DD', code, tempMax, tempMin, rainChance }], updatedAt, notice }
 */
export async function fetchJejuWeather() {
  const { data } = await client.get('/weather/jeju');
  return data;
}