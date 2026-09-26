/**
 * 질문 하나.
 * SINGLE_SELECT: 누르면 그 값으로 교체 (라디오처럼 동작)
 *   - 선택지가 7개(여행 스타일 1~7점)면 척도 모양: 양 끝에 첫/마지막 선택지 이름을 보여줌
 * MULTI_SELECT : 누르면 추가/제거 (체크박스처럼 동작), max를 넘으면 더 못 고름
 *   - ranked(여행 동기·테마): 고른 순서대로 ①②③ 순위 표시. 빼면 뒤 순위가 한 칸씩 당겨짐
 *   - selected 배열 순서 = 고른 순서 = 순위 → 그대로 서버에 보냄 (1순위를 AI에 전달)
 */
export default function QuestionCard({ question, max, ranked = false, selected, onChange }) {
  const single = question.responseType === 'SINGLE_SELECT';
  const full = !single && max != null && selected.length >= max;
  const scale = single && question.options.length === 7;

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

  if (scale) {
    const first = question.options[0];
    const last = question.options[question.options.length - 1];
    const current = question.options.find((o) => selected.includes(o.value));
    return (
      <div className="question">
        <p className="q-title">{question.name}</p>
        <div className="scale">
          <span className="scale-end">{first.name}</span>
          <div className="scale-dots">
            {question.options.map((o) => (
              <button
                key={o.value}
                type="button"
                className={selected.includes(o.value) ? 'scale-dot on' : 'scale-dot'}
                title={o.name}
                onClick={() => click(o.value)}
              >
                {o.value}
              </button>
            ))}
          </div>
          <span className="scale-end right">{last.name}</span>
        </div>
        <p className="scale-picked">{current ? `${current.value}점 · ${current.name}` : '점수를 골라 주세요'}</p>
      </div>
    );
  }

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
      {ranked && selected.length > 0 && (
        <p className="rank-line">
          {selected.map((v, i) => (
            <span key={v} className="rank-tag">
              {i + 1}순위 {question.options.find((o) => o.value === v)?.name}
            </span>
          ))}
        </p>
      )}
      <div className={single && question.options.length === 2 ? 'chips two' : 'chips'}>
        {question.options.map((o) => {
          const index = selected.indexOf(o.value);
          const on = index >= 0;
          return (
            <button
              key={o.value}
              type="button"
              className={on ? 'chip on' : 'chip'}
              disabled={!on && full}
              title={o.description ?? undefined}
              onClick={() => click(o.value)}
            >
              {ranked && on && <b className="rank-badge">{index + 1}</b>}
              {o.name}
            </button>
          );
        })}
      </div>
    </div>
  );
}