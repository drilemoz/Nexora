# Nexora • v0.1.0-alpha.16 (versionCode 16)

**Alpha.16:** central de modelos com busca, ordenação, menu de ações e perfis reais de inferência por GGUF (Econômico, Equilibrado, Personalizado). Contexto, threads, temperatura, Top-P, Top-K, Min-P e penalidade de repetição são aplicados pelo motor pinned ao **próximo carregamento**; limite de resposta é usado no próximo envio. A interface de conversas foi atualizada com Nexora à direita e usuário à esquerda, formatação básica Markdown, copiar/detalhes e regeneração da última resposta com recarga do contexto. Barra de RAM mostra porcentagens disponíveis e em uso; painel expande/recolhe ao tocar no cabeçalho. Janela Recursos com ícones, diagnóstico compacto, filtros por sessão/versão e JSON salvo em Downloads/Nexora diretamente por MediaStore, sem upload. PDFs indicam extração parcial e somente trechos no prompt. Catálogo com busca/ordenação e aviso conservador de RAM. **Exige teste completo Android/JNI na CI e no aparelho** antes de publicar como validada.

**Alpha.15:** painel de desempenho compacto com seta para detalhes, RAM disponível medida e barra proporcional. Temperatura exibida é da bateria; estado térmico vem do Android (código bruto e, se suportada, margem térmica). Progresso durante a geração é indeterminado, sem porcentagem artificial. Nenhum motor ou modelo é alterado.

**Alpha.14:** nomes de exibição configuráveis em Configurações → Modelos de IA → detalhes → Renomear. O nome original do GGUF não muda, e é mostrado nos detalhes e no toque prolongado no cabeçalho do chat. Ex.: `Qwen3-4B-Q4_K_M.gguf` aparece como `Qwen 3 • 4B`. Restaurar padrão limpa apenas o apelido.

Android nativo Kotlin, modelos GGUF locais via llama.cpp, com catálogo de downloads opcionais. **Não usa servidor para inferência.**

## Alpha.13: fim de geração, matemática confiável e diagnóstico real
- Patch adicional no JNI/llama.cpp fixado: distingue fim natural (EOG), limite de tokens e erro nativo; mede quantidade real de tokens, quantidade de tokens do prompt e tempo de processamento/geração. Não confundir com os fragmentos recebidos na interface.
- A resposta não será rotulada "concluída" quando atingir limite ou terminar por erro/desconhecido. O painel apresenta tok/s nativo ao fim da resposta e frag./s somente durante o streaming.
- Expressões matemáticas simples iniciadas por "Quanto", "Calcule" etc. recebem avaliação decimal local, com precedência, parênteses e porcentagem, sem inventar ferramenta externa. Consultas ambíguas ou sintaxe não suportada continuam para o modelo, com risco habitual de erro.
- Diagnóstico registra resumo nativo da geração e amostras de PSS/RAM/temperatura a cada 15s enquanto o chat está aberto, sem conteúdo das mensagens. Versão no JSON vem do PackageManager, corrigindo o antigo valor fixo alpha.11.
- Nenhuma mudança nas threads ou nas credenciais/identidade. Build completo e teste físico pendentes.

## Alpha.12: desempenho e publicacao unica de APK
- GitHub Actions gera apenas um APK de instalacao: Release assinado quando os quatro Secrets estao presentes, Debug sem Secrets. A mesma assinatura estavel continua valida.
- Motor fixado ao mesmo commit, com patch checado no bootstrap: janela de contexto 4096 (antes 8192) para reduzir memoria KV; corrige contador de tokens gerados e posicao efetiva apos truncamento de prompt.
- Motor suprime logs de prompts e de cada token de entrada, evitando custo e exposição de texto no Logcat. Streaming usa filtro incremental e atualizacao da UI no maximo 8 vezes/s, reduzindo trabalho redundante. Amostras de PSS e estado termico a cada 5s fora da thread principal.
- Limitacao: contexto de 4096 pode truncar PDFs e historicos grandes. Nao significa melhora garantida de tokens/s. Validar no aparelho.

## Alpha.11: interface, gerenciamento e carregamento

- Carregamento automático/manual protegido por diálogo centralizado, cancelamento cooperativo e execução independente do ciclo de vida da tela. O motor desta versão não fornece à UI porcentagem nativa; são mostrados tempo decorrido e histórico quando disponível.
- Botão de CPU/RAM/temperatura na tela inicial; símbolo Nexora com fundo para não desaparecer em superfície clara; lupa para abrir pesquisa sob demanda.
- Conversas com menu inferior padronizado e confirmação de exclusão temática; nenhuma nova conversa pode ser criada sem modelo pronto, mas histórico antigo permanece acessível.
- Lista de modelos com cartões e detalhes de arquivo; importação SAF copia GGUF para armazenamento privado (comportamento já existente). Após carga manual, retorno à tela inicial.
- Medidor de geração corrige a rotulagem: fragmentos/s não são necessariamente tokens/s; log diferencia cancelamento manual de interrupção por saída da tela.

## Alpha.10: Diagnóstico e correção do Build 4

- Ícone de bug ao lado da engrenagem; painel de CPU do processo, RAM Android, PSS da Nexora, temperatura da bateria e pressão térmica.
- Eventos persistentes limitados e exportação JSON sob demanda. Sem upload, sem serviço permanente e sem registrar conteúdo de mensagens/PDFs.
- Testes de catálogo atualizados de 3 para 4 modelos; URL Qwen3 4B fixada em commit.

## Alpha.9: Carregamento automático e correção do build

