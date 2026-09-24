/** 권역 필터 칩. regions: ['동부', '서부', ...] / value: '전체' 또는 권역 이름 */
export default function RegionFilter({ regions, value, onChange }) {
  return (
    <div className="chips">
      {['전체', ...regions].map((name) => (
        <button
          key={name}
          type="button"
          className={value === name ? 'chip on' : 'chip'}
          onClick={() => onChange(name)}
        >
          {name}
        </button>
      ))}
    </div>
  );
}