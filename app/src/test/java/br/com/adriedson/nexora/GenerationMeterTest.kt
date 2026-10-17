package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class GenerationMeterTest {
    @Test fun empty() { val m = GenerationMeter(); m.start(1_000L); val r=m.reading(2_000L); assertNull(r.fragmentsPerSecond); assertNull(r.timeToFirstSeconds); assertEquals(0,r.fragments) }
    @Test fun timings() { val m=GenerationMeter(); m.start(1_000_000_000L); m.onFragment("um", 2_000_000_000L); m.onFragment("dois", 2_500_000_000L); m.onFragment("três",3_000_000_000L); val r=m.reading(3_100_000_000L); assertEquals(3,r.fragments); assertEquals(1.0,r.timeToFirstSeconds!!,0.00001); assertEquals(2.0,r.fragmentsPerSecond!!,0.00001) }
    @Test fun ignoreEmptyFragments() {val m=GenerationMeter(); m.start(0L); m.onFragment("",1000L); assertEquals(0,m.reading(1000L).fragments)}
    @Test fun singleFragmentNoSpeed() {val m=GenerationMeter(); m.start(0L); m.onFragment("oi",1000L); assertNull(m.reading(1000L).fragmentsPerSecond)}
    @Test fun resetBetweenPrompts() {val m=GenerationMeter(); m.start(0L); m.onFragment("oi",1000L); m.start(5000L); assertEquals(0,m.reading(6000L).fragments)}
}
