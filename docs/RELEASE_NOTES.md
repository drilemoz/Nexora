# Nexora 0.1.0-alpha.24

**Correção de compilação nos controles Material do histórico.**

- Corrige `:app:processDebugResources` no build #16: os botões Selecionar e Todas/Nenhuma usam agora `android:insetTop="0dp"` e `android:insetBottom="0dp"` no lugar de `app:`, de acordo com o MaterialButton.
- Mesmas funcionalidades da alpha.23 e mesma aparência esperada, sem alterações no motor GGUF, conversa, PDF ou parâmetros de inferência.
- A validação local de XML, dos atributos e testes estáticos não equivale a build Android; é necessário reexecutar GitHub Actions e testar no aparelho.

# Nexora 0.1.0-alpha.23

**Exportar conversas com contexto técnico, painel realmente minimizável e seleção consistente com Material.**

- Nova ação Exportar no topo do chat: confirma a presença de mensagens pessoais, salva um JSON de perguntas e respostas completas em Downloads/Nexora, oferece compartilhamento voluntário via Android. Inclui nome GGUF, snapshot de RAM/temperatura, estado do template e estatísticas nativas da última geração quando presentes. Não incorpora texto dos PDFs nem registros completos privados.
- Painel Desempenho local com três estados: minimizado (somente faixa), resumido e expandido. Botão independente de minimizar/restaurar junto à seta; padrão inicial minimizado para liberar área de leitura. Preferência persiste.
- Selecionar e Todas/Nenhuma agora usam Material OutlinedButtons com temas existentes. Exclusão em lote e confirmações preservadas.
- Sem alteração em GGUF, JNI, SQLite, rede ou assinatura. Testes em aparelho e build Android devem validar visual e compartilhamento.

# Nexora 0.1.0-alpha.22

**Templates GGUF automáticos e diagnóstico verificável.**

- Usa template do próprio GGUF via llama.cpp fixado. Adiciona verificação sintética dos papéis e do histórico, sem gerar resposta. Sem template declarado: identifica modo legado (não aplicado); template incompatível: bloqueia o envio.
- Nova tela Configurações → Compatibilidade dos modelos com nome de arquivo completo, identificação, origem, aplicação, revisão do motor, resultado, botão Verificar template e Copiar diagnóstico.
- Jinja do motor é usado nos turnos quando a verificação é aprovada; tokenização impede BOS duplicado. Modo de raciocínio Qwen continua sem alterações, não verificado semanticamente nesta validação.
- Personalizado continua desabilitado enquanto não houver aplicação segura de overrides arbitrários. Não aplica template de um GGUF a outro. Eventos guardam apenas códigos, sem textos privados.
- Validações locais, testes NDK/Android e ensaios em modelos reais descritos em `docs/VALIDATION.md`.

# Nexora 0.1.0-alpha.21

**Modelo ativo na tela inicial, datas reais e exclusão em lote.**

- Abaixo do título **Nexora** aparece o apelido/nome do GGUF carregado, ou seu estado de carregamento/ausência.
- Ao lado do primeiro título do histórico (Hoje, data ou Fixadas) aparecem **Selecionar** e a lixeira; ao selecionar, é possível marcar várias conversas, marcar/desmarcar todas as listadas e excluir somente as selecionadas.
- Fora do modo de seleção, a lixeira exclui todo o histórico. Ambas as operações exigem confirmação e informam que mensagens e textos de PDF também serão eliminados, **sem apagar modelos GGUF**.
- Separação diária do histórico no horário local: HOJE apenas no dia vigente e datas dd/MM/aaaa para os demais; fixadas mantêm destaque e exibem dia no subtítulo. Atualiza após meia-noite e ao retomar a atividade.
- SQLite preserva schema v1; não houve alteração em modelos, código nativo, threads, consumo ou assinatura. Compilação real Android, testes de interação, temas e performance ainda pendentes.

# Nexora 0.1.0-alpha.20

**Painel de desempenho realocado para o topo do chat.**

- Cartão DESEMPENHO LOCAL agora aparece abaixo de IA LOCAL ATIVA, antes do histórico da conversa; o editor e os anexos permanecem na parte inferior.
- O painel mantém as métricas e o controle de expansão; nenhuma mudança em GGUF, JNI, SQLite, assinatura ou política de amostragem.
- Testes de regressão estáticos de hierarquia do XML e ids; compilação Android/Gradle e teste visual no aparelho ainda pendentes.

# Nexora 0.1.0-alpha.19

**Otimizações do chat e do armazenamento, sem alterar o motor de IA.**

- Streaming exibe texto novo a até 5 atualizações/s (antes até 8), sem redesenhar Markdown para conteúdo de raciocínio oculto; medidor continua visível com periodicidade reduzida.
- Atualizações incrementais do RecyclerView alteram só o texto; decorações e listeners da bolha são preservados, e a rolagem manual não é interrompida.
- Regex do Markdown são compiladas uma vez; processamento de linhas evita lista intermediária. Filtro `<think>` não aloca strings temporárias para caracteres comuns.
- SQLite preserva registros antigos inalterados quando novos textos são salvos, incluindo a janela móvel de 200 mensagens. Transações, formato, limite por mensagem e conversas existentes permanecem intactos.
- Novos testes unitários para decisões de atualização do texto e persistência incremental; sem modificações de GGUF, JNI, API Android ou política de assinatura.
- Pendente validação Android/Gradle da alpha.19 e testes físicos de responsividade, memória, autonomia e carregamento do Qwen3-4B; não há benchmark comprovando ganho no aparelho.

