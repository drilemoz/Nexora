# Nexora alpha.12 • auditoria de desempenho e distribuição

## Por que havia dois APKs

A alpha.11 executava `assembleDebug` sempre e `assembleRelease` quando havia os quatro Secrets de assinatura. O workflow publicava os dois artifacts. Não são ABI diferentes: `abiFilters=arm64-v8a` para ambos. A alpha.12 passa a compilar/publicar **somente Release assinado** com Secrets completos, **senão somente Debug**. Assinatura e applicationId foram preservados. Se apenas parte dos Secrets existir, o job falha em vez de gerar APK ambíguo.

## Diagnóstico de desempenho

- No diagnóstico alpha.10 enviado pelo usuário, o GGUF Qwen3-4B-Q4_K_M registrou cargas concluídas entre ~11,7 e 19,8s e 16 fragmentos em uma resposta, com ~4s até o primeiro fragmento. Isso NÃO é benchmark nativo de tokens/s.
- PSS do processo ~3,8 GiB, RAM disponível observada abaixo de 1GiB em certos períodos e status térmico Android SEVERE. Bateria ~37C não mede temperatura do SoC.
- O motor nativo da revisão `ea0c4c2c85fb183ec0fbd30a1af4e4275a18a064` define `DEFAULT_CONTEXT_SIZE=8192`, `BATCH_SIZE=512`, e escolhe 2 a 4 threads com headroom 2. A alpha.12 ajusta **somente n_ctx padrão para 4096**; não altera threads/batch sem teste físico. Isso reduz o teto de memória do contexto, mas reduz a capacidade de texto de entrada.
- O cálculo nativo após processar um prompt somava `user_prompt_size` a `current_position` e outra vez ao limite de saída. Corrigido para usar contagem efetiva após eventual truncamento e limite `current_position+n_predict`.
- Foram removidos do JNI os logs do conteúdo de prompts e loops de logs de todos os tokens de entrada. Outros logs técnicos continuam.
- O chat reconstruía texto filtrado e atualizava UI/RecyclerView por fragmento. O filtro agora processa o conteúdo incrementalmente; atualização visual limitada a aprox. 8 vezes por segundo.
- O chat amostrava memória PSS no thread principal em intervalos de 2s. A amostragem agora ocorre em `Dispatchers.Default`, a cada 5s, apenas com a Activity em primeiro plano.

## Limites e riscos

- A memória maior é em parte esperada por modelo 4B Q4 e buffers do llama.cpp; não há prova de vazamento. Menor contexto não garante mais tokens/s.
- Reduzir janela de contexto pode truncar PDFs e diálogos longos; o app já limita trechos de PDF e mostra extratos parciais quando aplicável. O motor nativo ainda pode truncar silenciosamente prompts acima do contexto.
- Troca de conversa ainda libera e recarrega o modelo para isolar KV cache. É um custo de 12 a 20 segundos observado em operações de carga, e deve ser otimizado somente depois de API de reset seguro e testes do isolamento.
- O aviso de pressão térmica severa é emitido pelo Android. Não foi aplicado overclock, seleção forçada de núcleo nem aumento de threads.
- Ainda não foi compilado APK Android nesta análise. A lib nativa é obtida por `bootstrap.sh` durante o workflow e os patches verificam âncoras exatas do commit fixado.

## Protocolo comparativo recomendado no Redmi Note 11 Pro+

1. Registrar temperatura de bateria, pressão térmica e RAM livre antes de testar. Parar se aquecimento ou instabilidade aumentar.
2. Em cada versão, carregar o mesmo GGUF e medir tempo até pronto, PSS e RAM disponível após estabilizar.
3. Usar um mesmo texto curto e um PDF de duas páginas, em conversas novas. Medir tempo até o primeiro fragmento, duração total e contagem de fragmentos. Fragmentos/s **não equivalem** a tokens/s.
4. Repetir três vezes em condições térmicas semelhantes, anotando aquecimento e interrupções. Verificar que o botão Cancelar finaliza com segurança.
5. Verificar atualização alpha.11 → alpha.12 mantendo conversas, GGUF e a mesma chave de assinatura. Não desinstalar o app sem proteger arquivos e histórico.
6. Se o modelo começar a truncar material importante, reconsiderar 4096 vs. 8192 e medir o custo de cada configuração antes de escolher.

## Validação estática local

- `bash -n scripts/bootstrap.sh`: passou.
- PSI Kotlin: 38 arquivos sem erros sintáticos.
- 31 testes de oito grupos JVM executados em executor JUnit compatível, incluindo todas as divisões de tags `<think>`.
- 40 XMLs válidos e workflow YAML analisado.
- Ainda pendentes: Gradle Android completo, execução dos patches no build real, revisão dos outputs assinados, benchmark no telefone e testes de PDFs grandes.
