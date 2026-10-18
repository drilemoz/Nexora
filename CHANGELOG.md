## 0.1.0-alpha.24 (versionCode 24) • 17/10/2026

- Corrige o erro AAPT2 `attribute insetBottom/insetTop not found` no `item_conversation.xml`: substitui os atributos `app:` por `android:` nos dois `MaterialButton` de seleção de histórico, preservando insets 0dp e a aparência.
- Adiciona `scripts/verify_material_buttons_alpha24.py` ao CI para detectar futuros usos de `app:inset*` em botões Material.
- Sem alterações em llama.cpp/JNI, tokenização, cálculo, mensagens, base de dados ou assinatura. Teste AAPT2/Gradle real ainda pendente.

## 0.1.0-alpha.23 (versionCode 23) • 17/10/2026

- Botão de exportação no topo da conversa e JSON consentido com histórico completo e diagnóstico de inferência, sem texto de PDFs.
- Modo minimizado para o painel de desempenho, independente de expandir os detalhes; recolhe métricas e barra de RAM.
- Ações Selecionar e Todas/Nenhuma atualizadas para Material outlined, com toque mais consistente nos temas.
- Versões e documentos sincronizados; sem mudanças na engine.

## 0.1.0-alpha.22 (versionCode 22) • 17/10/2026
- Integração com os templates embutidos no GGUF via llama.cpp fixado; Jinja aplicado quando validação sintética de papéis e histórico passa; preservação de marcadores e proteção de BOS duplicado.
- Detecção e estado reportados por JNI; proíbe fallback silencioso quando o template existe mas falha. Sem template: status não aplicado e compatibilidade legada explicitada.
- Nova tela Configurações → Compatibilidade dos modelos, Verificar template e Copiar diagnóstico seguro. Modo Personalizado bloqueado por falta de caminho arbitrário validado.
- Eventos somente com enums, sem conteúdo privado; validação estática e unitária; compilação Android e teste com GGUF real pendentes.

## 0.1.0-alpha.21 (versionCode 21) • 17/10/2026
- Na tela inicial, mostra o GGUF realmente carregado sob o nome Nexora, com apelido de apresentação e estados de carregamento/ausência.
- Controles compactos de seleção e lixeira ao lado do primeiro cabeçalho da lista de conversas: selecionar várias por toque ou pressão longa, marcar/desmarcar todas as visíveis e cancelar.
- Exclusão múltipla transacional e opção separada de excluir todas as conversas, ambas com confirmação e contagem; FK SQLite remove mensagens e PDFs associados, mantendo modelos GGUF.
- Cabeçalhos do histórico por dia local: HOJE apenas no dia corrente e dd/MM/aaaa para cada dia anterior. Conversas fixadas também incluem a data no subtítulo. Virada da meia-noite atualiza agrupamento enquanto a tela está visível.
- Nova bateria de testes para datas/fuso, regressão de layout e exclusões SQLite por cascata. Nenhuma modificação no schema do banco, inferência, permissões ou assinatura.
- Build Android/Gradle completo e validação UI em aparelho pendentes.

## 0.1.0-alpha.20 (versionCode 20) • 17/10/2026
- Interface de conversa: painel Desempenho local movido do rodapé para abaixo do indicador IA LOCAL ATIVA, antes da lista de mensagens.
- Mantidos status, velocidade, PSS, bateria, RAM, pressão térmica e expansão de detalhes, sem modificar o monitoramento ou o armazenamento.
- Campo de mensagem, anexos e status continuam no rodapé; RecyclerView permanece com altura flexível e rolagem independente.
- Validação de posicionamento XML adicionada ao workflow; build Android e teste visual em aparelho permanecem pendentes.

