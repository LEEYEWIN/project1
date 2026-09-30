package kr.fast.Jejuro.ResponseDTO;


//[4페이지 찜]

/** 찜 한 건 = 찜 ID + 관광지 정보 */
public record BookmarkResponse(Long bookmarkId, PoiSummaryResponse poi) {
}
