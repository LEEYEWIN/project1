package kr.fast.Jejuro.Controller;

// [8페이지 후기]

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.RequestDTO.FeedbackRequest;
import kr.fast.Jejuro.ResponseDTO.FeedbackResponse;
import kr.fast.Jejuro.Config.CurrentUser;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Service.FeedbackService;

@RestController
@RequestMapping("/api/travels/{travelId}/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUser currentUser;

    public FeedbackController(FeedbackService feedbackService, CurrentUser currentUser) {
        this.feedbackService = feedbackService;
        this.currentUser = currentUser;
    }

    /** 저장된 피드백. 아직 없으면 204(No Content) */
    @GetMapping
    public ResponseEntity<FeedbackResponse> find(@PathVariable("travelId") Long travelId) {
        return feedbackService.find(travelId, currentUser.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 저장·수정 */
    @PutMapping
    public FeedbackResponse save(@PathVariable("travelId") Long travelId, @Valid @RequestBody FeedbackRequest req) {
        return feedbackService.save(travelId, currentUser.id(), req);
    }
}