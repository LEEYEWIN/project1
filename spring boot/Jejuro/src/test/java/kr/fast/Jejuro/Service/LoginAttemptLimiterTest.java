package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import kr.fast.Jejuro.Config.ApiException;

/** 로그인 실패 제한: 이메일별 5번, 성공하면 초기화, 다른 이메일은 영향 없음 */
class LoginAttemptLimiterTest {
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    @Test
    void blocksAfterFiveFailuresForSameEmail() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.check("a@x.com", "1.1.1.1"));
            limiter.recordFailure("a@x.com", "1.1.1.1");
        }
        ApiException e = assertThrows(ApiException.class, () -> limiter.check("a@x.com", "9.9.9.9"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertDoesNotThrow(() -> limiter.check("b@x.com", "9.9.9.9"));
    }

    @Test
    void successClearsFailures() {
        for (int i = 0; i < 4; i++) limiter.recordFailure("a@x.com", "1.1.1.1");
        limiter.recordSuccess("a@x.com");
        for (int i = 0; i < 4; i++) limiter.recordFailure("a@x.com", "1.1.1.1");
        assertDoesNotThrow(() -> limiter.check("a@x.com", "2.2.2.2"));
    }

    @Test
    void blocksOneIpAfterManyFailuresAcrossEmails() {
        for (int i = 0; i < 30; i++) limiter.recordFailure("u" + i + "@x.com", "3.3.3.3");
        assertThrows(ApiException.class, () -> limiter.check("new@x.com", "3.3.3.3"));
    }
}
