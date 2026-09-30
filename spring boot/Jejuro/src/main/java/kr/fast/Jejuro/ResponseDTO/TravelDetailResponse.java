package kr.fast.Jejuro.ResponseDTO;

// [7페이지 여행 상세 · 3페이지 추천 기준 안내]

import java.time.LocalDate;
import java.util.List;

/**
 * 여행 상세. 여행당 경로는 1개(route), 확정한 경로의 일정과 후기까지 한 번에 내려준다.
 * - imported / sourcePostId: 커뮤니티 글의 경로를 가져와 만든 여행이면 true / 그 글 번호 (설문·AI 추천 없음)
 * - survey: 설문 답변 요약 (추천 목록 화면의 "추천 기준"에 표시). 가져온 여행은 빈 목록
 * - route: 이 여행의 경로 요약 (아직 없으면 null)
 * - placeCount / placedCount: 여행 장소 수 / 그중 경로에 배치된 수 → 같아야 일정 확정 가능
 */
public record TravelDetailResponse(
        Long travelId,
        Integer travelNo,
        String travelName,
        LocalDate startDate,
        LocalDate endDate,
        int tripDays,
        String phase,
        List<String> regionNames,
        List<CompanionItem> companions,
        boolean imported,
        Long sourcePostId,
        List<SurveyItem> survey,
        RouteSummaryResponse route,
        int placeCount,
        int placedCount,
        RouteDetailResponse adoptedRoute,
        FeedbackResponse feedback,
        boolean canWriteFeedback) {

    public record CompanionItem(int seq, String relation, String gender, String ageGroup) {
    }

    /**
     * 설문 질문 하나의 답.
     * ranked=true(여행 동기·테마)이면 answers가 1순위부터 순서대로 들어 있고, AI에는 1순위만 전달된다.
     */
    public record SurveyItem(Long preferenceId, String question, List<String> answers, boolean ranked) {
    }
}
