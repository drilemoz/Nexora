# Nexora • v0.1.0-alpha.8 (versionCode 8)

Android nativo Kotlin, modelos GGUF locais via llama.cpp, com catálogo de downloads opcionais. **Não usa servidor para inferência.**

## Alpha.8: Conversas, PDFs e modelos

- **Nova tela inicial** baseada no layout de referência: Nexora, Nova conversa, busca no histórico, seções Hoje/Ontem/Anteriores/Fixadas, menu de três pontos para Renomear/Fixar/Excluir. O modelo não precisa estar carregado para consultar conversas antigas.
- **Histórico SQLite** em `nexora_history.db`, uma conversa por ID com mensagens e PDFs independentes; mantém até 200 mensagens por conversa. Tenta migrar `conversation.json` da alpha.7 para uma nova conversa e renomeia o arquivo original para `conversation.migrated.json` somente após salvar. Não apaga arquivo em caso de erro.
- **Isolamento de contexto:** um modelo em memória; ao alternar para uma conversa diferente a engine é descarregada e recarregada antes de responder, e parte das últimas mensagens da conversa é incluída no primeiro prompt para continuidade. Isso pode causar espera e não reconstrói perfeitamente sessões longas.
- **PDF offline:** botão de clipe no chat, seletor SAF, extração local de texto com `com.tom-roush:pdfbox-android:2.0.27.0` (Apache 2.0). Salva texto extraído no banco associado à conversa e exibe chips de anexos, removíveis. Admite PDF com até 12 MiB, extrai até 40 páginas e 24 mil caracteres (truncamento informado), até cinco documentos por conversa. Usa trechos de até 1600 caracteres de até dois PDFs mais recentes por pergunta. Não extrai texto de documentos digitalizados sem OCR; não envia arquivos a servidor.
- **Gerenciamento de modelos movido** para `Configurações → Modelos de IA`, onde há três botões Importar, Modelos e Baixar, relação de GGUF instalados com tamanho real, Carregar/Excluir e resumo do espaço livre/total e espaço ocupado pelos GGUF. Catálogo e barra de downloads em MB continuam.
- Mantidos temas, medidor de desempenho, controle Qwen3 Direto/Pensar, downloads com SHA-256 e assinatura estável com os quatro Secrets existentes. Identificador Android `br.com.adriedson.nexora` inalterado.

## Como usar

1. Abra Nexora: a página principal lista suas conversas e permite criar outras.
2. Configurações → **Modelos de IA** → Baixar ou Importar GGUF → Carregar. O modelo é único em memória.
3. Volte à tela inicial e toque Nova conversa ou em uma já existente. O modelo escolhido aparece no cabeçalho do chat, que permite abrir o gerenciador se necessário.
4. Toque no clipe 📎 para selecionar um PDF com texto (até 12 MiB). Aguarde a extração; um chip com nome/páginas aparecerá. Pergunte sobre o PDF. Apenas o texto relevante e limitado é oferecido ao modelo; ele pode errar.
5. Use a busca por títulos e conteúdo, ou o menu de cada chat para fixar, renomear e excluir, com confirmação para exclusão.

## Build e requisitos

- Android 13+ / ARM64, Java 17, Android API 36, NDK 29, CMake 3.31.6.
- ZIP apenas do código Kotlin/recursos e scripts; `scripts/bootstrap.sh` prepara o wrapper, catálogo Gradle e motor llama.cpp fixado em commit. Compilação requer internet e dependência PDFBox-Android em Maven Central.
- GitHub Actions **Gerar APK Nexora**: `Nexora-v0.1.0-alpha.8-apk-debug` e `Nexora-v0.1.0-alpha.8-apk-release-assinado` (se os quatro Secrets configurados).
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Nunca inserir credenciais nem pesos no repositório.

## Limitações