## 0.1.0-alpha.19 (versionCode 19) • 17/10/2026
- Reduz redraw de respostas: até 5 atualizações de texto por segundo em vez de até 8; renderiza apenas quando novos caracteres visíveis surgem, sem redraw para tokens ocultos de `<think>`.
- RecyclerView recebe payload apenas de texto durante streaming; evita reconstruir fundo, dimensões e handlers das mensagens; mantém a posição de leitura ao rolar para cima.
- Markdown reutiliza Regex compiladas e lineSequence() para reduzir alocações na renderização; filtro `<think>` evita novas strings no caminho comum por caractere.
- Salvamento SQLite passa a preservar sufixo das linhas antigas correspondente ao prefixo da nova sequência, apagando apenas linhas obsoletas e inserindo apenas novas; limite de 200 mensagens e 32.000 caracteres por item preservados, sem migração.
- Testes unitários acrescentados para política visual e alinhamento incremental do histórico. Modelo GGUF, native/JNI, contexto, permissões e assinatura sem alterações.
- Otimização de carga na UI/SQLite inferida do código; nenhuma melhoria de tokens/s, PSS, bateria ou temperatura foi demonstrada por medição física.

## 0.1.0-alpha.18 (versionCode 18) • 17/10/2026
- Patches auditáveis no llama.cpp fixado: remove o vazamento do texto formatado e dos tokens de saída no Logcat; mantém diagnóstico por contagens sem revelar conteúdo.
- Corrige posições de batches após deslocamento do KV, sem recontar tokens do prompt. Impõe limite de saída pelo contador nativo, que não recua quando o contexto desliza.
- JNI agora limpa KV, histórico e sampler de cada conversa sem recarregar pesos GGUF. Troca de chat e regeneração usam reset leve; cancelamentos, interrupções anormais e limpeza do histórico invalidam o contexto para o próximo envio.
- Protege liberação parcial do motor em falhas de load/prepare; reduz materialização de strings completas durante streaming para as atualizações de UI.
- CI verifica invariantes dos patches após bootstrap; mesma revisão upstream, package, banco SQLite, modelos e assinatura.
- Build Android/JNI, testes de inferência no telefone e medições de performance continuam pendentes.

# Changelog Nexora

## 0.1.0-alpha.16 (versionCode 16)
- Biblioteca GGUF redesenhada: cartões, identificação amigável, tamanho/quantização, pesquisa e ordenação; menus de carregar, descarregar, detalhes, renomear, ajustes e excluir.
- Perfis individuais reais de inferência. O JNI pinned recebe contexto 2048/4096/8192, threads 2–4, temperatura 0–1,5, Top-P, Top-K, Min-P, repeat penalty e limite de tokens; aplicado após recarregar o modelo.
- Mensagens: usuário à esquerda, Nexora à direita, balões com tema; renderização segura e básica de Markdown, copiar, detalhes e regeneração da última resposta com contexto reinicializado.
- Barra de RAM de altura ampliada, duas porcentagens reais com acessibilidade, expansão e minimização do desempenho pelo cabeçalho.
- Diagnóstico compacto: filtro da última sessão/versão/todos, eventos recolhidos, ações de exportar/limpar acima do log e gravação direta via MediaStore em Downloads/Nexora com nome amigável e JSON preservado.
- Pop-up Recursos do aparelho com ícones por medição, catálogo com busca, ordenação e aviso sobre RAM, PDF com indicação de extração/consulta por trechos.
- Validação local sintática e estática; build Android, JNI com pinned upstream e testes em aparelho pendentes.


## 0.1.0-alpha.15 (versionCode 15)
- Painel de desempenho compacto com velocidade, PSS, temperatura da bateria, RAM disponível real e barra referente à RAM, não à velocidade.
- Expansão de detalhes com leitura térmica crua do Android, margem térmica se suportada, versão de métricas nativas, pico de PSS e identificação completa do GGUF.
- Progresso de inferência somente indeterminado durante a geração, sem porcentagens fictícias.
- Diagnóstico e painel de recursos mostram separadamente bateria e estado térmico; eventos incluem código térmico bruto e headroom quando disponível.
- Leituras continuam periódicas apenas com conversa/diagnóstico visível, sem serviços de fundo, usando temas existentes.
- Compilação Android completa e teste no aparelho ainda pendentes.

