package kr.fast.Jejuro.Controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {

        String email = body.get("email");

        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "이메일을 입력해 주세요."
            );
        }

        service.sendCode(email, request.getRemoteAddr());

        return Map.of(
                "message",
                "인증번호를 전송했습니다."
        );
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(
            @RequestBody Map<String, String> body,
            HttpServletRequest request
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

        // 틀림·만료·횟수 초과는 서비스가 알맞은 메시지로 400을 던진다.
        // 맞으면 이 브라우저 세션에만 인증 완료를 표시한다.
        service.verifyCode(email, code, request.getSession());

        return Map.of(
                "verified",
                true
        );
    }
}