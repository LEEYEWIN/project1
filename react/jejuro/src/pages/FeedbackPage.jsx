import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchFeedback, saveFeedback } from '../api/feedbackApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { createPost } from '../api/communityApi.js';
import ImagePicker from '../components/common/ImagePicker.jsx';
import { errorMessage } from '../api/client.js';
import StarRating from '../components/feedback/StarRating.jsx';
import SpotChecklist from '../components/feedback/SpotChecklist.jsx';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';

const STATUSES = [
  { value: 'COMPLETED', label: '계획대로 다녀왔어요' },
  { value: 'PARTIAL', label: '일부만 다녀왔어요' },
  { value: 'NOT_TAKEN', label: '여행을 가지 못했어요' },
];

const REASONS = [
  { value: 'TIME_SHORTAGE', label: '시간 부족' },
  { value: 'CHANGE_OF_MIND', label: '계획·마음 변경' },
  { value: 'PERSONAL_REASON', label: '개인 사정' },
  { value: 'WEATHER', label: '날씨' },
  { value: 'POI_ISSUE', label: '관광지 사정(휴무 등)' },
  { value: 'OTHER', label: '기타' },
];

/**
 * 8페이지: 다녀온 후 후기
 * ① 수행 결과·만족도·이유 → TRAVEL_FEEDBACK (PUT, 다시 저장하면 수정)
 * ② (선택) 커뮤니티 후기 글 → COMMUNITY_POST
 * 입력 규칙은 서버와 같다: COMPLETED=별점만 / PARTIAL=별점+이유 / NOT_TAKEN=이유만
 * 관광지별 결과(TRAVEL_FEEDBACK_SPOT): 일부만 다녀왔으면 못 간 곳을 1곳 이상 표시, 간 곳은 좋았어요/아쉬워요(선택)
 */