# Histórico Nexora

## 0.1.0-alpha.14 • 17/10/2026
- Nomes de exibição amigáveis na lista, detalhes, carregamento, tela inicial e conversas; `Qwen3-4B-Q4_K_M.gguf` aparece como `Qwen 3 • 4B` por padrão.
- Opção **Renomear** e **Restaurar padrão** em detalhes do modelo, com apelido persistido em preferências privadas e aplicado também em conversas existentes.
- O arquivo GGUF original, caminhos, associação no SQLite, metadados da quantização e identificação técnica para o llama.cpp **não são alterados**; nome original permanece visível nos detalhes e ao pressionar o rótulo do chat.
- Não altera inferência, limite de contexto, consumo de RAM, assinatura, permissões ou formato do banco.
- Validação Android/Gradle e teste visual no telefone continuam pendentes.


## 0.1.0-alpha.12 • 17/10/2026
- CI publica uma variante por execucao: Release assinado com Secrets ou Debug fallback; elimina a lista duplicada de instaladores no GitHub Manager.
- Patches auditaveis no motor fixado: n_ctx 8192 → 4096 e correcoes do limite de geracao e da contagem apos truncamento do prompt. Preserva o commit upstream. Janelas grandes exigem testes antes de futuras mudancas.
- Removidos logs do texto de prompts e token a token no JNI, reduzindo custo de diagnosticos e vazamento acidental em Logcat.
- Filtro de raciocinio incremental em vez de reprocessar texto inteiro por fragmento; atualizacao visual limitada a ~8 fps.
- Leitura PSS e bateria fora da thread principal, com intervalo de 5 s enquanto a conversa esta aberta.
- Sem novos recursos. Aceleracao nativa/threads nao ajustadas sem benchmark, pois podem aumentar calor e consumo.

## 0.1.0-alpha.11 • 17/10/2026
- Carregamento do último modelo e cargas manuais transferidos para um escopo do processo, evitando cancelamentos por recriação da tela.
- Modal de carregamento centralizado e bloqueante, com Cancelar cooperativo, tempo decorrido e informação honesta sobre progresso nativo indisponível.
- Logotipo visível na tela inicial, pesquisa por lupa, menu de conversas em bottom sheet, confirmação de exclusão padronizada.
- Diagnóstico rápido de CPU/RAM/pressão térmica, detalhes dos modelos, botões em cores do tema, carregamento com retorno automático à tela inicial.
- Validação de modelo antes de criar conversas e nomenclatura de fragmentos/s corrigida para não se passar por tok/s.
- Razão do cancelamento de inferência registrada para facilitar investigação do PDF interrompido.
- Sem alteração na revisão do llama.cpp ou no formato do banco de dados.


## 0.1.0-alpha.10 • 17/10/2026
- Corrige os 2 testes do catálogo no workflow Build 4: quatro modelos, inclusive Qwen3 4B, e revisão fixada no repositório oficial para o Qwen3 4B.
- Novo ícone de bug ao lado das Configurações na tela inicial e página exclusiva Diagnóstico.
- Diagnóstico: CPU do processo relativa a um núcleo e à capacidade dos núcleos, RAM global do Android, PSS da Nexora, temperatura da bateria, pressão térmica; sem alegar acesso à CPU global.
- Registro em JSONL com limite de 256 KiB, eventos de carregamento, contexto, geração, PDF, encerramentos inesperados; sem prompts, textos dos PDFs, senhas ou caminhos.
- Botões para exportar JSON via seletor do Android e limpar registros após confirmação.
- Amostragem de 3 segundos SOMENTE com a página aberta e log de métricas no máximo a cada 15 segundos. Zero serviço persistente/sem wakelock.
- Compilação Gradle/Android e testes físicos pendentes.

