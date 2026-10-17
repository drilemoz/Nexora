# Nexora • análise dos registros alpha.10 • 17/10/2026

Fonte: `Nexora-diagnostico-1792213839983.json`, Android SDK 33, Xiaomi 21091116UG, 8 processadores lógicos. Relatório original fornecido pelo usuário, não incluído aqui para evitar duplicar registros de dispositivo.

- Carregamentos registrados como prontos em aproximadamente 11,7 s a 19,8 s (Qwen3-4B-Q4_K_M.gguf).
- Diversos `app.start` e `model.load.start` sem `model.load.ready` subsequente no relatório. O motivo não é identificado por esse recorte; hipóteses incluem término do processo, falha ou interrupção.
- A extração do PDF retornou `pages=2 chars=2464 truncated=false`. A inferência iniciou e recebeu `inference.cancel` após ~58 s. A alpha.10 não registrava o motivo do cancelamento.
- Amostras observadas: PSS ~3837 a 3878 MiB, RAM disponível ~835 a 988 MiB, pressão térmica `severa`. Não são prova de OOM nem temperatura da CPU.

## Estratégia da alpha.11
- Separar job de carga do ciclo de vida da Activity; bloquear outras interações com modal enquanto carrega.
- Cancelamento cooperativo com limpeza somente após o motor retornar, e marcador de tentativa automática interrompida preservado contra encerramento de processo.
- Registrar em próximos diagnósticos `inference.cancel` com causa identificável (`user` ou `app_background`), em vez de inferir a causa retroativamente.
- Continuar medindo fragmentos/s, sem classificá-los indevidamente como tok/s nativos.

## Testes Android pendentes
1. Carregar, cancelar e tentar carregar novamente o mesmo GGUF, sem vazamento de contexto.
2. Recriar Activity/rotacionar a tela durante a carga; modal deve sobreviver por estado compartilhado.
3. Abrir e fechar outra conversa com e sem modelo selecionado; nenhuma conversa vazia nova deve surgir.
4. Anexar PDF e sair voluntariamente da tela para confirmar registro da causa da interrupção.
5. Executar Qwen3-4B por 10 min registrando PSS, RAM livre, pressão térmica e fragmentos/s.
6. Verificar aparência em temas claro, escuro e AMOLED.
