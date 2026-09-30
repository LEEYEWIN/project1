package kr.fast.Jejuro.ResponseDTO;

//[3-1페이지 전체 관광지 목록]

import java.util.List;

/**
* 전체 관광지 검색 결과 한 페이지.
* page는 0부터 시작, totalPages가 0이면 결과 없음.
*/
public record PoiPageResponse(
     List<PoiSummaryResponse> items,
     int page,
     int size,
     int totalPages,
     long totalCount) {
}