import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { requestRecommendations } from '../api/recommendApi.js';
import { errorMessage } from '../api/client.js';
import { saveRecommendation } from '../utils/recommendStorage.js';

const MESSAGES = [
  '여행 취향을 분석하고 있어요',
  '비슷한 여행자들의 코스를 살펴보는 중',
  '선택한 권역의 관광지를 고르는 중',
  '추천 목록을 정리하고 있어요',
];

/**
 * 2페이지: AI 추천 중
 * 들어오자마자 추천 API를 호출하고, 기다리는 동안 문구가 바뀌는 로딩 화면을 보여준다.
 * 성공하면 결과를 sessionStorage에 저장하고 3페이지로 이동한다.
 */
export default function RecommendingPage() {
  const { travelId } = useParams();
  const navigate = useNavigate();
  const [msgIndex, setMsgIndex] = useState(0);
  const [error, setError] = useState('');
  const started = useRef(false); // 개발 모드(StrictMode)에서 useEffect가 두 번 실행돼도 AI는 한 번만 호출

  const run = async () => {
    setError('');
    try {
      const { pois } = await requestRecommendations(travelId);
      saveRecommendation(travelId, pois);
      navigate(`/travels/${travelId}/recommendations`, { replace: true, state: { pois } });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    run();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [travelId]);

  // 2초마다 로딩 문구 교체
  useEffect(() => {
    if (error) return undefined;
    const timer = setInterval(() => setMsgIndex((i) => (i + 1) % MESSAGES.length), 2000);
    return () => clearInterval(timer);
  }, [error]);

  return (
    <main className="page center">
      {error ? (
        <div className="card">
          <h1>추천을 받지 못했어요</h1>
          <p className="error">{error}</p>
          <div className="actions">
            <button type="button" className="btn ghost" onClick={() => navigate('/travels')}>
              내 여행으로
            </button>
            <button type="button" className="btn primary" onClick={run}>
              다시 시도
            </button>
          </div>
        </div>
      ) : (
        <div className="ai-loading">
          <div className="orbit" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <h1>AI가 여행지를 고르고 있어요</h1>
          <p key={msgIndex} className="fade">
            {MESSAGES[msgIndex]}…
          </p>
        </div>
      )}
    </main>
  );
}