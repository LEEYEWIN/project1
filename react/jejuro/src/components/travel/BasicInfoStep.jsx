import DateRangePicker from './DateRangePicker.jsx';
import { formatDate } from '../../utils/format.js';

/**
 * 1단계: 여행 이름, 날짜, 권역
 * 날짜는 달력에서 고른다: 오늘부터, 이미 여행이 있는 날(busy)은 고를 수 없음
 */
export default function BasicInfoStep({ form, update, regions, busy = [] }) {
  const toggleRegion = (regionId) => {
    const selected = form.regionIds.includes(regionId)
      ? form.regionIds.filter((id) => id !== regionId)
      : [...form.regionIds, regionId];
    update({ regionIds: selected });
  };

  return (
    <section className="card">
      <label className="field">
        여행 이름
        <input
          type="text"
          maxLength={100}
          value={form.travelName}
          placeholder="예) 가을 제주 2박 3일"
          onChange={(e) => update({ travelName: e.target.value })}
        />
      </label>

      <div className="field">
        여행 날짜
        <span className="hint small">
          {form.startDate
            ? `${formatDate(form.startDate)} ~ ${form.endDate ? formatDate(form.endDate) : '종료일을 골라 주세요'}`
            : '달력에서 시작일과 종료일을 차례로 눌러 주세요.'}
        </span>
        <DateRangePicker
          start={form.startDate}
          end={form.endDate}
          busy={busy}
          onChange={({ start, end }) => update({ startDate: start, endDate: end })}
        />
      </div>

      <fieldset className="field">
        <legend>여행할 지역</legend>
        <div className="chips">
          <button
            type="button"
            className={form.regionMode === 'ALL' ? 'chip on' : 'chip'}
            onClick={() => update({ regionMode: 'ALL', regionIds: [] })}
          >
            제주 전체
          </button>
          <button
            type="button"
            className={form.regionMode === 'SELECTED' ? 'chip on' : 'chip'}
            onClick={() => update({ regionMode: 'SELECTED' })}
          >
            권역 선택
          </button>
        </div>

        {form.regionMode === 'SELECTED' && (
          <div className="chips">
            {regions.map((r) => (
              <button
                key={r.regionId}
                type="button"
                className={form.regionIds.includes(r.regionId) ? 'chip on' : 'chip'}
                onClick={() => toggleRegion(r.regionId)}
              >
                {r.name}
              </button>
            ))}
          </div>
        )}
      </fieldset>
    </section>
  );
}