package com.makerpress.app.core.rendering

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.makerpress.app.domain.*
import java.io.File
import kotlin.math.min

class PrintDocumentRenderer(private val context: Context) {
    private data class Piece(val height: Int, val draw: (Canvas, Float) -> Unit)
    fun render(document: PrintDocument, profile: PrinterProfile): Bitmap {
        profile.validate()
        val width = profile.width - 2 * profile.margin
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        fun text(value: String, size: Float = 24f, bold: Boolean = false, alignment: Alignment = Alignment.LEFT, spacing: Int = 8): Piece {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = size; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, tp, width)
                .setAlignment(when (alignment) { Alignment.LEFT -> Layout.Alignment.ALIGN_NORMAL; Alignment.CENTER -> Layout.Alignment.ALIGN_CENTER; Alignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE })
                .setIncludePad(true).build()
            return Piece(layout.height + spacing) { c, y -> c.save(); c.translate(profile.margin.toFloat(), y); layout.draw(c); c.restore() }
        }
        fun grid(size: Int, values: List<String>, sudoku: Boolean, marks: List<WordPlacement> = emptyList()): Piece {
            val cell = (width - 4).toFloat() / size; val extent = cell * size
            return Piece(extent.toInt() + 16) { c, y ->
                val x = profile.margin + 2f; val top = y + 2
                paint.typeface = if (sudoku) Typeface.DEFAULT else Typeface.MONOSPACE
                paint.textSize = cell * .60f; paint.textAlign = Paint.Align.CENTER
                paint.style = Paint.Style.FILL; paint.color = Color.BLACK
                values.forEachIndexed { i, v -> c.drawText(v, x + (i % size + .5f) * cell, top + (i / size + .5f) * cell - (paint.ascent() + paint.descent()) / 2, paint) }
                if (sudoku) for (i in 0..size) {
                    paint.strokeWidth = if (i % (if (size == 4) 2 else 3) == 0) 3f else 1f
                    c.drawLine(x + i * cell, top, x + i * cell, top + extent, paint)
                    c.drawLine(x, top + i * cell, x + extent, top + i * cell, paint)
                }
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                for (p in marks) {
                    val x1 = x + (p.col + .5f) * cell; val y1 = top + (p.row + .5f) * cell
                    val x2 = x1 + (p.word.length - 1) * p.dc * cell; val y2 = y1 + (p.word.length - 1) * p.dr * cell
                    c.save(); c.rotate(Math.toDegrees(kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat(), x1, y1)
                    val length = kotlin.math.hypot(x2 - x1, y2 - y1)
                    c.drawRoundRect(x1 - cell * .42f, y1 - cell * .44f, x1 + length + cell * .42f, y1 + cell * .44f, cell / 2, cell / 2, paint); c.restore()
                }
                paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.LEFT
            }
        }
        val pieces = mutableListOf<Piece>()
        fun drawing(height: Int, draw: (Canvas) -> Unit): Piece = Piece((height * width / 360f).toInt() + 12) { c, y ->
            c.save(); c.translate(profile.margin.toFloat(), y); c.scale(width / 360f, width / 360f); draw(c); c.restore()
        }
        for (block in document.blocks) when (block) {
            is PrintBlock.Kakooma -> {
                val l=block.puzzle.language
                pieces += text(l.text("Encontre, em cada grupo, o único número que é a soma de dois outros. Use três posições diferentes e circule a soma.", "In each group, find the only number that is the sum of two others. Use three different positions and circle the sum."),20f)
                pieces += text("ID: ${block.puzzle.seed}",16f)
                block.puzzle.groups.forEachIndexed { i,g ->
                    pieces += text("${i+1}.",22f,true)
                    pieces += drawing(300) { ActivityDrawing.numberBond(it,g,block.answers) }
                }
                pieces += text(l.text("Gerador original MakerPress. Inspirado em Kakooma® de Tang Math; não oficial.", "Original MakerPress generator. Inspired by Tang Math's Kakooma®; unofficial."),15f)
            }
            is PrintBlock.Dots -> {
                val p=block.puzzle; val l=p.language
                pieces += text(l.text("Ligue os pontos de 1 a ${p.points.size}, na ordem. No final, ligue ${p.points.size} a 1. Que desenho apareceu?", "Connect dots 1 to ${p.points.size} in order. Finish by connecting ${p.points.size} to 1. What picture appears?"),20f)
                pieces += text("ID: ${p.seed}",16f)
                pieces += drawing((p.points.maxOf { it.y } + 40).toInt()) { ActivityDrawing.dots(it,p,block.answers) }
                pieces += text(if(block.answers) p.subject.label(l) else l.text("Meu desenho: __________________\nAgora decore e pinte!", "My drawing: __________________\nNow decorate and color!"),20f)
            }
            is PrintBlock.Game -> {
                val l = block.language; val level = block.level
                pieces += text("${level.game} • ${level.seed}", 18f, true)
                pieces += drawing(650) { ActivityDrawing.game(it, level) }
                pieces += text(if (level.game == PaperGame.GOLF) l.text("Tacadas: ____  Meta: ${level.target}", "Strokes: ____  Target: ${level.target}") else l.text("Escudos: ○ ○ ○   Cristais: ◇ ◇ ◇\nTurnos: ____", "Shields: ○ ○ ○   Crystals: ◇ ◇ ◇\nTurns: ____"), 20f)
                pieces += text(PaperGameGenerator.rules(level.game, l), 18f)
                pieces += text(l.text("Design original MakerPress • inspiração: Gladden Design", "Original MakerPress design • inspired by Gladden Design"), 15f)
            }
            is PrintBlock.Study -> {
                val s = block.sheet
                pieces += text("${s.topic.label(s.language)} • ${s.seed}", 18f)
                s.questions.forEachIndexed { i, q ->
                    pieces += text("${i + 1}. ${q.prompt}", 22f, spacing = 10)
                    pieces += text(q.skill, 15f)
                    q.figure?.let { f -> pieces += drawing(if(f.kind == "graph") 350 else 205) { ActivityDrawing.figure(it, f, block.answers) } }
                    if(block.answers) pieces += text(q.solution, 20f, spacing = 24)
                    else pieces += text("________________________________\n\n________________________________", 18f, spacing = 24)
                }
            }
            is PrintBlock.Text -> pieces += text(block.value, block.size, block.bold, block.alignment, block.spacing)
            is PrintBlock.Rule -> pieces += Piece(block.spacing + 2) { c, y -> paint.color = Color.BLACK; paint.strokeWidth = 2f; c.drawLine(profile.margin.toFloat(), y + block.spacing / 2, (profile.width - profile.margin).toFloat(), y + block.spacing / 2, paint) }
            is PrintBlock.Sudoku -> pieces += grid(block.puzzle.size, (if (block.answers) block.puzzle.solution else block.puzzle.cells).map { if (it == 0) "" else it.toString() }, true)
            is PrintBlock.WordSearch -> { pieces += grid(block.puzzle.size, block.puzzle.cells, false, if (block.answers) block.puzzle.words else emptyList()); pieces += text(block.puzzle.words.joinToString(" • ") { it.word }, 19f) }
            is PrintBlock.Math -> block.challenge.questions.forEachIndexed { i, q ->
                val op = when (q.operation) { Operation.ADD -> "+"; Operation.SUBTRACT -> "−"; Operation.MULTIPLY -> "×"; Operation.DIVIDE -> "÷"; else -> "?" }
                pieces += text("%02d. %d %s %d = %s".format(i + 1, q.a, op, q.b, if (block.answers) q.answer.toString() else "_____"), 23f, spacing = 22)
            }
            is PrintBlock.Qr -> {
                require(block.value.isNotBlank() && block.value.toByteArray().size <= 1800) { "O QR deve conter de 1 a 1800 bytes." }
                val matrix = QRCodeWriter().encode(block.value, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.CHARACTER_SET to "UTF-8"))
                val scale = min(width, block.size) / matrix.width
                require(scale >= 2) { "QR muito denso. Reduza o conteúdo ou aumente o tamanho." }
                val side = matrix.width * scale
                pieces += Piece(side + 12) { c, y ->
                    paint.color = Color.BLACK; paint.isAntiAlias = false
                    val left = (profile.width - side) / 2f
                    for (r in 0 until matrix.height) for (col in 0 until matrix.width) if (matrix[col, r]) c.drawRect(left + col * scale, y + r * scale, left + (col + 1) * scale, y + (r + 1) * scale, paint)
                    paint.isAntiAlias = true
                }
            }
            is PrintBlock.Image -> {
                require(block.asset.matches(Regex("[a-f0-9-]+\\.png"))) { "Referência de imagem inválida." }
                val file = File(context.filesDir, "images/${block.asset}")
                val original = BitmapFactory.decodeFile(file.path) ?: error("Imagem salva não encontrada. Importe novamente.")
                val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(block.rotation.toFloat()) }, true)
                val zoom = block.zoom.coerceIn(1f, 4f)
                val cw = (rotated.width / zoom).toInt().coerceAtLeast(1); val ch = (rotated.height / zoom).toInt().coerceAtLeast(1)
                val cropped = Bitmap.createBitmap(rotated, ((rotated.width - cw) * block.cropX.coerceIn(0f, 1f)).toInt(), ((rotated.height - ch) * block.cropY.coerceIn(0f, 1f)).toInt(), cw, ch)
                val height = (width.toLong() * ch / cw).toInt().coerceAtLeast(1)
                require(height <= 8000) { "Imagem muito alta. Recorte antes de imprimir." }
                val scaled = Bitmap.createScaledBitmap(cropped, width, height, true)
                val mono = BitmapProcessor.monochrome(scaled, block.dither, block.brightness, block.contrast)
                listOf(original, rotated, cropped, scaled).distinct().forEach { if (it != mono) it.recycle() }
                pieces += Piece(height + 8) { c, y -> c.drawBitmap(mono, profile.margin.toFloat(), y, null); mono.recycle() }
            }
            is PrintBlock.Pattern -> pieces += Piece(240) { c, y ->
                for (i in 0..7) {
                    paint.color = Color.rgb(i * 255 / 7, i * 255 / 7, i * 255 / 7)
                    c.drawRect(profile.margin + i * width / 8f, y, profile.margin + (i + 1) * width / 8f, y + 80, paint)
                }
                paint.color = Color.BLACK; paint.strokeWidth = 1f
                for (x in 0..width step 16) c.drawLine(profile.margin + x.toFloat(), y + 90, profile.margin + x.toFloat(), y + 220, paint)
                for (dy in 90..220 step 16) c.drawLine(profile.margin.toFloat(), y + dy, (profile.width - profile.margin).toFloat(), y + dy, paint)
                c.drawRect(0f, y + 230, profile.width.toFloat(), y + 234, paint)
            }
        }
        val height = pieces.sumOf { it.height } + 2 * profile.margin
        require(height in 1..16_000 && height.toLong() * profile.width <= 8_000_000) { "Documento muito longo. Divida em partes menores." }
        val bitmap = Bitmap.createBitmap(profile.width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
        var y = profile.margin.toFloat(); pieces.forEach { it.draw(canvas, y); y += it.height }
        return BitmapProcessor.monochrome(bitmap, if (document.blocks.any { it is PrintBlock.Pattern }) Dither.FLOYD_STEINBERG else Dither.THRESHOLD).also { bitmap.recycle() }
    }
}
