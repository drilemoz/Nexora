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
