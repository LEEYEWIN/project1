package kr.fast.Jejuro.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    private final Map<String, VerificationInfo> verificationMap =
            new ConcurrentHashMap<>();

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCode(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        String code = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );

        verificationMap.put(
                normalizedEmail,
                new VerificationInfo(
                        code,
                        LocalDateTime.now().plusMinutes(5),
                        false
                )
        );

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(normalizedEmail);
        message.setSubject("[JEJURO] 이메일 인증번호");
        message.setText(
                "JEJURO 회원가입 인증번호는 [" + code + "] 입니다.\n\n"
                + "5분 이내에 입력해 주세요."
        );

        mailSender.send(message);
    }

    public boolean verifyCode(String email, String code) {

        String normalizedEmail = email.trim().toLowerCase();

        VerificationInfo info =
                verificationMap.get(normalizedEmail);

        if (info == null) {
            return false;
        }

        if (info.expiresAt().isBefore(LocalDateTime.now())) {
            verificationMap.remove(normalizedEmail);
            return false;
        }

        if (!info.code().equals(code)) {
            return false;
        }

        verificationMap.put(
                normalizedEmail,
                new VerificationInfo(
                        info.code(),
                        info.expiresAt(),
                        true
                )
        );

        return true;
    }

    public boolean isVerified(String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        VerificationInfo info =
                verificationMap.get(normalizedEmail);

        return info != null
                && info.verified()
                && info.expiresAt().isAfter(LocalDateTime.now());
    }

    public void consumeVerification(String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        verificationMap.remove(normalizedEmail);
    }

    private record VerificationInfo(
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {}
}