import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { createTravel, fetchMyTravels, fetchTravelCopy, fetchTravelForm } from '../api/travelApi.js';
import { busyBetween, busyRanges, todayIso } from '../utils/travelDates.js';
import { formatDate } from '../utils/format.js';
import { errorMessage, showError } from '../api/client.js';
import { savePickStyle } from '../utils/recommendStorage.js';
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
  pickStyle: '', // 'POPULAR' | 'UNIQUE'  설문 첫 질문(여행지 선택 성향). 서버로 보내지 않음
};

/**
 * 1페이지: 여행 만들기 + 설문 (3단계 폼)
 * - 날짜는 오늘부터, 이미 여행이 있는 날은 달력에서 고를 수 없다 (서버도 같은 규칙으로 검사)
 * - ?replace=여행번호 : 여행 상세의 "날짜·동행 바꿔 다시 만들기". 기존 여행 값이 채워진 채로 열리고,
 *   그 여행과는 기간이 겹쳐도 만들 수 있다. 새 여행을 확정할 때 기존 여행이 삭제된다.
 */
export default function TravelCreatePage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const replaceId = params.get('replace');
  const [meta, setMeta] = useState(null); // 서버에서 받은 권역·코드·질문
  const [form, setForm] = useState(INITIAL_FORM);
  const [step, setStep] = useState(0);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [travels, setTravels] = useState([]); // 내 여행 (이미 여행이 있는 날 표시)
  const [original, setOriginal] = useState(null); // 바꿔 만들 기존 여행 값

  // 페이지 진입 시 설문 틀 · 내 여행 기간 · (바꿔 만들기면) 기존 여행 값을 불러온다
  useEffect(() => {
    fetchTravelForm()
      .then(setMeta)
      .catch((e) => setError(errorMessage(e)));
    fetchMyTravels()
      .then(setTravels)
      .catch(() => setTravels([])); // 실패해도 서버가 겹침을 다시 검사
  }, []);

  useEffect(() => {
    if (!replaceId) return;
    fetchTravelCopy(replaceId)
      .then((c) => {
        setOriginal(c);
        const keepDates = c.startDate >= todayIso(); // 지난 날짜는 비워 두고 다시 고르게
        setForm({
          ...INITIAL_FORM,
          travelName: c.travelName,
          startDate: keepDates ? c.startDate : '',
          endDate: keepDates ? c.endDate : '',
          regionMode: c.regionMode,
          regionIds: c.regionIds ?? [],
          companions: c.companions ?? [],
          answers: Object.fromEntries((c.answers ?? []).map((a) => [a.preferenceId, a.values])),
        });
      })
      .catch((e) => setError(errorMessage(e)));
  }, [replaceId]);

  const busy = busyRanges(travels, replaceId);

  // 자식 컴포넌트가 form 일부만 바꿀 때 사용
  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));

  const validateStep = (index) => {
    if (index === 0) {
      if (!form.travelName.trim()) return '여행 이름을 입력하세요.';
      if (!form.startDate || !form.endDate) return '여행 날짜를 선택하세요.';
      if (form.startDate > form.endDate) return '종료일이 시작일보다 빠릅니다.';
      if (form.startDate < todayIso()) return '여행은 오늘부터 시작하는 날짜로 만들 수 있어요.';
      const hit = busyBetween(form.startDate, form.endDate, busy);
      if (hit) return `'${hit.name}' 여행(${hit.start} ~ ${hit.end})과 기간이 겹쳐요. 다른 날짜를 골라 주세요.`;
      if (form.regionMode === 'SELECTED' && form.regionIds.length === 0) return '권역을 하나 이상 고르세요.';
    }
    if (index === 1) {
      const incomplete = form.companions.some((c) => !c.relationCode || !c.genderCode || !c.ageGroupCode);
      if (incomplete) return '동반자 정보를 모두 선택하세요.';
      if (form.companions.length > 18) return '동반자는 최대 18명까지 입력할 수 있습니다.';
    }
    if (index === 2) {
      if (!form.pickStyle) return '여행지를 고를 때 어떤 쪽에 더 가까운지 골라 주세요.';
      for (const group of meta.groups) {
        for (const q of group.questions) {
          const count = (form.answers[q.preferenceId] ?? []).length;
          if (count < group.minSelections) {
            if (group.minSelections === 1) return `'${q.name}' 질문에 답해 주세요.`;
            return group.minSelections === group.maxSelections
              ? `'${q.name}'을(를) ${group.minSelections}개 골라 주세요. (지금 ${count}개)`
              : `'${q.name}'을(를) 최소 ${group.minSelections}개 골라 주세요. (지금 ${count}개)`;
          }
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

    // 화면용 form → 서버 요청 형식(TravelCreateRequest)으로 변환 (pickStyle 은 보내지 않음)
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
      replaceTravelId: replaceId ? Number(replaceId) : null,
    };
    // 바꿔 만들기인데 새 날짜가 기존 여행과 겹치지 않으면, 확정해도 기존 여행은 지워지지 않는다
    if (original && (form.endDate < original.startDate || form.startDate > original.endDate)
      && !window.confirm(`새 날짜가 기존 '${original.travelName}' 여행과 겹치지 않아요.\n기존 여행은 그대로 남으니 필요 없으면 여행 상세에서 삭제해 주세요.\n계속 만들까요?`)) {
      return;
    }

    setSubmitting(true);
    try {
      const { travelId } = await createTravel(payload);
      savePickStyle(travelId, form.pickStyle); // 추천 목록 안내 문구용 (이 브라우저 탭에만 보관)
      navigate(`/travels/${travelId}/recommending`); // 2페이지로
    } catch (e) {
      // 기간이 겹치는 여행(409) 등 막힌 경우는 알림창, 입력 오류는 화면 메시지
      showError(e, setError);
      setSubmitting(false);
    }
  };

  if (!meta) {
    return <main className="page">{error ? <p className="error">{error}</p> : <p>불러오는 중…</p>}</main>;
  }

  return (
    <main className="page">
      <h1>{replaceId ? '날짜·동행 바꿔 다시 만들기' : '새 제주 여행 만들기'}</h1>
      {original && (
        <p className="replace-note" role="note">
          기존 <b>‘{original.travelName}’</b>({formatDate(original.startDate)} ~ {formatDate(original.endDate)}) 여행의 값을 채워 두었어요.
          바뀐 항목만 고치고, 설문 답도 다시 확인해 주세요. 기존 여행은 ‘변경 전 일정’으로 보기만 할 수 있고,
          새 여행을 확정할 때 삭제돼요.
        </p>
      )}
      <StepIndicator steps={STEPS} current={step} />

      {step === 0 && <BasicInfoStep form={form} update={update} regions={meta.regions} busy={busy} />}
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