# Validação alpha.23 (código-fonte)

- Base: ZIP alpha.22, versão sequencial 0.1.0-alpha.23 / versionCode 23.
- Alterações restritas à interface, exportação manual consentida com contexto técnico e metadados.
- Verificação sintática Kotlin/XML, pacote ZIP, filename seguro, layout e código de exportação por inspeção automatizada.
- Exportação não inclui texto extraído de PDFs; mensagens inteiras só saem mediante confirmação local.
- Compilação JNI/Android, compatibilidade do compartilhamento MediaStore e validação em aparelho: pendentes até build CI ou teste físico.

# Validação alpha.22 (código-fonte)

- Base: ZIP alpha.21 e pin upstream llama.cpp `ea0c4c2c85fb183ec0fbd30a1af4e4275a18a064`; versionName 0.1.0-alpha.22 / versionCode 22.
- Revisão estática da API pinned (`llama_model_chat_template`, `common_chat_templates_apply`, tokenização, histórico). `scripts/patch_llama_templates.py` roda após as patches anteriores e `scripts/verify_template_alpha22.py` bloqueia ausência de invariantes.
- Testes locais de parser e relatório de enums em `ModelTemplateReportTest.kt`; XML e parsing Kotlin. Resultados locais: 7 testes novos Kotlin/JVM aprovados (runner com asserções), 62 arquivos Kotlin analisados sem erros sintáticos via parser PSI, 47 XML bem formados, scripts Python compilados e Bash validado. `scripts/verify_template_ui_alpha22.py` aprovou 12 verificações estáticas da UI, rotas e estado. Estes testes não executam o motor JNI.
- **Pendentes:** checkout bootstrap real, compilação JNI/Gradle Android completa, inferência com GGUF físico, Qwen3/Gemma3/SmolLM3/Llama/Phi, testes instrumentados com 4+ turnos, pressão de RAM e raciocínio. Suporte por família não pode ser declarado somente por heurística de marcadores.

# Validação 0.1.0-alpha.21 (código-fonte)

- Base: ZIP alpha.20, com `versionName 0.1.0-alpha.21` e `versionCode 21` sincronizados.
- [OK] Teste de hierarquia XML em `scripts/verify_history_alpha21.py`: modelo sob título, ações no primeiro cabeçalho, checkboxes e rotas de exclusão com confirmação.
- [OK] Compilação JVM isolada e 8 cenários de calendário: HOJE, datas anteriores, cruzamento de ano, datas futuras, fuso America/Sao_Paulo e fixadas. Adicionados 7 testes JUnit ao app e executados localmente em JVM isolada com stub de asserção; o runner JUnit oficial permanece pendente.
- [OK] Teste SQLite em memória com 1000 conversas, 700 exclusões em lote e exclusão de todas: cascata de mensagens e textos PDF e consistência. Esse teste reproduz o esquema/SQL mas não executa o wrapper Android.
- [OK] Parser PSI Kotlin: 59 arquivos, nenhum erro sintático; recursos XML, YAML do CI e scripts validados.
- [PENDENTE] Build Gradle/Android completo, execução instrumentada do ChatStore Android e inspeção da UI no aparelho (tema claro/escuro, 2 fontes, virada do dia, pesquisa e exclusões reais).
- Inferência GGUF, assinatura e esquema SQLite sem alteração.

# Validação 0.1.0-alpha.20 (código-fonte)

- Base: ZIP alpha.19; alteração pontual da posição do cartão no `activity_chat.xml`.
- [OK] Teste de regressão estático da hierarquia XML em `scripts/verify_chat_layout_alpha20.py` (executado localmente e no workflow).
- [OK] Identificadores dos componentes preservados, painel antes do RecyclerView de mensagens, rodapé/compositor depois, lista com peso 1 e detalhes recolhíveis.
- [OK] Verificação XML, Python, ZIP/CRC e versionamento local.
- [PENDENTE] Build Gradle/Android e teste visual/interativo no dispositivo para tema, teclados, rolagem e expansão.
- Sem alterações em motor llama.cpp, banco, modelos e métricas da alpha.19.

# Validação 0.1.0-alpha.19 (código-fonte)

## Escopo
- Base oficial: ZIP `Nexora-v0.1.0-alpha.18-fonte-corrigida.zip`; alpha.18 já obteve build Release assinado com sucesso no GitHub Actions #12 (não prova esta nova versão).
- Novo código: política de atualização de UI, renderização parcial, filtro de raciocínio otimizado e otimização de gravações no SQLite. Sem mudanças do motor nativo, banco versionado ou AndroidManifest.
- [OK] Kotlin PSI: **57 arquivos, 0 erros sintáticos**; 58 testes JVM executados (17 para funcionalidades alteradas, 41 herdados), **0 falhas**. Execução com stubs JVM locais, não é Gradle oficial.
- [OK] 46 arquivos XML válidos; YAML do workflow verificado; Python py_compile e bash -n aprovados.
- [OK] SQLite em memória com **800 operações** de inclusão, edição, exclusão e janela móvel, conferindo igualdade exata com a sequência anterior; valida o algoritmo reimplementado no teste, não executa código SQLite Android.
- [OK] Integridade ZIP e CRC serão verificados no empacotamento. Não inclui pesos GGUF, keystore ou APK.
- [PENDENTE] Gradle/Android e JNI reais da alpha.19, UI e testes físicos de Qwen3-4B. A alpha.18 compilou, mas a compilação não foi repetida para esta versão.
- Confirmar via GitHub Actions `:app:testDebugUnitTest` e `:app:assembleRelease` antes de instalar. Testar no aparelho fluxo de geração, histórico com 200+ mensagens, exclusões, regeneração e rolagem enquanto recebe tokens.
- Comparar processamento/CPU de UI e consumo em mesmas condições com alpha.18; reduções no código não são medições de desempenho real.

