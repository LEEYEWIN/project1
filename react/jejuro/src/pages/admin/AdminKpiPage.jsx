import { useEffect, useState } from 'react';
import { downloadTrainingCsv, fetchKpi } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';
import Loading from '../../components/common/Loading.jsx';
import {
  DatasetPanel,
  Funnel,
  MissReasons,
  PerformancePanel,
  PoiTable,
  SegmentTable,
  SummaryTiles,
  TrendChart,
} from '../../components/admin/KpiParts.jsx';

const PERIODS = [7, 30, 90];

/**
 * 관리자: AI 추천 KPI 대시보드 (/admin/kpi)
 * 추천 → 장소 담기 → 일정 확정 → 실제 방문 → 만족까지 추적해 AI 재학습 시점과 약점을 찾는다.
 * 관리자 계정(USER.role = ADMIN)만 볼 수 있다. 아니면 서버가 403.
 * 지표별 기준: docs/18_KPI_지표_설명.md
 */
export default function AdminKpiPage() {
  const [days, setDays] = useState(30);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [forbidden, setForbidden] = useState(false);
  const [segTab, setSegTab] = useState('companion');
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setError('');
    fetchKpi({ days })
      .then((res) => !cancelled && setData(res))
      .catch((e) => {
        if (cancelled) return;
        if (e?.response?.status === 403) setForbidden(true);
        else setError(errorMessage(e));
      });
    return () => {
      cancelled = true;
    };
  }, [days]);

  const exportCsv = async () => {
    setExporting(true);
    try {
      await downloadTrainingCsv();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setExporting(false);
    }
  };

  if (forbidden) {
    return (
      <div className="adm-empty-page">
        <h1>관리자만 볼 수 있어요</h1>
        <p>왼쪽 메뉴 아래 "테스트 회원"을 관리자 계정(1번)으로 바꾼 뒤 다시 열어 주세요.</p>
      </div>
    );
  }

  return (
    <div className="adm-page">
      <header className="adm-header">
        <div>
          <h1>AI 추천 KPI</h1>
          <p className="adm-muted">
            추천 → 장소 담기 → 일정 확정 → 실제 방문 → 만족까지 추적해 AI 재학습 시점과 약점을 찾습니다.
            {data && ` (${data.filter.from} ~ ${data.filter.to})`}
          </p>
        </div>
        <div className="adm-filters" role="group" aria-label="기간">
          {PERIODS.map((d) => (
            <button key={d} type="button" aria-pressed={days === d} className={days === d ? 'on' : ''} onClick={() => setDays(d)}>
              최근 {d}일
            </button>
          ))}
        </div>
      </header>

      {error && <p className="adm-error" role="alert">{error}</p>}
      {!data && !error && <Loading text="지표를 계산하는 중…" />}

      {data && (
        <>
          <SummaryTiles s={data.summary} />

          <section className="adm-grid-2">
            <Funnel steps={data.funnel} />
            <PerformancePanel p={data.performance} />
          </section>

          <TrendChart trend={data.trend} />

          <SegmentTable segments={data.segments} tab={segTab} onTab={setSegTab} />

          <section className="adm-grid-3">
            <PoiTable title="과추천 관광지" desc="추천은 많은데 담기지 않는 곳 (추천 3회 이상)" rows={data.overRecommended} mode="over" />
            <PoiTable title="AI가 놓친 관광지" desc="관광지 검색으로 직접 담은 곳 (추천 수와 비교)" rows={data.missed} mode="missed" />
            <MissReasons m={data.missReasons} />
          </section>

          <DatasetPanel
            d={data.dataset}
            ready={data.summary.retrainReady}
            threshold={data.summary.retrainThreshold}
            onExport={exportCsv}
            exporting={exporting}
          />
        </>
      )}
    </div>
  );
}