## Histórico anterior

## 0.1.0-alpha.10 (versionCode 10)
- Corrigido AAPT2: `android:horizontalScrollBarEnabled` inválido substituído por `android:scrollbars="none"` no chat.
- Carregamento automático opcional (ativado por padrão) do último GGUF **carregado com sucesso**, interno ou baixado. A escolha fica em Configurações → Inteligência Artificial.
- Indicador inicial com nome do modelo, tempo transcorrido e barra indeterminada; após uma carga medida do mesmo arquivo, apresenta porcentagem/ETA **estimados**, não porcentagem fornecida pelo llama.cpp.
- Guarda contra ciclo de falha: carga automática interrompida entre processos exige próximo carregamento manual.
- Serialização de carregamentos e trocas de conversa com Mutex; evita disputa de carga simultânea.
- Preservados modelo, chats, PDF, identificador, Secrets e assinatura.
- Testes locais sintáticos e de progresso; compilação Android completa e carregamento no aparelho ainda pendentes.

## 0.1.0-alpha.8
- Nova página inicial de conversas seguindo referência enviada, busca, novo chat, pin/rename/delete, ordenação temporal.
- Histórico SQLite com chats independentes, migração conservadora do conversation.json da alpha.7 e isolamento do contexto da engine.
- Suporte inicial a PDFs offline: SAF, PDFBox Android, extração de texto com limites, chips por conversa e trechos no prompt.
- Gerenciamento de modelos em Configurações, lista de tamanhos reais GGUF e medidor de armazenamento disponível/total.
- Mantidos temas, medidor de desempenho, download verificado, secrets de assinatura e identificador do aplicativo.
- Build Android, integração PDF e testes físicos pendentes.

## 0.1.0-alpha.7
- Medidor local no chat: régua horizontal indicativa de fluidez, velocidade estimada de fragmentos/s, latência para primeira saída, RAM disponível, PSS do processo, temperatura da bateria e pressão térmica. Atualização de leitura a cada 2 segundos apenas com tela visível.
- Catálogo: Qwen3 4B Q4_K_M oficial (2,5 GB), SHA-256 e confirmação de risco de falta de RAM.
- Sem alterações no package, Secrets e certificado; sem novos serviços em segundo plano.

# CHANGELOG

## 0.1.0-alpha.17 (versionCode 17) • 17/10/2026
- Redesenhados os cartões de modelos instalados com nome amigável em destaque e linha secundária de quantização/tamanho. O filename GGUF completo fica nos detalhes técnicos e nas descrições de acessibilidade, não espremido entre botões.
- Biblioteca de modelos apresenta cartões de duas linhas, respeitando busca, ordenação e apelidos existentes.
- Botão de excluir com ícone visual menor, mantendo alvo de toque de 48 dp e diálogo de confirmação; não apaga um modelo ativo.
- Modal de ações do modelo com ícones vetoriais consistentes em todas as cinco opções e área de toque de 52 dp, adaptado aos temas.
- Mantidos dados, modelos, perfil de inferência, diagnóstico, conversas, identificador Android e assinatura. Build e validação visual completos pendentes.

## 0.1.0-alpha.13 • 17/10/2026
- Contadores nativos de tokens e tempos por geração; identifica EOG/limite/erro no JNI com patch auditável sobre revisão fixa do llama.cpp.
- Corrige estado indevido "Resposta concluída" quando o modelo apenas esgotou o limite de tokens; falha de processamento de prompt não é tratada como sucesso.
- Calculadora aritmética determinística para pedidos simples e explícitos, sem envio de comandos nem execução de código de terceiros.
- Diagnóstico agora obtém versionName/versionCode do APK instalado (não de texto hardcoded) e registra PSS/RAM durante geração de forma limitada.
- Mantém context window 4096, threads, temas, histórico, GGUF, assinatura e workflow com um único APK.
- Verificações locais não substituem compilação Android e validação em aparelho.


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
