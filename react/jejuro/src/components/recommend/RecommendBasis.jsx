import { useState } from 'react';
import { formatDate } from '../../utils/format.js';

/**
 * 3페이지 위쪽 "추천 기준" 상자.
 * 이 목록이 "지금 이 여행" 기준 추천이라는 것과, 어떤 정보로 골랐는지를 보여 준다.
 * travel: TravelDetailResponse { travelName, startDate, endDate, tripDays, regionNames, companions, survey }
 *   survey: [{ preferenceId, question, answers: [...], ranked }]  ranked면 answers[0]이 1순위(AI 반영)
 */
export default function RecommendBasis({ travel, count }) {
  const [open, setOpen] = useState(false);
  if (!travel) return null;

  const ranked = travel.survey.filter((s) => s.ranked);
  const others = travel.survey.filter((s) => !s.ranked);
  const companions =
    travel.companions.length === 0 ? '혼자' : travel.companions.map((c) => `${c.relation}(${c.ageGroup})`).join(', ');

  return (
    <section className="basis" aria-labelledby="basis-title">
      <p className="basis-eyebrow">내가 만든 여행 기준 추천</p>
      <h2 id="basis-title">
        ‘{travel.travelName}’ 여행에 맞춘 관광지 {count}곳이에요
      </h2>

      <dl className="basis-facts">
        <div>
          <dt>여행 기간</dt>
          <dd>
            {formatDate(travel.startDate)} ~ {formatDate(travel.endDate)} ({travel.tripDays}일)
          </dd>
        </div>
        <div>
          <dt>권역</dt>
          <dd>{travel.regionNames.join(', ')}</dd>
        </div>
        <div>
          <dt>동반자</dt>
          <dd>{companions}</dd>
        </div>
        {ranked.map((s) => (
          <div key={s.preferenceId}>
            <dt>{s.question} 1순위</dt>
            <dd>
              <b>{s.answers[0]}</b>
              {s.answers.length > 1 && <small> (2·3순위: {s.answers.slice(1).join(', ')})</small>}
            </dd>
          </div>
        ))}
      </dl>

      <div className="basis-how">
        <h3>이렇게 추천했어요</h3>
        <ul>
          <li><b>여행 정보와 설문 답변</b>을 반영했어요. 성별·연령대·동반자·권역, 여행 스타일 점수와 동기·테마 1순위를 활용해요.</li>
          <li><b>비슷한 여행자들이 만족한 곳</b>을 점수 높은 순서로 골랐어요. 선택한 권역이 있다면 그 안에서 추천해요.</li>
          <li><b>비슷한 이름의 장소</b>는 한 곳만 넣었어요. 같은 해변이나 같은 산의 여러 입구가 중복되지 않도록 했어요.</li>
        </ul>
      </div>

      {others.length > 0 && (
        <>
          <button type="button" className="link-btn basis-toggle" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
            {open ? '설문 답변 접기 ▲' : '설문 답변 전체 보기 ▼'}
          </button>
          {open && (
            <ul className="basis-survey">
              {others.map((s) => (
                <li key={s.preferenceId}>
                  <span>{s.question}</span>
                  <b>{s.answers.join(', ')}</b>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  );
}