- Correção do atributo XML inválido que impedia `:app:processDebugResources`.
- Preferência de iniciar com o último modelo GGUF carregado com sucesso; opção desligável em Configurações.
- Tela inicial indica carregamento, nome do modelo, tempo decorrido e estimativa de progresso/tempo restante apenas quando existe histórico real da carga anterior do mesmo arquivo.
- Se a carga automática anterior tiver sido interrompida pelo Android, o próximo arranque exige carregamento manual por segurança.

## Alpha.8: Conversas, PDFs e modelos

- **Nova tela inicial** baseada no layout de referência: Nexora, Nova conversa, busca no histórico, seções Hoje/Ontem/Anteriores/Fixadas, menu de três pontos para Renomear/Fixar/Excluir. O modelo não precisa estar carregado para consultar conversas antigas.
- **Histórico SQLite** em `nexora_history.db`, uma conversa por ID com mensagens e PDFs independentes; mantém até 200 mensagens por conversa. Tenta migrar `conversation.json` da alpha.7 para uma nova conversa e renomeia o arquivo original para `conversation.migrated.json` somente após salvar. Não apaga arquivo em caso de erro.
- **Isolamento de contexto:** um modelo em memória; ao alternar para uma conversa diferente a engine é descarregada e recarregada antes de responder, e parte das últimas mensagens da conversa é incluída no primeiro prompt para continuidade. Isso pode causar espera e não reconstrói perfeitamente sessões longas.
- **PDF offline:** botão de clipe no chat, seletor SAF, extração local de texto com `com.tom-roush:pdfbox-android:2.0.27.0` (Apache 2.0). Salva texto extraído no banco associado à conversa e exibe chips de anexos, removíveis. Admite PDF com até 12 MiB, extrai até 40 páginas e 24 mil caracteres (truncamento informado), até cinco documentos por conversa. Usa trechos de até 1600 caracteres de até dois PDFs mais recentes por pergunta. Não extrai texto de documentos digitalizados sem OCR; não envia arquivos a servidor.
- **Gerenciamento de modelos movido** para `Configurações → Modelos de IA`, onde há três botões Importar, Modelos e Baixar, relação de GGUF instalados com tamanho real, Carregar/Excluir e resumo do espaço livre/total e espaço ocupado pelos GGUF. Catálogo e barra de downloads em MB continuam.
- Mantidos temas, medidor de desempenho, controle Qwen3 Direto/Pensar, downloads com SHA-256 e assinatura estável com os quatro Secrets existentes. Identificador Android `br.com.adriedson.nexora` inalterado.

## Como usar

1. Abra Nexora: a página principal lista conversas; para criar outra, carregue um modelo primeiro.
2. Configurações → **Modelos de IA** → Baixar ou Importar GGUF → Carregar. O modelo é único em memória.
3. Após o carregamento, a tela inicial é aberta automaticamente. Toque Nova conversa ou em uma já existente. O modelo escolhido aparece no cabeçalho do chat, que permite abrir o gerenciador se necessário.
4. Toque no clipe 📎 para selecionar um PDF com texto (até 12 MiB). Aguarde a extração; um chip com nome/páginas aparecerá. Pergunte sobre o PDF. Apenas o texto relevante e limitado é oferecido ao modelo; ele pode errar.
5. Abra a busca pela lupa e pesquise títulos ou conteúdo, ou o menu de cada chat para fixar, renomear e excluir, com confirmação para exclusão.

## Build e requisitos

- Android 13+ / ARM64, Java 17, Android API 36, NDK 29, CMake 3.31.6.
- ZIP apenas do código Kotlin/recursos e scripts; `scripts/bootstrap.sh` prepara o wrapper, catálogo Gradle e motor llama.cpp fixado em commit. Compilação requer internet e dependência PDFBox-Android em Maven Central.
- GitHub Actions **Gerar APK Nexora**: `Nexora-v0.1.0-alpha.16-apk-release-assinado` (com Secrets) **ou** `Nexora-v0.1.0-alpha.16-apk-debug` (sem Secrets).
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


## Carregamento automático (alpha.10)
- Abra Configurações → Modelos de IA e carregue um GGUF ao menos uma vez com sucesso.
- A Nexora registra apenas nome e pasta privada do último modelo. Por padrão, tenta carregá-lo na próxima abertura do app.
- O ajuste Carregar ao abrir, em Configurações, pode ser desativado.
- Enquanto carrega, a tela inicial mostra o nome, o tempo decorrido e a barra animada. A porcentagem e o tempo restante aparecem apenas se houver medição anterior **do mesmo arquivo com o mesmo tamanho e data de modificação**, como estimativas.
- Se o Android encerrar o processo durante um carregamento automático, a tentativa seguinte é bloqueada por segurança; carregue manualmente o GGUF e escolha outro modelo se estiver exigindo muita RAM.
- Não é um preload de segundo plano: a abertura continua possível antes do modelo terminar.


## Diagnóstico alpha.10
Acesse o ícone de bug à esquerda de Configurações na tela inicial. Há leituras atualizadas a cada 3 segundos **somente enquanto a página estiver visível**, uma fila circular persistente de eventos locais e exportação manual como JSON. CPU representa apenas consumo deste processo Android, em equivalentes de núcleo; o Android não disponibiliza medição global confiável sem privilégios. Registros não incluem conteúdo de mensagens, PDFs ou credenciais.

No Build 4, 2 testes de catálogo falharam por esperarem 3 modelos e um link não fixado para o 4B; agora o catálogo usa 4 e revision SHA estável.

### Painel local (alpha.15)
No chat, o painel compacto resume velocidade, PSS, bateria, RAM disponível e estado térmico informado pelo Android. Toque na seta para ver contadores nativos, nome completo do GGUF, pico de PSS, código térmico bruto e margem térmica apenas quando disponível (API 30+). Bateria não é CPU e não existe progresso percentual verdadeiro da resposta.
