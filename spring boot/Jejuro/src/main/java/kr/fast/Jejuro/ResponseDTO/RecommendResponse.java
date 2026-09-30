package kr.fast.Jejuro.ResponseDTO;


//[2페이지 AI 추천 중]

import java.util.List;

/** 2페이지 응답 = 3페이지에서 보여줄 추천 관광지 목록(추천 순위 순서) */
public record RecommendResponse(Long travelId, List<PoiSummaryResponse> pois) {
}