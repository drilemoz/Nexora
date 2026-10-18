# Nexora: Chat Templates automáticos (alpha.22)

## Integração com o motor

A revisão do llama.cpp permanece `ea0c4c2c85fb183ec0fbd30a1af4e4275a18a064`. O motor original já chama `common_chat_templates_init(g_model, "")`, que obtém `llama_model_chat_template(model, nullptr)` dos metadados do GGUF. Não injetamos strings de prompts de Qwen, Gemma, Llama, Phi ou SmolLM no C++.

O patch `scripts/patch_llama_templates.py` é aplicado ao final do bootstrap, sobre o motor fixado, e falha se as âncoras não estiverem presentes. A verificação nativa simula **system → user → assistant → user** por Jinja, compara os prefixos exigidos pelo processamento incremental e tokeniza o prompt sintético com `parse_special=true`. Não executa decode nem gera saída. Nomes de família são meras **identificações por marcadores**, não uma escolha de template. `ChatML_Qwen_SmolLM` é deliberadamente ambíguo.

Quando há template GGUF que falha na verificação, os métodos nativos recusam os prompts com código 7 em vez de aplicar ChatML de outro modelo silenciosamente. Quando não há template no GGUF, a versão conserva o comportamento legado de texto simples e apresenta explicitamente **não aplicado / fallback**; não declara template ativo.

Tokens especiais já incluídos no prompt são reconhecidos com `parse_special=true`; `add_special=false` evita adicionar BOS duas vezes, com um único BOS inicial adicionado somente quando o vocabulário exige e o prompt ainda não o contém. Não alteramos o sampler, número de threads, tamanho do KV ou dados do histórico.

## Raciocínio, multimodalidade e limitações

O patch mantém intactos os marcadores de raciocínio do GGUF e a opção de modo do chat Qwen existente; **não comprova** a correção semântica do `<think>` nem o suporte a imagens/áudio, que requerem testes com pesos GGUF reais. O teste verifica apenas **texto**. Em modelos sem papel `system`, o verificador tenta uma segunda sonda apenas com `user/assistant` e marca `ok_sem_system` quando aprovada; eventuais chamadas de `setSystemPrompt` são bloqueadas, sem descartar conteúdo silenciosamente. Templates Jinja que exijam campos multimodais podem ser marcados incompatíveis de forma conservadora. Isso evita alegar compatibilidade fictícia. Não existe campo manual/Jinja arbitrário seguro neste motor integrado: opção **Personalizado (avançado)** fica desabilitada, com explicação. Cada GGUF é verificado independentemente quando carregado; não se reutiliza o estado do modelo anterior.

## Privacidade e consumo

Diagnóstico compartilha apenas nome do arquivo (sem caminho), classificação, origem e resultado de teste. Nunca exporta conteúdo do template, prompts, conversas, PDFs nem texto da sonda. Os eventos armazenam somente códigos `template.detect`, `template.apply`, `template.verify` com enums. Verificação ocorre no carregamento ou por toque, **não em loop**, e não aumenta threads/inferência.

## Validação recomendada no Android

Com Qwen3, Gemma 3, SmolLM3, Llama e Phi em arquivos GGUF próprios: abrir Configurações → Compatibilidade dos modelos → Verificar template e anotar origem/aplicação/compatibilidade. Comparar duas conversas, troca de modelo, 4+ turnos e geração com/sem raciocínio. Executar build NDK no GitHub Actions; não considerar o patch compilado ou a compatibilidade dessas famílias comprovada apenas pela verificação estática.
