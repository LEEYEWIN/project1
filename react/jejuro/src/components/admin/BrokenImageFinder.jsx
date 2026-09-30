import { useRef, useState } from 'react';
import { fetchPoiImages } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';

const PARALLEL = 8;        // 한 번에 불러 볼 사진 수
const TIMEOUT_MS = 12000;  // 이 시간 안에 안 뜨면 깨진 것으로 봄

/** 사진 한 장을 브라우저에서 실제로 불러 본다 → 'ok' | 'error' | 'timeout' */
function probe(url) {
  return new Promise((resolve) => {
    const img = new window.Image();
    img.referrerPolicy = 'no-referrer'; // 화면 사진과 같은 조건 (블로그 사진의 외부 사이트 차단 피하기)
    const timer = setTimeout(() => {
      img.src = '';
      resolve('timeout');
    }, TIMEOUT_MS);
    img.onload = () => {
      clearTimeout(timer);
      resolve(img.naturalWidth > 0 ? 'ok' : 'error');
    };
    img.onerror = () => {
      clearTimeout(timer);
      resolve('error');
    };
    img.src = url;
  });
}

const host = (url) => {
  try {
    return new URL(url, window.location.origin).host;
  } catch {
    return url;
  }
};

/**
 * 관리자: 깨진 관광지 사진 찾기
 * - 사진 주소가 있는 관광지를 모두 받아 브라우저에서 한 장씩 불러 본다 (서버는 외부 사이트에 요청하지 않음)
 * - 안 뜨는 사진(주소 만료·다른 사이트에서 막음·파일 없음)만 모아 보여 주고 [사진 바꾸기]로 바로 편집
 * onEdit(poiId): 편집 창 열기
 */
export default function BrokenImageFinder({ onEdit }) {
  const [state, setState] = useState('idle'); // idle | running | done
  const [progress, setProgress] = useState({ done: 0, total: 0 });
  const [broken, setBroken] = useState([]);
  const [error, setError] = useState('');
  const stopRef = useRef(false);

  const run = async () => {
    setError('');
    setBroken([]);
    stopRef.current = false;
    let list;
    try {
      list = await fetchPoiImages();
    } catch (e) {
      setError(errorMessage(e));
      return;
    }
    setState('running');
    setProgress({ done: 0, total: list.length });
    let next = 0;
    let done = 0;
    const found = [];
    const worker = async () => {
      while (next < list.length && !stopRef.current) {
        const row = list[next++];
        const result = await probe(row.imageUrl);
        done += 1;
        if (result !== 'ok') {
          found.push({ ...row, result });
          setBroken([...found].sort((a, b) => a.poiId - b.poiId));
        }
        setProgress({ done, total: list.length });
      }
    };
    await Promise.all(Array.from({ length: PARALLEL }, worker));
    setState('done');
  };

  return (
    <section className="adm-card adm-imgcheck" aria-live="polite">
      <div className="adm-imgcheck-head">
        <div>
          <b>깨진 사진 찾기</b>
          <p className="adm-muted">
            관광지 사진을 이 브라우저에서 한 장씩 불러 봐서 안 뜨는 곳만 모아요. 블로그·검색 썸네일 같은 외부 사진은 주소가 만료되거나 다른
            사이트에서 쓰지 못하게 막혀 깨질 수 있어요.
          </p>
        </div>
        {state === 'running' ? (
          <button type="button" className="adm-btn" onClick={() => { stopRef.current = true; }}>
            멈추기
          </button>
        ) : (
          <button type="button" className="adm-btn dark" onClick={run}>
            {state === 'done' ? '다시 찾기' : '🔍 깨진 사진 찾기'}
          </button>
        )}
      </div>

      {error && <p className="adm-error">{error}</p>}
      {state !== 'idle' && (
        <p className="adm-imgcheck-progress">
          {state === 'running' ? '확인 중' : '확인 끝'} · {progress.done}/{progress.total}장 · 깨진 사진 <b className="num">{broken.length}</b>곳
          <progress max={progress.total || 1} value={progress.done} />
        </p>
      )}

      {broken.length > 0 && (
        <div className="adm-scroll">
          <table className="adm-tbl">
            <thead>
              <tr>
                <th>관광지</th>
                <th>사진 주소</th>
                <th>상태</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {broken.map((b) => (
                <tr key={b.poiId}>
                  <td>
                    <b>{b.poiName}</b> <span className="adm-muted">#{b.poiId}</span>
                    {b.hidden && <span className="adm-badge">숨김</span>}
                  </td>
                  <td className="adm-url">
                    <span className="adm-muted">{host(b.imageUrl)}</span>
                    <br />
                    <a href={b.imageUrl} target="_blank" rel="noreferrer">
                      주소 열어 보기
                    </a>
                  </td>
                  <td>
                    <span className="adm-badge bad">{b.result === 'timeout' ? '응답 없음' : '불러오기 실패'}</span>
                  </td>
                  <td className="r">
                    <button type="button" className="adm-btn small" onClick={() => onEdit(b.poiId)}>
                      사진 바꾸기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {state === 'done' && broken.length === 0 && !error && <p className="adm-muted">깨진 사진이 없어요.</p>}
    </section>
  );
}