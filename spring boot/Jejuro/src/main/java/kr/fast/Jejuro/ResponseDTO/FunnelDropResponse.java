package kr.fast.Jejuro.ResponseDTO;


//[관리자 AI KPI - 퍼널 이탈 로그]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
* GET /api/admin/kpi/funnel-drops 응답: 이탈 확정된 여행 목록 (TRAVEL_FUNNEL.funnel_status = 'DROPPED')
* counts = 못 간 단계(drop_step)별 건수 (필터 버튼 숫자)
*/
public record FunnelDropResponse(
     int page,
     int totalPages,
     long total,
     Map<String, Integer> counts,
     List<Item> items) {

 /**
  * 이탈 여행 한 건 + 근거가 되는 사실들.
  * reachedStep = 멈춘 단계, dropStep = 넘어가지 못한 단계
  * deadline = 이탈 판단 기한 (후기 단계는 종료일 + 14일, 그 외 종료일), overdueDays = 기한이 지난 날수
  * placeCount/placedCount = 담은 장소 수 / 경로에 배치된 수, recommendCount = 추천 받은 횟수(성공),
  * shownCount = 화면에 보여 준 추천 관광지 수(합), scheduledCount = 확정 일정의 관광지 수
  */
 public record Item(Long travelId, String travelName, String nickname, String reachedStep, String dropStep,
                    LocalDateTime createdAt, LocalDate endDate, LocalDate deadline, long overdueDays,
                    int placeCount, int placedCount, int recommendCount, int shownCount, int scheduledCount) {
 }
}