- Alpha de desenvolvimento: testes de build Gradle Android, UI, banco SQLite e extração PDF no telefone ainda pendentes; verificações locais não substituem esses testes.
- PDFs protegidos por senha ou só imagens exigem tratamento/OCR não implementado. Documentos grandes são parcialmente extraídos e modelos pequenos recebem somente trechos reduzidos; a Nexora não analisa integralmente arquivos extensos.
- Armazenamento "total/livre" refere-se ao volume de dados acessível via `StatFs(filesDir)`, não necessariamente à capacidade comercial do aparelho. O total de GGUF soma arquivos nos diretórios privados da Nexora; arquivos em outros aplicativos não entram na conta.
- Modelos de IA podem inventar respostas ou interpretar instruções presentes em PDF como texto de comando apesar do aviso no prompt. Não executar automaticamente código ou ações sugeridas pelo PDF.
- Após troca entre chats, o modelo recarrega para isolar contexto e pode demorar. O histórico textual resumido não recria exatamente o contexto interno anterior. Memória, temperatura e desempenho dependem do aparelho.
- Migração só dentro do mesmo package `br.com.adriedson.nexora`. O app anterior IA Local, com identificador diferente, permanece isolado pelo Android.

## Licenças

`llama.cpp` (MIT), `PdfBox-Android` (Apache 2.0), modelos e pesos com licenças independentes.

---

## Histórico anterior (alpha.7)

## Novo na alpha.7
- Tela inicial reconstruída com identidade Nexora, painel do modelo ativo, botão **Conversar** e ações **Importar**, **Modelos** e **Baixar** na mesma linha, com espaçamento e ícones.
- **Chat agora em tela própria:** cabeçalho exibe o arquivo GGUF atualmente carregado. Histórico local da Nexora e botão de limpar com confirmação.
- Barra de composição redesenhada com botão de envio por ícone e controle **Direto/Pensar** à esquerda da mensagem. Em modelos Qwen3, aplica `/no_think` ou `/think` à mensagem enviada ao motor. Para outros modelos, o controle é indisponível; não é um recurso universal do GGUF.
- Os tokens entre `<think>` e `</think>` são omitidos do chat, inclusive com marcações fracionadas na geração progressiva. A Nexora não mostra essa saída intermediária, mesmo se o Qwen3 ignorar `/no_think`.
- Configurações abre **nova tela Escolher tema** com quatro cartões clicáveis: Sistema, Claro, Escuro e Preto AMOLED (não há seletor por popup).
- Catálogo com cartões mais espaçados, botões Baixar/Página separados, painel de transferência com barra preenchida e bytes recebidos vs total (`30,0 de 300,0 MB`, se o servidor informar tamanho), pausa/erro e cancelamento. Com total desconhecido, apresenta bytes reais + tamanho estimado sem calcular porcentagem fictícia.
- Botão **Página** exige confirmar a saída em um diálogo personalizado antes de abrir `huggingface.co` no navegador.
- DownloadManager permanece responsável pelas transferências, inclusive com a tela apagada (condicionado às políticas do Android), com confirmação GGUF e SHA-256 antes de disponibilizar.
- Identificadores, manifesto, nomes de artifacts, documentação e Gradle sincronizados para `alpha.7`.

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
- `Nexora-v0.1.0-alpha.7-apk-debug`
- `Nexora-v0.1.0-alpha.7-apk-release-assinado` (se todos os quatro Secrets estiverem configurados)

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

## Novo na alpha.7: medidor de desempenho
No chat, o cartão **DESEMPENHO LOCAL** mostra o ritmo de fragmentos emitidos pela engine (aproximação de tokens/s), tempo até o primeiro fragmento, PSS do processo Nexora (não equivale ao total exclusivo do modelo), RAM livre reportada pelo Android, temperatura da bateria (não CPU) e pressão térmica do Android. Amostragens de memória/bateria ocorrem a cada 2 s **somente enquanto o chat está em primeiro plano**. O contador inclui fragmentos de raciocínio que não aparecem no chat. Um fragmento não-vazio não corresponde necessariamente a exatamente um token e não substitui benchmark nativo de llama.cpp. Para comparar modelos, use a mesma pergunta, modo de raciocínio e condições térmicas; avalie também tempo até início e uso de memória. Não há medição de corrente/energia implementada.

Novo catálogo: **Qwen3 4B Q4_K_M** (2,5 GB; SHA-256 validado conforme página oficial do arquivo). AVISO: o arquivo não é o total de RAM exigida! O motor e o KV cache exigem memória adicional; não é garantido que carregue no dispositivo do usuário. Se faltar RAM, não confundir com falha de integridade. Um modelo mais pesado pode não resultar em respostas mais rápidas ou corretas em todos os usos.

O gráfico horizontal é uma escala visual de 0 a 30 fragmentos/s, saturando acima desse valor; rótulos de fluidez são faixas indicativas da geração, não prova de precisão do conteúdo.
