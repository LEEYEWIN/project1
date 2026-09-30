package kr.fast.Jejuro.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Service.EmailVerificationService;

@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService service;

    public EmailVerificationController(
            EmailVerificationService service
    ) {
        this.service = service;
    }

    @PostMapping("/send")
    public Map<String, String> send(
            @RequestBody Map<String, String> body
    ) {

        String email = body.get("email");

        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "이메일을 입력해 주세요."
            );
        }

        service.sendCode(email);

        return Map.of(
                "message",
                "인증번호를 전송했습니다."
        );
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(
            @RequestBody Map<String, String> body
    ) {

        String email = body.get("email");
        String code = body.get("code");

        if (email == null || email.isBlank()
                || code == null || code.isBlank()) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "이메일과 인증번호를 입력해 주세요."
            );
        }

        boolean verified =
                service.verifyCode(email, code);

        if (!verified) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호가 올바르지 않거나 만료되었습니다."
            );
        }

        return Map.of(
                "verified",
                true
        );
    }
}