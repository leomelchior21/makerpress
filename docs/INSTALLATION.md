# Instalação e uso

Requer Android 8.0 ou superior. O aplicativo funciona offline; a internet só é usada quando um endereço de atualização está configurado.

1. Transfira `dist/MakerPress-v1.1.0-debug.apk` para o telefone, por USB, compartilhamento de arquivo ou outro meio de sua preferência.
2. Abra o APK no gerenciador de arquivos. Autorize a instalação por essa origem quando o Android solicitar e confirme.
3. Abra MakerPress. Nenhuma conta é necessária.
4. Ligue a impressora, coloque papel térmico na orientação correta e faça o pareamento nas configurações Bluetooth do Android. Use o PIN informado no manual/etiqueta da sua impressora; o aplicativo não presume um PIN.
5. Em **Impressora**, toque em **Encontrar impressoras pareadas**, conceda acesso a dispositivos próximos no Android 12+ e selecione a impressora.
6. Em **Printer Lab**, abra **HELLO MAKERPRESS**, confira a prévia e toque em **Imprimir**.
7. Confira a saída no papel. O status “Bytes enviados” não confirma que a impressora imprimiu. Registre a confirmação visual no laboratório somente após observar a saída.

Também é possível instalar por USB com `adb install -r dist/MakerPress-v1.1.0-debug.apk`.

## Criar

- **Text Studio:** escolha um modelo, escreva, ajuste fonte, alinhamento, negrito e separador. Modelos editados são preservados como documentos pelo botão Salvar.
- **Image Studio:** importe pelo seletor de fotos. A imagem é copiada para armazenamento privado, reduzida para até 1600 pontos no maior lado e orientada por EXIF. Zoom recorta, e os controles horizontal/vertical posicionam esse recorte. Ajuste brilho, contraste e conversão; a prévia ao vivo mostra o resultado térmico.
- **Sudoku:** escolha 4×4/9×9 e dificuldade. A geração verifica solução única. A classificação é uma heurística de remoção de pistas; a cobertura por singles é informada, sem promessa de classificação humana universal.
- **Word Search:** escolha uma das oito categorias offline em português/inglês ou substitua as palavras. Use vírgulas. Acentos são normalizados na grade; todas as palavras devem caber.
- **QR Studio:** use texto/URL, legenda e tamanho. Conteúdo denso demais é rejeitado para manter pelo menos dois pontos por módulo.
- **Math Challenges:** escolha aritmética, geometria, função afim, Bhaskara, física ou revisão mista. Alterne entre desenhar e interpretar gráficos. Use o gabarito separado para conferir os passos.

Todas as ferramentas abrem a mesma prévia. Salvar mantém a criação sem imprimir. Adicionar à fila agenda o trabalho, que começa quando você toca em Iniciar fila. Mostrar gabarito alterna para um documento separado com indicação explícita; nenhuma solução é acrescentada automaticamente às questões.

## Fila e histórico

Mantenha o app em primeiro plano durante a impressão. Ao sair do app, uma transmissão em andamento é interrompida e fica com resultado desconhecido quando puder ter enviado bytes. Confira o papel antes de enfileirar outra cópia. Nunca há repetição automática de um resultado incerto.

Histórico guarda conteúdo, sementes, puzzles efetivos e perfil usado na prévia. Trabalhos antigos continuam usando aquele perfil, garantindo a mesma largura; para usar um perfil diferente, crie uma nova versão do documento. Apagar histórico não remove imagens privadas automaticamente; isso evita quebrar referências compartilhadas.

## Atualizações no telefone

O publicador deve configurar a distribuição descrita em RELEASES.md. Em Ajustes, cole o endereço HTTPS público de `latest.json`, salve e verifique. O aplicativo consulta no máximo uma vez por dia ao abrir, além das consultas manuais.

Baixe, aguarde a validação e toque em Instalar. Se necessário, autorize MakerPress como origem de instalação, volte e toque novamente em Instalar. O instalador Android pede a confirmação final. Uma atualização com o mesmo pacote e a mesma chave preserva os dados. Não desinstale para atualizar.

O APK entregue para teste usa a chave debug local. Uma futura versão de produção assinada com outra chave não poderá atualizar essa instalação; será necessário desinstalar, perdendo os dados locais. Para a primeira instalação duradoura, configure a chave de produção antes de distribuir.

## Novas atividades

- **Golf / Galaxy:** gere uma prévia e imprima. O modo de novos níveis cria um jogo diferente por cópia e prepara a próxima prévia. Reimprimir pelo histórico preserva o nível salvo.
- **Kakooma:** circule a soma de dois outros números em cada grupo. Cada grupo tem exatamente uma equação válida; o gabarito mostra os operandos.
- **Connect the Dots:** escolha um desenho ou Surpresa; ligue os números em ordem e una o último ao primeiro. A dificuldade aumenta os pontos, e o gabarito revela a figura.
- **Idioma:** o botão PT-BR / EN no cabeçalho troca a interface e as novas atividades. A escolha fica salva.

Atualizações do GitHub: veja o endereço do manifesto e o passo a passo no [README](../README.md).
