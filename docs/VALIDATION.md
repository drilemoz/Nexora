# Verificações Nexora 0.1.0-alpha.7
- Fonte: alpha.5 do projeto IA Local, com mudança sequencial de versão e pacote.
- Testes locais de estrutura/XML, links internos, configuração do workflow, assinaturas e parser puro de `<think>` listados no relatório da entrega.
- **Não confundir testes sintáticos/isolados com compilação Android completa.** A confirmação final depende de workflow GitHub Actions e de testes reais no celular.
- Cenários importantes em aparelho: carregar GGUF, abrir ChatActivity, alternar modo Qwen3, streaming, SHA-256 em downloads, interrupção com tela desligada, troca de tema/rotação, preservar engine, instalações lado a lado com IA Local e assinatura.

## Alpha.7 (2026-10-17)
- Validação de XML, links de views, versionamento e ZIP executada localmente.
- GenerationMeter testado isoladamente com compilador Kotlin; JUnit Gradle e build Android dependem do workflow CI.
- DeviceSampler depende de APIs Android e requer teste no hardware real (temperatura da bateria disponível ou ausente, thermalStatus, PSS e RAM).
- Qwen3 4B listado com SHA-256 do arquivo oficial; download/verificação e carregamento não foram executados neste ambiente.
