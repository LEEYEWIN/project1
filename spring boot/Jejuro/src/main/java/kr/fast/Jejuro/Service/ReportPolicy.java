package kr.fast.Jejuro.Service;


// [커뮤니티 신고 - 처리 기준]

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 신고 사유별 처리 기준 (관리자 화면의 "권장 처리"와 문서 docs/19에 같은 내용).
 *
 * | 사유              | 권장 처리 | 작성자 제재                                    |
 * |-------------------|-----------|-----------------------------------------------|
 * | 음란·불법 SEXUAL   | 차단      | 30일 정지 (전에 조치된 신고가 있으면 영구 정지) |
 * | 개인정보 노출 PRIVACY | 차단   | 경고                                           |
 * | 욕설·비방 ABUSE    | 차단      | 경고 (경고 3회마다 자동 7일 정지)               |
 * | 스팸·광고 SPAM     | 차단      | 경고, 조치된 신고가 2번 이상이면 7일 정지         |
 * | 기타 OTHER         | 검토 후 결정 (기본 유지)                         |
 *
 * 차단 = 작성자 포함 모든 회원에게 "○○ 등의 사유로 게시글이 차단되었습니다" 알림 후 목록으로 (관리자만 내용 확인).
 * 권장은 참고용이며 관리자가 직접 고른다. 신고 여러 개면 가장 많이 받은 사유(같으면 더 무거운 사유) 기준.
 */
public final class ReportPolicy {

    /** 무거운 순서 */
    public static final Map<String, String> REASONS = new LinkedHashMap<>();
    static {
        REASONS.put("SEXUAL", "음란·불법");
        REASONS.put("PRIVACY", "개인정보 노출");
        REASONS.put("ABUSE", "욕설·비방");
        REASONS.put("SPAM", "스팸·광고");
        REASONS.put("OTHER", "기타");
    }

    /** 이 사유는 신고가 적어도 먼저 가린다 (auto-hide-count-serious) */
    public static boolean serious(String reason) {
        return "SEXUAL".equals(reason) || "PRIVACY".equals(reason);
    }

    /** 경고가 이 횟수의 배수가 되면 자동 7일 정지 */
    public static final int WARNINGS_FOR_SUSPEND = 3;

    public record Recommendation(String action, String sanction, String note) {
    }

    private ReportPolicy() {
    }

    public static String label(String reason) {
        return REASONS.getOrDefault(reason, reason);
    }

    /** 대표 사유: 가장 많이 받은 사유, 같으면 더 무거운 사유 */
    public static String mainReason(Map<String, Long> counts) {
        String best = "OTHER";
        long bestCount = -1;
        for (String r : REASONS.keySet()) {            // 무거운 순서로 돌면서 "더 많을 때만" 바꾼다
            long c = counts.getOrDefault(r, 0L);
            if (c > bestCount) {
                best = r;
                bestCount = c;
            }
        }
        return best;
    }

    /**
     * @param mainReason     대표 사유
     * @param priorAccepted  작성자가 전에 조치(차단·삭제)받은 글·댓글 수 (이번 건 제외)
     */
    public static Recommendation recommend(String mainReason, long priorAccepted) {
        return switch (mainReason) {
            case "SEXUAL" -> priorAccepted > 0
                    ? new Recommendation("BLOCK", "BAN", "음란·불법 재위반 → 차단 + 영구 정지")
                    : new Recommendation("BLOCK", "SUSPEND_30D", "음란·불법 → 차단 + 30일 정지");
            case "PRIVACY" -> new Recommendation("BLOCK", "WARNING", "개인정보 노출 → 차단 + 경고");
            case "ABUSE" -> new Recommendation("BLOCK", "WARNING", "욕설·비방 → 차단 + 경고 (경고 3회면 자동 7일 정지)");
            case "SPAM" -> priorAccepted >= 2
                    ? new Recommendation("BLOCK", "SUSPEND_7D", "스팸·광고 반복 → 차단 + 7일 정지")
                    : new Recommendation("BLOCK", "WARNING", "스팸·광고 → 차단 + 경고");
            default -> new Recommendation("KEEP", "NONE", "기타 → 내용을 보고 판단 (문제 없으면 유지)");
        };
    }

    /** 차단 안내 문구: "'욕설·비방' 등의 사유로 게시글이 차단되었습니다." */
    public static String blockedMessage(String reason, String what) {
        return "'" + label(reason) + "' 등의 사유로 " + what + "이 차단되었습니다.";
    }

    /** 화면용: 사유 코드·이름 목록 */
    public static List<Map<String, String>> reasonOptions() {
        return REASONS.entrySet().stream()
                .map(e -> Map.of("code", e.getKey(), "label", e.getValue()))
                .toList();
    }
}