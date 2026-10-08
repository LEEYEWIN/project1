package kr.fast.Jejuro.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import kr.fast.Jejuro.Config.ApiException;

/**
 * 로그인 실패 횟수 제한 (비밀번호 무차별 대입 방지).
 * - 같은 이메일: 15분 안에 5번 틀리면 15분 동안 로그인 불가 (맞는 비밀번호도 막힘 → 계정 보호)
 * - 같은 IP: 15분 안에 30번 틀리면 15분 동안 로그인 불가
 * 서버 메모리에 세므로 서버를 여러 대로 늘리면 서버마다 따로 센다(재시작하면 초기화).
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILS_PER_EMAIL = 5;
    static final int MAX_FAILS_PER_IP = 30;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> byEmail = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> byIp = new ConcurrentHashMap<>();

    /** 로그인 시도 전에 호출: 한도를 넘었으면 429 */
    public void check(String email, String ip) {
        Instant now = Instant.now();
        long wait = Math.max(waitSeconds(byEmail, email, MAX_FAILS_PER_EMAIL, now),
                waitSeconds(byIp, ip, MAX_FAILS_PER_IP, now));
        if (wait > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인을 너무 많이 실패했어요. " + WaitText.of(wait) + " 뒤에 다시 시도해 주세요.");
        }
    }

    /** 실패 기록 → 이 이메일로 더 틀려도 되는 남은 횟수 (0이면 방금 5번째 실패로 차단됨) */
    public int recordFailure(String email, String ip) {
        Instant now = Instant.now();
        int fails = add(byEmail, email, now);
        add(byIp, ip, now);
        return Math.max(MAX_FAILS_PER_EMAIL - fails, 0);
    }

    /** 로그인 성공 → 그 이메일의 실패 기록 삭제 */
    public void recordSuccess(String email) {
        if (email != null) byEmail.remove(email);
    }

    /** 한도를 넘었으면 다시 시도할 수 있을 때까지 남은 초, 아니면 0 (한도 아래로 내려가는 시점 = 오래된 실패가 15분을 넘기는 시점) */
    private static long waitSeconds(Map<String, Deque<Instant>> map, String key, int max, Instant now) {
        if (key == null) return 0;
        Deque<Instant> q = map.get(key);
        if (q == null) return 0;
        synchronized (q) {
            prune(q, now);
            if (q.size() < max) return 0;
            Instant releaseAt = q.stream().skip(q.size() - max).findFirst().orElse(now).plus(WINDOW);
            return Math.max(Duration.between(now, releaseAt).getSeconds(), 1);
        }
    }

    /** 기록을 더하고 지금 기간 안의 실패 횟수를 돌려준다 */
    private static int add(Map<String, Deque<Instant>> map, String key, Instant now) {
        if (key == null) return 0;
        Deque<Instant> q = map.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q, now);
            q.addLast(now);
            return q.size();
        }
    }

    private static void prune(Deque<Instant> q, Instant now) {
        Instant limit = now.minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().isBefore(limit)) q.removeFirst();
    }

    /** 오래된 기록 정리 (메모리가 계속 늘지 않게) */
    @Scheduled(fixedDelay = 600_000)
    public void cleanup() {
        Instant now = Instant.now();
        for (Map<String, Deque<Instant>> map : java.util.List.of(byEmail, byIp)) {
            map.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    prune(e.getValue(), now);
                    return e.getValue().isEmpty();
                }
            });
        }
    }
}
