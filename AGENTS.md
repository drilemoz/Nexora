# Nexora • Regras de desenvolvimento
- App Android nativo Kotlin, GGUF offline em CPU via llama.cpp. Internet somente para download explícito de modelos.
- Versão atual: `0.1.0-alpha.25` (`versionCode 25`). Próxima: `0.1.0-alpha.26` (`versionCode 26`).
- Em cada alteração, incrementar sequencialmente `versionName` e `versionCode`; sincronizar README, CHANGELOG, docs/VERSIONING, docs/RELEASE_NOTES, workflow e identidade.
- Identidade definitiva desde alpha.6: `br.com.adriedson.nexora`. Não restaurar o package antigo.
- As quatro credenciais de assinatura permanecem opcionais via GitHub Secrets; nunca distribuir keystore nem senhas no repositório.
- Nunca incluir pesos GGUF nem APKs proprietários ou blobs de terceiros sem licença.
- O motor vem de revisão fixada em `scripts/bootstrap.sh`; não alterar o pin sem testar API Kotlin/JNI e licença.
- Não declarar compilação Android, download ou inferência aprovados sem execução real.
- Mudança do package impede atualização in-place/migração automática de conversas e GGUF do app anterior.

- Medidor mostra fragmentos por segundo como aproximação, não contagem garantida de tokens. Não confundir temperatura da bateria com CPU, PSS com RAM exclusiva do modelo, ou RAM disponível com capacidade segura para carregar arquivos gigantes.

- Novos chats são armazenados em SQLite com migração conservadora do legado; o motor de inferência deve isolar conversas e anexos PDF não podem ser exportados sem ação do usuário. PDFBox-Android apenas extrai texto (sem OCR), mantenha limites contra consumo de RAM.

- Diagnóstico alpha.10: eventos limitados a 256KiB em armazenamento privado, sem texto de conversas, PDFs ou credenciais; CPU é apenas do processo, pressão térmica é a informada pelo Android. Medição periódica somente com tela Diagnóstico aberta, sem serviço foreground.

- Alpha.13: identificar término por EOG/limite/erro via contadores nativos, evitar "resposta concluída" quando a saída é cortada; cálculo decimal local apenas para expressão explícita e inequívoca.

- Alpha.14: apelidos são apenas apresentação; manter nome real de GGUF em ModelSession, ChatStore, ModelStartup e diagnósticos.

- Alpha.15: estado térmico do Android sempre rotulado como estado do sistema com código bruto; bateria separada, headroom somente quando suportado; painel compacto expande sob demanda. Nunca inventar temperatura da CPU nem progresso de resposta.

- Alpha.16: perfis por modelo aplicados somente no carregamento, via JNI pinned; não fingir aplicar opções sem recarga. Ao regenerar última resposta, o contexto é zerado. A barra de RAM é disponibilidade reportada pelo Android. JSON exportado a Downloads/Nexora via MediaStore sem compartilhamento automático. A renderização Markdown é deliberadamente limitada (negrito, itálico, títulos, listas e código), sem HTML ativo.

- Alpha.17: cartões de modelos compactos exibem apenas apelido, quantização e tamanho; filename completo permanece nos detalhes. Lixeira visual reduzida com área de toque 48 dp e confirmação; menu personalizado usa um ícone para cada uma das cinco ações.

- Alpha.18: reset KV + sampler sem descartar os pesos GGUF ao alternar/regenerar conversa; após cancelamento, limite ou falha, reconstruir contexto do histórico antes de permitir novo envio. Patches falham com revisão diferente e CI valida âncoras. Não prometer funcionamento sem validar a build JNI.

- Alpha.19: atualizar apenas TextView durante streaming, no máximo 5/s com novos caracteres visíveis; medidor até 1/s para raciocínio oculto; preservar rolagem manual. SQLite preserva linhas antigas iguais em transação sem alterar schema.

- Alpha.20: cartão de desempenho realocado para imediatamente abaixo do indicador de IA local e antes da lista de mensagens, fora da rolagem. Mantém telemetria, acessibilidade e expansão existente; anexos e editor permanecem ancorados embaixo.

- Alpha.21: cabeçalho principal informa modelo ativo, histórico por data local real e seleção/exclusão múltipla ou integral com confirmação; cascata SQLite apaga mensagens/PDFs, nunca arquivos GGUF. Ao mudar de dia, reclassificar HOJE sem alterar timestamps.

- Alpha.22: templates GGUF detectados e verificados por JNI com conteúdo sintético e Jinja; sem mudança do pin llama.cpp. Template incompatível bloqueia prompts, sem fallback silencioso para outro formato. Configurações exibem origem e resultado; modo personalizado indisponível até suporte seguro. Diagnóstico sem conteúdo de conversas/templating.

- Alpha.23: exportação manual e consentida de conversa completa + diagnóstico técnico (JSON em Downloads/Nexora, sem PDFs), painel de desempenho com terceira opção minimizada e seleção múltipla com botões Material outline.

- Alpha.24: botões Material da seleção múltipla usam `android:insetTop` e `android:insetBottom` (nunca `app:`); CI valida os atributos antes de executar AAPT2. Não modificar JNI/qualidade do modelo nesta correção de compilação.

- Alpha.25: em primeiro turno sem histórico/PDF, enviar pergunta bruta sem prefixo artificial. Exportar somente parâmetros de inferência efetivamente aplicados no carregamento da sessão atual; nunca afirmar que são do histórico todo. Nenhuma alteração em pesos GGUF ou sampler. Testes comparativos com PocketPal devem usar mesmo arquivo GGUF e configurações equivalentes.
