# Nexora • v0.1.0-alpha.6 (versionCode 6)

Nexora é um aplicativo Android **nativo Kotlin** de IA local, desenvolvido a partir do projeto IA Local. Executa GGUF offline usando o binding Android do **llama.cpp**. Baixa modelos via Hugging Face somente por ação explícita. Não usa servidor de inferência, API paga, anúncios ou telemetria.

## Identidade e migração
- Nome no launcher: **Nexora**. Android `applicationId` e `namespace`: **`br.com.adriedson.nexora`**.
- **MUDANÇA DE PACOTE:** o Android considera a Nexora outro app. Ela **não substitui automaticamente** `br.com.adriedson.ialocal` nem importa os modelos e conversas guardados no sandbox do app antigo. A assinatura permanente é a mesma se os Secrets existentes forem usados. Os dois APKs podem coexistir; configure os Secrets no repositório novo ou existente.
- Ícone adaptativo próprio integrado em `mipmap-anydpi-v26/ic_launcher.xml`, marca vetorial na interface.
- Nenhuma keystore, senha, token ou modelo GGUF incluído no código-fonte.

## Novo na alpha.6
- Tela inicial reconstruída com identidade Nexora, painel do modelo ativo, botão **Conversar** e ações **Importar**, **Modelos** e **Baixar** na mesma linha, com espaçamento e ícones.
- **Chat agora em tela própria:** cabeçalho exibe o arquivo GGUF atualmente carregado. Histórico local da Nexora e botão de limpar com confirmação.
- Barra de composição redesenhada com botão de envio por ícone e controle **Direto/Pensar** à esquerda da mensagem. Em modelos Qwen3, aplica `/no_think` ou `/think` à mensagem enviada ao motor. Para outros modelos, o controle é indisponível; não é um recurso universal do GGUF.
- Os tokens entre `<think>` e `</think>` são omitidos do chat, inclusive com marcações fracionadas na geração progressiva. A Nexora não mostra essa saída intermediária, mesmo se o Qwen3 ignorar `/no_think`.
- Configurações abre **nova tela Escolher tema** com quatro cartões clicáveis: Sistema, Claro, Escuro e Preto AMOLED (não há seletor por popup).
- Catálogo com cartões mais espaçados, botões Baixar/Página separados, painel de transferência com barra preenchida e bytes recebidos vs total (`30,0 de 300,0 MB`, se o servidor informar tamanho), pausa/erro e cancelamento. Com total desconhecido, apresenta bytes reais + tamanho estimado sem calcular porcentagem fictícia.
- Botão **Página** exige confirmar a saída em um diálogo personalizado antes de abrir `huggingface.co` no navegador.
- DownloadManager permanece responsável pelas transferências, inclusive com a tela apagada (condicionado às políticas do Android), com confirmação GGUF e SHA-256 antes de disponibilizar.
- Identificadores, manifesto, nomes de artifacts, documentação e Gradle sincronizados para `alpha.6`.

## Como usar
1. Abra Nexora > **Baixar** e escolha SmolLM2 360M, Qwen3 0.6B ou Qwen3 1.7B, ou **Importar** para selecionar um GGUF já existente.
2. Acompanhe MB recebidos e aguarde a checagem SHA-256. O modelo não será liberado se a assinatura/hash falharem.
3. Toque **Modelos** > modelo > **Carregar**. A tela inicial passará a mostrar o nome do modelo.
4. Toque em **Conversar**. O chat mostra o modelo escolhido no topo; alterne **Direto/Pensar** se for Qwen3.
5. Na engrenagem: **Configurações > Tema do aplicativo** abre a tela dedicada de seleção.
6. Para liberar RAM: **Modelos > Descarregar modelo ativo**.

## Build e requisitos
- Android 13+ / API 33+ e ARM64; minSdk atual exigido pela biblioteca nativa upstream.
- Java 17; Android API 36; NDK `29.0.13113456`; CMake `3.31.6`.
- O ZIP contém somente a fonte do aplicativo. **Não inclui o wrapper Gradle nem o código nativo completo.** O bootstrap obtém uma revisão fixa do upstream; é necessário internet no ambiente de compilação.

```bash
bash scripts/bootstrap.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

O workflow GitHub Actions **Gerar APK Nexora** executa preparação, testes e compilação. Artefatos previstos:
- `Nexora-v0.1.0-alpha.6-apk-debug`
- `Nexora-v0.1.0-alpha.6-apk-release-assinado` (se todos os quatro Secrets estiverem configurados)

## Secrets de assinatura
- `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Os Secrets gerados na alpha.5 da IA Local continuam utilizáveis, desde que sejam configurados no repositório que executará o workflow. O workflow grava a keystore apenas no runner, com permissões restritas, e a remove depois. **Nunca versionar as credenciais.**
- A troca de package impede a atualização dos dados do app antigo: a Nexora terá sandbox separado. Não desinstale o aplicativo antigo sem preservar seus GGUF originais e conversas, se necessários.

## Limitações honestas
- A compilação Android completa e os testes físicos desta versão precisam ocorrer no GitHub Actions e no telefone.
- A camada Qwen3 pensa ou responde diretamente segundo o comportamento do modelo; os comandos são apenas controles do formato de prompt, não uma garantia universal. O texto de raciocínio é ocultado da apresentação.
- O chat persiste as mensagens para leitura local, mas o contexto do motor não é reconstruído automaticamente a partir do arquivo em cada nova sessão.
- Um GGUF de 1,7B pode ficar lento ou causar falta de RAM. **Não** assumir desempenho, consumo ou autonomia antes de medir.
- Ao importar do SAF, o app faz uma cópia privada: reserve espaço para ela. Os modelos baixados ficam em diretório privado externo.
- Não há GPU configurada, assistente de voz, chamadas remotas ou inferência em segundo plano nesta alpha.

## Licença
- Motor nativo baseado no projeto [`llama.cpp` (MIT)](https://github.com/ggml-org/llama.cpp) em commit fixado pelo bootstrap. Pesos/modelos possuem licenças separadas.
