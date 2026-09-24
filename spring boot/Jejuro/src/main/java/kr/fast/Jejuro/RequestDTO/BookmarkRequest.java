package kr.fast.Jejuro.RequestDTO;


//[4페이지 찜]

import jakarta.validation.constraints.NotNull;

/** POST /api/travels/{travelId}/bookmarks  본문 예) { "poiId": 2001 } */
public record BookmarkRequest(@NotNull Long poiId) {
}