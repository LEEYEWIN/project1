package kr.fast.Jejuro.ResponseDTO;


//[관심없음 관광지 관리 - 이전에 추천받은 관광지]

import java.time.LocalDateTime;

/**
* 내 여행에서 AI가 추천해 화면에 보여 준 관광지 한 곳.
* disliked: 지금 관심없음으로 표시되어 있는지 (USER_POI_DISLIKE에 있으면 true)
* lastRecommendedAt: 가장 최근에 추천받은 시각, times: 추천받은 횟수(추천 요청 기준)
*/
public record RecommendedPoiResponse(PoiSummaryResponse poi, boolean disliked, LocalDateTime lastRecommendedAt, long times) {
}