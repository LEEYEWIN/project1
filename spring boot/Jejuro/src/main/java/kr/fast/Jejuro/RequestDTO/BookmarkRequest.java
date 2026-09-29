package kr.fast.Jejuro.RequestDTO;



//[4페이지 여행 장소 추가]

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
* POST /api/travels/{travelId}/bookmarks
* 본문 예) { "poiId": 2001, "source": "RECOMMEND" }
* source(선택): 어느 화면에서 담았나 — RECOMMEND(AI 추천 목록) / SEARCH(관광지 목록·상세). 생략하면 SEARCH
* (관리자 KPI의 "추천 채택률", "AI가 놓친 관광지" 계산에 사용)
*/
public record BookmarkRequest(
     @NotNull Long poiId,
     @Pattern(regexp = "RECOMMEND|SEARCH") String source) {

 public String sourceOrSearch() {
     return source == null ? "SEARCH" : source;
 }
}