import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { importRoute } from '../../api/communityApi.js';
import { fetchMyTravels } from '../../api/travelApi.js';
import { showError } from '../../api/client.js';
import { formatDate } from '../../utils/format.js';
import { addDays, busyRanges } from '../../utils/travelDates.js';
import DateRangePicker from '../travel/DateRangePicker.jsx';

/**
 * [커뮤니티 글 상세] 첨부된 최종 경로 아래 버튼
 * - [경로 링크 공유]: 휴대폰은 공유 창(navigator.share), PC는 링크 복사
 * - [내 여행으로 가져오기]: 여행 이름·시작일만 입력 → 새 여행 생성(경로·여행 장소 복사) → 경로 짜기 화면으로
 *   시작일은 달력에서 고른다: 오늘부터, 원래 일수만큼의 기간에 이미 내 여행이 있으면 고를 수 없음
 *   가져온 여행은 설문이 없어서 AI 추천은 받을 수 없고, 경로는 자유롭게 고친 뒤 확정한다.
 * post: { postId, title, travelName, route: { tripDays, days }, importCount }
 */
export default function RouteShareActions({ post, likeButton }) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [travelName, setTravelName] = useState(`${post.travelName ?? '제주 여행'} 따라가기`);
  const [startDate, setStartDate] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [travels, setTravels] = useState([]); // 내 여행 (이미 여행이 있는 날 표시)

  useEffect(() => {
    if (!open) return;
    fetchMyTravels().then(setTravels).catch(() => setTravels([])); // 실패해도 서버가 겹침을 다시 검사
  }, [open]);

  const tripDays = post.route.tripDays;
  const spotCount = post.route.days.reduce((n, d) => n + d.spots.length, 0);

  const share = async () => {
    const url = `${window.location.origin}/community/posts/${post.postId}`;
    setMessage('');
    try {
      if (navigator.share) {
        await navigator.share({ title: post.title, text: `제주 여행 경로 · ${tripDays}일 ${spotCount}곳`, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      setMessage('링크를 복사했어요. 원하는 곳에 붙여 넣어 공유하세요.');
    } catch (e) {
      if (e?.name === 'AbortError') return; // 공유 창을 닫음
      window.prompt('아래 링크를 복사해 공유하세요.', url);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!travelName.trim() || !startDate) {
      setError('여행 이름과 시작일을 입력해 주세요.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const { travelId } = await importRoute(post.postId, { travelName: travelName.trim(), startDate });
      navigate(`/travels/${travelId}/route`);
    } catch (err) {
      // 기간이 겹치는 여행(409)·신고된 글 등 막힌 경우는 알림창, 입력 오류는 화면 메시지
      showError(err, setError);
      setBusy(false);
    }
  };

  return (
    <div className="route-share">
      <div className="route-share-buttons">
        <button type="button" className="cm-btn" onClick={share}>
          경로 링크 공유
        </button>
        <button type="button" className="cm-btn dark" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
          내 여행으로 가져오기
        </button>
        {likeButton}
        {post.importCount > 0 && <span className="cm-muted">{post.importCount}명이 이 경로로 여행을 만들었어요</span>}
      </div>
      {message && (
        <p className="cm-muted" role="status">
          {message}
        </p>
      )}

      {open && (
        <form className="import-form" onSubmit={submit}>
          <p className="cm-sub">
            이 경로({tripDays}일 · {spotCount}곳)로 <b>새 여행</b>을 만들어요. 날짜별 방문 순서와 여행 장소가 그대로 복사되고,
            가져온 뒤 자유롭게 고칠 수 있어요. (설문을 하지 않으므로 AI 추천은 받을 수 없어요)
          </p>
          <label className="cm-field">
            여행 이름
            <input value={travelName} maxLength={100} onChange={(e) => setTravelName(e.target.value)} />
          </label>
          <div className="cm-field">
            시작일
            <DateRangePicker
              start={startDate}
              end={startDate ? addDays(startDate, tripDays - 1) : ''}
              fixedDays={tripDays}
              busy={busyRanges(travels)}
              onChange={({ start }) => setStartDate(start)}
            />
          </div>
          {startDate && (
            <p className="cm-muted">
              여행 기간: {formatDate(startDate)} ~ {formatDate(addDays(startDate, tripDays - 1))} ({tripDays}일)
            </p>
          )}
          {error && (
            <p className="error-text" role="alert">
              {error}
            </p>
          )}
          <div className="cm-form-actions">
            <button type="button" className="cm-btn" onClick={() => setOpen(false)}>
              취소
            </button>
            <button type="submit" className="cm-btn dark" disabled={busy}>
              {busy ? '만드는 중…' : '새 여행 만들기'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
