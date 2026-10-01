/** 관리자 KPI: 여행 단계별 도달과 확정 이탈률 */
const pct = (v) => (v == null || Number.isNaN(v) ? '—' : `${(v * 100).toFixed(1)}%`);
const num = (v) => (v == null ? '—' : Number(v).toLocaleString());
const confirmedDrop = (s) => (s.count + s.dropped === 0 ? null : s.dropped / (s.count + s.dropped));

export default function FunnelPanel({ steps }) {
  const main = steps.filter((s) => s.key !== 'SHARED');
  const shared = steps.find((s) => s.key === 'SHARED');
  const first = main[0]?.count ?? 0;
  const worst = main
    .slice(1)
    .reduce((w, s) => ((confirmedDrop(s) ?? -1) > (w ? confirmedDrop(w) ?? -1 : -1) ? s : w), null);
  const worstKey = worst && confirmedDrop(worst) > 0 ? worst.key : null;

  return (
    <section className="adm-card" aria-labelledby="funnel-title">
      <div className="adm-card-head">
        <h2 id="funnel-title">사용자 여정 퍼널</h2>
        {shared && <span className="adm-muted">커뮤니티 공유 {num(shared.count)}건 (선택 단계)</span>}
      </div>

      {first === 0 ? (
        <p className="adm-empty">이 기간에 만든 여행이 없어요.</p>
      ) : (
        <div className="fn-overview">
          <div className="fn-chart" style={{ gridTemplateColumns: `repeat(${main.length}, minmax(0, 1fr))` }}>
            {main.map((s) => (
              <div key={s.key} className="fn-stage">
                <div className="fn-val num">{num(s.count)}</div>
                <div className="fn-pct num">{pct(s.count / first)}</div>
                <div className="fn-barbox">
                  <div
                    className="fn-bar"
                    style={{ height: `${Math.max((s.count / first) * 100, 1)}%` }}
                    title={`${s.key === 'CREATED' ? '여행 작성' : s.label} ${s.count}건 (시작 대비 ${pct(s.count / first)})`}
                  />
                </div>
                <div className="fn-name">{s.key === 'CREATED' ? '여행 작성' : s.label}</div>
              </div>
            ))}
          </div>
          <div className="fn-gaps" style={{ gridTemplateColumns: `repeat(${main.length}, minmax(0, 1fr))` }}>
            {main.slice(1).map((s) => (
              <div key={s.key} className={s.key === worstKey ? 'fn-gap worst' : 'fn-gap'}>
                <span>
                  → 확정 이탈 <b className="num">{pct(confirmedDrop(s))}</b>
                  <br />
                  <span className="adm-muted">이탈 {num(s.dropped)} · 진행 중 {num(s.waiting)}</span>
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
