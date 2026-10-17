package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class LocalArithmeticTest {
    @Test fun userExample() {
        val result = LocalArithmetic.tryCalculate("Quanto 27+9282*8989*368/09*25%")!!
        assertEquals("852.900.317,6667", result.value)
        assertTrue(result.approximate)
    }
    @Test fun exactAndPrecedence() {
        assertEquals("14", LocalArithmetic.tryCalculate("Calcule 2+3*4")?.value)
        assertEquals("1,5", LocalArithmetic.tryCalculate("Quanto (2+4)/4")?.value)
        assertEquals("25", LocalArithmetic.tryCalculate("Quanto 100*25%")?.value)
        assertEquals("-3", LocalArithmetic.tryCalculate("Quanto 2-5")?.value)
        assertEquals("370.370.367", LocalArithmetic.tryCalculate("Quanto 123456789*3")?.value)
    }
    @Test fun rejectUnsupported() {
        assertNull(LocalArithmetic.tryCalculate("Quanto pesa um carro de 4 toneladas?"))
        assertNull(LocalArithmetic.tryCalculate("Quanto 2/0"))
        assertNull(LocalArithmetic.tryCalculate("Quanto 2^10"))
        assertNull(LocalArithmetic.tryCalculate("Quanto 2 + abc"))
    }
}
