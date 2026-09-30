import client from './client.js';

/**
 * 관광지 위치·날짜별 날씨 (서버가 Open-Meteo에서 받아 1시간 보관, 오늘부터 16일까지)
 * spots: [{ latitude, longitude, date: 'YYYY-MM-DD' }]
 * → { items: [{ date, amCode, pmCode, tempMin, tempMax } | null (예보 없음)], updatedAt, notice } (보낸 순서 그대로)
 */
export async function fetchSpotWeather(spots) {
  const { data } = await client.post('/weather/spots', { spots });
  return data;
}