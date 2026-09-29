package kr.fast.Jejuro.ResponseDTO;

// [7페이지 내 여행 (목록 + 달력)]

import java.time.LocalDate;
import java.util.List;

/**
 * 내 여행 목록 카드 한 장 + 달력에 그릴 일차별 일정.
 * phase: BEFORE(여행 전) / DURING(여행 중) / AFTER(다녀옴) — 오늘 날짜 기준 계산값
 * placeCount: 여행 장소 수, spotCount: 그중 경로에 배치된 방문지 수
 * imported: 커뮤니티 글의 경로를 가져와 만든 여행
 * days: 경로에 방문지가 있는 일차만 [{ dayNo, date, spots: ["성산일출봉", ...] }]
 */
public record TravelSummaryResponse(
        Long travelId,
        Integer travelNo,
        String travelName,
        LocalDate startDate,
        LocalDate endDate,
        int tripDays,
        String phase,
        List<String> regionNames,
        int companionCount,
        int routeCount,
        Long adoptedRouteId,
        boolean hasFeedback,
        boolean imported,
        int placeCount,
        int spotCount,
        List<DayPlan> days) {

    public record DayPlan(int dayNo, LocalDate date, List<String> spots) {
    }
}
