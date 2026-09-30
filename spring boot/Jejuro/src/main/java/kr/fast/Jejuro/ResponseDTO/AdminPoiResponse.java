package kr.fast.Jejuro.ResponseDTO;


//[관리자 관광지 관리]

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 관광지 목록 한 페이지 + 데이터 점검 숫자 */
public record AdminPoiResponse(
     int page,
     int totalPages,
     long totalCount,
     Checks checks,
     List<Row> items) {

 /**
  * 데이터 점검 (전체 기준, 숨긴 곳·삭제한 곳 제외)
  * aiRecommend AI 추천 대상(학습 275곳) / manualOnly 직접 선택만(AI 추천에 안 나옴)
  * noImage 사진 없음 / noDescription 소개 없음 / outOfJeju 좌표 오류
  */
 public record Checks(long total, long hidden, long deleted, long aiRecommend, long manualOnly,
                      long noImage, long noDescription, long outOfJeju) {
 }

 public record Row(Long poiId, String poiName, String address, String categoryCode, String categoryName,
                   Integer regionId, String regionName, String imageUrl, boolean hidden, boolean deleted,
                   boolean aiRecommend, boolean noImage, boolean noDescription, boolean outOfJeju,
                   long bookmarkCount, long recommendCount) {
 }

 /** 수정 화면: 전체 값 + AI 추천 대상 여부 + 연결된 AI 이름 + 쓰이는 곳 */
 public record Detail(Long poiId, String poiName, String address, BigDecimal latitude, BigDecimal longitude,
                      String categoryCode, Integer regionId, String description, String detailDescription,
                      String imageUrl, String phone, String homepage, String openingHours, String closedDays,
                      String fee, String parking, LocalDateTime hiddenAt, LocalDateTime deletedAt,
                      boolean aiRecommend, List<Mapping> mappings, Usage usage) {
 }

 /** ai=true: AI 추천 이름 / false: 원본 데이터 ID(VJ:…, TOUR:…) */
 public record Mapping(String sourcePoiId, boolean ai) {
 }

 /** 이 관광지를 쓰는 곳 (숨기기 전에 확인) */
 public record Usage(long bookmarks, long routeSpots, long recommended, long feedbackSpots) {
 }

 /** 사진 점검 한 줄 (관리자 [깨진 사진 찾기]) */
 public record ImageRow(Long poiId, String poiName, String imageUrl, boolean hidden) {
 }

 /** 폼 선택지 */
 public record Options(List<Option> regions, List<Option> categories) {
 }

 public record Option(String value, String label) {
 }
}