import QuestionCard from './QuestionCard.jsx';

/**
 * 3단계: 설문. 그룹 → 질문 순으로 그린다. 선택 개수 규칙은 그룹의 min/max를 따른다.
 * 여행 동기·테마(3개 필수)는 고른 순서가 순위 → 배열 순서 그대로 서버에 보내 1순위를 AI에 전달.
 */
export default function SurveyStep({ form, update, groups }) {
  const setAnswer = (preferenceId, values) =>
    update({ answers: { ...form.answers, [preferenceId]: values } });

  return (
    <>
      {groups.map((group) => (
        <section className="card" key={group.groupCode}>
          <h2>
            {group.groupName}
            <small>{ruleText(group)}</small>
          </h2>
          {group.questions.map((q) => (
            <QuestionCard
              key={q.preferenceId}
              question={q}
              max={group.maxSelections}
              ranked={group.minSelections > 1}
              selected={form.answers[q.preferenceId] ?? []}
              onChange={(values) => setAnswer(q.preferenceId, values)}
            />
          ))}
        </section>
      ))}
    </>
  );
}

function ruleText(group) {
  if (group.maxSelections === 1) return '하나씩 선택';
  if (group.minSelections > 1 && group.minSelections === group.maxSelections) {
    return `${group.minSelections}개 선택 · 먼저 고른 것이 1순위`;
  }
  if (group.maxSelections == null) return `${group.minSelections}개 이상 선택`;
  return `${group.minSelections}~${group.maxSelections}개 선택`;
}