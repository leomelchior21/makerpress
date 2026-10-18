# Validação da KP-1025

**Compatibilidade física ainda não verificada.** Nenhuma impressora estava conectada ao ambiente de desenvolvimento. Testes com transporte falso validam software, não a impressão na KP-1025.

Hipóteses iniciais: papel 58 mm, área útil aproximada 48 mm, 384 pontos, Bluetooth Classic RFCOMM/SPP com UUID `00001101-0000-1000-8000-00805F9B34FB`, ESC/POS. Não há serviços BLE inventados nem protocolo proprietário presumido.

## Roteiro físico

1. Anote modelo, firmware (se disponível), telefone e Android.
2. Pareie pelo Android, selecione no MakerPress e conecte. Registre separadamente o resultado da conexão.
3. Imprima HELLO, incluindo a linha com acentos. A tipografia é rasterizada pelo Android.
4. Imprima o teste raster, a grade e a largura. Confirme que nenhuma coluna foi cortada.
5. Leia o QR de teste com outro aplicativo/celular.
6. Imprima uma imagem em cada modo: limiar, Floyd–Steinberg e Atkinson.
7. Imprima puzzles 4×4, 9×9, 10×10 e 12×12; confira legibilidade e gabaritos.
8. Execute duas cópias e vários trabalhos. Desligue a impressora no meio de um trabalho; confira resultado desconhecido e ausência de repetição automática.
9. Saia do aplicativo durante o envio e reabra; confira recuperação de histórico sem reenvio automático.
10. Exporte os logs e registre **resultado físico observado**; somente então confirme no laboratório.

## Diagnóstico

- Falha de conexão: verifique Bluetooth ligado, pareamento, permissão de dispositivos próximos, bateria e se outro app ocupa a conexão. O aplicativo usa SPP; uma impressora somente BLE exige outro transporte e especificação real do fabricante.
- Conecta e envia, mas nada sai: confira papel térmico, tampa, alimentação e comando. Troque `GS v 0` por `ESC *` e teste novamente.
- Imagem incompleta: reduza bytes por bloco (por exemplo, 128), aumente pausa (por exemplo, 80 ms) e teste uma página curta. Os valores são parâmetros de diagnóstico, não configurações certificadas.
- Imagem cortada: ajuste largura em múltiplos de oito. A largura escolhida é aplicada à prévia e ao documento novo.
- Papel com manchas/baixa legibilidade: ajuste brilho/contraste/dither e confira qualidade do papel.

O transporte não consulta sensores de papel, tampa ou temperatura. Uma conexão ociosa rompida pode ser percebida apenas no próximo envio. “Bytes enviados” significa que a escrita Bluetooth terminou; não é confirmação mecânica.
