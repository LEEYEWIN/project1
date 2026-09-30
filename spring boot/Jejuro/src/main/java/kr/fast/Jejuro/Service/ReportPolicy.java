package kr.fast.Jejuro.Service;

// [커뮤니티 신고 - 처리 기준]

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 신고 처리 기준 (관리자 화면 "처리 기준"과 문서 docs/20에 같은 내용). 회원 제재(정지·경고)는 하지 않고 글·댓글만 처리한다.
 *
 * | 상태                 | 회원에게 보이는 것                                                   |
 * |----------------------|---------------------------------------------------------------------|
 * | 신고됨 (검토 중)      | 목록·상세 제목 자리에 "신고된 게시글입니다" (내용·사진·경로는 가림)     |
 * | 관리자 차단 (BLOCK)   | 글을 열면 "욕설·비방 등의 사유로 차단되었습니다." 알림 → 목록으로, 목록에서 빠짐 |
 * | 관리자 반려 (KEEP)    | 원래대로 정상 표시                                                   |
 *
 * 사유 4가지: 음란·불법 / 개인정보 노출 / 욕설·비방 / 스팸·광고 (무거운 순서)
 * 차단 사유 기본값 = 가장 많이 받은 신고 사유 (같으면 더 무거운 사유). 관리자가 바꿀 수 있다.
 */
public final class ReportPolicy {

    /** 무거운 순서 */
    public static final Map<String, String> REASONS = new LinkedHashMap<>();
    static {
        REASONS.put("SEXUAL", "음란·불법");
        REASONS.put("PRIVACY", "개인정보 노출");
        REASONS.put("ABUSE", "욕설·비방");
        REASONS.put("SPAM", "스팸·광고");
    }

    private ReportPolicy() {
    }

    public static String label(String reason) {
        return REASONS.getOrDefault(reason, reason);
    }

    /** 대표 사유: 가장 많이 받은 사유, 같으면 더 무거운 사유 */
    public static String mainReason(Map<String, Long> counts) {
        String best = "ABUSE";
        long bestCount = 0;
        for (String r : REASONS.keySet()) {            // 무거운 순서로 돌면서 "더 많을 때만" 바꾼다
            long c = counts.getOrDefault(r, 0L);
            if (c > bestCount) {
                best = r;
                bestCount = c;
            }
        }
        return best;
    }

    /** 차단 안내 문구: "욕설·비방 등의 사유로 차단되었습니다." */
    public static String blockedMessage(String reason) {
        return label(reason) + " 등의 사유로 차단되었습니다.";
    }

    /** 화면용: 사유 코드·이름 목록 */
    public static List<Map<String, String>> reasonOptions() {
        return REASONS.entrySet().stream()
                .map(e -> Map.of("code", e.getKey(), "label", e.getValue()))
                .toList();
    }
}