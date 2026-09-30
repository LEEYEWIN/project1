package kr.fast.Jejuro.RequestDTO;

// [관리자 신고 처리]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /api/admin/reports/{targetType}/{targetId}/handle
 * action     : KEEP 반려(정상 표시) / BLOCK 차단(사유 알림 후 목록으로)
 * blockReason: 차단 사유 (BLOCK일 때, SEXUAL / PRIVACY / ABUSE / SPAM). 생략하면 가장 많이 받은 신고 사유
 */
public record ReportHandleRequest(
        @NotBlank @Pattern(regexp = "KEEP|BLOCK") String action,
        @Pattern(regexp = "SEXUAL|PRIVACY|ABUSE|SPAM") String blockReason) {
}