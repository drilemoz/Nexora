package br.com.adriedson.nexora

/** Limites de atualização visual: não acordar o RecyclerView para tokens ocultos de <think>. */
internal object StreamingUiPolicy {
    const val TEXT_FRAME_NS = 200_000_000L // no máximo 5 atualizações de texto por segundo
    const val METER_FRAME_NS = 1_000_000_000L // medidor continua ativo durante raciocínio oculto

    fun shouldRenderText(visibleChars: Int, renderedChars: Int, nowNs: Long, lastFrameNs: Long): Boolean =
        visibleChars > renderedChars && (lastFrameNs == 0L || nowNs - lastFrameNs >= TEXT_FRAME_NS)

    fun shouldUpdateMeter(nowNs: Long, lastFrameNs: Long): Boolean =
        lastFrameNs == 0L || nowNs - lastFrameNs >= METER_FRAME_NS

    /** Se a pessoa rolou o histórico para cima, não forçar a volta ao fim. */
    fun shouldAutoScroll(lastVisibleIndex: Int, responseIndex: Int): Boolean =
        lastVisibleIndex >= responseIndex - 1
}
