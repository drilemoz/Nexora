package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class ModelFilesTest {
    @Test fun magic() {
        assertTrue(ModelFiles.hasGgufMagic(byteArrayOf(71, 71, 85, 70, 0)))
        assertFalse(ModelFiles.hasGgufMagic(byteArrayOf(90, 73, 80, 0)))
        assertFalse(ModelFiles.hasGgufMagic(byteArrayOf()))
    }
    @Test fun safeFileName() {
        assertEquals("modelo.gguf", ModelFiles.safeName("/outros/modelo.gguf"))
        assertEquals("a_b.gguf", ModelFiles.safeName("a b.gguf"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsWrongFile() {
        ModelFiles.safeName("app.apk")
    }
}
