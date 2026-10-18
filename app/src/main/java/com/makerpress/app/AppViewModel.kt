package com.makerpress.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makerpress.app.core.bluetooth.PairedPrinter
import com.makerpress.app.core.rendering.ImageImporter
import com.makerpress.app.data.*
import com.makerpress.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EditorState(
    val tool: String = "Text Studio", val title: String = "", val body: String = "", val font: Float = 24f,
    val bold: Boolean = false, val align: Alignment = Alignment.LEFT, val separator: Boolean = false, val spacing: Int = 8,
    val asset: String = "", val rotation: Int = 0, val zoom: Float = 1f, val cropX: Float = .5f, val cropY: Float = .5f,
    val brightness: Float = 0f, val contrast: Float = 1f, val dither: Dither = Dither.FLOYD_STEINBERG,
    val size: Int = 9, val difficulty: Difficulty = Difficulty.MEDIUM, val seed: Long = kotlin.random.Random.nextLong(),
    val words: String = WordSearchGenerator.categories.values.first(), val category: String = "Animais", val english: Boolean = false,
    val diagonal: Boolean = true, val reverse: Boolean = false, val operation: Operation = Operation.MIXED,
    val topic: StudyTopic = StudyTopic.MIXED, val graphMode: GraphMode = GraphMode.RANDOM,
    val language: AppLanguage = AppLanguage.PT_BR, val freshGames: Boolean = true,
    val dotSubject: DotSubject = DotSubject.SURPRISE,
    val count: Int = 10, val qrSize: Int = 320, val caption: String = ""
)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as MakerPressApp
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val jobs = app.jobs.jobs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val connection = app.printer.transport.state
    var tab by mutableIntStateOf(0)
    var selectingPrinter by mutableStateOf(false)
    var editor by mutableStateOf<EditorState?>(null)
    var preview by mutableStateOf<PrintDocument?>(null)
    var bitmap by mutableStateOf<Bitmap?>(null)
    var previewProfile by mutableStateOf(PrinterProfile())
    var copies by mutableIntStateOf(1)
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var paired by mutableStateOf<List<PairedPrinter>>(emptyList())
    private var queueTask: Job? = null
    private val ready = viewModelScope.async { app.queue.recover() }
    fun task(block: suspend () -> Unit) { viewModelScope.launch { try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message = e.message ?: "Não foi possível concluir. Tente novamente." } } }
    fun updateSettings(transform: (AppSettings) -> AppSettings) = task { app.settings.update(transform) }
    fun changeLanguage(language: AppLanguage) {
        updateSettings { it.copy(language = language) }
        editor = editor?.let { e ->
            val index = WordSearchGenerator.categories.keys.indexOf(e.category).coerceAtLeast(0)
            val builtIn = e.words == WordSearchGenerator.english[index] || e.words == WordSearchGenerator.categories.values.elementAt(index)
            e.copy(language = language, english = if(builtIn) language == AppLanguage.EN else e.english,
                words = if(builtIn) { if(language == AppLanguage.EN) WordSearchGenerator.english[index] else WordSearchGenerator.categories.values.elementAt(index) } else e.words)
        }
        if(preview != null && editor != null) generate()
    }
    fun devices() = task { paired = withContext(Dispatchers.IO) { app.printer.paired() }; if (paired.isEmpty()) message = "Nenhum aparelho pareado. Faça o pareamento nas configurações Bluetooth do Android." }
    fun connect(address: String = settings.value.printerAddress) = task {
        require(address.isNotBlank()) { "Selecione uma impressora pareada." }
        app.settings.update { it.copy(printerAddress = address) }
        app.printer.log("Conectando por RFCOMM/SPP padrão.")
        try { app.printer.transport.connect(address); app.printer.log("Bluetooth conectado; impressão física não verificada.") }
        catch (e: Exception) { error("Não foi possível conectar. Confira pareamento, energia e outros aplicativos. ${e.javaClass.simpleName}") }
    }
    fun disconnect() = task { app.printer.transport.disconnect(); app.printer.log("Desconectado pelo usuário.") }
    fun openTool(name: String) { editor = EditorState(tool = name, size = if (name == "Word Search") 10 else 9, dither = settings.value.dither, language = settings.value.language, english = settings.value.language == AppLanguage.EN, words = if(settings.value.language == AppLanguage.EN) WordSearchGenerator.english.first() else WordSearchGenerator.categories.values.first()); copies = settings.value.copies }
    private fun document(e: EditorState): PrintDocument {
        val title = e.title.ifBlank { e.tool }; val l = e.language
        val blocks = mutableListOf<PrintBlock>()
        if (e.title.isNotBlank() || e.tool in listOf("Sudoku", "Word Search", "Math Challenges", "Golf", "Galaxy", "Kakooma", "Connect the Dots")) blocks += PrintBlock.Text(title, 28f, true, Alignment.CENTER)
        when (e.tool) {
            "Text Studio" -> { require(e.body.isNotBlank()) { "Digite o texto para imprimir." }; require(e.body.length <= 15000); if (e.separator) blocks += PrintBlock.Rule(); blocks += PrintBlock.Text(e.body, e.font, e.bold, e.align, e.spacing) }
            "Image Studio" -> { require(e.asset.isNotBlank()) { "Importe uma imagem primeiro." }; blocks += PrintBlock.Image(e.asset, e.rotation, e.zoom, e.cropX, e.cropY, e.brightness, e.contrast, e.dither) }
            "Sudoku" -> {
                val p = SudokuGenerator.generate(e.size, e.difficulty, e.seed)
                val label = (if(l == AppLanguage.PT_BR) listOf("Fácil", "Médio", "Difícil") else listOf("Easy", "Medium", "Hard"))[e.difficulty.ordinal]
                blocks += PrintBlock.Text("$label • SD-${e.seed}\n${p.cells.count { it != 0 }} ${l.text("pistas", "clues")}", 16f)
                blocks += PrintBlock.Sudoku(p)
            }
            "Word Search" -> {
                val p = WordSearchGenerator.generate(e.words.split(',', '\n', ';'), e.size, e.diagonal, e.reverse, e.seed)
                blocks += PrintBlock.Text("${UiStrings.translate(e.category, l)} • WS-${e.seed}", 16f); blocks += PrintBlock.WordSearch(p)
            }
            "QR Studio" -> { blocks += PrintBlock.Qr(e.body, e.qrSize); if (e.caption.isNotBlank()) blocks += PrintBlock.Text(e.caption, 20f, alignment = Alignment.CENTER) }
            "Math Challenges" -> {
                blocks += PrintBlock.Text(l.text("Nome: ____________________", "Name: ____________________"), 20f, spacing = 20)
                blocks += PrintBlock.Study(StudyGenerator.generate(e.topic, e.difficulty, e.count, e.seed, l, e.graphMode, e.operation))
            }
            "Kakooma" -> blocks += PrintBlock.Kakooma(KakoomaGenerator.generate(e.difficulty,e.count,e.seed,l))
            "Connect the Dots" -> blocks += PrintBlock.Dots(DotGenerator.generate(e.dotSubject,e.difficulty,e.seed,l))
            "Golf", "Galaxy" -> blocks += PrintBlock.Game(PaperGameGenerator.generate(if(e.tool == "Golf") PaperGame.GOLF else PaperGame.GALAXY, e.difficulty, e.seed), l)
        }
        return PrintDocument(title, blocks)
    }
    fun generate(regenerate: Boolean = false) = task {
        if (busy) return@task
        busy = true
        try {
            if (regenerate) editor = editor?.copy(seed = kotlin.random.Random.nextLong())
            val e = editor ?: return@task
            val doc = withContext(Dispatchers.Default) { document(e) }
            render(doc, settings.value.profile)
        } finally { busy = false }
    }
    private suspend fun render(doc: PrintDocument, profile: PrinterProfile) {
        val rendered = withContext(Dispatchers.Default) { app.renderer.render(doc, profile) }
        bitmap = rendered; preview = doc; previewProfile = profile
    }
    fun openJob(job: PrintJob) = task { busy = true; try { editor = null; copies = job.copies; render(app.jobs.document(job), app.jobs.profile(job)) } finally { busy = false } }
    fun editCurrent() {
        val doc = preview ?: return
        if (editor == null) {
            val texts = doc.blocks.filterIsInstance<PrintBlock.Text>().filterNot { it.value in listOf("GABARITO", "ANSWER KEY") }
            val title = texts.firstOrNull()?.value?.takeIf { it == doc.title } ?: ""
            val base = EditorState(title = title)
            editor = when (val block = doc.blocks.firstOrNull { it !is PrintBlock.Text && it !is PrintBlock.Rule }) {
                is PrintBlock.Image -> base.copy(tool = "Image Studio", asset = block.asset, rotation = block.rotation, zoom = block.zoom, cropX = block.cropX, cropY = block.cropY, brightness = block.brightness, contrast = block.contrast, dither = block.dither)
                is PrintBlock.Sudoku -> base.copy(tool = "Sudoku", size = block.puzzle.size, seed = block.puzzle.seed, difficulty = block.puzzle.difficulty)
                is PrintBlock.WordSearch -> base.copy(tool = "Word Search", size = block.puzzle.size, seed = block.puzzle.seed, words = block.puzzle.words.joinToString(",") { it.word }, diagonal = block.puzzle.words.any { it.dc != 0 && it.dr != 0 }, reverse = block.puzzle.words.any { it.dc < 0 || it.dr < 0 })
                is PrintBlock.Qr -> base.copy(tool = "QR Studio", body = block.value, qrSize = block.size, caption = texts.lastOrNull()?.value?.takeIf { it != title } ?: "")
                is PrintBlock.Kakooma -> base.copy(tool = "Kakooma", seed = block.puzzle.seed, count = block.puzzle.groups.size, difficulty = block.puzzle.difficulty, language = block.puzzle.language)
                is PrintBlock.Dots -> base.copy(tool = "Connect the Dots", seed = block.puzzle.seed, dotSubject = block.puzzle.requestedSubject, difficulty = block.puzzle.difficulty, language = block.puzzle.language)
                is PrintBlock.Game -> base.copy(tool = if(block.level.game == PaperGame.GOLF) "Golf" else "Galaxy", seed = block.level.seed, difficulty = block.level.difficulty, language = block.language, freshGames = false)
                is PrintBlock.Study -> base.copy(tool = "Math Challenges", seed = block.sheet.seed, count = block.sheet.questions.size, topic = block.sheet.topic, graphMode = block.sheet.graphMode, difficulty = block.sheet.difficulty, language = block.sheet.language, operation = block.sheet.operation)
                is PrintBlock.Math -> base.copy(topic = StudyTopic.ARITHMETIC, tool = "Math Challenges", seed = block.challenge.seed, count = block.challenge.questions.size)
                else -> texts.lastOrNull()?.let { base.copy(body = it.value, font = it.size, bold = it.bold, align = it.alignment, spacing = it.spacing, separator = doc.blocks.any { b -> b is PrintBlock.Rule }) } ?: base
            }
        }
        preview = null
    }
    fun answerKey() = task {
        val current = preview ?: return@task
        val toggled = current.blocks.filterNot { it is PrintBlock.Text && it.value in listOf("GABARITO", "ANSWER KEY") }.map { when (it) { is PrintBlock.Sudoku -> it.copy(answers = !it.answers); is PrintBlock.WordSearch -> it.copy(answers = !it.answers); is PrintBlock.Math -> it.copy(answers = !it.answers); is PrintBlock.Study -> it.copy(answers = !it.answers); is PrintBlock.Kakooma -> it.copy(answers = !it.answers); is PrintBlock.Dots -> it.copy(answers = !it.answers); else -> it } }
        val answers = toggled.any { (it is PrintBlock.Sudoku && it.answers) || (it is PrintBlock.WordSearch && it.answers) || (it is PrintBlock.Math && it.answers) || (it is PrintBlock.Study && it.answers) || (it is PrintBlock.Kakooma && it.answers) || (it is PrintBlock.Dots && it.answers) }
        val blocks = if (answers) listOf(PrintBlock.Text(settings.value.language.text("GABARITO", "ANSWER KEY"), 24f, true, Alignment.CENTER)) + toggled else toggled
        render(current.copy(blocks = blocks), previewProfile)
    }
    fun save(queued: Boolean, print: Boolean = false) {
        if(busy) return
        val doc = preview ?: return
        val e = editor
        val selectedCopies = copies
        val profile = previewProfile
        busy = true
        task {
        try {
        ready.await()
        if (print) check(connection.value is com.makerpress.app.core.printing.PrinterConnectionState.Connected) { "Conecte uma impressora na aba Impressora." }
        if (queued && e != null && e.freshGames && doc.blocks.any { it is PrintBlock.Game }) {
            // The first queued board is exactly the preview. Every extra copy gets its own level.
            repeat(selectedCopies) { index ->
                val next = if(index == 0) doc else withContext(Dispatchers.Default) { document(e.copy(seed = kotlin.random.Random.nextLong())) }
                app.jobs.save(next, profile, 1, true)
            }
            val nextEditor = e.copy(seed = kotlin.random.Random.nextLong())
            editor = nextEditor
            render(withContext(Dispatchers.Default) { document(nextEditor) }, previewProfile)
        } else app.jobs.save(doc, profile, selectedCopies, queued)
        message = if (queued) "Documento adicionado à fila." else "Criação salva no histórico."
        if (print) runQueue()
        } finally { busy = false }
        }
    }
    fun runQueue() {
        if (queueTask?.isActive == true) return
        queueTask = viewModelScope.launch { ready.await(); if (connection.value !is com.makerpress.app.core.printing.PrinterConnectionState.Connected) { message = "Conecte uma impressora antes de iniciar a fila."; return@launch }; app.queue.run() }
    }
    fun pausePrinting() { queueTask?.cancel() }
    fun cancel(job: PrintJob) = task { app.jobs.dao.cancel(job.id) }
    fun delete(job: PrintJob) = task { app.jobs.dao.delete(job.id) }
    fun requeue(job: PrintJob) = task { app.jobs.save(app.jobs.document(job), app.jobs.profile(job), job.copies, true) }
    fun importImage(uri: Uri) = task {
        busy = true
        try { val asset = withContext(Dispatchers.IO) { ImageImporter(app).import(uri) }; editor = editor?.copy(asset = asset) } finally { busy = false }
    }
    fun lab(kind: String) = task {
        val blocks = when (kind) {
            "HELLO" -> listOf(PrintBlock.Text("HELLO MAKERPRESS", 26f, true), PrintBlock.Text("Conexão ≠ impressão confirmada\nAção • coração • criação", 21f))
            "QR" -> listOf(PrintBlock.Qr("MakerPress: Create. Print. Play."))
            else -> listOf(PrintBlock.Text("TESTE: $kind\n${settings.value.profile.width} pontos", 20f), PrintBlock.Pattern(kind))
        }
        editor = null; render(PrintDocument("Printer Lab • $kind", blocks), settings.value.profile)
    }
}
