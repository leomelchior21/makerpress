package com.makerpress.app

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import com.makerpress.app.domain.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.PT_BR }
@Composable fun ui(value: String) = UiStrings.translate(value, LocalAppLanguage.current)
@Composable fun UiText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
                      fontWeight: FontWeight? = null, style: TextStyle = androidx.compose.material3.LocalTextStyle.current) {
    androidx.compose.material3.Text(ui(text), modifier, color = color, fontWeight = fontWeight, style = style)
}
object UiStrings {
    private val translations = """
CONSTRUA UMA PONTE|BUILD A BRIDGE
Crie uma estrutura capaz de sustentar um pequeno peso.|Create a structure that can hold a small weight.
Materiais: papel, fita e barbante.|Materials: paper, tape and string.
NOME: __________________|NAME: __________________
DATA: __________________|DATE: __________________
ENTRADA Nº 001|TICKET No. 001
Evento: __________________|Event: __________________
Data: ____________________|Date: ____________________
NÃO ESQUECER|REMEMBER
Uma ideia ganha vida quando sai do papel.|An idea comes to life when it leaves the paper.
Descubra a soma escondida|Discover the hidden sum
Ponto a ponto, uma surpresa|A surprise, dot by dot
Connect the Dots|Connect the Dots
Grupos|Groups
Desenho|Drawing
Circule o único número que é a soma de dois outros. Cada grupo tem uma única solução verificada.|Circle the only number that is the sum of two others. Every group has one verified solution.
A dificuldade controla a quantidade de pontos. Ligue os números em ordem e feche o desenho. O gabarito revela a figura.|Difficulty controls the number of dots. Connect the numbers in order and close the drawing. The answer key reveals the picture.
Notas, bilhetes e missões|Notes, tickets and missions
Sua galeria em papel|Your gallery on paper
Um pequeno desafio lógico|A little logic challenge
Palavras para descobrir|Words to discover
Conecte papel e ideias|Connect paper and ideas
Matemática e física com diagramas|Math and physics with diagrams
Um novo campo em cada impressão|A new course with each print
Trace sua missão entre as estrelas|Chart your mission among the stars
Criar|Create
Fila|Queue
Impressora|Printer
Ajustes|Settings
Voltar|Back
Bluetooth conectado|Bluetooth connected
Conectando…|Connecting…
Impressora desconectada|Printer disconnected
Falha na conexão|Connection failed
Nova versão disponível →|New version available →
Uma ideia.|One idea.
Um pedacinho de papel.|A little piece of paper.
O que vamos criar hoje?|What shall we create today?
Última criação|Last creation
Abrir criação →|Open creation →
Impressão de teste|Test print
Dificuldade|Difficulty
Fácil|Easy
Médio|Medium
Difícil|Hard
Título (opcional)|Title (optional)
Modelo|Template
Texto simples|Plain text
Nota|Note
Ingresso|Ticket
Citação|Quote
Lembrete|Reminder
Seu texto|Your text
Tamanho|Size
Alinhamento|Alignment
Esquerda|Left
Centro|Center
Direita|Right
Negrito|Bold
Linha separadora|Divider
Espaço após texto: |Spacing after text:[space]
Importar da galeria|Import from gallery
Trocar imagem|Change image
Imagem importada e salva no aparelho.|Image imported and saved on this device.
Girar 90°|Rotate 90°
Zoom / recorte|Zoom / crop
Posição horizontal do recorte|Horizontal crop position
Posição vertical do recorte|Vertical crop position
Brilho|Brightness
Contraste|Contrast
Conversão|Conversion
Limiar|Threshold
Grade|Grid
Uma única solução. A dificuldade usa pistas e cobertura por singles; não representa uma classificação humana garantida.|One solution. Difficulty uses clues and single-candidate coverage; it is not a guaranteed human rating.
Palavras em inglês|English words
Tema|Theme
Animais|Animals
Espaço|Space
Tecnologia|Technology
Ciência|Science
Sustentabilidade|Sustainability
Esportes|Sports
Matemática|Mathematics
Programação|Programming
Palavras separadas por vírgula|Comma-separated words
Permitir diagonais|Allow diagonals
Permitir palavras invertidas|Allow reversed words
Texto ou URL|Text or URL
Legenda (opcional)|Caption (optional)
Tamanho máximo|Maximum size
Gerado offline, com módulos inteiros e margem branca para leitura.|Generated offline with whole modules and a white scanning margin.
Operações|Operations
Adição|Addition
Subtração|Subtraction
Multiplicação|Multiplication
Divisão|Division
Mistas|Mixed
Questões|Questions
Conteúdo|Content
Gráficos|Graphs
Ensino médio • BNCC. Questões com espaço de resolução e gabarito separado.|Brazilian secondary school • BNCC. Working space and a separate answer key.
Novo nível a cada impressão|New level with each print
A prévia é a primeira impressão. Cópias extras recebem novos níveis. O histórico permite repetir o mesmo nível.|The preview is the first print. Extra copies get new levels. History lets you reprint the same level.
Visualizar impressão|Preview print
GABARITO · impressão separada|ANSWER KEY · separate print
PRÉVIA TÉRMICA · |THERMAL PREVIEW ·[space]
 pontos| dots
Editar|Edit
Salvar|Save
Adicionar à fila|Add to queue
Novo desafio|New challenge
Gerar outro|Generate another
Mostrar questões|Show questions
Mostrar gabarito|Show answer key
Gerar novo|Generate new
Saída monocromática exata|Exact monochrome output
Mostrar questões|Show questions
Mostrar gabarito|Show answer key
Gerar novo|Generate new
Saída monocromática exata|Exact monochrome output
Gabarito|Answer key
Voltar ao desafio|Back to challenge
Cópias|Copies
Imprimir gabarito|Print answer key
Imprimir|Print
 gabarito| answer key
Selecionar impressora|Select printer
Fila & criações|Queue & creations
O envio funciona enquanto o aplicativo está aberto. Bytes enviados não confirmam a saída no papel.|Sending works while the app is open. Sent bytes do not confirm paper output.
Imprimindo…|Printing…
Iniciar fila|Start queue
Seu papel começa aqui|Your paper starts here
Salve uma criação ou adicione um documento à fila.|Save a creation or add a document to the queue.
 cópia(s) · | copy/copies ·[space]
Salvo|Saved
Pendente|Pending
Renderizando|Rendering
Enviando|Sending
Bytes enviados|Bytes sent
Falhou|Failed
Cancelado|Cancelled
Resultado desconhecido|Unknown result
Abrir|Open
Enfileirar cópia|Queue copy
Cancelar|Cancel
Excluir|Delete
Sua impressora|Your printer
Primeiro, pareie a KP-1025 nas configurações Bluetooth do Android. Não buscamos dispositivos nem solicitamos localização.|First pair the KP-1025 in Android Bluetooth settings. We do not scan devices or request location.
Abrir Bluetooth do Android|Open Android Bluetooth
Encontrar impressoras pareadas|Find paired printers
 · selecionada| · selected
Reconectar|Reconnect
Desconectar|Disconnect
KP-1025: 384 pontos e ESC/POS são hipóteses. Conectar → enviar bytes → conferir fisicamente o papel.|KP-1025: 384 dots and ESC/POS are assumptions. Connect → send bytes → inspect the paper.
Escala de cinza / raster|Grayscale / raster
QR de teste|Test QR
Grade de teste|Test grid
Largura imprimível|Printable width
Confirmar impressão vista no papel|Confirm visible print on paper
Exportar diagnóstico|Export diagnostics
Perfil de impressão|Print profile
Largura em pontos (múltiplo de 8)|Width in dots (multiple of 8)
Comando de imagem|Image command
Se conectar sem imprimir, experimente ESC *. Confira largura e energia. Não há confirmação de papel pelo protocolo.|If connected without printing, try ESC *. Check width and power. The protocol cannot confirm paper output.
Bytes por bloco · 64–4096|Bytes per chunk · 64–4096
Pausa entre blocos · 0–500 ms|Chunk delay · 0–500 ms
Avanço final: |Final feed:[space]
 linhas| lines
Margens: |Margins:[space]
Salvar perfil|Save profile
Calculando…|Calculating…
Limpar histórico?|Clear history?
Criações salvas e trabalhos concluídos serão removidos. Trabalhos pendentes permanecem.|Saved creations and completed jobs will be removed. Pending jobs remain.
Limpar|Clear
Aplicativo|Application
Sistema|System
Claro|Light
Escuro|Dark
Idioma|Language
Imagens e arquivos locais: |Local images and files:[space]
Limpar histórico|Clear history
Preferências de impressão|Print preferences
Cópias padrão|Default copies
Conversão padrão de imagem|Default image conversion
Prévia: resolução exata de impressão, sem redução de qualidade.|Preview: exact printing resolution with no quality reduction.
Impressora, largura, margens e Printer Lab →|Printer, width, margins and Printer Lab →
Instalar atualização?|Install update?
O Android pedirá sua confirmação. Seus dados serão preservados em uma atualização compatível.|Android will ask for confirmation. Compatible updates preserve your data.
Continuar|Continue
Depois|Later
Atualizações|Updates
Versão |Version[space]
 · código | · code[space]
 · teste/debug| · test/debug
Endereço HTTPS do manifesto|Manifest HTTPS address
O publicador fornece este endereço. Nenhum servidor é usado para criar ou imprimir.|The publisher provides this address. Creating and printing need no server.
Salvar e verificar atualizações|Save and check for updates
Nova versão: |New version:[space]
Cancelar download|Cancel download
Baixar atualização|Download update
Instalar atualização|Install update
Após autorizar a instalação por este aplicativo, volte e toque em Instalar novamente.|After allowing installation from this app, return and tap Install again.
Documento adicionado à fila.|Document added to the queue.
Criação salva no histórico.|Creation saved to history.
Conecte uma impressora na aba Impressora.|Connect a printer in the Printer tab.
Conecte uma impressora antes de iniciar a fila.|Connect a printer before starting the queue.
Nenhum aparelho pareado. Faça o pareamento nas configurações Bluetooth do Android.|No paired devices. Pair a device in Android Bluetooth settings.
Selecione uma impressora pareada.|Select a paired printer.
Digite o texto para imprimir.|Enter text to print.
Importe uma imagem primeiro.|Import an image first.
Você está na versão mais recente.|You have the latest version.
Atualização disponível: |Update available:[space]
Não foi possível verificar: |Could not check:[space]
. A impressão offline continua disponível.|. Offline printing remains available.
APK verificado. Pronto para instalar pelo Android.|APK verified. Ready for Android installation.
Download cancelado.|Download cancelled.
Atualização rejeitada: |Update rejected:[space]
""".trimIndent().lines().map { it.replace("[space]", " ") }.filter { it.contains('|') }.associate { it.substringBefore('|') to it.substringAfter('|') }
    fun translate(value: String, language: AppLanguage): String {
        if(language == AppLanguage.PT_BR) return value
        translations[value]?.let { return it }
        if(value.contains('\n')) return value.lines().joinToString("\n") { translate(it, language) }
        // Only sentence fragments explicitly intended for dynamic UI labels are substituted.
        var result = value
        translations.filterKeys { it.endsWith(" ") || it.startsWith(" ") || it in listOf("Uma ideia.", "Um pedacinho de papel.") }
            .entries.sortedByDescending { it.key.length }.forEach { (pt,en) -> result = result.replace(pt,en) }
        if(result != value) translations.entries.filter { !it.key.contains(' ') }.forEach { (pt,en) ->
            if(result.endsWith(pt)) result = result.dropLast(pt.length) + en
        }
        return result
    }
}
