package kr.fast.Jejuro.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Config.ApiException;
import kr.fast.Jejuro.Repository.UserRepository;

/**
 * 회원가입 이메일 인증.
 * - 인증번호는 DB EMAIL_VERIFICATION에 해시로 저장한다 → 서버를 재시작하거나 여러 서버로 나눠도 이어서 확인 가능.
 *   유효시간은 발송 후 5분, 다시 받으면 이전 번호는 무효.
 * - 발송 제한: 같은 이메일은 60초에 1번·1시간에 5번, 같은 IP는 1시간에 20번.
 * - 확인 제한: 번호 하나당 5번 틀리면 그 번호는 무효(다시 받아야 함).
 * - 인증 완료 표시는 "인증한 브라우저의 서버 세션"에만 남긴다. 다른 사람이 같은 이메일로 가입을 시도해도
 *   그 사람 세션에는 인증 표시가 없으므로 가입할 수 없다. 가입에 성공하면 표시를 지운다(1회용).
 *   인증 후 가입까지 따로 시간 제한은 없고, 세션이 끝나면(30분 동안 요청 없음) 다시 인증한다.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    static final String PURPOSE = "SIGNUP";
    static final String SESSION_KEY = "verifiedSignupEmail";
    static final int CODE_MINUTES = 5;
    static final int RESEND_SECONDS = 60;
    static final int MAX_SENDS_PER_EMAIL_HOUR = 5;
    static final int MAX_SENDS_PER_IP_HOUR = 20;
    static final int MAX_FAILS = 5;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final SecureRandom random = new SecureRandom();
    /** IP별 최근 1시간 발송 시각 (서버마다 따로 세는 보조 제한. 이메일별 제한은 DB로 모든 서버가 공유) */
    private final Map<String, List<LocalDateTime>> sendsByIp = new ConcurrentHashMap<>();

    public EmailVerificationService(JavaMailSender mailSender, JdbcTemplate jdbc, UserRepository users) {
        this.mailSender = mailSender;
        this.jdbc = jdbc;
        this.users = users;
    }

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    /** 인증번호 발송 */
    public void sendCode(String rawEmail, String clientIp) {
        String email = normalize(rawEmail);
        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("이메일 형식을 확인해 주세요.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 로그인하거나 다른 이메일을 입력해 주세요.");
        }
        LocalDateTime now = LocalDateTime.now();
        checkIpLimit(clientIp, now);

        Timestamp last = jdbc.query("""
                SELECT MAX(created_at) FROM EMAIL_VERIFICATION WHERE email = ? AND purpose = ?
                """, rs -> rs.next() ? rs.getTimestamp(1) : null, email, PURPOSE);
        if (last != null) {
            // DATETIME은 초 단위로 반올림되어 저장되므로 음수가 나오지 않게 0 이상으로 맞춘다
            long waited = Math.max(0, java.time.Duration.between(last.toLocalDateTime(), now).getSeconds());
            if (waited < RESEND_SECONDS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "인증번호는 1분에 한 번만 다시 받을 수 있어요. " + (RESEND_SECONDS - waited) + "초 뒤에 다시 시도해 주세요.");
            }
        }
        Integer recent = jdbc.queryForObject("""
                SELECT COUNT(*) FROM EMAIL_VERIFICATION WHERE email = ? AND purpose = ? AND created_at > ?
                """, Integer.class, email, PURPOSE, Timestamp.valueOf(now.minusHours(1)));
        if (recent != null && recent >= MAX_SENDS_PER_EMAIL_HOUR) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "인증번호를 너무 많이 요청했어요. 1시간 뒤에 다시 시도해 주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        // 이전 번호는 무효로 하고 새 번호 저장
        jdbc.update("UPDATE EMAIL_VERIFICATION SET used_at = ? WHERE email = ? AND purpose = ? AND used_at IS NULL",
                Timestamp.valueOf(now), email, PURPOSE);
        jdbc.update("""
                INSERT INTO EMAIL_VERIFICATION (email, purpose, code_verifier, expires_at, failed_count, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """, email, PURPOSE, hash(email, code), Timestamp.valueOf(now.plusMinutes(CODE_MINUTES)),
                Timestamp.valueOf(now));
        recordIpSend(clientIp, now);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[JEJURO] 이메일 인증번호");
        message.setText("JEJURO 회원가입 인증번호는 [" + code + "] 입니다.\n\n"
                + CODE_MINUTES + "분 이내에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패 email={} : {}", email, e.getMessage());
            // 보내지 못한 번호는 지운다 (남겨 두면 1분 재전송 제한·발송 횟수에 걸려 바로 다시 시도할 수 없음)
            jdbc.update("DELETE FROM EMAIL_VERIFICATION WHERE email = ? AND purpose = ? AND used_at IS NULL", email, PURPOSE);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    /**
     * 인증번호 확인. 맞으면 번호를 사용 처리하고, 이 세션에만 인증 완료를 표시한다.
     * 틀리면 남은 횟수를 알려 주고, 5번 틀리면 번호를 무효로 한다.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyCode(String rawEmail, String code, HttpSession session) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT verification_id, code_verifier, expires_at, failed_count
                  FROM EMAIL_VERIFICATION
                 WHERE email = ? AND purpose = ? AND used_at IS NULL
                 ORDER BY verification_id DESC
                 LIMIT 1
                 FOR UPDATE
                """, email, PURPOSE);
        if (rows.isEmpty()) {
            throw ApiException.badRequest("인증번호가 올바르지 않거나 만료되었습니다. 인증번호를 다시 받아 주세요.");
        }
        Map<String, Object> row = rows.get(0);
        long id = ((Number) row.get("verification_id")).longValue();
        LocalDateTime expiresAt = toLocalDateTime(row.get("expires_at"));
        int failed = ((Number) row.get("failed_count")).intValue();

        if (expiresAt.isBefore(now)) {
            jdbc.update("UPDATE EMAIL_VERIFICATION SET used_at = ? WHERE verification_id = ?", Timestamp.valueOf(now), id);
            throw ApiException.badRequest("인증 시간이 지났어요. 인증번호를 다시 받아 주세요.");
        }
        if (!MessageDigest.isEqual(hash(email, code).getBytes(StandardCharsets.UTF_8),
                String.valueOf(row.get("code_verifier")).getBytes(StandardCharsets.UTF_8))) {
            int fails = failed + 1;
            if (fails >= MAX_FAILS) {
                jdbc.update("UPDATE EMAIL_VERIFICATION SET failed_count = ?, used_at = ? WHERE verification_id = ?",
                        fails, Timestamp.valueOf(now), id);
                throw ApiException.badRequest("인증번호를 " + MAX_FAILS + "번 틀려 이 번호는 더 이상 쓸 수 없어요. 인증번호를 다시 받아 주세요.");
            }
            jdbc.update("UPDATE EMAIL_VERIFICATION SET failed_count = ? WHERE verification_id = ?", fails, id);
            throw ApiException.badRequest("인증번호가 올바르지 않습니다. (남은 입력 횟수 " + (MAX_FAILS - fails) + "번)");
        }
        jdbc.update("UPDATE EMAIL_VERIFICATION SET used_at = ? WHERE verification_id = ?", Timestamp.valueOf(now), id);
        session.setAttribute(SESSION_KEY, email);
    }

    /** 이 세션이 이 이메일을 인증했는지 */
    public boolean isVerified(HttpSession session, String rawEmail) {
        if (session == null) return false;
        Object verified = session.getAttribute(SESSION_KEY);
        return verified instanceof String v && v.equals(normalize(rawEmail));
    }

    /** 가입 완료 → 인증 표시 소비(같은 인증으로 두 번 가입 불가) */
    public void consumeVerification(HttpSession session) {
        if (session != null) session.removeAttribute(SESSION_KEY);
    }

    /** 하루 지난 인증 기록과 1시간 지난 IP 기록 정리 (매시 정각) */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbc.update("DELETE FROM EMAIL_VERIFICATION WHERE created_at < ?", Timestamp.valueOf(now.minusDays(1)));
        } catch (RuntimeException e) {
            log.warn("이메일 인증 기록 정리 실패: {}", e.getMessage());
        }
        sendsByIp.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                e.getValue().removeIf(t -> t.isBefore(now.minusHours(1)));
                return e.getValue().isEmpty();
            }
        });
    }

    private void checkIpLimit(String ip, LocalDateTime now) {
        if (ip == null) return;
        List<LocalDateTime> times = sendsByIp.get(ip);
        if (times == null) return;
        synchronized (times) {
            times.removeIf(t -> t.isBefore(now.minusHours(1)));
            if (times.size() >= MAX_SENDS_PER_IP_HOUR) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "인증번호 요청이 너무 많아요. 잠시 후 다시 시도해 주세요.");
            }
        }
    }

    private void recordIpSend(String ip, LocalDateTime now) {
        if (ip == null) return;
        List<LocalDateTime> times = sendsByIp.computeIfAbsent(ip, k -> new java.util.ArrayList<>());
        synchronized (times) {
            times.add(now);
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime t) return t;
        if (value instanceof Timestamp t) return t.toLocalDateTime();
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }

    /** 인증번호 원문은 저장하지 않는다 (이메일과 묶어 SHA-256) */
    private static String hash(String email, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((email + ":" + (code == null ? "" : code.strip())).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
