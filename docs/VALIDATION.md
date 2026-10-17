# Validação 0.1.0-alpha.12

- Logs Build 4: :app:testDebugUnitTest, 26 testes, 2 falhas em ModelCatalogTest (esperava 3 entradas; Qwen3 4B usava main).
- Corrigido teste para 4 entradas únicas e pin de commit Qwen3 4B; testes não desativados.
- Verificações XML, análise Kotlin local, smoke tests e ZIP registradas na entrega.
- Build Android/Gradle completo e medição em aparelho ainda necessários.
- Medições CPU são estimativas a partir de Process.getElapsedCpuTime e relógio monotônico; PSS não é o consumo exclusivo de RAM do modelo; temperatura é da bateria.

## alpha.12: verificacoes locais
- Auditado o script `scripts/bootstrap.sh` contra a revisao nativa fixada; todas as seis ancoras estao presentes no arquivo upstream.
- `bash -n` passou; Kotlin PSI: 38 arquivos; XML: 40 arquivos; YAML valido.
- Executados 31 testes Kotlin/JVM locais com executor de assercoes compativel JUnit (oito grupos).
- Diagnostico e roteiro: `docs/PERFORMANCE_AUDIT_ALPHA12.md`.
- Confirmar 1 APK por workflow, conforme presenca dos quatro Secrets (os valores parciais continuam resultando em erro).
- Testar filtro de raciocinio em todas as divisoes de tags e fragmentos no teste unitario.
- Verificar patches de contexto/geracao aplicados exatamente a revisao fixada.
- Build Android no GitHub e testes fisicos de memoria/CPU, PDF e atualizacao por assinatura ainda pendentes.

## alpha.11: avaliação local
- XML e JSON verificados com parsers; integridade ZIP e CRC conferidos no empacotamento.
- Código Kotlin: 38 fontes passaram na análise sintática PSI, sem erros de parser.
- Build Android completo exige bootstrap externo do llama.cpp; não executado neste ambiente.
- Pendências obrigatórias no dispositivo: testar cancelamento de carga, mudança de tema, fluxo de PDF, consumo de RAM e diálogo em rotação de tela.
