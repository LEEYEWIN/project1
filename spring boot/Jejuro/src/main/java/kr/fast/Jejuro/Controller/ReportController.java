package kr.fast.Jejuro.Controller;


// [커뮤니티 신고]

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.ReportRequest;
import kr.fast.Jejuro.Service.ReportPolicy;
import kr.fast.Jejuro.Service.ReportService;

@RestController
@RequestMapping("/api/community/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    /** 신고 사유 목록 [{ code: "SPAM", label: "스팸·광고" }, ...] */
    @GetMapping("/reasons")
    public List<Map<String, String>> reasons() {
        return ReportPolicy.reasonOptions();
    }

    /**
     * 신고하기 → { "hidden": true } (신고되면 관리자 확인 전까지 "신고된 게시글입니다"로 표시)
     * 409: 이미 신고함·이미 차단됨 / 400: 내 글
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Boolean> report(@Valid @RequestBody ReportRequest req) {
        return Map.of("hidden", reportService.report(currentUser.id(), req));
    }
}