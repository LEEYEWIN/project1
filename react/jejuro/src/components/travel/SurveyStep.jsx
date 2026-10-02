import QuestionCard from './QuestionCard.jsx';

/** 설문 첫 질문: 여행지 선택 성향. 화면 안내 문구용이라 서버에 보내지 않는다. */
export const PICK_STYLES = [
  { value: 'POPULAR', label: 'A. 다른 사람들이 많이 선택하고 검증한 장소가 더 끌려요.' },
  { value: 'UNIQUE', label: 'B. 내 취향에 맞고 나만의 느낌이 있는 장소가 더 끌려요.' },
];

/**
 * 3단계: 설문. 그룹 → 질문 순으로 그린다. 선택 개수 규칙은 그룹의 min/max를 따른다.
 * 여행 동기·테마(3개 필수)는 고른 순서가 순위 → 배열 순서 그대로 서버에 보내 1순위를 AI에 전달.
 * 맨 위 안내: 이 답변이 "이 여행"의 AI 추천에 어떻게 쓰이는지 먼저 알려 준다.
 */
export default function SurveyStep({ form, update, groups }) {
  const setAnswer = (preferenceId, values) =>
    update({ answers: { ...form.answers, [preferenceId]: values } });

  return (
    <>
      <section className="survey-intro" aria-label="설문 안내">
        <h2>이 설문으로 ‘{form.travelName.trim() || '이번 여행'}’의 관광지를 추천해요</h2>
        <ul>
          <li>
            답변은 <b>이번 여행에만</b> 쓰여요. 여행마다 설문을 따로 하므로, 같이 가는 사람·목적에 맞게 골라 주세요.
          </li>
          <li>
            <b>순위를 고르는 질문</b>(여행 동기·테마)은 <b>먼저 누른 것이 1순위</b>예요.
            <b> 1순위가 AI 추천에 직접 반영</b>되고, 2·3순위는 여행 기록으로 저장돼요.
          </li>
          <li>1~7점 질문은 가운데(4점)가 “보통”이에요. 한쪽으로 갈수록 그 성향이 강하게 반영돼요.</li>
          <li>추천 목록 화면 위쪽에서 어떤 답변을 기준으로 추천했는지 다시 볼 수 있어요.</li>
        </ul>
      </section>

      <section className="card" aria-labelledby="pick-style-title">
        <h2 id="pick-style-title">
          여행지를 고를 때, 나는 어떤 쪽에 더 가까운가요?
          <small>하나 선택</small>
        </h2>
        <div className="question">
          <div className="chips two" role="radiogroup" aria-labelledby="pick-style-title">
            {PICK_STYLES.map((o) => (
              <button
                key={o.value}
                type="button"
                role="radio"
                aria-checked={form.pickStyle === o.value}
                className={form.pickStyle === o.value ? 'chip on' : 'chip'}
                onClick={() => update({ pickStyle: o.value })}
              >
                {o.label}
              </button>
            ))}
          </div>
        </div>
      </section>

      {groups.map((group) => {
        const ranked = group.minSelections > 1;
        return (
          <section className="card" key={group.groupCode}>
            <h2>
              {group.groupName}
              <small>{ruleText(group)}</small>
            </h2>
            {ranked && (
              <p className="rank-notice">
                순위에 따라 추천이 달라져요. <b>가장 중요한 것부터</b> 눌러 주세요. 다시 누르면 빠지고, 뒤 순위가 한 칸씩 당겨져요.
              </p>
            )}
            {group.questions.map((q) => (
              <QuestionCard
                key={q.preferenceId}
                question={q}
                max={group.maxSelections}
                ranked={ranked}
                selected={form.answers[q.preferenceId] ?? []}
                onChange={(values) => setAnswer(q.preferenceId, values)}
              />
            ))}
          </section>
        );
      })}
    </>
  );
}

function ruleText(group) {
  if (group.maxSelections === 1) return '하나씩 선택';
  if (group.minSelections > 1 && group.minSelections === group.maxSelections) {
    return `${group.minSelections}개 선택 · 먼저 고른 것이 1순위 (1순위가 추천에 반영)`;
  }
  if (group.maxSelections == null) return `${group.minSelections}개 이상 선택`;
  return `${group.minSelections}~${group.maxSelections}개 선택`;
}