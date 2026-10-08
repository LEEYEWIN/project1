package kr.fast.Jejuro.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WaitTextTest {
    @Test
    void formats() {
        assertEquals("1초", WaitText.of(0));
        assertEquals("45초", WaitText.of(45));
        assertEquals("1분", WaitText.of(60));
        assertEquals("2분", WaitText.of(61));
        assertEquals("15분", WaitText.of(900));
    }
}
