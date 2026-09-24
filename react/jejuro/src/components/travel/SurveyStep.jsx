import QuestionCard from './QuestionCard.jsx';

/** 3단계: 설문. 그룹 → 질문 순으로 그린다. 선택 개수 규칙은 그룹의 min/max를 따른다. */
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
  if (group.maxSelections == null) return `${group.minSelections}개 이상 선택`;
  return `${group.minSelections}~${group.maxSelections}개 선택`;
}