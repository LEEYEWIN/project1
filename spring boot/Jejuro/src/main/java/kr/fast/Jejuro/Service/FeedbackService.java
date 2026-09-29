package kr.fast.Jejuro.Service;


//[8페이지 후기]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Entity.ExecutionStatus;
import kr.fast.Jejuro.Entity.ReasonCode;
import kr.fast.Jejuro.Entity.TravelFeedback;
import kr.fast.Jejuro.Entity.TravelFeedbackReason;
import kr.fast.Jejuro.Entity.TravelFeedbackSpot;
import kr.fast.Jejuro.ResponseDTO.RouteDetailResponse;
import kr.fast.Jejuro.Repository.TravelFeedbackSpotRepository;
import kr.fast.Jejuro.RequestDTO.FeedbackRequest;
import kr.fast.Jejuro.RequestDTO.FeedbackRequest.ReasonReq;
import kr.fast.Jejuro.ResponseDTO.FeedbackResponse;
import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Entity.Travel;
import kr.fast.Jejuro.Repository.TravelFeedbackReasonRepository;
import kr.fast.Jejuro.Repository.TravelFeedbackRepository;

@Service
public class FeedbackService {

 private final TravelAccessService travelAccessService;
 private final TravelFeedbackRepository feedbackRepository;
 private final TravelFeedbackReasonRepository reasonRepository;
 private final TravelFeedbackSpotRepository spotRepository;
 private final RouteService routeService;

 public FeedbackService(TravelAccessService travelAccessService, TravelFeedbackRepository feedbackRepository,
                        TravelFeedbackReasonRepository reasonRepository, TravelFeedbackSpotRepository spotRepository,
                        RouteService routeService) {
     this.travelAccessService = travelAccessService;
     this.feedbackRepository = feedbackRepository;
     this.reasonRepository = reasonRepository;
     this.spotRepository = spotRepository;
     this.routeService = routeService;
 }

 /** 저장된 피드백 조회. 없으면 빈 값(Optional.empty) */
 @Transactional(readOnly = true)
 public Optional<FeedbackResponse> find(Long travelId, Long userId) {
     travelAccessService.getOwned(travelId, userId);
     return feedbackRepository.findByTravelId(travelId).map(this::toResponse);
 }

 /**
  * 피드백 저장·수정(한 트랜잭션).
  * 규칙: 채택한 경로가 있어야 함 / 여행 종료일부터(종료일 당일 포함)
  *       COMPLETED  → 만족도 필수, 사유 없음
  *       PARTIAL    → 만족도 필수, 사유 1개 이상
  *       NOT_TAKEN  → 만족도 없음, 사유 1개 이상
  * 관광지별 결과(spots, 선택): 확정 일정에 있는 관광지만.
  *       COMPLETED  → 모두 "갔어요"여야 함
  *       PARTIAL    → "못 갔어요"가 1곳 이상 (필수)
  *       보내지 않은 확정 일정 관광지는 "갔어요"로 저장 → 확정 일정 관광지마다 항상 1행
  *       NOT_TAKEN  → 저장하지 않음
  */
 @Transactional
 public FeedbackResponse save(Long travelId, Long userId, FeedbackRequest req) {
     Travel travel = travelAccessService.getOwned(travelId, userId);
     if (travel.getAdoptedRouteId() == null) {
         throw new ApiException(HttpStatus.CONFLICT, "일정을 먼저 확정하세요.");
     }
     if (LocalDate.now().isBefore(travel.getEndDate())) {
         throw ApiException.badRequest("후기는 여행 종료일(" + travel.getEndDate() + ")부터 남길 수 있습니다.");
     }
     validate(req);
     validateSpots(travel, req);

     LocalDateTime now = LocalDateTime.now();
     Integer score = req.executionStatus() == ExecutionStatus.NOT_TAKEN ? null : req.satisfactionScore();

     TravelFeedback feedback = feedbackRepository.findByTravelId(travelId)
             .map(f -> {
                 f.update(travel.getAdoptedRouteId(), req.executionStatus(), score, now);
                 return f;
             })
             .orElseGet(() -> feedbackRepository.save(new TravelFeedback(
                     travelId, travel.getAdoptedRouteId(), req.executionStatus(), score, now)));

     // 사유는 전부 지우고 다시 넣는다(수정 시 상태가 바뀌어도 깔끔하게 맞춰짐)
     reasonRepository.deleteByFeedbackId(feedback.getFeedbackId());
     if (req.executionStatus() != ExecutionStatus.COMPLETED) {
         reasonRepository.saveAll(req.reasonsOrEmpty().stream()
                 .map(r -> new TravelFeedbackReason(feedback.getFeedbackId(), r.reasonCode(), blankToNull(r.reasonText())))
                 .toList());
     }
     // 관광지별 결과도 전부 지우고 다시 넣는다.
     // 확정 일정의 모든 관광지에 1행씩: 보낸 결과는 그대로, 안 보낸 곳은 "갔어요"(못 간 곳만 고르는 화면이므로)
     // → 재학습 라벨(AI_TRAINING_DATASET)과 방문률이 빈칸 없이 계산된다
     spotRepository.deleteByFeedbackId(feedback.getFeedbackId());
     if (req.executionStatus() != ExecutionStatus.NOT_TAKEN) {
         Map<Long, FeedbackRequest.SpotReq> sent = req.spotsOrEmpty().stream()
                 .collect(Collectors.toMap(FeedbackRequest.SpotReq::poiId, sp -> sp));
         spotRepository.saveAll(plannedPoiIds(travel).stream()
                 .map(poiId -> {
                     FeedbackRequest.SpotReq sp = sent.get(poiId);
                     return sp == null
                             ? new TravelFeedbackSpot(feedback.getFeedbackId(), poiId, true, null)
                             : new TravelFeedbackSpot(feedback.getFeedbackId(), poiId, sp.visited(), sp.reaction());
                 })
                 .toList());
     }
     return toResponse(feedbackRepository.findByTravelId(travelId).orElseThrow());
 }

