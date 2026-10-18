# Arquitetura

Kotlin 2.1.20, AGP 8.9.2, Gradle 8.11.1, Java 17, compile/target SDK 35, min SDK 26. Versões fixadas e mutuamente compatíveis; não são uma afirmação de que sejam as mais recentes.

- `domain`: modelos serializáveis e geradores puros. Sementes e puzzles efetivos são salvos.
- `core/bluetooth`: dispositivos já pareados e transporte RFCOMM/SPP. Somente BLUETOOTH_CONNECT no Android 12+, BLUETOOTH legado até 30; sem descoberta/localização.
- `core/rendering`: Canvas/StaticLayout, QR ZXing com módulos inteiros, processamento monocromático e importação privada. Limites de documento e amostragem de imagens limitam memória.
- `core/printing`: comandos ESC/POS, bandas raster de 128 linhas, transporte abstrato, chunking e fila serial.
- `data`: repositórios, Room com migrações explícitas e DataStore. Documentos tipados são codificados em JSON estruturado nas linhas Room; imagens ficam em arquivos privados.
- `AppViewModel` / `UpdateViewModel`: estado de apresentação e tarefas em coroutines. IO/renderização não bloqueiam a thread de UI.
- `MainActivity`: interface Compose Material 3, dez editores, prévia comum e quatro abas, PT-BR / EN e temas claro/escuro.

Fluxo: conteúdo → PrintDocument → Bitmap → monocromático → ESC/POS → PrinterTransport. A prévia e o envio usam o mesmo renderer e snapshot de PrinterProfile. História preserva conteúdo, não somente pixels.

Fila: salvo ou pendente → claim atômico → renderizando → enviando → bytes enviados. Falhas antes de escrever são falhas seguras; qualquer tentativa de write que falha pode ter enviado bytes e vira resultado desconhecido. Interrupções persistidas em SENDING são recuperadas como UNKNOWN; RENDERING vira FAILED. Pendentes nunca começam automaticamente no reinício. Uma única execução protegida por Mutex processa em ordem de ID; a primeira falha para a execução.

Impressão depende do primeiro plano. Ao sair da Activity (exceto rotação), o trabalho é cancelado. Não há serviço de impressão em background. Cada envio preserva o progresso no Room, mas porcentagem só mede bytes.

Sudoku usa um tabuleiro válido permutado, remoções com contagem MRV de até duas soluções e limites determinísticos de tentativas/nós (no máximo uma tentativa por célula, 20 mil nós por tentativa). Isso preserva reprodutibilidade sem depender da velocidade do aparelho. As metas de pistas dependem do nível. A cobertura por naked singles é informada, mas não governa a classificação; dificuldade humana precisa de calibração futura. O puzzle efetivo salvo é a fonte de verdade para reimprimir.

Caça-palavras usa tentativas limitadas, encaixe de palavras longas primeiro, sobreposição compatível, posições/direções registradas e preenchimento aleatório. O gabarito circunda exatamente as palavras registradas; pode haver ocorrências acidentais no preenchimento.

Updates usam somente HTTPS, consentimento de download e instalador Android. A URL é configurável, sem endpoints de produção fictícios. Dados de usuário não são enviados. Logs não incluem corpo de documentos nem endereço Bluetooth; a exportação exige ação do usuário.

O backup automático Android está desativado para não restaurar bancos sem os assets correspondentes. Atualizações normais preservam armazenamento privado; desinstalação/limpar dados remove conteúdo. O MVP não oferece exportação de biblioteca ou coleta automática de imagens órfãs.

Referências de compatibilidade: [AGP 8.9 e Gradle 8.11.1](https://developer.android.com/build/releases/agp-8-9-0-release-notes), [permissões Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) e [plugin do compilador Compose](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler).

## Activities added in 1.1.0

`PaperGameGenerator`, `StudyGenerator`, `KakoomaGenerator` and `DotGenerator` are seeded domain generators. New serialized PrintBlock variants are additive, preserving existing saved documents. `ActivityDrawing` renders vector diagrams and boards at native printer width; rendering never changes a seed. `AppSettings.language` has a backward-compatible PT-BR default, and the initial empty manifest address resolves to the public repository endpoint.

Game batches snapshot the preview before suspension, save each random copy as an individual job, then advance the editor seed. History requeues the stored document. Settings and database schemas need no destructive migrations.
