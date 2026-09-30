/** 1단계: 여행 이름, 날짜, 권역 */
export default function BasicInfoStep({ form, update, regions }) {
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

      <div className="row">
        <label className="field">
          시작일
          <input type="date" value={form.startDate} onChange={(e) => update({ startDate: e.target.value })} />
        </label>
        <label className="field">
          종료일
          <input
            type="date"
            value={form.endDate}
            min={form.startDate || undefined}
            onChange={(e) => update({ endDate: e.target.value })}
          />
        </label>
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