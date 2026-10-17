# Nexora alpha.13 • diagnóstico de inferência

## Problema observado
O relatório alpha.11 de 17/10/2026 mostrou `fragments=512`, execução de aproximadamente 151 segundos e UI dizendo "Resposta concluída" embora o texto matemático tenha terminado sem o valor correto. Fragmentos UTF-8 emitidos não equivalem aos tokens amostrados pelo motor. A API upstream `sendUserPrompt` retorna `Flow<String>`, sem razão de término, e o JNI pode retornar `null` em EOG, limite de geração ou erro de decode. A interface anterior não distinguia os casos.

## Mudanças alpha.13
- Modificação auditável via `scripts/patch_llama_metrics.py`, chamada depois do patch conservador alpha.12 no `scripts/bootstrap.sh` e aplicada apenas sobre o commit fixado.
- API Kotlin expõe `lastGenerationStats` preenchida após a conclusão do fluxo e limpa ao começar nova geração ou em cancelamentos/falhas.
- JNI registra `reason`: `1=eog`, `2=limit`, `3=decode_error`, `0=unknown`. Não considera um limite como término natural.
- Contador `generatedTokens` inclui tokens amostrados nativamente (EOG, se presente); contagem de fragmentos continua distinta. `promptTokens` conta entrada realmente processada após truncamento. `promptMs` mede tokenização + prefill, `generationMs` mede decodificação; taxa nativa aproximada = `tokens*1000/generationMs`.
- Exibe `tok/s (nativo)` depois do término; durante streaming ainda usa `frag./s`. O diagnóstico `inference.end` contém motivo, tokens e tempos, e `inference.sample` registra PSS, RAM disponível e pressão térmica a cada ~15 segundos quando o chat visível está gerando, sem texto de conversa.
- Corrigido bug do diagnóstico alpha.12: identificador de versão e `versionCode` não são mais literais alpha.11, mas vêm de PackageManager.
- Cálculo decimal por parser restrito de operações `+ - * / % ( )` apenas para pedidos matemáticos inequívocos; sem execução de código, rede ou acesso a ferramentas externas. Respostas aproximadas são marcadas com `≈`.

## Limitações e testes físicos
- Erro por descontinuidade de UTF-8, cancelamento pelo usuário e encerramento do Android não necessariamente fornecem razão nativa completa. `UNKNOWN` não significa resposta completa.
- A amostra periódica pode perder picos entre intervalos. PSS é memória proporcional atribuída ao processo, não o consumo exclusivo da IA. Temperatura exibida é da bateria; estado térmico vem do Android.
- Contexto ainda limitado a 4096; máximo por resposta Direto 512 e Pensar 768 tokens. Não aumentar sem benchmark e avaliação da bateria.
- Build Android com JNI deve rodar no CI. Verificar funcionalmente EOG, limite, `decode_error`, cancelamento, progressão/queda de PSS, Matemática e app instalado. Não afirmar otimização medida antes do benchmark no dispositivo.

## Experimento sugerido
Medir com mesma versão, modelo, pergunta e condição térmica, por três execuções. Registrar `prompt`, `ppMs`, `tokens`, `tgMs`, `tps`, `pssPeak`, `ramMin`, `thermal` e motivo final. A versão do JSON deve corresponder ao APK instalado; os registros antigos podem persistir e devem ser separados temporalmente.
