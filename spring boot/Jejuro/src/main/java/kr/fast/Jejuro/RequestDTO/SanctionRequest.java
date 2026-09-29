package kr.fast.Jejuro.RequestDTO;


// [관리자 회원 관리 - 제재]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/admin/users/{userId}/sanctions
 * 본문 예) { "type": "SUSPEND_7D", "reason": "욕설 반복" }
 * type: WARNING 경고 / SUSPEND_7D 7일 정지 / SUSPEND_30D 30일 정지 / BAN 영구 정지 / RELEASE 정지 해제
 */
public record SanctionRequest(
        @NotBlank @Pattern(regexp = "WARNING|SUSPEND_7D|SUSPEND_30D|BAN|RELEASE") String type,
        @NotBlank(message = "사유를 입력해 주세요.") @Size(max = 150, message = "사유는 150자까지 쓸 수 있습니다.") String reason) {
}