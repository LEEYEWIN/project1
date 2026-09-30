package kr.fast.Jejuro.ResponseDTO;



//[관리자 AI 추천 KPI 대시보드]

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
* GET /api/admin/kpi 응답. 화면의 구역 순서대로 들어 있다.
* 비율은 0~1 (화면에서 %로 바꿈). 데이터가 없으면 null.
*/
public record AdminKpiResponse(
   Filter filter,
   Summary summary,
   List<FunnelStep> funnel,
   Trend trend,
   Map<String, List<Segment>> segments,   // 기준(companion/age/region/days/motive) → 세그먼트 목록
   List<PoiStat> overRecommended,
   List<PoiStat> missed,
   MissReasons missReasons,
   Dataset dataset) {

public record Filter(int days, LocalDate from, LocalDate to) {
}

/**
* 상단 카드 6개.
* retrainReady = 재학습 데이터셋에 들어간 여행 수(후기까지 끝난 여행), retrainThreshold = 재학습 기준(100)
*/
public record Summary(
       int requestCount, int recommendedTravels, int reRequestCount,
       Double adoptionRate, Double adoptionRatePrev,
       Double scheduleRate, Double scheduleRatePrev,
       Double visitRate, Double avgSatisfaction, int feedbackCount,
       int retrainReady, int retrainThreshold) {
}

/**
* 퍼널 한 단계 (TRAVEL_FUNNEL 뷰).
* count = 이 단계까지 온 여행 수(가장 멀리 간 단계 기준 누적), rate = 앞 단계 대비 전환율,
* dropped = 이 단계로 넘어가지 못하고 이탈 확정된 여행 수,
* waiting = 아직 진행 중이라 이 단계 바로 앞에 머물러 있는 여행 수
* → 앞 단계 count = 이 단계 count + dropped + waiting (공유 단계 제외)
*/
public record FunnelStep(String key, String label, int count, Double rate, int dropped, int waiting) {
}

/** 주별 채택률 (최근 8주) */
public record Trend(List<TrendPoint> points) {
}

public record TrendPoint(LocalDate weekStart, int items, Double adoptionRate) {
}

/** 세그먼트 한 줄. level = OK / WATCH / WEAK / LOW_DATA */
public record Segment(String name, int travels, Double adoptionRate, Double visitRate, Double avgSatisfaction,
                     String level) {
}

/** 관광지 표 한 줄 (과추천 / AI가 놓친 곳) */
public record PoiStat(Long poiId, String name, int recommended, int added, int searchAdded, Double adoptionRate) {
}

/** 못 간 이유 */
public record MissReasons(int partialCount, List<Reason> reasons, List<String> oftenMissed) {
}

public record Reason(String code, String label, int count, Double rate) {
}

/** 재학습 데이터셋 (AI_TRAINING_DATASET 뷰): 라벨별 행 수, 전체 행 수, 여행 수 */
public record Dataset(List<LabelCount> labels, int total, int travels, DataQuality quality) {
}

public record LabelCount(int label, String name, int count) {
}

public record DataQuality(Double feedbackResponseRate, Double spotInputRate, Double surveyMissingRate,
                         int outlierTravels) {
}
}