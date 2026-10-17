## 0.1.0-alpha.7
- Medidor local no chat: régua horizontal indicativa de fluidez, velocidade estimada de fragmentos/s, latência para primeira saída, RAM disponível, PSS do processo, temperatura da bateria e pressão térmica. Atualização de leitura a cada 2 segundos apenas com tela visível.
- Catálogo: Qwen3 4B Q4_K_M oficial (2,5 GB), SHA-256 e confirmação de risco de falta de RAM.
- Sem alterações no package, Secrets e certificado; sem novos serviços em segundo plano.

# CHANGELOG

## 0.1.0-alpha.7 • Nexora
- Rebranding completo: nome, applicationId, namespace, ícone adaptativo e identidade visual.
- Tela inicial dedicada aos modelos e botão Conversar; chat independente com nome do GGUF no topo.
- Tela própria para seleção dos quatro temas, sem pop-up.
- Comando Qwen3 Direto/Pensar ao lado do botão de envio; ocultação de blocos `<think>` em streaming.
- Catálogo com botões separados, barra real em MB e diálogo de confirmação ao sair para a página externa.
- Assinatura opcional via Secrets da alpha.5 mantida; versionCode 6.
- Android compilação e validação no aparelho pendentes.

## Histórico anterior (projeto IA Local)
# Histórico de alterações

## 0.1.0-alpha.5 (versionCode 5)
- Assinatura persistente opcional Debug e Release via quatro GitHub Secrets; nada sensível no código-fonte.
- Workflow cria Artifact Release assinado somente quando todos os Secrets estão configurados e fora de pull requests.
- Mantido Debug padrão sem Secrets e incluída validação de configuração parcial.
- Gerada uma chave PKCS12 independente, com pacote privado de importação para GitHub Manager (entregue separadamente).
- Versões e documentação sincronizadas; recursos offline da alpha.4 preservados.
- Build Android da alpha.5 ainda pendente no GitHub Actions.

## 0.1.0-alpha.4 (versionCode 4)
- Catálogo interno com 3 modelos GGUF e links fixos/revisados do Hugging Face.
- DownloadManager com progresso, cancelamento e recuperação ao reabrir a tela.
- Validação GGUF e hash SHA-256 antes de disponibilizar o arquivo; download não duplica o GGUF.
- Integração com Meus modelos e compatibilidade com importação manual.
- Internet somente para download; inferência offline e temas da alpha.3 preservados.

# Changelog

## 0.1.0-alpha.3 (versionCode 3) • 2026-10-17
- Adiciona engrenagem de Configurações no cabeçalho e tela de ajustes.
- Implementa seleção persistente de temas Sistema, Claro, Escuro e AMOLED.
- Atualiza as cores dos painéis, chat e barras para acompanhar o tema; AMOLED usa preto puro.
- Mantém referência ao modelo carregado ao recriar a Activity após troca de tema.
- Adiciona três testes unitários de valores de tema e sincroniza metadados.
- Compilação Android e validação em aparelho pendentes.

## 0.1.0-alpha.2 (versionCode 2) • 2026-10-16
- Corrige falha do primeiro workflow na etapa Android SDK: pacote `tools` removido pelo Google.
- Atualiza `android-actions/setup-android` de v3 para v4, fixa `packages: platform-tools`.
- Instala SDK API 36, NDK 29.0.13113456 e CMake 3.31.6 por `sdkmanager --install`.
- Sincroniza versão do Android, nome do Artifact e documentação.
- Build Android efetivo permanece pendente de execução no GitHub Actions.


## 0.1.0-alpha.1 (versionCode 1) • 2026-10-16
- Primeira versão Kotlin Android.
- Chat offline com streaming, interrupção, histórico local e limpeza.
- Importação GGUF via seletor de documentos Android e cópia atômica.
- Gerenciador local de modelos, carregamento, troca e remoção.
- Integração com biblioteca Android oficial do llama.cpp por fonte fixada.
- CI para testes unitários e APK Debug ARM64.
