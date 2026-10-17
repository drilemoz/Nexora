# Nexora 0.1.0-alpha.8 • versionCode 8

Tela inicial reformulada em central de conversas com SQLite, pesquisa, fixação, renomeação, exclusão confirmada e migração do histórico alfa anterior. Novo botão PDF no chat para extrair textos locais com limites definidos e adicionar trechos de documentos às perguntas, sem enviar documentos à internet. Configurações > Modelos de IA reúne importação, downloads, carregamento e tamanhos de modelos instalados; mostra armazenamento livre/total do volume de dados. Motor recarrega ao alternar conversas para evitar vazamento de contexto. Preservados assinatura, temas, desempenho e downloads verificados.

**Pendente:** build Android/CI e teste físico com modelos GGUF e PDFs reais. PDFs só imagem ainda exigem OCR. Documento truncado é identificado.

# Nexora 0.1.0-alpha.7 • versionCode 6

Primeira versão com identidade Nexora. Tela inicial para gerenciamento de modelos GGUF, botão Conversar para entrar no chat, chat com cabeçalho de modelo, controle de raciocínio Qwen3 Direto/Pensar, exibição apenas da resposta final (filtragem `<think>`), seleção dos quatro temas em tela separada, catálogo reorganizado com botões espaçados e barra de download em MB, confirmação antes de sair para Hugging Face, ícone adaptativo personalizado.

**Compatibilidade:** Android 13+ ARM64. **Atenção:** applicationId mudou para `br.com.adriedson.nexora`; instala como outro app, sem acesso automático aos dados privados de `br.com.adriedson.ialocal`. Secrets de assinatura anteriores são compatíveis, mas não migram dados. APK Android e testes em hardware pendentes.

### Alpha.7
Medidor integrado à tela de chat: ritmo aproximado de geração, latência, memória do processo, RAM do sistema, temperatura da bateria e estado térmico. Qwen3 4B Q4_K_M adicionado ao catálogo com hash oficial e aviso de risco de falta de RAM. Compilação Android e execução física ainda pendentes.
