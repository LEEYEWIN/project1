import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createTravel, fetchTravelForm } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import StepIndicator from '../components/travel/StepIndicator.jsx';
import BasicInfoStep from '../components/travel/BasicInfoStep.jsx';
import CompanionStep from '../components/travel/CompanionStep.jsx';
import SurveyStep from '../components/travel/SurveyStep.jsx';

const STEPS = ['여행 정보', '동반자', '여행 취향'];

const INITIAL_FORM = {
  travelName: '',
  startDate: '',
  endDate: '',
  regionMode: 'ALL', // 'ALL' | 'SELECTED'
  regionIds: [],
  companions: [], // [{ relationCode, genderCode, ageGroupCode }]
  answers: {}, // { 101: [1], 201: [1, 3], ... }  질문 ID → 고른 값 배열
};

/** 1페이지: 여행 만들기 + 설문 (3단계 폼) */
export default function TravelCreatePage() {
  const navigate = useNavigate();
  const [meta, setMeta] = useState(null); // 서버에서 받은 권역·코드·질문
  const [form, setForm] = useState(INITIAL_FORM);
  const [step, setStep] = useState(0);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // 페이지 진입 시 설문 틀을 불러온다
  useEffect(() => {
    fetchTravelForm()
      .then(setMeta)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  // 자식 컴포넌트가 form 일부만 바꿀 때 사용
  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));

  const validateStep = (index) => {
    if (index === 0) {
      if (!form.travelName.trim()) return '여행 이름을 입력하세요.';
      if (!form.startDate || !form.endDate) return '여행 날짜를 선택하세요.';
      if (form.startDate > form.endDate) return '종료일이 시작일보다 빠릅니다.';
      if (form.regionMode === 'SELECTED' && form.regionIds.length === 0) return '권역을 하나 이상 고르세요.';
    }
    if (index === 1) {
      const incomplete = form.companions.some((c) => !c.relationCode || !c.genderCode || !c.ageGroupCode);
      if (incomplete) return '동반자 정보를 모두 선택하세요.';
    }
    if (index === 2) {
      for (const group of meta.groups) {
        for (const q of group.questions) {
          const count = (form.answers[q.preferenceId] ?? []).length;
          if (count < group.minSelections) return `'${q.name}' 질문에 답해 주세요.`;
          if (group.maxSelections != null && count > group.maxSelections) {
            return `'${q.name}'은(는) 최대 ${group.maxSelections}개까지 고를 수 있습니다.`;
          }
        }
      }
    }
    return '';
  };

  const goNext = () => {
    const msg = validateStep(step);
    setError(msg);
    if (!msg) setStep(step + 1);
  };

  const goBack = () => {
    setError('');
    setStep(step - 1);
  };

  const submit = async () => {
    const msg = validateStep(2);
    setError(msg);
    if (msg) return;

    // 화면용 form → 서버 요청 형식(TravelCreateRequest)으로 변환
    const payload = {
      travelName: form.travelName.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
      regionMode: form.regionMode,
      regionIds: form.regionMode === 'SELECTED' ? form.regionIds : [],
      companions: form.companions,
      answers: Object.entries(form.answers).map(([preferenceId, values]) => ({
        preferenceId: Number(preferenceId),
        values,
      })),
    };

    setSubmitting(true);
    try {
      const { travelId } = await createTravel(payload);
      navigate(`/travels/${travelId}/recommending`); // 2페이지로
    } catch (e) {
      setError(errorMessage(e));
      setSubmitting(false);
    }
  };

  if (!meta) {
    return <main className="page">{error ? <p className="error">{error}</p> : <p>불러오는 중…</p>}</main>;
  }

  return (
    <main className="page">
      <h1>새 제주 여행 만들기</h1>
      <StepIndicator steps={STEPS} current={step} />

      {step === 0 && <BasicInfoStep form={form} update={update} regions={meta.regions} />}
      {step === 1 && <CompanionStep form={form} update={update} meta={meta} />}
      {step === 2 && <SurveyStep form={form} update={update} groups={meta.groups} />}

      {error && <p className="error">{error}</p>}

      <div className="actions">
        {step > 0 && (
          <button type="button" className="btn ghost" onClick={goBack} disabled={submitting}>
            이전
          </button>
        )}
        {step < STEPS.length - 1 ? (
          <button type="button" className="btn primary" onClick={goNext}>
            다음
          </button>
        ) : (
          <button type="button" className="btn primary" onClick={submit} disabled={submitting}>
            {submitting ? '저장 중…' : 'AI 추천 받기'}
          </button>
        )}
      </div>
    </main>
  );
}