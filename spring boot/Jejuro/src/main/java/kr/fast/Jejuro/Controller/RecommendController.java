package kr.fast.Jejuro.Controller;



// [2페이지 AI 추천 중]

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.ResponseDTO.RecommendResponse;
import kr.fast.Jejuro.Service.RecommendService;

@RestController
@RequestMapping("/api/travels/{travelId}/recommendations")
public class RecommendController {

    private final RecommendService recommendService;
    private final CurrentUser currentUser;

    public RecommendController(RecommendService recommendService, CurrentUser currentUser) {
        this.recommendService = recommendService;
        this.currentUser = currentUser;
    }

    /** 이미 받은 추천 목록 다시 보기 (추천 기록 RECOMMEND_ITEM 기준). 받은 적 없으면 404 */
    @GetMapping
    public RecommendResponse latest(@PathVariable("travelId") Long travelId) {
        return recommendService.latest(travelId, currentUser.id());
    }

    /** 2페이지: AI 추천 요청 (이미 받은 추천이 있으면 AI를 다시 부르지 않고 그 결과) */
    @PostMapping
    public RecommendResponse recommend(@PathVariable("travelId") Long travelId) {   // ← ("travelId") 필수
        return recommendService.recommend(travelId, currentUser.id());
    }
}