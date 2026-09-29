import { useEffect, useState } from 'react';
import { downloadFunnelDropsCsv, fetchFunnelDrops } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';

/**
 * 관리자 KPI: 사용자 여정 퍼널 (VIEW TRAVEL_FUNNEL)
 * ① 단계별 도달 (가로 퍼널, 항상 보임)
 * ② 단계 사이 이탈 분해 + ③ 이탈 로그 → [이탈 분석 열기] 토글 안 (열 때 이탈 로그를 불러옴)
 *
 * 확정 이탈률 = 이탈 확정 ÷ (다음 단계로 감 + 이탈 확정) — 아직 진행 중인 여행은 빼고 계산
 * 이탈 확정 기준: 일정 확정 전 단계는 여행 종료일이 지남 / 후기는 종료일 + 14일이 지남
 */

const pct = (v) => (v == null || Number.isNaN(v) ? '—' : `${(v * 100).toFixed(1)}%`);
const num = (v) => (v == null ? '—' : Number(v).toLocaleString());

/** 확정 이탈률 (결과가 정해진 여행만) */
const confirmedDrop = (s) => (s.count + s.dropped === 0 ? null : s.dropped / (s.count + s.dropped));

export default function FunnelPanel({ steps, days }) {
  const [open, setOpen] = useState(false);
  const main = steps.filter((s) => s.key !== 'SHARED'); // 커뮤니티 공유는 선택 단계
  const shared = steps.find((s) => s.key === 'SHARED');
  const first = main[0]?.count ?? 0;
  const worst = main
    .slice(1)
    .reduce((w, s) => ((confirmedDrop(s) ?? -1) > (w ? confirmedDrop(w) ?? -1 : -1) ? s : w), null);
  const worstKey = worst && confirmedDrop(worst) > 0 ? worst.key : null;

  return (
    <section className="adm-card" aria-labelledby="funnel-title">
      <div className="adm-card-head">
        <h2 id="funnel-title">
          사용자 여정 퍼널 <span className="adm-muted">기간 안에 만든 여행 {num(first)}건 · 막대 = 그 단계까지 온 여행 수</span>
        </h2>
        {shared && (
          <span className="adm-muted">
            커뮤니티 공유 {num(shared.count)}건 (선택 단계)
          </span>
        )}
      </div>

      {first === 0 ? (
        <p className="adm-empty">이 기간에 만든 여행이 없어요.</p>
      ) : (
        <>
          {/* ① 가로 퍼널 */}
          <div className="fn-chart" style={{ gridTemplateColumns: `repeat(${main.length}, minmax(0, 1fr))` }}>
            {main.map((s) => (
              <div key={s.key} className="fn-stage">
                <div className="fn-val num">{num(s.count)}</div>
                <div className="fn-pct num">{pct(s.count / first)}</div>
                <div className="fn-barbox">
                  <div
                    className="fn-bar"
                    style={{ height: `${Math.max((s.count / first) * 100, 1)}%` }}
                    title={`${s.label} ${s.count}건 (시작 대비 ${pct(s.count / first)})`}
                  />
                </div>
                <div className="fn-name">{s.label}</div>
              </div>
            ))}
          </div>
          <div className="fn-gaps" style={{ gridTemplateColumns: `repeat(${main.length}, minmax(0, 1fr))` }}>
            {main.slice(1).map((s) => (
              <div key={s.key} className={s.key === worstKey ? 'fn-gap worst' : 'fn-gap'}>
                <span>
                  → 확정 이탈 <b className="num">{pct(confirmedDrop(s))}</b>
                  <br />
                  <span className="adm-muted">
                    이탈 {num(s.dropped)} · 진행 중 {num(s.waiting)}
                  </span>
                </span>
              </div>
            ))}
          </div>

          <button
            type="button"
            className="fn-toggle"
            aria-expanded={open}
            aria-controls="funnel-analysis"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? '이탈 분석 닫기 ▴' : '이탈 분석 열기 ▾'}
            <span className="adm-muted"> 단계 사이 분해 · 이탈 로그</span>
          </button>

          {open && (
            <div id="funnel-analysis" className="fn-analysis">
              <Breakdown main={main} worstKey={worstKey} />
              <DropLog key={days} days={days} main={main} />
            </div>
          )}
        </>
      )}
    </section>
  );
}

