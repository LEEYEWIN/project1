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
                  <span className="adm-muted">이탈 {num(s.dropped)} · 진행 중 {num(s.waiting)}</span>
                </span>
              </div>
            ))}
          </div>
          <p className="adm-muted fn-rule">
            확정 이탈 기준: 추천 받음~일정 확정은 <b>출발일</b>이 지나도록 다음 단계로 못 가면, 후기 작성은 <b>종료일 + 14일</b>까지 쓰지 않으면 이탈로 봅니다.
            기한이 남은 여행은 &quot;진행 중&quot;으로 셉니다.
          </p>
        </div>
      )}
    </section>
  );
}