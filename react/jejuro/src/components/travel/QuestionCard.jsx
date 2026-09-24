/*========================



==========================*//**
 * 질문 하나.
 * SINGLE_SELECT: 누르면 그 값으로 교체 (라디오처럼 동작)
 * MULTI_SELECT : 누르면 추가/제거 (체크박스처럼 동작), max를 넘으면 더 못 고름
 */
export default function QuestionCard({ question, max, selected, onChange }) {
  const single = question.responseType === 'SINGLE_SELECT';
  const full = !single && max != null && selected.length >= max;

  const click = (value) => {
    if (single) {
      onChange([value]);
      return;
    }
    if (selected.includes(value)) {
      onChange(selected.filter((v) => v !== value));
    } else if (!full) {
      onChange([...selected, value]);
    }
  };

  return (
    <div className="question">
      <p className="q-title">
        {question.name}
        {!single && max != null && (
          <span className="count">
            {selected.length}/{max}
          </span>
        )}
      </p>
      <div className={single && question.options.length === 2 ? 'chips two' : 'chips'}>
        {question.options.map((o) => {
          const on = selected.includes(o.value);
          return (
            <button
              key={o.value}
              type="button"
              className={on ? 'chip on' : 'chip'}
              disabled={!on && full}
              title={o.description ?? undefined}
              onClick={() => click(o.value)}
            >
              {o.name}
            </button>
          );
        })}
      </div>
    </div>
  );
}