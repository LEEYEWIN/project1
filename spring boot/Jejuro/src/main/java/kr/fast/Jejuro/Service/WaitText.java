package kr.fast.Jejuro.Service;

/** "N초" / "N분" 형태의 남은 시간 문구 (분은 올림: 61초 → 2분) */
final class WaitText {
    private WaitText() {
    }

    static String of(long seconds) {
        long s = Math.max(seconds, 1);
        if (s < 60) return s + "초";
        return ((s + 59) / 60) + "분";
    }
}
