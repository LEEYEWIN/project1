package kr.fast.Jejuro.RequestDTO;


//[관리자 신고 처리]

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
* POST /api/admin/reports/{targetType}/{targetId}/handle
* action : KEEP 문제 없음(다시 보이기) / BLOCK 차단(사유 표시) / DELETE 삭제
* blockReason: 차단 사유 (BLOCK일 때, SEXUAL / PRIVACY / ABUSE / SPAM / OTHER). 생략하면 가장 많이 받은 신고 사유
* sanction: 작성자 제재 NONE / WARNING / SUSPEND_7D / SUSPEND_30D / BAN (KEEP이면 NONE만)
* memo   : 제재 사유로 남길 말 (선택, 없으면 신고 사유로)
*/
public record ReportHandleRequest(
     @NotBlank @Pattern(regexp = "KEEP|BLOCK|DELETE") String action,
     @Pattern(regexp = "SEXUAL|PRIVACY|ABUSE|SPAM|OTHER") String blockReason,
     @Pattern(regexp = "NONE|WARNING|SUSPEND_7D|SUSPEND_30D|BAN") String sanction,
     @Size(max = 150) String memo) {

 public String sanctionOrNone() {
     return sanction == null ? "NONE" : sanction;
 }
}