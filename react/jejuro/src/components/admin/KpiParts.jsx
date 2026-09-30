/**
 * 관리자 AI KPI 화면 조각들. 서버 응답(AdminKpiResponse)의 한 구역씩 그린다.
 * 비율 값은 0~1 → pct()로 % 표시, 값이 없으면 "—"
 */

export const pct = (v, digits = 1) => (v == null ? '—' : `${(v * 100).toFixed(digits)}%`);
const num = (v) => (v == null ? '—' : Number(v).toLocaleString());
const fixed = (v, d = 2) => (v == null ? '—' : Number(v).toFixed(d));

/** %p 차이 문구: ▲ 4.1%p */
function delta(now, prev) {
  if (now == null || prev == null) return null;
  const d = (now - prev) * 100;
  if (Math.abs(d) < 0.05) return { text: '변화 없음', cls: '' };
  return { text: `${d > 0 ? '▲' : '▼'} ${Math.abs(d).toFixed(1)}%p · 이전 기간`, cls: d > 0 ? 'up' : 'down' };
}

// ------------------------------------------------------------------ 요약 카드

export function SummaryTiles({ s }) {
  const adopt = delta(s.adoptionRate, s.adoptionRatePrev);
  const sched = delta(s.scheduleRate, s.scheduleRatePrev);
  const ready = s.retrainThreshold ? Math.min(s.retrainReady / s.retrainThreshold, 1) : 0;
  return (
    <section className="adm-tiles" aria-label="요약 지표">
      <div className="adm-card adm-tile">
        <div className="adm-tile-label">추천 요청</div>
        <div className="adm-tile-value num">{num(s.requestCount)}</div>
        <div className="adm-tile-sub">
          여행 {num(s.recommendedTravels)}건 · 재추천 {num(s.reRequestCount)}건
        </div>
      </div>
      <div className="adm-card adm-tile accent">
        <div className="adm-tile-label">추천 채택률</div>
        <div className="adm-tile-value num">{pct(s.adoptionRate)}</div>
        <div className={`adm-tile-sub ${adopt?.cls ?? ''}`}>{adopt?.text ?? '보여 준 추천 중 담긴 비율'}</div>
      </div>
      <div className="adm-card adm-tile">
        <div className="adm-tile-label">일정 반영률</div>
        <div className="adm-tile-value num">{pct(s.scheduleRate)}</div>
        <div className={`adm-tile-sub ${sched?.cls ?? ''}`}>{sched?.text ?? '확정 일정에 들어간 비율'}</div>
      </div>
      <div className="adm-card adm-tile">
        <div className="adm-tile-label">실제 방문률</div>
        <div className="adm-tile-value num">{pct(s.visitRate)}</div>
        <div className="adm-tile-sub">후기의 관광지별 "갔어요"</div>
      </div>
      <div className="adm-card adm-tile">
        <div className="adm-tile-label">평균 만족도</div>
        <div className="adm-tile-value num">
          {fixed(s.avgSatisfaction, 1)}
          <span className="adm-tile-unit"> / 5</span>
        </div>
        <div className="adm-tile-sub">후기 {num(s.feedbackCount)}건</div>
      </div>
      <div className="adm-card adm-tile dark">
        <div className="adm-tile-label">재학습 데이터 (후기 완료 여행)</div>
        <div className="adm-tile-value num">
          {num(s.retrainReady)}
          <span className="adm-tile-unit"> / {num(s.retrainThreshold)}</span>
        </div>
        <div className="adm-progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(ready * 100)}>
          <div style={{ width: `${ready * 100}%` }} />
        </div>
        <div className="adm-tile-sub">재학습 기준 {Math.round(ready * 100)}% 도달</div>
      </div>
    </section>
  );
}

// ------------------------------------------------------------------ 퍼널

