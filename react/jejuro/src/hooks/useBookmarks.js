import { useCallback, useEffect, useState } from 'react';
import { addBookmark, fetchBookmarks, removeBookmark } from '../api/bookmarkApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { errorMessage, showError } from '../api/client.js';
import { warnClosedDays } from '../utils/closedDay.js';

/**
 * 여행 장소(예전 이름: 찜) 상태 관리 훅 (추천·여행 장소·관광지 화면 공용). travelId가 없으면 아무것도 하지 않는다.
 * 여행 장소 = 이 여행의 경로(일정)에 넣을 관광지.
 * - bookmarks: [{ bookmarkId, poi }]
 * - isBookmarked(poiId), toggle(poi)
 * toggle은 "낙관적 업데이트": 화면을 먼저 바꾸고, 서버가 실패하면 원래대로 되돌린다.
 * ensure(poi): 여행 장소에 없으면 추가한다(루트에 추가하기 전에 사용). 실패하면 예외를 그대로 던짐.
 * source: 이 화면에서 담으면 서버에 남길 출처 — 'RECOMMEND'(AI 추천 목록) / 'SEARCH'(기본)
 * locked: 수정이 잠긴 여행이면 true → 장소 추가·빼기 버튼을 모두 비활성화 (서버도 409로 막음)
 *   - 확정한 일정은 출발 전날까지 장소를 바꿀 수 있고, 출발일 당일부터 잠김
 *   - "날짜·동행 바꿔 다시 만들기"로 새 여행을 만드는 중인 변경 전 여행도 잠김
 * lockMessage: 잠긴 이유 안내 문구
 * confirmed: 일정을 확정한 여행 → 새로 담은 장소는 경로에 배치해야 확정 일정에 들어간다고 알림
 * 자연관광지가 아닌 곳을 담으면 "휴무일을 확인한 뒤 방문하세요" 경고창을 먼저 띄운다.
 */
export default function useBookmarks(travelId, source = 'SEARCH') {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(new Set()); // 요청 중인 poiId (연타 방지)
  const [locked, setLocked] = useState(false); // 수정 잠금 여부
  const [lockMessage, setLockMessage] = useState('');
  const [confirmed, setConfirmed] = useState(false); // 일정 확정 여부

  const reload = useCallback(async () => {
    if (!travelId) {
      // 여행 없이 들어온 관광지 화면(메인 → 관광지): 장소 추가 기능 없음
      setLoading(false);
      return;
    }
    setLoading(true);
    // 확정 여부는 실패해도 목록은 보여 준다 (서버가 확정 여행 추가를 409로 막음)
    fetchTravelDetail(travelId)
      .then((t) => {
        setLocked(Boolean(t?.editLocked));
        setLockMessage(lockText(t));
        setConfirmed(Boolean(t?.adoptedRoute));
      })
      .catch(() => {});
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
    if (locked) {
      setError(lockMessage);
      return;
    }
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
        if (confirmed) window.alert(UNPLACED_WARNING);
      }
      setError('');
    } catch (e) {
      setBookmarks(before); // 실패 → 되돌리기
      showError(e, setError); // 확정된 여행·숨긴 관광지·이미 담음 등은 알림창
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

  return { bookmarks, loading, error, isBookmarked, toggle, ensure, pending, reload, locked, lockMessage, confirmed };
}

/** 확정한 여행에 장소를 새로 담았을 때 경고 */
export const UNPLACED_WARNING =
  '확정한 일정에 새로 담은 장소예요. 경로 짜기에서 날짜별로 배치해야 확정 일정에 들어가요.\n출발 전까지 배치하지 않으면 확정 일정에서 빠집니다.';

/** 수정이 잠긴 이유 (여행 상세 응답 기준) */
export function lockText(t) {
  if (!t?.editLocked) return '';
  if ((t.overlaps ?? []).some((o) => o.newer)) {
    return '새 여행으로 바꿔 만드는 중이라 이 여행은 변경 전 일정으로 보기만 할 수 있어요.';
  }
  return '출발일부터는 확정한 일정을 바꿀 수 없어요. 달라진 일정은 여행 후 후기에 남겨 주세요.';
}