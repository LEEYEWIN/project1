package kr.fast.Jejuro.RequestDTO;


// [커뮤니티 신고]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/community/reports
 * 본문 예) { "targetType": "POST", "targetId": 12, "reasonCode": "SPAM", "detail": "" }
 * reasonCode: SEXUAL 음란·불법 / PRIVACY 개인정보 노출 / ABUSE 욕설·비방 / SPAM 스팸·광고
 * detail: 선택 (200자)
 */
public record ReportRequest(
        @NotNull @Pattern(regexp = "POST|COMMENT") String targetType,
        @NotNull Long targetId,
        @NotBlank @Pattern(regexp = "SEXUAL|PRIVACY|ABUSE|SPAM", message = "신고 사유를 골라 주세요.") String reasonCode,
        @Size(max = 200, message = "자세한 내용은 200자까지 쓸 수 있습니다.") String detail) {
}