export function Funnel({ steps }) {
  const max = Math.max(...steps.map((s) => s.count), 1);
  const main = steps.filter((s) => s.key !== 'SHARED'); // 공유는 선택 단계라 "가장 큰 이탈" 판단에서 제외
  // 가장 많이 빠지는 단계 (전환율 최저)
  const worst = main.slice(1).reduce((w, s) => (s.rate != null && (w == null || s.rate < w.rate) ? s : w), null);
  return (
    <div className="adm-card">
      <div className="adm-card-head">
        <h2>사용자 여정 퍼널</h2>
        <span className="adm-muted">기간 안에 만든 여행 · 가장 멀리 간 단계까지 누적 · 앞 단계 대비 전환율</span>
      </div>
      <ul className="adm-funnel">
        {steps.map((s) => (
          <li key={s.key} className={worst?.key === s.key ? 'worst' : ''}>
            <span className="adm-funnel-label">{s.label}</span>
            <span className="adm-bar-track">
              <span className="adm-bar" style={{ width: `${(s.count / max) * 100}%` }} title={`${s.label} ${s.count}`} />
            </span>
            <span className="adm-funnel-val num">
              {num(s.count)} {s.rate != null && <em>{pct(s.rate, 0)}</em>}
            </span>
            {(s.dropped > 0 || s.waiting > 0) && (
              <span className="adm-funnel-drop">
                {s.dropped > 0 && <b>이탈 {num(s.dropped)}</b>}
                {s.waiting > 0 && <span> 진행 중 {num(s.waiting)}</span>}
              </span>
            )}
          </li>
        ))}
      </ul>
      {worst && (
        <p className="adm-note">
          <b>
            {main[main.indexOf(worst) - 1].label} → {worst.label}
          </b>{' '}
          단계 이탈이 가장 큼 ({pct(1 - worst.rate, 0)}). <b>이탈</b> = 종료일이 지났는데 다음 단계로 못 감(후기는 종료 +14일),{' '}
          <b>진행 중</b> = 아직 기간이 남음
        </p>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ AI 성능 (혼동 행렬)

/**
 * 예측 = 화면에 추천했나, 정답 = 후기에서 "갔어요"
 * 정확도 = (TP+TN)/전체, 정밀도 = TP/(TP+FP), 재현율 = TP/(TP+FN), F1 = 2·정밀도·재현율/(정밀도+재현율)
 */
const PERF = [
  { key: 'accuracy', label: '정확도', formula: '(TP+TN) ÷ 전체', desc: '추천·비추천 판단이 맞은 비율' },
  { key: 'precision', label: '정밀도', formula: 'TP ÷ (TP+FP)', desc: '추천한 곳 중 실제로 간 비율' },
  { key: 'recall', label: '재현율', formula: 'TP ÷ (TP+FN)', desc: '실제로 간 곳 중 AI가 추천한 비율' },
  { key: 'f1', label: 'F1 점수', formula: '2PR ÷ (P+R)', desc: '정밀도·재현율의 균형' },
];

export function PerformancePanel({ p }) {
  const total = p.tp + p.fp + p.fn + p.tn;
  return (
    <div className="adm-card">
      <div className="adm-card-head">
        <h2>AI 모델 성능</h2>
        <span className="adm-muted">이 기간 후기가 끝난 여행 {num(p.travels)}건 · 관광지 {num(total)}곳</span>
      </div>
      {total === 0 ? (
        <p className="adm-empty">후기까지 끝난 여행이 아직 없어요. 여행이 끝나고 후기가 쌓이면 계산됩니다.</p>
      ) : (
        <>
          <div className="adm-perf">
            {PERF.map((m) => (
              <div key={m.key} className="adm-perf-item">
                <span className="adm-tile-label">{m.label}</span>
                <b className="num">{pct(p[m.key])}</b>
                <span className="adm-muted num">{m.formula}</span>
                <span className="adm-muted">{m.desc}</span>
              </div>
            ))}
          </div>
          <table className="adm-tbl adm-matrix" aria-label="혼동 행렬">
            <thead>
              <tr>
                <th />
                <th className="r">실제로 감</th>
                <th className="r">안 감</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">AI가 추천함</th>
                <td className="r num ok">TP {num(p.tp)}</td>
                <td className="r num bad">FP {num(p.fp)}</td>
              </tr>
              <tr>
                <th scope="row">추천 안 함</th>
                <td className="r num bad">FN {num(p.fn)}</td>
                <td className="r num">TN {num(p.tn)}</td>
              </tr>
            </tbody>
          </table>
          <p className="adm-note">
            <b>추천 안 함</b> = AI 후보였지만 상위 10곳에 못 든 곳 + AI 결과에 아예 없던 곳(FN, 검색으로 담고 방문). 추천
            서비스에서는 정답(방문)이 적어 정확도가 높게 나오기 쉬우므로 <b>정밀도·재현율</b>을 함께 봅니다.
          </p>
        </>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ 주별 추이 (선 그래프)

export function TrendChart({ trend }) {
  const W = 640;
  const H = 236;
  const x0 = 48;
  const x1 = 600;
  const top = 30;
  const bottom = 190;
  const values = trend.points.map((p) => p.adoptionRate).filter((v) => v != null);
  const hi = Math.max(0.1, Math.ceil((Math.max(...values, 0) * 100 + 5) / 5) * 5 / 100);
  const lo = Math.max(0, Math.floor((Math.min(...values, hi) * 100 - 5) / 5) * 5 / 100);
  const x = (i) => x0 + ((x1 - x0) / 7) * i;
  const y = (v) => bottom - ((v - lo) / (hi - lo || 1)) * (bottom - top);
  const pts = trend.points.map((p, i) => ({ ...p, i })).filter((p) => p.adoptionRate != null);
  const ticks = [lo, lo + (hi - lo) / 3, lo + ((hi - lo) * 2) / 3, hi];
  const last = pts[pts.length - 1];

  return (
    <div className="adm-card">
      <div className="adm-card-head">
        <h2>
          추천 채택률 추이 <span className="adm-muted">최근 8주 · 주 단위</span>
        </h2>
        <span className="adm-muted">보여 준 추천 중 담긴 비율</span>
      </div>
      {pts.length === 0 ? (
        <p className="adm-empty">최근 8주 동안 추천 기록이 없어요.</p>
      ) : (
        <svg
          width={W}
          height={H}
          viewBox={`0 0 ${W} ${H}`}
          className="adm-svg"
          role="img"
          aria-label={`주별 추천 채택률 ${pts.map((p) => pct(p.adoptionRate)).join(', ')}`}
        >
          {ticks.map((t) => (
            <g key={t}>
              <line x1={x0} x2={x1} y1={y(t)} y2={y(t)} stroke="#ECE8DF" />
              <text x={x0 - 12} y={y(t) + 4} textAnchor="end" className="adm-axis">
                {Math.round(t * 100)}%
              </text>
            </g>
          ))}
          <polyline
            points={pts.map((p) => `${x(p.i)},${y(p.adoptionRate)}`).join(' ')}
            fill="none"
            stroke="#2F6FDB"
            strokeWidth="2"
            strokeLinejoin="round"
          />
          {pts.map((p) => (
            <circle key={p.i} cx={x(p.i)} cy={y(p.adoptionRate)} r="5" fill="#2F6FDB" stroke="#fff" strokeWidth="2">
              <title>
                {p.weekStart} 주 · {pct(p.adoptionRate)} (추천 {p.items}곳)
              </title>
            </circle>
          ))}
          {last && (
            <text x={x(last.i)} y={y(last.adoptionRate) - 14} textAnchor="middle" className="adm-axis strong">
              {pct(last.adoptionRate)}
            </text>
          )}
          {trend.points.map((p, i) => (
            <text key={p.weekStart} x={x(i)} y={H - 18} textAnchor="middle" className="adm-axis">
              {Number(p.weekStart.slice(5, 7))}/{Number(p.weekStart.slice(8, 10))}
            </text>
          ))}
        </svg>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ 세그먼트

const SEGMENT_TABS = [
  { key: 'companion', label: '동반자 유형' },
  { key: 'age', label: '연령대' },
  { key: 'region', label: '권역' },
  { key: 'motive', label: '여행 동기 1순위' },
  { key: 'days', label: '여행 일수' },
];
const LEVEL = {
  OK: { text: '양호', cls: '' },
  WATCH: { text: '관찰', cls: '' },
  WEAK: { text: '재학습 보강 필요', cls: 'weak' },
  LOW_DATA: { text: '데이터 부족', cls: 'muted' },
};

/** 채택률이 높을수록 진한 파랑 (한 색 단계) */
function heat(v) {
  if (v == null) return '#F3F1EC';
  const steps = ['#EEF3FC', '#DCE8FB', '#CFDDF8', '#BCD0F5', '#A9C3F2', '#95B5EE'];
  return steps[Math.min(steps.length - 1, Math.floor(v * 100 / 8))];
}

export function SegmentTable({ segments, tab, onTab }) {
  const rows = segments[tab] ?? [];
  return (
    <section className="adm-card" aria-labelledby="seg-title">
      <div className="adm-card-head">
        <h2 id="seg-title">
          세그먼트별 성능 <span className="adm-muted">어디서 약한가</span>
        </h2>
        <div role="tablist" aria-label="세그먼트 기준" className="adm-tabs">
          {SEGMENT_TABS.map((t) => (
            <button key={t.key} type="button" role="tab" aria-selected={tab === t.key} className={tab === t.key ? 'on' : ''} onClick={() => onTab(t.key)}>
              {t.label}
            </button>
          ))}
        </div>
      </div>
      {rows.length === 0 ? (
        <p className="adm-empty">이 기간에 추천 기록이 없어요.</p>
      ) : (
        <table className="adm-tbl">
          <thead>
            <tr>
              <th>세그먼트</th>
              <th className="r">여행 수</th>
              <th>추천 채택률</th>
              <th className="r">실제 방문률</th>
              <th className="r">평균 만족도</th>
              <th>판정</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.name} className={r.level === 'WEAK' ? 'weak' : ''}>
                <td>{r.name}</td>
                <td className="r num">{num(r.travels)}</td>
                <td>
                  <span className="adm-heat num" style={{ background: heat(r.adoptionRate) }}>
                    {pct(r.adoptionRate, 0)}
                  </span>
                </td>
                <td className="r num">{pct(r.visitRate, 0)}</td>
                <td className="r num">{fixed(r.avgSatisfaction, 1)}</td>
                <td>
                  <span className={`adm-level ${LEVEL[r.level].cls}`}>{LEVEL[r.level].text}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <p className="adm-note">채택률 칸은 진할수록 높음. 여행 5건 미만은 "데이터 부족", 전체 평균의 70% 미만이면 "재학습 보강 필요"</p>
    </section>
  );
}

// ------------------------------------------------------------------ 관광지 표 · 못 간 이유

export function PoiTable({ title, desc, rows, mode }) {
  return (
    <div className="adm-card">
      <h2>{title}</h2>
      <p className="adm-muted">{desc}</p>
      {rows.length === 0 ? (
        <p className="adm-empty">해당하는 관광지가 없어요.</p>
      ) : (
        <table className="adm-tbl">
          <thead>
            <tr>
              <th>관광지</th>
              {mode === 'over' ? (
                <>
                  <th className="r">추천</th>
                  <th className="r">채택률</th>
                </>
              ) : (
                <>
                  <th className="r">직접 담음</th>
                  <th className="r">추천</th>
                </>
              )}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.poiId}>
                <td>{r.name}</td>
                {mode === 'over' ? (
                  <>
                    <td className="r num">{num(r.recommended)}</td>
                    <td className="r num strong">{pct(r.adoptionRate, 0)}</td>
                  </>
                ) : (
                  <>
                    <td className="r num strong">{num(r.searchAdded)}</td>
                    <td className="r num">{num(r.recommended)}</td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export function MissReasons({ m }) {
  const max = Math.max(...m.reasons.map((r) => r.count), 1);
  return (
    <div className="adm-card">
      <h2>못 간 이유</h2>
      <p className="adm-muted">"일부만 다녀옴" 후기 {num(m.partialCount)}건 기준 (복수 선택)</p>
      {m.partialCount === 0 ? (
        <p className="adm-empty">아직 "일부만 다녀옴" 후기가 없어요.</p>
      ) : (
        <ul className="adm-reasons">
          {m.reasons.map((r) => (
            <li key={r.code}>
              <span>{r.label}</span>
              <span className="adm-bar-track">
                <span className="adm-bar" style={{ width: `${(r.count / max) * 100}%` }} title={`${r.label} ${r.count}건`} />
              </span>
              <span className="num">{pct(r.rate, 0)}</span>
            </li>
          ))}
        </ul>
      )}
      {m.oftenMissed.length > 0 && <p className="adm-note">자주 빠지는 곳: {m.oftenMissed.join(', ')}</p>}
    </div>
  );
}

// ------------------------------------------------------------------ 재학습 데이터셋

const LABEL_COLOR = ['#F6D9BF', '#EFB585', '#D9763A', '#8F4210'];

export function DatasetPanel({ d, ready, threshold, onExport, exporting }) {
  const canRetrain = ready >= threshold;
  return (
    <section className="adm-card" aria-labelledby="ds-title">
      <div className="adm-card-head">
        <div>
          <h2 id="ds-title">재학습 데이터셋</h2>
          <p className="adm-muted">
            후기까지 끝난 여행만 · (여행, 추천 관광지)마다 1줄 · 여행 {num(d.travels)}건 / {num(d.total)}줄 (AI_TRAINING_DATASET)
          </p>
        </div>
        <div className="adm-actions">
          <button type="button" className="adm-btn" onClick={onExport} disabled={exporting || d.total === 0}>
            {exporting ? '만드는 중…' : '학습 데이터 CSV 내려받기'}
          </button>
          <button
            type="button"
            className="adm-btn dark"
            disabled={!canRetrain}
            title={canRetrain ? 'AI 담당에게 CSV를 전달하고 새 버전을 학습하세요' : `후기 완료 여행 ${threshold}건부터 요청할 수 있어요`}
          >
            재학습 요청 ({num(ready)}/{num(threshold)})
          </button>
        </div>
      </div>

      {d.total > 0 && (
        <div className="adm-stack" aria-hidden="true">
          {d.labels.map((l) =>
            l.count > 0 ? (
              <span key={l.label} style={{ flexGrow: l.count, background: LABEL_COLOR[l.label] }} title={`${l.label}점 ${l.name} ${l.count}건`} />
            ) : null
          )}
        </div>
      )}
      <ul className="adm-labels four">
        {d.labels.map((l) => (
          <li key={l.label}>
            <i style={{ background: LABEL_COLOR[l.label] }} />
            <span>
              <b>{l.label}점</b> {l.name}
              <br />
              <span className="num">{num(l.count)}</span> <span className="adm-muted">{pct(d.total ? l.count / d.total : null)}</span>
            </span>
          </li>
        ))}
      </ul>

      <div className="adm-ds-grid">
        <div>
          <h3>라벨 기준</h3>
          <table className="adm-tbl">
            <tbody>
              <tr>
                <td className="num strong">3</td>
                <td>방문 + 좋았어요</td>
                <td className="adm-muted">후기 관광지별 "갔어요" + "좋았어요"</td>
              </tr>
              <tr>
                <td className="num strong">2</td>
                <td>실제 방문</td>
                <td className="adm-muted">"갔어요" (아쉬워요·선택 안 함 포함)</td>
              </tr>
              <tr>
                <td className="num strong">1</td>
                <td>일정 확정</td>
                <td className="adm-muted">확정 일정에 넣었지만 "못 갔어요"</td>
              </tr>
              <tr>
                <td className="num strong">0</td>
                <td>추천만 됨</td>
                <td className="adm-muted">AI가 추천했지만 일정에 넣지 않음</td>
              </tr>
            </tbody>
          </table>
          <p className="adm-note">
            대상: 일정 확정 + 후기(모두 다녀옴·일부만 다녀옴)까지 끝난 여행. 진행 중인 여행은 라벨이 아직 바뀔 수 있어 넣지 않습니다.
          </p>
        </div>
        <div>
          <h3>데이터 품질</h3>
          <dl className="adm-dl">
            <div>
              <dt>후기 응답률 <span className="adm-muted">(끝난 확정 여행 중)</span></dt>
              <dd className="num">{pct(d.quality.feedbackResponseRate, 0)}</dd>
            </div>
            <div>
              <dt>관광지별 후기 입력률</dt>
              <dd className="num">{pct(d.quality.spotInputRate, 0)}</dd>
            </div>
            <div>
              <dt>설문 미완료 여행</dt>
              <dd className="num">{pct(d.quality.surveyMissingRate)}</dd>
            </div>
            <div>
              <dt>이상치 <span className="adm-muted">(15일 초과 여행)</span></dt>
              <dd className="num">{num(d.quality.outlierTravels)}건</dd>
            </div>
          </dl>
        </div>
      </div>
    </section>
  );
}