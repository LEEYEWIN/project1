import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchFeedback, saveFeedback } from '../api/feedbackApi.js';
import { fetchTravelDetail } from '../api/travelApi.js';
import { createPost } from '../api/communityApi.js';
import { errorMessage } from '../api/client.js';
import StarRating from '../components/feedback/StarRating.jsx';
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
 */
export default function FeedbackPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [travel, setTravel] = useState(null);
  const [status, setStatus] = useState('COMPLETED');
  const [score, setScore] = useState(0);
  const [reasons, setReasons] = useState({}); // { WEATHER: '', OTHER: '가족 일정' }
  const [share, setShare] = useState(false);
  const [post, setPost] = useState({ title: '', content: '' });
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

  const validate = () => {
    if (needScore && !score) return '만족도를 선택하세요.';
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
        <p className="empty">최종 경로를 채택하고 여행 종료일이 되면 후기를 남길 수 있습니다.</p>
      </main>
    );
  }

  return (
    <main className="page">
      <h1>{travel.travelName} 다녀온 후기</h1>

      <section className="card">
        <h2>여행은 어떠셨나요?</h2>
        <div className="chips column">
          {STATUSES.map((s) => (
            <button
              key={s.value}
              type="button"
              className={status === s.value ? 'chip on' : 'chip'}
              onClick={() => setStatus(s.value)}
            >
              {s.label}
            </button>
          ))}
        </div>

        {needScore && (
          <div className="field">
            만족도
            <StarRating value={score} onChange={setScore} />
          </div>
        )}

        {needReason && (
          <div className="field">
            {status === 'PARTIAL' ? '일부만 다녀온 이유' : '가지 못한 이유'} (여러 개 선택)
            <div className="chips">
              {REASONS.map((r) => (
                <button
                  key={r.value}
                  type="button"
                  className={r.value in reasons ? 'chip on' : 'chip'}
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

      <section className="card">
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