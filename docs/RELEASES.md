# Current channel: MakerPress 1.1.0

Repository: https://github.com/leomelchior21/makerpress
Manifest: https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json

Version 1.1.0 / code 2 continues the existing local debug/test channel. Keep using the same Android debug certificate from this workstation for compatible updates. A different computer may generate a different certificate, which Android will reject as an update. Never commit the signing key.

To publish the next local test update: increase versionName/versionCode in app/build.gradle.kts, update CHANGELOG, run the build checks, verify the APK with apksigner, then run scripts/release_manifest.py with the real APK path, repository, version and code. Commit and push the matching source, then run `python scripts/publish_test_release.py dist/latest.json`. The publisher uploads a draft before making all three assets public and never silently overwrites an existing release.

The production workflow below requires the owner's stable production key and repository secrets. Switching to a different certificate is not a compatible update for the test channel. Plan that migration explicitly. Production keys are not created or uploaded by the local publisher.

Production tag builds remain disabled until the repository variable `MAKERPRESS_PRODUCTION_RELEASES` is set to `true`. Manual production workflow runs still require all signing secrets. The ordinary Android checks run on every push to main and never publish an APK with a runner-generated debug key.

# Assinatura, publicação e atualização

O pacote fixo é `com.makerpress.app`. A versão inicial é 1.0.0/código 1. Nunca mude o applicationId nem gere uma chave nova por lançamento.

## Chave de produção: configuração única pelo proprietário

Nenhuma chave privada de produção foi criada. Com a aprovação e custódia do proprietário, execute `keytool -genkeypair -keystore <diretório-seguro-fora-do-repositório>/makerpress-release.jks -alias makerpress -keyalg RSA -keysize 4096 -validity 10000`. O keytool pedirá as senhas; não as coloque na linha de comando ou em logs.

Guarde duas cópias criptografadas em locais distintos, além das senhas em gerenciador de senhas. Registre alias e impressão digital pública do certificado. Teste a recuperação antes de distribuir. Perder a chave impede atualizar instalações existentes com este fluxo de distribuição.

Variáveis locais para compilar produção:

- `MAKERPRESS_KEYSTORE`: caminho absoluto fora do repositório.
- `MAKERPRESS_STORE_PASSWORD`, `MAKERPRESS_KEY_ALIAS`, `MAKERPRESS_KEY_PASSWORD`: credenciais dessa mesma chave.
- `VERSION_NAME` e `VERSION_CODE`: versão pública e inteiro estritamente crescente.

Execute `./gradlew testDebugUnitTest lintDebug assembleRelease`. A tarefa de release falha se as credenciais faltarem. Não publique `app-release-unsigned.apk`. Confirme com `apksigner verify --verbose <apk>`.

## GitHub Actions

1. Use o repositório `leomelchior21/makerpress`.
2. Configure os secrets `MAKERPRESS_KEYSTORE_BASE64` (keystore codificado em base64), `MAKERPRESS_STORE_PASSWORD`, `MAKERPRESS_KEY_ALIAS`, `MAKERPRESS_KEY_PASSWORD`.
3. Defina a variável de repositório `MAKERPRESS_VERSION_CODE` para o próximo inteiro antes de criar a tag, ou informe código e nome no disparo manual.
4. Atualize `docs/CHANGELOG.md` e crie a tag correspondente à próxima versão (superior a 1.1.0) O workflow usa a versão da tag (remove `v`) e o código configurado.
5. O workflow testa, executa lint, compila com a chave estável, verifica a assinatura e publica APK, checksum e `latest.json` com URL/hash/tamanho calculados do arquivo real.

O manifesto fica no endereço real `https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json` após a primeira publicação. Esse endereço já é o padrão no aplicativo 1.1.0. Configure esse endereço em Ajustes no telefone.

O repositório de distribuição deve ser público. Para código privado, publique os três arquivos verificados em um repositório público separado ou endpoint HTTPS independente, e gere a URL do manifesto para esse destino. Nunca coloque tokens privados dentro do APK. O workflow incluído publica no próprio repositório e precisa ser adaptado antes de usar fonte privada.

O script verifica que o código supera a última release pública; não edite releases antigas nem use códigos fora de ordem. O workflow serializa publicações concorrentes. Uma falha depois da criação parcial de uma release requer inspeção e correção explícita; o workflow não sobrescreve assets existentes silenciosamente.

## Contrato do manifesto

Campos obrigatórios: `versionName` string, `versionCode` inteiro, `minimumSdk` inteiro, `apkUrl` HTTPS, `sha256` com 64 dígitos hexadecimais, `sizeBytes` inteiro positivo (máximo 150 MB). `changelog` é uma lista de strings. `scripts/release_manifest.py` gera todos os valores reais após a compilação.

O app limita manifesto a 64 KB, valida tamanho declarado/recebido, SHA-256, pacote, versionCode/versionName, SDK e conjunto de certificados atuais via PackageManager. Não suporta rotação de chave: mantém a identidade estável. Android é a autoridade final para verificar a assinatura criptográfica do APK e a compatibilidade na instalação.

## Teste de atualização necessário antes de produção

Instale release código 1 com a chave estável. Salve impressora/perfil, imagem, texto, puzzles e trabalhos. Publique código 2 com a mesma chave, baixe e instale pelo app. Confirme todos os registros após reabrir. Teste internet indisponível, cancelamento, hash errado, tamanho errado, pacote diferente, assinatura diferente e falta de autorização para instalação. O teste real de atualização no telefone depende de chave/endpoint/dispositivo e não deve ser confundido com testes JVM.

Não use migração destrutiva do Room. Há migração explícita 1→2 para o campo de diagnóstico do trabalho. Mudanças futuras exigem nova versão, migração explícita e teste preservando dados.