export default function FeedbackPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [travel, setTravel] = useState(null);
  const [status, setStatus] = useState('COMPLETED');
  const [score, setScore] = useState(0);
  const [reasons, setReasons] = useState({}); // { WEATHER: '', OTHER: '가족 일정' }
  const [spots, setSpots] = useState({}); // { [poiId]: { visited, reaction } }
  const [rateEach, setRateEach] = useState(false); // 계획대로 다녀왔을 때 관광지별 평가 펼치기
  const [share, setShare] = useState(false);
  const [post, setPost] = useState({ title: '', content: '', imageUrl: null });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  // 여행 정보 + 기존 피드백(수정일 때) 불러오기
  useEffect(() => {
    Promise.all([fetchTravelDetail(travelId), fetchFeedback(travelId)])
      .then(([t, fb]) => {
        setTravel(t);
        setPost((p) => ({ ...p, title: `${t.travelName} 후기` }));
        if (fb) {
          setStatus(fb.executionStatus);
          setScore(fb.satisfactionScore ?? 0);
          setReasons(Object.fromEntries(fb.reasons.map((r) => [r.reasonCode, r.reasonText ?? ''])));
          setSpots(Object.fromEntries((fb.spots ?? []).map((sp) => [sp.poiId, { visited: sp.visited, reaction: sp.reaction }])));
          if ((fb.spots ?? []).some((sp) => sp.reaction)) setRateEach(true);
        }
      })
      .catch((e) => setError(errorMessage(e)));
  }, [travelId]);

  const needScore = status !== 'NOT_TAKEN';
  const needReason = status !== 'COMPLETED';

  const toggleReason = (code) =>
    setReasons((prev) => {
      const next = { ...prev };
      if (code in next) delete next[code];
      else next[code] = '';
      return next;
    });

  // 확정 일정의 관광지 전체 (기본값: 갔어요)
  const plannedIds = (travel?.adoptedRoute?.days ?? []).flatMap((d) => d.spots.map((sp) => sp.poi.poiId));
  const spotPayload = () => {
    if (status === 'NOT_TAKEN') return [];
    // 관광지별 평가 패널을 접어도(rateEach=false) 이미 고른 평가는 그대로 보낸다.
    // 빈 배열을 보내면 서버가 모든 곳을 "갔어요·반응 없음"으로 덮어써 저장된 평가가 지워진다.
    return plannedIds.map((poiId) => {
      const v = spots[poiId] ?? { visited: true, reaction: null };
      const visited = status === 'COMPLETED' ? true : v.visited !== false;
      return { poiId, visited, reaction: visited ? v.reaction ?? null : null };
    });
  };

  const validate = () => {
    if (needScore && !score) return '만족도를 선택하세요.';
    if (status === 'PARTIAL' && !spotPayload().some((sp) => !sp.visited)) return '못 간 곳을 하나 이상 눌러 주세요.';
    if (needReason && Object.keys(reasons).length === 0) return '이유를 하나 이상 고르세요.';
    if ('OTHER' in reasons && !reasons.OTHER.trim()) return '기타 이유를 입력하세요.';
    if (share && (!post.title.trim() || !post.content.trim())) return '후기 글의 제목과 내용을 입력하세요.';
    return '';
  };

  const submit = async () => {
    const msg = validate();
    setError(msg);
    if (msg) return;
    setSaving(true);
    try {
      await saveFeedback(travelId, {
        executionStatus: status,
        satisfactionScore: needScore ? score : null,
        reasons: needReason
          ? Object.entries(reasons).map(([reasonCode, reasonText]) => ({ reasonCode, reasonText: reasonText || null }))
          : [],
        spots: spotPayload(),
      });
    } catch (e) {
      // 후기 저장 자체가 실패 → 이 화면에 머물며 오류 표시
      setError(errorMessage(e));
      setSaving(false);
      return;
    }

    if (share) {
      try {
        // travelId를 함께 보내면 게시판에 이 여행의 최종 경로가 같이 표시된다
        await createPost({
          postType: 'REVIEW',
          title: post.title.trim(),
          content: post.content.trim(),
          travelId: Number(travelId),
          imageUrl: post.imageUrl,
        });
      } catch (e) {
        // 후기는 이미 저장됨 → 알려주고 그대로 이동
        window.alert(`후기는 저장했지만 게시판 글은 올리지 못했습니다.\n${errorMessage(e)}`);
      }
    }
    navigate(`/travels/${travelId}`);
  };

  if (error && !travel) return <main className="page"><ErrorBox message={error} /></main>;
  if (!travel) return <main className="page"><Loading /></main>;
  if (!travel.canWriteFeedback) {
    return (
      <main className="page">
        <p className="empty">일정을 확정하고 여행 종료일이 되면 후기를 남길 수 있습니다.</p>
      </main>
    );
  }

  return (
    <main className="page feedback-page">
      <h1>{travel.travelName} 다녀온 후기</h1>

      <section className="card feedback-main">
        <div className="feedback-section feedback-status-section">
          <h2>여행은 어떠셨나요?</h2>
          <p>이번 여행의 결과를 선택해 주세요.</p>
          <div className="chips column">
          {STATUSES.map((s) => (
            <button
              key={s.value}
              type="button"
              className={status === s.value ? 'chip on' : 'chip'}
              aria-pressed={status === s.value}
              onClick={() => setStatus(s.value)}
            >
              {s.label}
            </button>
          ))}
          </div>
        </div>

        {needScore && (
          <div className="feedback-section feedback-rating-section">
            <h2>여행 만족도</h2>
            <p>별을 눌러 여행 전체의 만족도를 알려 주세요.</p>
            <StarRating value={score} onChange={setScore} />
            <span className="feedback-score-caption">{score ? `${score}점 선택됨` : '별점을 선택해 주세요'}</span>
          </div>
        )}

        {status === 'PARTIAL' && travel.adoptedRoute && (
          <div className="feedback-section feedback-spot-section">
            <h2>관광지별 방문 기록</h2>
            <p>못 간 곳을 눌러 주세요. 다녀온 곳은 좋았어요·아쉬워요도 선택할 수 있어요.</p>
            <SpotChecklist route={travel.adoptedRoute} value={spots} onChange={setSpots} canMiss />
          </div>
        )}

        {status === 'COMPLETED' && travel.adoptedRoute && (
          <div className="feedback-section feedback-spot-section">
            <button type="button" className="feedback-spot-toggle" aria-expanded={rateEach} onClick={() => setRateEach((v) => !v)}>
              <span>관광지별 평가 <small>선택 사항</small></span>
              <span>{rateEach ? '접기 ▲' : '평가하기 ▼'}</span>
            </button>
            {rateEach && <SpotChecklist route={travel.adoptedRoute} value={spots} onChange={setSpots} canMiss={false} />}
          </div>
        )}

        {needReason && (
          <div className="field feedback-reasons-section">
            {status === 'PARTIAL' ? '일부만 다녀온 이유' : '가지 못한 이유'} (여러 개 선택)
            <div className="chips">
              {REASONS.map((r) => (
                <button
                  key={r.value}
                  type="button"
                  className={r.value in reasons ? 'chip on' : 'chip'}
                  aria-pressed={r.value in reasons}
                  onClick={() => toggleReason(r.value)}
                >
                  {r.label}
                </button>
              ))}
            </div>
            {'OTHER' in reasons && (
              <input
                type="text"
                maxLength={50}
                placeholder="기타 이유 (50자 이내)"
                value={reasons.OTHER}
                onChange={(e) => setReasons({ ...reasons, OTHER: e.target.value })}
              />
            )}
          </div>
        )}
      </section>

      <section className="card feedback-share-card">
        <label className="check">
          <input type="checkbox" checked={share} onChange={(e) => setShare(e.target.checked)} />
          후기 게시판에도 글 올리기 <small className="muted">(최종 경로가 함께 공개됩니다)</small>
        </label>
        {share && (
          <>
            <label className="field">
              제목
              <input
                type="text"
                maxLength={200}
                value={post.title}
                onChange={(e) => setPost({ ...post, title: e.target.value })}
              />
            </label>
            <label className="field">
              내용
              <textarea
                rows={6}
                value={post.content}
                placeholder="좋았던 곳, 아쉬웠던 점, 다음 여행자에게 팁을 남겨 주세요."
                onChange={(e) => setPost({ ...post, content: e.target.value })}
              />
            </label>
            <div className="field">
              사진 (선택)
              <ImagePicker value={post.imageUrl} onChange={(imageUrl) => setPost((p) => ({ ...p, imageUrl }))} />
            </div>
          </>
        )}
      </section>

      <ErrorBox message={error} />
      <div className="actions">
        <button type="button" className="btn ghost" onClick={() => navigate(`/travels/${travelId}`)}>
          취소
        </button>
        <button type="button" className="btn primary" onClick={submit} disabled={saving}>
          {saving ? '저장 중…' : '후기 저장'}
        </button>
      </div>
    </main>
  );
}
