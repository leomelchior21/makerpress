package com.makerpress.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makerpress.app.core.printing.PrinterConnectionState
import com.makerpress.app.core.updates.UpdateViewModel
import com.makerpress.app.data.PrintJob
import com.makerpress.app.domain.*
import com.makerpress.app.domain.Alignment as TextAlignment
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var vm: AppViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent {
            val model: AppViewModel = viewModel(); vm = model
            val updates: UpdateViewModel = viewModel()
            MakerPress(model, updates)
        }
    }
    override fun onStop() { super.onStop(); if (!isChangingConfigurations) vm?.pausePrinting() }
}

private val Ink = Color(0xff181818)
private val Lime = Color(0xffc6f36b)
private val tools = listOf(
    Triple("Text Studio", "Notas, bilhetes e missões", Icons.Outlined.TextFields),
    Triple("Image Studio", "Sua galeria em papel", Icons.Outlined.Image),
    Triple("Sudoku", "Um pequeno desafio lógico", Icons.Outlined.GridOn),
    Triple("Word Search", "Palavras para descobrir", Icons.Outlined.Search),
    Triple("QR Studio", "Conecte papel e ideias", Icons.Outlined.QrCode2),
    Triple("Math Challenges", "Matemática e física com diagramas", Icons.Outlined.Calculate),
    Triple("Golf", "Um novo campo em cada impressão", Icons.Outlined.GridOn),
    Triple("Galaxy", "Trace sua missão entre as estrelas", Icons.Outlined.GridOn),
    Triple("Kakooma", "Descubra a soma escondida", Icons.Outlined.Calculate),
    Triple("Connect the Dots", "Ponto a ponto, uma surpresa", Icons.Outlined.Image)
)
@Composable private fun MakerPress(vm: AppViewModel, updates: UpdateViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val dark = settings.theme == "dark" || (settings.theme == "system" && isSystemInDarkTheme())
    val colors = if (dark) darkColorScheme(primary = Lime, onPrimary = Ink, background = Color(0xff151713), surface = Color(0xff22251f), onSurface = Color(0xfff1f3e9), secondaryContainer = Color(0xff39462c))
        else lightColorScheme(primary = Ink, onPrimary = Lime, background = Color(0xfff6f5f0), surface = Color.White, onSurface = Ink, secondaryContainer = Lime, onSecondaryContainer = Ink, outlineVariant = Color(0xffe5e5e0))
    CompositionLocalProvider(LocalAppLanguage provides settings.language) {
    MaterialTheme(colorScheme = colors, shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))) {
        val snack = remember { SnackbarHostState() }
        LaunchedEffect(vm.message) { vm.message?.let { snack.showSnackbar(UiStrings.translate(it, settings.language)); vm.message = null } }
        val back: () -> Unit = { if (vm.selectingPrinter) vm.selectingPrinter = false else if (vm.preview != null) { vm.preview = null; vm.bitmap = null } else vm.editor = null }
        BackHandler(vm.preview != null || vm.editor != null) { back() }
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vm.preview != null || vm.editor != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("Voltar")) }
                        Column(Modifier.weight(1f)) {
                            UiText(vm.preview?.title ?: vm.editor?.tool ?: "MakerPress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            if (vm.preview == null && vm.editor == null) UiText("Create. Print. Play.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { vm.changeLanguage(if(settings.language == AppLanguage.PT_BR) AppLanguage.EN else AppLanguage.PT_BR) }) { UiText(if(settings.language == AppLanguage.PT_BR) "PT-BR / EN" else "EN / PT-BR", style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { vm.preview = null; vm.editor = null; vm.tab = 2 }) { Icon(Icons.Outlined.Print, "Abrir impressora") }
                    }
                }
            },
            bottomBar = {
                if (vm.preview == null && vm.editor == null) NavigationBar {
                    listOf("Criar" to Icons.Outlined.AddCircleOutline, "Fila" to Icons.Outlined.ReceiptLong, "Impressora" to Icons.Outlined.Print, "Ajustes" to Icons.Outlined.Tune).forEachIndexed { i, item ->
                        NavigationBarItem(selected = vm.tab == i, onClick = { vm.tab = i }, icon = { Icon(item.second, item.first) }, label = { UiText(item.first) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                when {
                    vm.selectingPrinter -> PrinterScreen(vm, settings, connection)
                    vm.preview != null -> PreviewScreen(vm, connection)
                    vm.editor != null -> EditorScreen(vm)
                    else -> when (vm.tab) {
                        0 -> HomeScreen(vm, jobs, connection, updates.release != null)
                        1 -> QueueScreen(vm, jobs)
                        2 -> PrinterScreen(vm, settings, connection)
                        else -> SettingsScreen(vm, settings, updates)
                    }
                }
            }
        }
    }
}
}
@Composable private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { UiText(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); content() }
    }
}
private fun connectionText(state: PrinterConnectionState) = when (state) {
    is PrinterConnectionState.Connected -> "Bluetooth conectado"
    PrinterConnectionState.Connecting -> "Conectando…"
    PrinterConnectionState.Disconnected -> "Impressora desconectada"
    is PrinterConnectionState.Error -> "Falha na conexão"
}
@Composable private fun HomeScreen(vm: AppViewModel, jobs: List<PrintJob>, connection: PrinterConnectionState, update: Boolean) = Page {
    AssistChip(onClick = { vm.tab = 2 }, label = { UiText(connectionText(connection)) }, leadingIcon = { Icon(Icons.Outlined.Bluetooth, null, Modifier.size(18.dp)) })
    if (update) Card(onClick = { vm.tab = 3 }) { UiText("Nova versão disponível →", Modifier.padding(16.dp)) }
    UiText("Uma ideia.\nUm pedacinho de papel.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    UiText("O que vamos criar hoje?", color = MaterialTheme.colorScheme.onSurfaceVariant)
    tools.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (name, subtitle, icon) ->
                Card(onClick = { vm.openTool(name) }, modifier = Modifier.weight(1f).heightIn(min = 168.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Lime) { Icon(icon, null, Modifier.padding(9.dp).size(24.dp), tint = Ink) }
                        UiText(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        UiText(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    jobs.firstOrNull()?.let { job -> Section("Última criação") { UiText(job.title); TextButton(onClick = { vm.openJob(job) }) { UiText("Abrir criação →") } } }
    OutlinedButton(onClick = { vm.lab("HELLO") }, modifier = Modifier.fillMaxWidth()) { UiText("Impressão de teste") }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun <T> Choices(label: String, values: List<T>, selected: T, name: (T) -> String = { it.toString() }, choose: (T) -> Unit) {
    Column { UiText(label, style = MaterialTheme.typography.labelLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { value -> FilterChip(selected == value, { choose(value) }, label = { UiText(name(value)) }) } } }
}
@Composable private fun Field(label: String, value: String, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { UiText(label) }, minLines = lines, maxLines = if (lines > 1) 12 else 1, singleLine = lines == 1)
}
@Composable private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { UiText(label, Modifier.weight(1f)); Switch(checked, change) }
}
@Composable private fun Range(label: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    Column { UiText(label, style = MaterialTheme.typography.labelLarge); Slider(value, change, valueRange = range) }
}
@Composable private fun DifficultyPicker(e: EditorState, change: (EditorState) -> Unit) {
    Choices("Dificuldade", Difficulty.entries, e.difficulty, { listOf("Fácil", "Médio", "Difícil")[it.ordinal] }) { change(e.copy(difficulty = it)) }
}
@Composable private fun EditorScreen(vm: AppViewModel) {
    val e = vm.editor ?: return
    val change: (EditorState) -> Unit = { vm.editor = it }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.importImage(uri) }
    Page {
        Field("Título (opcional)", e.title) { change(e.copy(title = it.take(120))) }
        when (e.tool) {
            "Text Studio" -> {
                var template by remember { mutableStateOf("Texto simples") }
                Choices("Modelo", listOf("Texto simples", "Nota", "Ingresso", "Citação", "Lembrete", "Maker Mission"), template) {
                    template = it
                    change(e.copy(title = if (it == "Maker Mission") "MISSION 014" else UiStrings.translate(it, e.language), body = UiStrings.translate(when (it) {
                        "Maker Mission" -> "CONSTRUA UMA PONTE\n\nCrie uma estrutura capaz de sustentar um pequeno peso.\n\nMateriais: papel, fita e barbante.\n\nNOME: __________________\n\nDATA: __________________"
                        "Ingresso" -> "ENTRADA Nº 001\n\nEvento: __________________\nData: ____________________"
                        "Lembrete" -> "NÃO ESQUECER\n\n☐ ______________________\n☐ ______________________"
                        "Citação" -> "Uma ideia ganha vida quando sai do papel."
                        else -> ""
                    }, e.language), separator = it != "Texto simples"))
                }
                Field("Seu texto", e.body, 6) { change(e.copy(body = it.take(15000))) }
                Choices("Tamanho", listOf(18f, 24f, 30f, 36f), e.font, { "${it.toInt()} px" }) { change(e.copy(font = it)) }
                Choices("Alinhamento", TextAlignment.entries, e.align, { listOf("Esquerda", "Centro", "Direita")[it.ordinal] }) { change(e.copy(align = it)) }
                Toggle("Negrito", e.bold) { change(e.copy(bold = it)) }
                Toggle("Linha separadora", e.separator) { change(e.copy(separator = it)) }
                Range("Espaço após texto: ${e.spacing} px", e.spacing.toFloat(), 0f..40f) { change(e.copy(spacing = it.toInt())) }
            }
            "Image Studio" -> {
                Button(onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { UiText(if (e.asset.isEmpty()) "Importar da galeria" else "Trocar imagem") }
                if (e.asset.isNotEmpty()) {
                    UiText("Imagem importada e salva no aparelho.")
                    OutlinedButton(onClick = { change(e.copy(rotation = (e.rotation + 90) % 360)) }) { UiText("Girar 90° · ${e.rotation}°") }
                    Range("Zoom / recorte", e.zoom, 1f..4f) { change(e.copy(zoom = it)) }
                    Range("Posição horizontal do recorte", e.cropX, 0f..1f) { change(e.copy(cropX = it)) }
                    Range("Posição vertical do recorte", e.cropY, 0f..1f) { change(e.copy(cropY = it)) }
                    Range("Brilho", e.brightness, -100f..100f) { change(e.copy(brightness = it)) }
                    Range("Contraste", e.contrast, .5f..2.5f) { change(e.copy(contrast = it)) }
                    Choices("Conversão", Dither.entries, e.dither, { listOf("Limiar", "Floyd–Steinberg", "Atkinson")[it.ordinal] }) { change(e.copy(dither = it)) }
                    LiveImagePreview(vm, e)
                }
            }
            "Sudoku" -> {
                Choices("Grade", listOf(4, 9), e.size, { "$it × $it" }) { change(e.copy(size = it)) }
                DifficultyPicker(e, change)
                UiText("Uma única solução. A dificuldade usa pistas e cobertura por singles; não representa uma classificação humana garantida.", style = MaterialTheme.typography.bodySmall)
            }
            "Word Search" -> {
                Toggle("Palavras em inglês", e.english) {
                    val index = WordSearchGenerator.categories.keys.indexOf(e.category).coerceAtLeast(0)
                    change(e.copy(english = it, words = if (it) WordSearchGenerator.english[index] else WordSearchGenerator.categories.values.elementAt(index)))
                }
                Choices("Tema", WordSearchGenerator.categories.keys.toList(), e.category) {
                    val index = WordSearchGenerator.categories.keys.indexOf(it)
                    change(e.copy(category = it, words = if (e.english) WordSearchGenerator.english[index] else WordSearchGenerator.categories.getValue(it)))
                }
                Field("Palavras separadas por vírgula", e.words, 4) { change(e.copy(words = it.take(1000))) }
                Choices("Grade", listOf(10, 12), e.size, { "$it × $it" }) { change(e.copy(size = it)) }
                Choices("Dificuldade", Difficulty.entries, e.difficulty, { listOf("Fácil", "Médio", "Difícil")[it.ordinal] }) { change(e.copy(difficulty = it, diagonal = it != Difficulty.EASY, reverse = it == Difficulty.HARD)) }
                Toggle("Permitir diagonais", e.diagonal) { change(e.copy(diagonal = it)) }
                Toggle("Permitir palavras invertidas", e.reverse) { change(e.copy(reverse = it)) }
            }
            "QR Studio" -> {
                Field("Texto ou URL", e.body, 3) { change(e.copy(body = it.take(1800))) }
                Field("Legenda (opcional)", e.caption) { change(e.copy(caption = it.take(300))) }
                Choices("Tamanho máximo", listOf(192, 256, 320, 360), e.qrSize, { "$it px" }) { change(e.copy(qrSize = it)) }
                UiText("Gerado offline, com módulos inteiros e margem branca para leitura.", style = MaterialTheme.typography.bodySmall)
            }
            "Kakooma" -> {
                DifficultyPicker(e, change)
                Choices("Grupos", listOf(5,10,15), e.count) { change(e.copy(count = it)) }
                UiText("Circule o único número que é a soma de dois outros. Cada grupo tem uma única solução verificada.")
            }
            "Connect the Dots" -> {
                Choices("Desenho", DotSubject.entries, e.dotSubject, { it.label(e.language) }) { change(e.copy(dotSubject = it)) }
                DifficultyPicker(e, change)
                UiText("A dificuldade controla a quantidade de pontos. Ligue os números em ordem e feche o desenho. O gabarito revela a figura.")
            }
            "Golf", "Galaxy" -> {
                DifficultyPicker(e, change)
                Toggle("Novo nível a cada impressão", e.freshGames) { change(e.copy(freshGames = it)) }
                UiText("A prévia é a primeira impressão. Cópias extras recebem novos níveis. O histórico permite repetir o mesmo nível.")
                UiText(PaperGameGenerator.rules(if(e.tool == "Golf") PaperGame.GOLF else PaperGame.GALAXY, e.language))
            }
            "Math Challenges" -> {
                Choices("Conteúdo", StudyTopic.entries, e.topic, { it.label(e.language) }) { change(e.copy(topic = it)) }
                if(e.topic in listOf(StudyTopic.LINEAR, StudyTopic.QUADRATIC, StudyTopic.MIXED)) Choices("Gráficos", GraphMode.entries, e.graphMode, { it.label(e.language) }) { change(e.copy(graphMode = it)) }
                UiText("Ensino médio • BNCC. Questões com espaço de resolução e gabarito separado.")
                if(e.topic in listOf(StudyTopic.ARITHMETIC, StudyTopic.MIXED)) Choices("Operações", Operation.entries, e.operation, { listOf("Adição", "Subtração", "Multiplicação", "Divisão", "Mistas")[it.ordinal] }) { change(e.copy(operation = it)) }
                DifficultyPicker(e, change)
                Choices("Questões", listOf(5, 10, 15), e.count) { change(e.copy(count = it)) }
            }
        }
        Button(onClick = { vm.generate() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Outlined.Visibility, null); Spacer(Modifier.width(8.dp)); UiText("Visualizar impressão") }
    }
}
@Composable private fun LiveImagePreview(vm: AppViewModel, e: EditorState) {
    var image by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(e) {
        kotlinx.coroutines.delay(200)
        try {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                vm.app.renderer.render(PrintDocument("Imagem", listOf(PrintBlock.Image(e.asset, e.rotation, e.zoom, e.cropX, e.cropY, e.brightness, e.contrast, e.dither))), vm.settings.value.profile)
            }
            image = result; error = ""
        } catch (ex: Exception) { if (ex is kotlinx.coroutines.CancellationException) throw ex; error = ex.message ?: "Falha na imagem." }
    }
    image?.let { Image(it.asImageBitmap(), "Prévia térmica ao vivo", Modifier.fillMaxWidth().heightIn(max = 400.dp).background(Color.White)) }
    if (error.isNotEmpty()) UiText(error, color = MaterialTheme.colorScheme.error)
}
@Composable private fun PreviewScreen(vm: AppViewModel, connection: PrinterConnectionState) {
    val doc = vm.preview ?: return
    val hasAnswers = doc.blocks.any { it is PrintBlock.Sudoku || it is PrintBlock.WordSearch || it is PrintBlock.Math || it is PrintBlock.Study || it is PrintBlock.Kakooma || it is PrintBlock.Dots }
    val isAnswer = doc.blocks.any { (it is PrintBlock.Sudoku && it.answers) || (it is PrintBlock.WordSearch && it.answers) || (it is PrintBlock.Math && it.answers) || (it is PrintBlock.Study && it.answers) || (it is PrintBlock.Kakooma && it.answers) || (it is PrintBlock.Dots && it.answers) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UiText(if (isAnswer) "GABARITO · impressão separada" else "PRÉVIA TÉRMICA · ${vm.previewProfile.width} pontos", style = MaterialTheme.typography.labelMedium)
            vm.bitmap?.let { Image(it.asImageBitmap(), ui("Saída monocromática exata"), Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height).background(Color.White)) }
            if (hasAnswers) OutlinedButton(onClick = { vm.answerKey() }) { UiText(if (isAnswer) "Mostrar questões" else "Mostrar gabarito") }
            if (doc.blocks.none { it is PrintBlock.Pattern }) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.editCurrent() }) { UiText("Editar") }
                if ((hasAnswers || doc.blocks.any { it is PrintBlock.Game }) && vm.editor != null) TextButton(onClick = { vm.generate(true) }) { UiText("Gerar novo") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.save(false) }, enabled = !vm.busy) { UiText("Salvar") }
                OutlinedButton(onClick = { vm.save(true) }, enabled = !vm.busy) { UiText("Adicionar à fila") }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UiText("Cópias", Modifier.weight(1f)); IconButton(onClick = { vm.copies = (vm.copies - 1).coerceAtLeast(1) }) { UiText("−") }; UiText(vm.copies.toString()); IconButton(onClick = { vm.copies = (vm.copies + 1).coerceAtMost(20) }) { UiText("+") }
                }
                UiText(connectionText(connection), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { if (connection is PrinterConnectionState.Connected) vm.save(true, true) else vm.selectingPrinter = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    UiText(if (connection is PrinterConnectionState.Connected) "Imprimir${if (isAnswer) " gabarito" else ""}" else "Selecionar impressora")
                }
            }
        }
    }
}
private fun statusName(state: String) = mapOf("SAVED" to "Salvo", "PENDING" to "Pendente", "RENDERING" to "Renderizando", "SENDING" to "Enviando", "SENT" to "Bytes enviados", "FAILED" to "Falhou", "CANCELLED" to "Cancelado", "UNKNOWN" to "Resultado desconhecido")[state] ?: state
@Composable private fun QueueScreen(vm: AppViewModel, jobs: List<PrintJob>) {
    val running by vm.app.queue.running.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item {
    UiText("Fila & criações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    UiText("O envio funciona enquanto o aplicativo está aberto. Bytes enviados não confirmam a saída no papel.")
    Button(onClick = { vm.runQueue() }, enabled = !running) { UiText(if (running) "Imprimindo…" else "Iniciar fila") }
    if (jobs.isEmpty()) Section("Seu papel começa aqui") { UiText("Salve uma criação ou adicione um documento à fila.") }
    }
    items(jobs, key = { it.id }) { job ->
        Section(job.title) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JobThumbnail(vm, job)
                Column { UiText("${job.copies} cópia(s) · ${statusName(job.state)}", fontWeight = FontWeight.Bold); UiText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(job.createdAt)), style = MaterialTheme.typography.bodySmall) }
            }
            if (job.state == "RENDERING") LinearProgressIndicator(Modifier.fillMaxWidth())
            if (job.state == "SENDING") { LinearProgressIndicator(progress = { job.progress / 100f }, modifier = Modifier.fillMaxWidth()); UiText("${job.progress}%") }
            if (job.message.isNotBlank()) UiText(job.message, style = MaterialTheme.typography.bodySmall)
            Row { TextButton(onClick = { vm.openJob(job) }) { UiText("Abrir") }; if (job.state !in listOf("SENDING", "RENDERING", "PENDING")) TextButton(onClick = { vm.requeue(job) }) { UiText("Enfileirar cópia") } }
            Row { if (job.state == "PENDING") TextButton(onClick = { vm.cancel(job) }) { UiText("Cancelar") }; if (job.state !in listOf("SENDING", "RENDERING")) TextButton(onClick = { vm.delete(job) }) { UiText("Excluir") } }
        }
    }
    }
}
@Composable private fun JobThumbnail(vm: AppViewModel, job: PrintJob) {
    var thumb by remember(job.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(job.id) { thumb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { val full = vm.app.renderer.render(vm.app.jobs.document(job), vm.app.jobs.profile(job)); android.graphics.Bitmap.createScaledBitmap(full, 72, (full.height.toLong() * 72 / full.width).toInt().coerceAtLeast(1), false).also { if (it != full) full.recycle() } }.getOrNull() } }
    thumb?.let { Image(it.asImageBitmap(), null, Modifier.size(48.dp, 64.dp).background(Color.White)) }
}
@Composable private fun PrinterScreen(vm: AppViewModel, settings: AppSettings, connection: PrinterConnectionState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) vm.devices() else vm.message = "Permita acesso a dispositivos próximos para usar a impressora Bluetooth." }
    Page {
        Section("Sua impressora") {
            UiText(connectionText(connection), fontWeight = FontWeight.Bold)
            if (connection is PrinterConnectionState.Error) UiText(connection.message)
            UiText("Primeiro, pareie a KP-1025 nas configurações Bluetooth do Android. Não buscamos dispositivos nem solicitamos localização.")
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) { UiText("Abrir Bluetooth do Android") }
            Button(onClick = { if (Build.VERSION.SDK_INT >= 31) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else vm.devices() }) { UiText("Encontrar impressoras pareadas") }
            vm.paired.forEach { p -> OutlinedButton(onClick = { vm.connect(p.address) }, enabled = connection !is PrinterConnectionState.Connecting) { UiText("${p.name}${if (settings.printerAddress == p.address) " · selecionada" else ""}\n${p.address}") } }
            Row {
                TextButton(onClick = { vm.connect() }, enabled = settings.printerAddress.isNotBlank() && connection !is PrinterConnectionState.Connecting) { UiText("Reconectar") }
                TextButton(onClick = { vm.disconnect() }) { UiText("Desconectar") }
            }
        }
        Section("Printer Lab") {
            UiText("KP-1025: 384 pontos e ESC/POS são hipóteses. Conectar → enviar bytes → conferir fisicamente o papel.")
            listOf("HELLO" to "HELLO MAKERPRESS", "RASTER" to "Escala de cinza / raster", "QR" to "QR de teste", "GRID" to "Grade de teste", "WIDTH" to "Largura imprimível").forEach { (kind, label) -> OutlinedButton(onClick = { vm.lab(kind) }, modifier = Modifier.fillMaxWidth()) { UiText(label) } }
            TextButton(onClick = { vm.app.printer.log("Usuário confirmou visualmente uma impressão no papel."); vm.message = "Confirmação visual registrada pelo usuário." }) { UiText("Confirmar impressão vista no papel") }
            TextButton(onClick = { exportLogs(context, vm) }) { UiText("Exportar diagnóstico") }
        }
        PrinterProfileEditor(vm, settings)
    }
}
@Composable private fun PrinterProfileEditor(vm: AppViewModel, settings: AppSettings) {
    var width by remember(settings.profile) { mutableStateOf(settings.profile.width.toString()) }
    var chunk by remember(settings.profile) { mutableStateOf(settings.profile.chunkSize.toString()) }
    var delay by remember(settings.profile) { mutableStateOf(settings.profile.delayMs.toString()) }
    var mode by remember(settings.profile) { mutableStateOf(settings.profile.mode) }
    var feed by remember(settings.profile) { mutableFloatStateOf(settings.profile.feed.toFloat()) }
    var margin by remember(settings.profile) { mutableFloatStateOf(settings.profile.margin.toFloat()) }
    Section("Perfil de impressão") {
        Field("Largura em pontos (múltiplo de 8)", width) { width = it }
        Choices("Comando de imagem", RasterMode.entries, mode, { if (it == RasterMode.GS_V_0) "GS v 0" else "ESC *" }) { mode = it }
        UiText("Se conectar sem imprimir, experimente ESC *. Confira largura e energia. Não há confirmação de papel pelo protocolo.", style = MaterialTheme.typography.bodySmall)
        Field("Bytes por bloco · 64–4096", chunk) { chunk = it }
        Field("Pausa entre blocos · 0–500 ms", delay) { delay = it }
        Range("Avanço final: ${feed.toInt()} linhas", feed, 0f..10f) { feed = it }
        Range("Margens: ${margin.toInt()} pontos", margin, 0f..32f) { margin = it }
        Button(onClick = {
            val p = PrinterProfile(width.toIntOrNull() ?: 0, chunk.toIntOrNull() ?: 0, delay.toLongOrNull() ?: -1, feed.toInt(), margin.toInt(), mode)
            vm.updateSettings { p.validate(); it.copy(profile = p) }
        }) { UiText("Salvar perfil") }
    }
}
private fun exportLogs(context: android.content.Context, vm: AppViewModel) {
    runCatching {
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val file = File(dir, "makerpress-diagnostics.txt")
        file.writeText("MakerPress ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nAndroid ${Build.VERSION.SDK_INT}\nPerfil: ${vm.settings.value.profile}\nKP-1025: compatibilidade não certificada.\n" + vm.app.printer.logs.joinToString("\n"))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Exportar diagnóstico"))
    }.onFailure { vm.message = "Não foi possível exportar o diagnóstico." }
}
@Composable private fun SettingsScreen(vm: AppViewModel, settings: AppSettings, updates: UpdateViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var clear by remember { mutableStateOf(false) }
    var storage by remember { mutableStateOf("Calculando…") }
    LaunchedEffect(Unit) { storage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { "%.1f MB".format(context.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1048576.0) } }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { UiText("Limpar histórico?") }, text = { UiText("Criações salvas e trabalhos concluídos serão removidos. Trabalhos pendentes permanecem.") }, confirmButton = { TextButton(onClick = { vm.task { vm.app.jobs.dao.clearHistory() }; clear = false }) { UiText("Limpar") } }, dismissButton = { TextButton(onClick = { clear = false }) { UiText("Voltar") } })
    Page {
        Section("Aplicativo") {
            Choices("Tema", listOf("system", "light", "dark"), settings.theme, { mapOf("system" to "Sistema", "light" to "Claro", "dark" to "Escuro").getValue(it) }) { value -> vm.updateSettings { it.copy(theme = value) } }
            UiText("Idioma: Português (Brasil)")
            UiText("Imagens e arquivos locais: $storage")
            TextButton(onClick = { clear = true }) { UiText("Limpar histórico") }
            TextButton(onClick = { exportLogs(context, vm) }) { UiText("Exportar diagnóstico") }
        }
        Section("Preferências de impressão") {
            Choices("Cópias padrão", listOf(1, 2, 3, 5), settings.copies) { value -> vm.updateSettings { it.copy(copies = value) } }
            Choices("Conversão padrão de imagem", Dither.entries, settings.dither, { listOf("Limiar", "Floyd–Steinberg", "Atkinson")[it.ordinal] }) { value -> vm.updateSettings { it.copy(dither = value) } }
            UiText("Prévia: resolução exata de impressão, sem redução de qualidade.")
            TextButton(onClick = { vm.tab = 2 }) { UiText("Impressora, largura, margens e Printer Lab →") }
        }
        UpdateScreen(vm, updates, settings)
    }
}
@Composable private fun UpdateScreen(vm: AppViewModel, updates: UpdateViewModel, settings: AppSettings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember(settings.manifestUrl) { mutableStateOf(settings.manifestUrl) }
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { UiText("Instalar atualização?") }, text = { UiText("O Android pedirá sua confirmação. Seus dados serão preservados em uma atualização compatível.") }, confirmButton = { TextButton(onClick = {
        confirm = false
        if (!context.packageManager.canRequestPackageInstalls()) context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${context.packageName}")))
        else scope.launch { try { context.startActivity(updates.verifiedInstallIntent()) } catch (e: Exception) { vm.message = e.message } }
    }) { UiText("Continuar") } }, dismissButton = { TextButton(onClick = { confirm = false }) { UiText("Depois") } })
    Section("Atualizações") {
        UiText("Versão ${BuildConfig.VERSION_NAME} · código ${BuildConfig.VERSION_CODE}${if (BuildConfig.DEBUG) " · teste/debug" else ""}", fontWeight = FontWeight.Bold)
        Field("Endereço HTTPS do manifesto", url) { url = it }
        UiText("O publicador fornece este endereço. Nenhum servidor é usado para criar ou imprimir.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { vm.task { require(url.isBlank() || url.startsWith("https://")) { "Use HTTPS." }; vm.app.settings.update { it.copy(manifestUrl = url.trim(), lastUpdateCheck = 0) }; updates.check() } }, enabled = !updates.busy) { UiText("Salvar e verificar atualizações") }
        if (updates.status.isNotBlank()) UiText(updates.status)
        if (updates.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        updates.release?.let { release ->
            UiText("Nova versão: ${release.versionName}", style = MaterialTheme.typography.titleMedium)
            release.changelog.forEach { UiText("• $it") }
            if (updates.busy) { UiText("Download: ${updates.progress}%"); TextButton(onClick = { updates.cancel() }) { UiText("Cancelar download") } }
            else if (updates.apk == null) Button(onClick = { updates.download() }) { UiText("Baixar atualização") }
            else Button(onClick = { confirm = true }) { UiText("Instalar atualização") }
            TextButton(onClick = { vm.tab = 0 }) { UiText("Depois") }
        }
        UiText("Após autorizar a instalação por este aplicativo, volte e toque em Instalar novamente.", style = MaterialTheme.typography.bodySmall)
    }
}