 /** 확정 일정의 관광지 번호 (방문 순서대로) */
 private List<Long> plannedPoiIds(Travel travel) {
     RouteDetailResponse route = routeService.adoptedRouteForPublic(travel);
     return route == null ? List.of()
             : route.days().stream().flatMap(d -> d.spots().stream()).map(sp -> sp.poi().poiId()).distinct().toList();
 }

 /** 관광지별 결과 검사: 확정 일정의 관광지만, 중복 없이, 수행 결과와 맞게 */
 private void validateSpots(Travel travel, FeedbackRequest req) {
     List<FeedbackRequest.SpotReq> spots = req.spotsOrEmpty();
     if (req.executionStatus() == ExecutionStatus.PARTIAL && spots.isEmpty()) {
         throw ApiException.badRequest("못 간 곳을 하나 이상 눌러 주세요.");
     }
     if (spots.isEmpty() || req.executionStatus() == ExecutionStatus.NOT_TAKEN) {
         return;
     }
     Set<Long> planned = new HashSet<>(plannedPoiIds(travel));
     Set<Long> seen = new HashSet<>();
     for (FeedbackRequest.SpotReq sp : spots) {
         if (!planned.contains(sp.poiId())) {
             throw ApiException.badRequest("확정한 일정에 없는 관광지입니다: " + sp.poiId());
         }
         if (!seen.add(sp.poiId())) {
             throw ApiException.badRequest("같은 관광지가 두 번 들어왔습니다: " + sp.poiId());
         }
     }
     long missed = spots.stream().filter(sp -> !sp.visited()).count();
     if (req.executionStatus() == ExecutionStatus.COMPLETED && missed > 0) {
         throw ApiException.badRequest("못 간 곳이 있으면 '일부만 다녀왔어요'를 선택해 주세요.");
     }
     if (req.executionStatus() == ExecutionStatus.PARTIAL && missed == 0) {
         throw ApiException.badRequest("못 간 곳을 하나 이상 눌러 주세요.");
     }
 }

 private void validate(FeedbackRequest req) {
     List<ReasonReq> reasons = req.reasonsOrEmpty();
     switch (req.executionStatus()) {
         case COMPLETED -> {
             if (req.satisfactionScore() == null) throw ApiException.badRequest("만족도를 선택하세요.");
         }
         case PARTIAL -> {
             if (req.satisfactionScore() == null) throw ApiException.badRequest("만족도를 선택하세요.");
             if (reasons.isEmpty()) throw ApiException.badRequest("일부만 다녀온 이유를 하나 이상 고르세요.");
         }
         case NOT_TAKEN -> {
             if (reasons.isEmpty()) throw ApiException.badRequest("여행하지 못한 이유를 하나 이상 고르세요.");
         }
     }
     if (new HashSet<>(reasons.stream().map(ReasonReq::reasonCode).toList()).size() != reasons.size()) {
         throw ApiException.badRequest("같은 이유가 중복되었습니다.");
     }
     for (ReasonReq r : reasons) {
         if (r.reasonCode() == ReasonCode.OTHER && (r.reasonText() == null || r.reasonText().isBlank())) {
             throw ApiException.badRequest("기타 이유를 입력하세요.");
         }
     }
 }

 private FeedbackResponse toResponse(TravelFeedback f) {
     List<FeedbackResponse.Reason> reasons = reasonRepository.findByFeedbackIdOrderByFeedbackReasonId(f.getFeedbackId())
             .stream()
             .map(r -> new FeedbackResponse.Reason(r.getReasonCode().name(), r.getReasonText()))
             .toList();
     List<FeedbackResponse.Spot> spots = spotRepository.findByFeedbackIdOrderByFeedbackSpotId(f.getFeedbackId())
             .stream()
             .map(sp -> new FeedbackResponse.Spot(sp.getPoiId(), Boolean.TRUE.equals(sp.getVisited()), sp.getReaction()))
             .toList();
     return new FeedbackResponse(f.getFeedbackId(), f.getRouteId(), f.getExecutionStatus().name(),
             f.getSatisfactionScore(), f.getAnsweredAt(), reasons, spots);
 }

 private String blankToNull(String s) {
     return s == null || s.isBlank() ? null : s.trim();
 }
}