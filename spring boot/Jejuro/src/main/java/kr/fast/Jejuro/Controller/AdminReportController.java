package kr.fast.Jejuro.Controller;


// [관리자 신고 처리]

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.fast.Jejuro.Config.AdminGuard;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.RequestDTO.ReportHandleRequest;
import kr.fast.Jejuro.ResponseDTO.AdminReportResponse;
import kr.fast.Jejuro.Service.ReportService;

/** 관리자 전용 (USER.role = ADMIN). 아니면 403 */
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final AdminGuard adminGuard;
    private final CurrentUser currentUser;

    public AdminReportController(ReportService reportService, AdminGuard adminGuard, CurrentUser currentUser) {
        this.reportService = reportService;
        this.adminGuard = adminGuard;
        this.currentUser = currentUser;
    }

    /** GET /api/admin/reports?status=PENDING|DONE&type=POST|COMMENT&page=0 (신고된 글·댓글 단위) */
    @GetMapping
    public AdminReportResponse list(@RequestParam(name = "status", defaultValue = "PENDING") String status,
                                    @RequestParam(name = "type", required = false) String type,
                                    @RequestParam(name = "page", defaultValue = "0") int page) {
        adminGuard.check();
        return reportService.list(status, type, page);
    }

    /** 차단 해제: POST /api/admin/reports/POST/12/unblock */
    @PostMapping("/{targetType}/{targetId}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable("targetType") String targetType, @PathVariable("targetId") Long targetId) {
        adminGuard.check();
        reportService.unblock(targetType.toUpperCase(), targetId);
    }

    /**
     * 처리: POST /api/admin/reports/POST/12/handle
     * 본문 { "action": "BLOCK", "blockReason": "ABUSE" } 또는 { "action": "KEEP" } (반려)
     */
    @PostMapping("/{targetType}/{targetId}/handle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(@PathVariable("targetType") String targetType, @PathVariable("targetId") Long targetId,
                       @Valid @RequestBody ReportHandleRequest req) {
        adminGuard.check();
        reportService.handle(currentUser.id(), targetType.toUpperCase(), targetId, req);
    }
}