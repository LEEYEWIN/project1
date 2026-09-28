import { useCallback, useEffect, useState } from 'react';
import { addBookmark, fetchBookmarks, removeBookmark } from '../api/bookmarkApi.js';
import { errorMessage } from '../api/client.js';

/**
 * 찜 상태 관리 훅 (추천·찜·관광지 화면 공용). travelId가 없으면 아무것도 하지 않는다.
 * - bookmarks: [{ bookmarkId, poi }]
 * - isBookmarked(poiId), toggle(poi)
 * toggle은 "낙관적 업데이트": 화면을 먼저 바꾸고, 서버가 실패하면 원래대로 되돌린다.
 * ensure(poi): 찜이 안 되어 있으면 찜한다(루트에 추가하기 전에 사용). 실패하면 예외를 그대로 던짐.
 */
export default function useBookmarks(travelId) {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(new Set()); // 요청 중인 poiId (연타 방지)

  const reload = useCallback(async () => {
    if (!travelId) {
      // 여행 없이 들어온 관광지 화면(메인 → 관광지): 찜 기능 없음
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

    setPending((s) => new Set(s).add(poi.poiId));
    setBookmarks(on ? before.filter((b) => b.poi.poiId !== poi.poiId) : [{ bookmarkId: null, poi }, ...before]);

    try {
      if (on) {
        await removeBookmark(travelId, poi.poiId);
      } else {
        const saved = await addBookmark(travelId, poi.poiId);
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
    const saved = await addBookmark(travelId, poi.poiId);
    setBookmarks((list) => (list.some((b) => b.poi.poiId === poi.poiId) ? list : [saved, ...list]));
  };

  return { bookmarks, loading, error, isBookmarked, toggle, ensure, pending, reload };
}