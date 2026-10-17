package br.com.adriedson.nexora

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuMathTest {
    @Test fun cpuOneCore() { assertEquals(100.0, CpuMath.corePercent(1000L,1000L),0.0001) }
    @Test fun multiThreaded() { assertEquals(200.0, CpuMath.corePercent(2000L,1000L),0.0001) }
    @Test fun emptyInterval() { assertEquals(0.0, CpuMath.corePercent(100L,0L),0.0001) }
    @Test fun invalidInterval() { assertEquals(0.0, CpuMath.corePercent(-10L,100L),0.0001) }
}
