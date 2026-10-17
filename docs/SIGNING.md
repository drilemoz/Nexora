# Assinatura do Nexora

## Configuração dos Secrets
Importe o `.txt` privado fornecido separadamente via GitHub Manager > Repositório > Secrets > Importar, selecionando o repositório do Nexora.
São quatro: `KEYSTORE_BASE64` (PKCS12 em Base64 sem quebras), `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`.

*Sem Secrets*: gera **apenas APK Debug**, com certificado de debug do runner, que pode mudar de uma compilação para outra.
*Com os quatro Secrets*: gera **apenas APK Release**, com certificado fixo, permitindo atualizações futuras quando applicationId permanecer igual.

## Segurança
- **Não** publique o `.txt`, JSON, `.p12` nem cole a Base64 no código, GitHub, Issues ou chat público.
- Guarde um backup privado da keystore e senhas. Perder a chave impossibilita atualizar apps instalados com ela.
- Não inclua as credenciais como Artifacts no GitHub Actions nem imprima valores em logs.
- Os Secrets são usados pelo job apenas fora de eventos `pull_request`.
- APKs antigos de outra assinatura não podem ser atualizados diretamente sobre uma instalação existente.
- Antes de desinstalar um APK antigo, salve modelos originais GGUF e dados que desejar preservar. Este projeto ainda não oferece backup/restauração de histórico.

## Verificação
Em computador com Android Build Tools, rode `apksigner verify --print-certs arquivo.apk` para conferir o certificado instalado e o novo. O SHA-256 do certificado de referência é fornecido separadamente com os arquivos privados.

## Renomeação para Nexora
Desde alpha.6 o applicationId é `br.com.adriedson.nexora`. Reutilize os quatro Secrets da assinatura estável alpha.5. O novo pacote instala separadamente e não acessa o sandbox do app antigo. Não envie a keystore para o repositório.
