package br.com.adriedson.nexora

import org.junit.Assert.*
import org.junit.Test

class MessageSaveDiffTest {
    @Test fun initialSave() = assertEquals(0, MessageSaveDiff.retainedCount(emptyList<String>(), listOf("A", "B")))
    @Test fun appendOnly() = assertEquals(2, MessageSaveDiff.retainedCount(listOf("A", "B"), listOf("A", "B", "C")))
    @Test fun rolling200Limit() = assertEquals(3, MessageSaveDiff.retainedCount(listOf("A", "B", "C", "D"), listOf("B", "C", "D", "E")))
    @Test fun editingLastMessage() = assertEquals(0, MessageSaveDiff.retainedCount(listOf("A", "B"), listOf("A", "C")))
    @Test fun clearingConversation() = assertEquals(0, MessageSaveDiff.retainedCount(listOf("A", "B"), emptyList<String>()))
    @Test fun unchangedHistory() = assertEquals(2, MessageSaveDiff.retainedCount(listOf("A", "B"), listOf("A", "B")))
    @Test fun duplicateMessages() = assertEquals(2, MessageSaveDiff.retainedCount(listOf("A", "A", "B"), listOf("A", "B", "C")))
}
