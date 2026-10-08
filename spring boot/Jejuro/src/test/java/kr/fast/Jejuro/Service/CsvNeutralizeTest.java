package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** CSV 수식 주입 방지: 수식으로 읽힐 값만 ' 를 붙이고 숫자·일반 글자는 그대로 */
class CsvNeutralizeTest {
    @Test
    void neutralizesFormulas() {
        assertEquals("'=HYPERLINK(\"x\")", FunnelDropService.neutralize("=HYPERLINK(\"x\")"));
        assertEquals("'+cmd", FunnelDropService.neutralize("+cmd"));
        assertEquals("'@SUM(A1)", AdminKpiService.neutralize("@SUM(A1)"));
        assertEquals("'-cmd", AdminKpiService.neutralize("-cmd"));
    }

    @Test
    void keepsNumbersAndText() {
        assertEquals("-1.5", AdminKpiService.neutralize("-1.5"));
        assertEquals("0", AdminKpiService.neutralize("0"));
        assertEquals("제주 여행", FunnelDropService.neutralize("제주 여행"));
        assertEquals("", FunnelDropService.neutralize(""));
    }
}
