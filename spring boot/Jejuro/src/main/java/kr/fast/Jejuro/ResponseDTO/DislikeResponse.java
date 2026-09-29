package kr.fast.Jejuro.ResponseDTO;


//[관심없음 관광지 관리]

import java.time.LocalDateTime;

/** 관심없음으로 표시한 관광지 한 곳 (카드용 관광지 정보 + 표시한 시각) */
public record DislikeResponse(PoiSummaryResponse poi, LocalDateTime createdAt) {
}