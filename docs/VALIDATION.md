# Validação 0.1.0-alpha.16 (código-fonte)

- Sintaxe Kotlin: validar 53 fontes; XML: validar 41 recursos; scripts Python via `py_compile` e shell via `bash -n`.
- Configuração do llama.cpp: executar bootstrap com revisão fixa e confirmar aplicação fail-fast de ambos os patches (métricas JNI e preferências); compilar C++/Kotlin e testar seleção de perfil.
- Testar perfis Econ/Eq/Personalizado, troca de modelo, reload, contexto, limite de tokens, thread, temperatura e top-k/p; validar a ausência de falha JNI.
- Testar tema claro/escuro/AMOLED e fonte grande na biblioteca, modal, cabeçalho do chat, mensagens com Markdown, toolbar e painel de RAM.
- Testar exportação MediaStore em Downloads/Nexora, nome sem sobrescrever exportações, URI legível, JSON válido, filtro por sessão e versão.
- Testar anexos PDF parciais, cancelamento e regeneração da última resposta (recarrega contexto e refaz o prompt).
- Validar CPU somente Nexora, pressão térmica Android vs bateria, pico PSS e percentuais RAM; não prometer CPU °C.
- **Pendente:** build completo no GitHub Actions e testes no Redmi; análise estática não comprova execução do aplicativo.

# Validação 0.1.0-alpha.15

- Validar layout compacto e expansão nos temas claro, escuro e AMOLED em aparelho, com teclado e fontes ampliadas.
- Verificar leitura da bateria versus status bruto Android e headroom NaN/indisponível; não misturar unidades nem interpretar headroom como CPU °C.
- Verificar RAM disponível e seu percentual apenas se total > 0, atualizações a cada 5s com Activity visível.
- Conferir progresso indeterminado somente durante geração, sem porcentagem inventada.
- Executar ThermalDisplayTest e suite Gradle/JNI na CI; build completo pendente neste ambiente.

# Validação 0.1.0-alpha.14

- Verificar nomes Qwen3-4B / Qwen_Qwen3-1.7B, alias de usuário e rejeição de nomes inválidos.
- Verificar modelo original preservado ao carregar, alternar entre conversas, reiniciar e restaurar nome padrão.
- Validar unidade Kotlin/JVM de ModelDisplayNames, XML e CRC ZIP; Android/Gradle completo ainda pendente.

# Validação 0.1.0-alpha.13

- Conjunto de verificações locais: verificação de ancoragem dos patches native/Kotlin, testes do avaliador decimal e análise de sintaxe.
- Pendências: compilar Gradle/Android com pin do llama.cpp na CI; verificar no dispositivo campos de tokens nativos, razões de término e estabilidade de PSS.
- Verificar especialmente EOG, geração por limite, erro JNI e cancelamento de inferência em segundo plano.

## Histórico: alpha.12

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

## Alpha.13 • verificações detalhadas
- Compatibilidade upstream: revisão exata de `scripts/bootstrap.sh` é exigida; o patch é fail-fast se as âncoras não existirem. Sem git/Android SDK completos neste container; validação final da ABI no GitHub Actions.
- `LocalArithmeticTest` e `GenerationOutcomeTest` verificam cálculo decimal e classificação de EOG/limite/erro.
- O diagnóstico pré-alpha.13 da alpha.12 continha literal alpha.11. Agora PackageManager fornece a versão real; checar no APK.