# Validação 0.1.0-alpha.18 (código-fonte)

## Resultado local de 17/10/2026

- [OK] ZIP alpha.17 usado como base, sem alteração do package `br.com.adriedson.nexora` nem do banco SQLite.
- [OK] Versão `0.1.0-alpha.18`, versionCode 18, workflows e documentação sincronizados.
- [OK] Parser Kotlin PSI: **53 fontes de código e teste, zero erros sintáticos**.
- [OK] XML: **46 recursos válidos**; YAML do GitHub Actions analisado; Python `py_compile`; `bash -n`.
- [OK] Filtro ThinkingTextFilter/GenerationOutcome compilados com kotlinc e **179 cenários** de fragmentação e limitação de saída verificados em runner JVM independente.
- [OK] `patch_llama_safety.py` aplicado a fixture com as **16 âncoras previstas**, seguida de verificação de 10 invariantes por `verify_native_alpha18.py`. É apenas teste das transformações, **não é compilação real do upstream**.
- [PENDENTE] Bootstrap completo com revisão fixada `ea0c4c2c85fb183ec0fbd30a1af4e4275a18a064`, JNI/NDK, Gradle, JUnit oficial e APK: ambiente local sem acesso de rede ao GitHub, Android SDK ou dependências Gradle.

## Matriz de testes necessária no GitHub Actions e no telefone

1. Executar `bash scripts/bootstrap.sh`; falha de âncora deve interromper build. A etapa `verify_native_alpha18.py` valida regras de privacidade, deslocamento KV, tokens e cleanup antes dos testes Gradle.
2. Compilar `:app:testDebugUnitTest` e `:app:assembleDebug` (ou Release com os quatro Secrets); observar erros C++/JNI e da API `InferenceEngine.resetConversation()`.
3. Com GGUF pequeno, gerar exatamente 32 e 128 tokens de saída máxima, inclusive após repetidas trocas do contexto. Limite nativo jamais deve ultrapassar o solicitado. Comparar reason EOG vs LIMIT vs DECODE_ERROR e texto visível, sem assumir EOG ocorrerá.
4. Com 2.048 tokens de janela, enviar prompts longos em batches; confirmar ausência de posições KV inválidas, duplicação de tokens ou erro de decode ao deslizar o contexto.
5. Em duas conversas alternadas, validar contexto isolado sem novo carregamento de pesos; regenerar resposta, limpar conversa e interromper geração manualmente. Pergunta posterior deve recuperar histórico resumido sem reaproveitar KV anterior.
6. Bloquear a tela durante inferência: continuar cancelando no onStop (comportamento deliberado); reabrir e verificar envio posterior. Validar fluxo equivalente com um PDF parcialmente extraído e sem OCR.
7. Forçar erro durante carregamento (GGUF inválido ou memória insuficiente) e tentar carregar modelo menor depois. Monitorar PSS, estado térmico e desaparecimento de recursos nativos.
8. Verificar Logcat (Debug e Release) com frase sentinela em prompt, PDF e resposta; **nenhum texto de conversa deve aparecer**, inclusive por mensagens formatadas ou tokens de saída. O nome/caminho do GGUF ainda pode aparecer em logs de infraestrutura e merece análise separada.
9. Comparar latência de troca de chat e regeneração com alpha.17 no mesmo aparelho, modelo, contexto e estado térmico. Sem afirmar ganho de desempenho até medir.

## Limitações e riscos

- Não houve compilação de `llama.cpp` nem inferência real na máquina que produziu o ZIP. A fixture demonstra aplicação dos patches apenas; se o pin ou estrutura do motor diferirem, o bootstrap deve falhar antes de gerar APK.
- Conversas antigas continuam salvas. Uma reconstrução de histórico é resumida (últimas quatro mensagens, limite de caracteres) e não recompõe completamente a sessão anterior.
- Cancelamentos não ativam inferência em segundo plano; a sessão deverá ser recomposta quando a tela estiver ativa.
- Não houve benchmark de CPU, energia, PSS ou RAM de um telefone; ganhos de consumo e tempo de resposta são hipóteses de implementação.

---

# Validação 0.1.0-alpha.17 (código-fonte)

Alterações alpha.17: cartões compactos de modelos instalados e da biblioteca, ações com ícones, exclusão visual reduzida com confirmação preservada e bloqueio durante carregamento. Verificações locais realizadas: 53 arquivos Kotlin com PSI (0 erro sintático); 46 XML válidos; referências aos seis ícones usadas no menu; testes de invariantes de rotulagem e exclusão; YAML do workflow, Python e bash válidos; versionCode 17 e metadados sincronizados. Compilação Android, interação real e visual no dispositivo pendentes.

- Sintaxe Kotlin: 53 fontes aprovadas por PSI; XML: 46 recursos válidos; scripts Python via `py_compile` e shell via `bash -n`.
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
