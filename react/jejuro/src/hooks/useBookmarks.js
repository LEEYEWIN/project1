import { useCallback, useEffect, useState } from 'react';
import { addBookmark, fetchBookmarks, removeBookmark } from '../api/bookmarkApi.js';
import { errorMessage } from '../api/client.js';
import { warnClosedDays } from '../utils/closedDay.js';

/**
 * 여행 장소(예전 이름: 찜) 상태 관리 훅 (추천·여행 장소·관광지 화면 공용). travelId가 없으면 아무것도 하지 않는다.
 * 여행 장소 = 이 여행의 경로(일정)에 넣을 관광지.
 * - bookmarks: [{ bookmarkId, poi }]
 * - isBookmarked(poiId), toggle(poi)
 * toggle은 "낙관적 업데이트": 화면을 먼저 바꾸고, 서버가 실패하면 원래대로 되돌린다.
 * ensure(poi): 여행 장소에 없으면 추가한다(루트에 추가하기 전에 사용). 실패하면 예외를 그대로 던짐.
 * source: 이 화면에서 담으면 서버에 남길 출처 — 'RECOMMEND'(AI 추천 목록) / 'SEARCH'(기본)
 * 자연관광지가 아닌 곳을 담으면 "휴무일을 확인한 뒤 방문하세요" 경고창을 먼저 띄운다.
 */
export default function useBookmarks(travelId, source = 'SEARCH') {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(new Set()); // 요청 중인 poiId (연타 방지)

  const reload = useCallback(async () => {
    if (!travelId) {
      // 여행 없이 들어온 관광지 화면(메인 → 관광지): 장소 추가 기능 없음
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setBookmarks(await fetchBookmarks(travelId));
      setError('');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [travelId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const isBookmarked = (poiId) => bookmarks.some((b) => b.poi.poiId === poiId);

  const toggle = async (poi) => {
    if (pending.has(poi.poiId)) return;
    const before = bookmarks;
    const on = isBookmarked(poi.poiId);
    if (!on) warnClosedDays(poi);

    setPending((s) => new Set(s).add(poi.poiId));
    setBookmarks(on ? before.filter((b) => b.poi.poiId !== poi.poiId) : [{ bookmarkId: null, poi }, ...before]);

    try {
      if (on) {
        await removeBookmark(travelId, poi.poiId);
      } else {
        const saved = await addBookmark(travelId, poi.poiId, source);
        setBookmarks((list) => list.map((b) => (b.poi.poiId === poi.poiId ? saved : b)));
      }
      setError('');
    } catch (e) {
      setBookmarks(before); // 실패 → 되돌리기
      setError(errorMessage(e));
    } finally {
      setPending((s) => {
        const next = new Set(s);
        next.delete(poi.poiId);
        return next;
      });
    }
  };

  const ensure = async (poi) => {
    if (isBookmarked(poi.poiId)) return;
    warnClosedDays(poi);
    const saved = await addBookmark(travelId, poi.poiId, source);
    setBookmarks((list) => (list.some((b) => b.poi.poiId === poi.poiId) ? list : [saved, ...list]));
  };

  return { bookmarks, loading, error, isBookmarked, toggle, ensure, pending, reload };
}