/** ② 단계 사이 이탈 분해: 앞 단계에 도착한 여행 = 다음 단계로 감 + 이탈 확정 + 진행 중 */
function Breakdown({ main, worstKey }) {
  return (
    <div>
      <h3>
        ② 단계 사이 이탈 분해 <span className="adm-muted">앞 단계에 도착한 여행이 어떻게 됐나</span>
      </h3>
      <div className="fn-legend">
        <span>
          <i className="s-pass" />
          다음 단계로 감
        </span>
        <span>
          <i className="s-drop" />
          이탈 확정 (기한이 지났는데 못 감)
        </span>
        <span>
          <i className="s-wait" />
          진행 중 (기한 전 → 판단 보류)
        </span>
      </div>
      <div className="fn-rows">
        {main.slice(1).map((s, i) => {
          const prev = main[i];
          return (
            <div key={s.key} className={s.key === worstKey ? 'fn-row worst' : 'fn-row'}>
              <div>
                <b>
                  {prev.label} → {s.label}
                </b>
                <span className="adm-muted num">도착 {num(prev.count)}건</span>
              </div>
              <div className="fn-stack" role="img" aria-label={`다음 단계 ${s.count}, 이탈 ${s.dropped}, 진행 중 ${s.waiting}`}>
                {s.count > 0 && <span className="s-pass" style={{ flexGrow: s.count }} title={`다음 단계로 감 ${s.count}건`} />}
                {s.dropped > 0 && <span className="s-drop" style={{ flexGrow: s.dropped }} title={`이탈 확정 ${s.dropped}건`} />}
                {s.waiting > 0 && <span className="s-wait" style={{ flexGrow: s.waiting }} title={`진행 중 ${s.waiting}건`} />}
              </div>
              <div className="fn-rate">
                <div>
                  <span className="num">{pct(prev.count ? s.count / prev.count : null)}</span>
                  <small>단순 전환율</small>
                </div>
                <div>
                  <b className="num">{pct(confirmedDrop(s))}</b>
                  <small>확정 이탈률</small>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <p className="adm-note">
        <b>확정 이탈률 = 이탈 확정 ÷ (다음 단계로 감 + 이탈 확정)</b> — 결과가 정해진 여행만으로 계산해 아직 준비 중인 여행 때문에
        이탈이 부풀려지지 않습니다. <b>이탈 확정 기준</b>: 일정 확정 전 단계는 여행 종료일이 지나면, 후기는 종료일 + 14일이 지나면.
      </p>
    </div>
  );
}

/** 근거 문구: 멈춘 단계에서 DB로 바로 확인되는 사실만 */
function evidence(d) {
  switch (d.dropStep) {
    case 'SURVEYED':
      return '설문 답변 없음';
    case 'RECOMMENDED':
      return `AI 추천 ${d.recommendCount}회 · 담은 장소 ${d.placeCount}곳`;
    case 'PLACED_ANY':
      return `AI 추천 ${d.recommendCount}회(${d.shownCount}곳 노출) · 담은 장소 0곳`;
    case 'PLACED_ALL':
      return `담은 장소 ${d.placeCount}곳 중 ${d.placedCount}곳만 배치`;
    case 'ADOPTED':
      return `${d.placedCount}곳 모두 배치했지만 일정 확정 안 함`;
    case 'REVIEWED':
      return `확정 일정 ${d.scheduledCount}곳 · 후기 기한 ${d.deadline} 지남`;
    default:
      return '';
  }
}

/** ③ 이탈 로그: 이탈 확정된 여행 한 건씩 (서버에서 20건씩) */
function DropLog({ days, main }) {
  const [step, setStep] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const labelOf = Object.fromEntries(main.map((s) => [s.key, s.label]));

  useEffect(() => {
    let cancelled = false;
    setError('');
    fetchFunnelDrops({ days, step, page })
      .then((res) => !cancelled && setData(res))
      .catch((e) => !cancelled && setError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [days, step, page]);

  const exportCsv = async () => {
    setBusy(true);
    try {
      await downloadFunnelDropsCsv({ days, step });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const choose = (key) => {
    setStep(key);
    setPage(0);
  };

  const allCount = data ? Object.values(data.counts).reduce((a, b) => a + b, 0) : 0;

  return (
    <div>
      <div className="adm-card-head">
        <h3>
          ③ 이탈 로그 <span className="adm-muted">이탈 확정된 여행 한 건 한 건 (근거 포함)</span>
        </h3>
        <button type="button" className="adm-btn small" disabled={busy || !data || data.total === 0} onClick={exportCsv}>
          {busy ? '만드는 중…' : 'CSV 내려받기'}
        </button>
      </div>
      {data && (
        <div className="adm-tabs fn-filters">
          <button type="button" className={step === '' ? 'on' : ''} onClick={() => choose('')}>
            전체 {num(allCount)}
          </button>
          {Object.entries(data.counts).map(([key, n]) => (
            <button key={key} type="button" className={step === key ? 'on' : ''} onClick={() => choose(key)}>
              {labelOf[key] ?? key}에서 이탈 {num(n)}
            </button>
          ))}
        </div>
      )}
      {error && <p className="adm-error">{error}</p>}
      {!data ? (
        <p className="adm-empty">불러오는 중…</p>
      ) : data.items.length === 0 ? (
        <p className="adm-empty">이탈 확정된 여행이 없어요.</p>
      ) : (
        <div className="adm-scroll">
          <table className="adm-tbl">
            <thead>
              <tr>
                <th>여행</th>
                <th>멈춘 단계 → 못 간 단계</th>
                <th>만든 날</th>
                <th>여행 종료일</th>
                <th className="r">기한 지난 지</th>
                <th>근거</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((d) => (
                <tr key={d.travelId}>
                  <td>
                    <b>#{d.travelId}</b> {d.travelName}
                    <br />
                    <span className="adm-muted">{d.nickname}</span>
                  </td>
                  <td>
                    {labelOf[d.reachedStep] ?? d.reachedStep} → <span className="adm-badge bad">{labelOf[d.dropStep] ?? d.dropStep}</span>
                  </td>
                  <td className="num">{String(d.createdAt).slice(0, 10)}</td>
                  <td className="num">{d.endDate}</td>
                  <td className="r num">{num(d.overdueDays)}일</td>
                  <td className="adm-muted">{evidence(d)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.totalPages > 1 && (
        <nav className="adm-pager" aria-label="이탈 로그 페이지">
          <button type="button" className="adm-btn" disabled={page <= 0} onClick={() => setPage(page - 1)}>
            ‹ 이전
          </button>
          <span className="num">
            {page + 1} / {data.totalPages}
          </span>
          <button type="button" className="adm-btn" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>
            다음 ›
          </button>
        </nav>
      )}
      <p className="adm-note">근거 칸은 멈춘 단계에서 DB로 바로 확인되는 사실만 적습니다 (담은·배치된 장소 수, 추천 횟수, 후기 기한).</p>
    </div>
  );
}