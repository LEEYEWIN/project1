package kr.fast.Jejuro.Service;


//[8페이지 후기]

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Entity.ExecutionStatus;
import kr.fast.Jejuro.Entity.ReasonCode;
import kr.fast.Jejuro.Entity.TravelFeedback;
import kr.fast.Jejuro.Entity.TravelFeedbackReason;
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

 public FeedbackService(TravelAccessService travelAccessService, TravelFeedbackRepository feedbackRepository,
                        TravelFeedbackReasonRepository reasonRepository) {
     this.travelAccessService = travelAccessService;
     this.feedbackRepository = feedbackRepository;
     this.reasonRepository = reasonRepository;
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
  */
 @Transactional
 public FeedbackResponse save(Long travelId, Long userId, FeedbackRequest req) {
     Travel travel = travelAccessService.getOwned(travelId, userId);
     if (travel.getAdoptedRouteId() == null) {
         throw new ApiException(HttpStatus.CONFLICT, "최종 경로를 먼저 채택하세요.");
     }
     if (LocalDate.now().isBefore(travel.getEndDate())) {
         throw ApiException.badRequest("후기는 여행 종료일(" + travel.getEndDate() + ")부터 남길 수 있습니다.");
     }
     validate(req);

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
     return toResponse(feedbackRepository.findByTravelId(travelId).orElseThrow());
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
     return new FeedbackResponse(f.getFeedbackId(), f.getRouteId(), f.getExecutionStatus().name(),
             f.getSatisfactionScore(), f.getAnsweredAt(), reasons);
 }

 private String blankToNull(String s) {
     return s == null || s.isBlank() ? null : s.trim();
 }
}