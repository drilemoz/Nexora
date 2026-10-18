package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class StreamingUiPolicyTest {
    @Test fun thinkTokensDoNotInvalidateText() {
        assertFalse(StreamingUiPolicy.shouldRenderText(0, 0, 1L, 0L))
        assertFalse(StreamingUiPolicy.shouldRenderText(5, 5, 1L, 0L))
        assertTrue(StreamingUiPolicy.shouldRenderText(5, 0, 1L, 0L))
    }
    @Test fun textFramesAreThrottled() {
        assertFalse(StreamingUiPolicy.shouldRenderText(9, 5, 199_999_999L, 1L))
        assertTrue(StreamingUiPolicy.shouldRenderText(9, 5, 200_000_001L, 1L))
    }
    @Test fun meterWorksEvenWhenResponseIsHidden() {
        assertTrue(StreamingUiPolicy.shouldUpdateMeter(1L, 0L))
        assertFalse(StreamingUiPolicy.shouldUpdateMeter(500_000_000L, 1L))
        assertTrue(StreamingUiPolicy.shouldUpdateMeter(1_000_000_001L, 1L))
    }
    @Test fun scrollingRespectsReaderPosition() {
        assertFalse(StreamingUiPolicy.shouldAutoScroll(4, 10))
        assertTrue(StreamingUiPolicy.shouldAutoScroll(9, 10))
    }
}
