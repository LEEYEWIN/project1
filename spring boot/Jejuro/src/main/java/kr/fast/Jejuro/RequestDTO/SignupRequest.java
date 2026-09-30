package kr.fast.Jejuro.RequestDTO;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record SignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 입력해 주세요.") String password,
    @NotBlank @Size(min = 2, max = 30) String nickname,
    @NotNull @Past(message = "생년월일을 확인해 주세요.") LocalDate birthDate,
    @NotNull @Min(1) @Max(2) Integer genderCode) {}