# Nexora 0.1.0-alpha.18

**Correções de inferência, memória e privacidade.** Revisão do llama.cpp e parâmetros de perfis preservados.

- Impede exposição de texto formatado e tokens de saída no Logcat do JNI.
- Corrige limite de tokens emitidos e sincronização de posições quando o KV faz context shifting; falhas de decode permanecem identificadas.
- Reinicia estado da conversa sem descarregar/recarregar o GGUF ao trocar de chat ou regenerar. Após cancelamento/erro/limite, preserva o histórico visual e recompõe contexto no próximo envio; mensagens anteriores incluídas de forma resumida, não fiel a uma sessão completa.
- Em falhas parciais, cleanup do JNI libera ponteiros alocados com verificações para valores nulos.
- Filtro do streaming evita copiar o texto completo a cada fragmento.
- Ainda é uma alpha: é obrigatório validar compilação JNI/Android e fluxo de inferência/cancelamentos num aparelho; os testes de verificação do patch são estáticos.

# Nexora 0.1.0-alpha.17

**Ajustes de interface dos modelos:** identificação enxuta com nome amigável, quantização e tamanho, menu completo com ícones para as cinco ações e lixeira menor (sem perder área de toque ou confirmação). Filename técnico completo permanece nos detalhes. Sem alteração no motor e na assinatura.

- Nova biblioteca visual GGUF com menus, busca e ordenação, sem renomear os arquivos físicos.
- Configurações reais por modelo: perfis Econômico, Equilibrado e Personalizado; alterar parâmetros exige recarregar o GGUF, exceto limite de tokens das próximas solicitações. Contextos maiores podem aumentar RAM e causar falha de carregamento.
- Chat alinhado (usuário à esquerda, Nexora à direita), Markdown básico, copiar e regenerar última resposta com reinicialização de contexto (pode demorar).
- RAM com porcentagens dentro da barra, detalhes de desempenho expansíveis e janela de recursos com ícones.
- Diagnóstico compacto, filtros sessão/versão e exportação JSON direta e privada na pasta Downloads/Nexora. Não há envio automático.
- Aviso de PDFs parcialmente extraídos e trechos efetivamente utilizados.
- Compilação Android completa/JNI e testes físicos de temas, download, armazenamento e inferência não executados neste ambiente.

# Nexora 0.1.0-alpha.15

- Painel local de desempenho redesenhado, compacto e expansível.
- Bateria não é CPU: leitura do estado térmico Android com código numérico e headroom da API 30+ quando suportado.
- Barra de RAM mostra disponibilidade medida; indicação indeterminada durante a geração, sem porcentagem falsa.
- Redução de texto repetitivo durante espera, visual consistente com os temas.
- Sem mudança de motor GGUF, modelos, assinatura ou histórico.
- Build Android e validação física pendentes.

# Nexora 0.1.0-alpha.14

- Nome amigável automático para arquivos GGUF conhecidos e personalização local por modelo.
- Ação Renomear nos detalhes e Restaurar padrão no editor, sem mexer no GGUF ou no histórico das conversas.
- Apelido aplicado a carregamento, lista, conversas e nome ativo; nome original sempre consultável nos detalhes.
- Compatibilidade da biblioteca e do motor preservada, sem novos serviços.
- Build Android e teste físico pendentes.

# Nexora 0.1.0-alpha.13

- Diferencia saída concluída (EOG) de resposta incompleta ao atingir limite de tokens ou erro de decode.
- Mostra tokens/s nativos medidos após geração e registra tempos de prompt/geração, quantidade de tokens e razão de término.
- Calcula localmente apenas expressões aritméticas explícitas e seguras, identificando aproximações decimais.
- Telemetria limitada de PSS/RAM/estado térmico durante a inferência; diagnóstico obtém versão real do pacote instalado.
- Interface, PDF e assinatura preservados; validação em aparelho pendente.

## Versão anterior 0.1.0-alpha.12

- Novo ícone Diagnóstico na tela inicial: CPU do processo, RAM do sistema, memória Nexora, bateria, pressão térmica.
- Log local de eventos de inferência, PDF, modelo e falhas, com exportação JSON e limpeza.
- Corrigidas as 2 falhas do teste ModelCatalogTest da alpha.9.
- Sem coleta remota e sem processamento permanente em segundo plano.
- Verificar compilação Android e uso no dispositivo após publicar.

## Desempenho alpha.12
- Apenas um artefato APK: Release assinado quando configurado, Debug quando nao houver Secrets.
- 4096 tokens de contexto padrao (antes 8192) para reduzir a memoria KV; limite de geracao corrigido na engine nativa fixada.
- Log do JNI omite conteudo de prompts e varredura por token de entrada.
- Streaming de resposta e diagnostico com menos trabalho na thread principal.
- Alteracoes nao benchmarkadas no aparelho; usuarios com PDFs extensos devem observar o limite menor.

## Melhorias alpha.11
Carregamento de modelo no centro da tela com bloqueio de navegação, cancelamento cooperativo, retorno após carga manual, novos painéis e menus padronizados, lupa de busca, nome técnico completo nos detalhes e correção do logotipo.
