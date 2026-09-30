package kr.fast.Jejuro.RequestDTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawalRequest(
    @NotBlank @Size(max = 72) String password,
    @AssertTrue(message = "탈퇴 내용을 확인해 주세요.") boolean confirmed
) {}
