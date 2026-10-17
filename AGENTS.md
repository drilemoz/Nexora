# Nexora • Regras de desenvolvimento
- App Android nativo Kotlin, GGUF offline em CPU via llama.cpp. Internet somente para download explícito de modelos.
- Versão atual: `0.1.0-alpha.15` (`versionCode 15`). Próxima: `0.1.0-alpha.16` (`versionCode 16`).
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
