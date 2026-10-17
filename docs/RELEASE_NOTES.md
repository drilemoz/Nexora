# Nexora 0.1.0-alpha.12

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
