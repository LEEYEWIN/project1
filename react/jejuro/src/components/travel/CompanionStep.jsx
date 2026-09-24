const EMPTY = { relationCode: '', genderCode: '', ageGroupCode: '' };

/** 2단계: 동반자 (본인 제외). 혼자 여행이면 아무도 추가하지 않고 다음으로. */
export default function CompanionStep({ form, update, meta }) {
  const list = form.companions;

  const add = () => update({ companions: [...list, { ...EMPTY }] });
  const remove = (index) => update({ companions: list.filter((_, i) => i !== index) });
  const change = (index, key, value) =>
    update({
      companions: list.map((c, i) => (i === index ? { ...c, [key]: value === '' ? '' : Number(value) } : c)),
    });

  const select = (index, key, options, placeholder) => (
    <select value={list[index][key]} onChange={(e) => change(index, key, e.target.value)}>
      <option value="">{placeholder}</option>
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.name}
        </option>
      ))}
    </select>
  );

  return (
    <section className="card">
      <p className="hint">함께 가는 사람을 추가하세요. 혼자라면 바로 다음을 누르세요.</p>

      {list.map((_, i) => (
        <div className="companion" key={i}>
          <strong>동반자 {i + 1}</strong>
          {select(i, 'relationCode', meta.relations, '관계')}
          {select(i, 'genderCode', meta.genders, '성별')}
          {select(i, 'ageGroupCode', meta.ageGroups, '연령대')}
          <button type="button" className="btn ghost small" onClick={() => remove(i)}>
            삭제
          </button>
        </div>
      ))}

      <button type="button" className="btn ghost" onClick={add}>
        + 동반자 추가
      </button>
    </section>
  );
}