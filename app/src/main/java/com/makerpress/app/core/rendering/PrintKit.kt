package com.makerpress.app.core.rendering

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** A vertical slice of a print: its height in dots and how to draw it at a y offset. */
class PrintPiece(val height: Int, val draw: (Canvas, Float) -> Unit)

/**
 * Hand-made 1-bit textures. Thermal heads have no gray, so every "shade" is a pixel pattern
 * designed at the printer's native dot pitch (no scaling, no dithering noise).
 */
enum class Texture { FAIRWAY, GREEN, SAND, WATER, NEBULA, ASTEROID, ROCK, CHECKER }

object PrintKit {
    val headline: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val body: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    /** Small print loses hairlines when thresholded to 1-bit; medium weight keeps every stroke. */
    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val wave = IntArray(20) { (1.6 * sin(2 * PI * it / 20)).roundToInt() }
    private fun hash(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 255
    }

    fun ink(t: Texture, x: Int, y: Int, tone: Int): Boolean = when (t) {
        Texture.FAIRWAY -> when (tone) {
            -1 -> x % 4 == 0 && y % 4 == 0
            1 -> x % 2 == 0 && y % 2 == 0
            else -> (x % 4 == 0 && y % 4 == 0) || (x % 4 == 2 && y % 4 == 2)
        }
        Texture.GREEN -> when (tone) {
            -1 -> x % 2 == 1 && y % 2 == 1
            1 -> (x + y) % 2 == 0
            else -> (x % 2 == 1 && y % 2 == 1) || (x % 2 == 0 && y % 2 == 0 && (x + y) % 4 == 0)
        }
        Texture.SAND -> (x + y) % (5 - tone) == 0
        Texture.WATER -> when (tone) {
            -1 -> Math.floorMod(y + wave[x % 20], 6) >= 2
            1 -> Math.floorMod(y + wave[x % 20], 8) != 0
            else -> Math.floorMod(y + wave[x % 20], 6) != 0
        }
        Texture.NEBULA -> hash(x, y) < 30 + tone * 12
        Texture.ASTEROID -> hash(x, y) >= 46 - tone * 16
        Texture.ROCK -> { val p = 4 - tone; Math.floorMod(x + y, p) == 0 || Math.floorMod(x - y, p) == 0 }
        Texture.CHECKER -> (x + y) % 2 == 0
    }

    fun paint(size: Float = 16f, face: Typeface = body) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = size; typeface = face }

    /** Metaball union of grid cells: neighbours melt into rounded, organic regions. */
    fun blobMask(width: Int, height: Int, cols: Int, rows: Int, ox: Float, oy: Float, step: Float, clip: Rect, inside: (Int) -> Boolean): BooleanArray {
        val field = FloatArray(width * height)
        val radius = (1.5f * step).toInt(); val sig2 = 2f * (0.45f * step) * (0.45f * step)
        val side = 2 * radius + 1
        val stamp = FloatArray(side * side) { val dx = it % side - radius; val dy = it / side - radius; exp(-(dx * dx + dy * dy) / sig2) }
        for (cell in 0 until cols * rows) {
            if (!inside(cell)) continue
            val px = (ox + cell % cols * step).roundToInt(); val py = (oy + cell / cols * step).roundToInt()
            for (dy in -radius..radius) {
                val yy = py + dy; if (yy !in 0 until height) continue
                val row = yy * width; val srow = (dy + radius) * side + radius
                for (dx in -radius..radius) { val xx = px + dx; if (xx in 0 until width) field[row + xx] += stamp[srow + dx] }
            }
        }
        return BooleanArray(width * height) { i -> field[i] > 0.5f && clip.contains(i % width, i / width) }
    }

    fun text(value: String, size: Float, width: Int, margin: Int, face: Typeface = body, spacing: Int = 8, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): PrintPiece {
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint(size, face), width).setAlignment(align).setIncludePad(true).build()
        return PrintPiece(layout.height + spacing) { c, y -> c.save(); c.translate(margin.toFloat(), y); layout.draw(c); c.restore() }
    }

    /** Bold title on the left, a quiet middle label and a strong right label (par, target), over a rule. */
    fun header(left: String, middle: String, right: String, width: Int, margin: Int) = PrintPiece(40) { c, y ->
        val big = paint(26f, headline); val mid = paint(17f); val r = paint(19f, headline)
        val x = margin.toFloat()
        c.drawText(left, x, y + 26, big)
        if (middle.isNotEmpty()) c.drawText(middle, x + big.measureText(left) + 12, y + 25, mid)
        if (right.isNotEmpty()) { r.textAlign = Paint.Align.RIGHT; c.drawText(right, x + width, y + 26, r) }
        val line = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        c.drawLine(x, y + 34, x + width, y + 34, line)
    }

    // ------------------------------------------------------------------ legend
    sealed interface Swatch {
        /** dot: 0 none, 1 black grid dot, 2 knocked-out white dot. */
        data class Tex(val texture: Texture?, val dot: Int = 1) : Swatch
        class Icon(val draw: (Canvas, Float, Float, Float) -> Unit) : Swatch
    }
    data class LegendItem(val swatch: Swatch, val title: String, val effect: String)

    fun legend(title: String, items: List<LegendItem>, width: Int, margin: Int, tone: Int): PrintPiece {
        val lines = (items.size + 1) / 2; val height = 26 + lines * 40
        return PrintPiece(height) { c, y ->
            val art = PixelArt(width, height, tone); val colW = width / 2
            fun origin(i: Int) = (i % 2) * colW to 26 + (i / 2) * 40
            items.forEachIndexed { i, item ->
                val (bx, by) = origin(i)
                (item.swatch as? Swatch.Tex)?.texture?.let { art.fill(art.roundRect(bx, by, bx + 34, by + 26, 8f), it) }
            }
            val bmp = art.toBitmap(); val cv = Canvas(bmp)
            val head = paint(14f, headline); cv.drawText(title.uppercase(), 0f, 14f, head)
            val line = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
            cv.drawLine(head.measureText(title.uppercase()) + 8, 9f, width.toFloat(), 9f, line)
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.6f }
            items.forEachIndexed { i, item ->
                val (bx, by) = origin(i); val cx = bx + 17f; val cy = by + 13f
                when (val s = item.swatch) {
                    is Swatch.Tex -> {
                        cv.drawRoundRect(bx + .5f, by + .5f, bx + 33.5f, by + 25.5f, 8f, 8f, stroke)
                        if (s.dot == 1) Glyphs.dot(cv, cx, cy, 1f) else if (s.dot == 2) Glyphs.whiteDot(cv, cx, cy, 1f)
                    }
                    is Swatch.Icon -> s.draw(cv, cx, cy, .9f)
                }
                val titlePaint = paint(14f, bold); val effectPaint = paint(12f, medium)
                val room = colW - 44f
                while (effectPaint.measureText(item.effect) > room && effectPaint.textSize > 9f) effectPaint.textSize -= .5f
                cv.drawText(item.title, bx + 42f, by + 11f, titlePaint)
                cv.drawText(item.effect, bx + 42f, by + 25f, effectPaint)
            }
            c.drawBitmap(bmp, margin.toFloat(), y, null); bmp.recycle()
        }
    }

    // ------------------------------------------------------------------ trackers
    enum class Mark { CIRCLE, DIAMOND, HEART, SQUARE }
    data class Track(val label: String, val count: Int, val mark: Mark, val highlight: Int = 0, val numbered: Boolean = false)

    /** Each inner list is one line; tracks on a line sit side by side and wrap when long. */
    fun tracker(lines: List<List<Track>>, width: Int, margin: Int, note: String = ""): PrintPiece {
        val pitch = 30f; val perRow = (width / pitch).toInt()
        fun rowsOf(line: List<Track>) = if (line.size == 1) (line[0].count + perRow - 1) / perRow else 1
        val height = lines.sumOf { 20 + rowsOf(it) * 32 } + 4
        return PrintPiece(height) { c, y ->
            val label = paint(13f, headline); val num = paint(11f, medium).apply { textAlign = Paint.Align.CENTER }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
            var top = y
            if (note.isNotEmpty()) { val n = paint(12f, medium).apply { textAlign = Paint.Align.RIGHT }; c.drawText(note, margin + width.toFloat(), top + 12, n) }
            for (line in lines) {
                var x0 = margin.toFloat()
                for (track in line) {
                    c.drawText(track.label.uppercase(), x0, top + 13, label)
                    for (i in 0 until track.count) {
                        val col = if (line.size == 1) i % perRow else i; val row = if (line.size == 1) i / perRow else 0
                        val cx = x0 + 11 + col * pitch; val cy = top + 34 + row * 32
                        when (track.mark) {
                            Mark.CIRCLE -> c.drawCircle(cx, cy, 11f, p)
                            Mark.SQUARE -> c.drawRect(cx - 10, cy - 10, cx + 10, cy + 10, p)
                            Mark.DIAMOND -> c.drawPath(Path().apply { moveTo(cx, cy - 12); lineTo(cx + 10, cy); lineTo(cx, cy + 12); lineTo(cx - 10, cy); close() }, p)
                            Mark.HEART -> Glyphs.heartOutline(c, cx, cy, p)
                        }
                        if (i + 1 == track.highlight) { p.strokeWidth = 1f; c.drawCircle(cx, cy, 14.5f, p); p.strokeWidth = 2f }
                        if (track.numbered) c.drawText("${i + 1}", cx, cy + 4, num)
                    }
                    x0 += 20 + track.count * pitch
                }
                top += 20 + rowsOf(line) * 32
            }
        }
    }

    /** Course scorecard: hole numbers, par row and one row per player, with totals. */
    fun scorecard(pars: List<Int>, players: Int, holeLabel: String, parLabel: String, playerLabel: String, totalLabel: String, width: Int, margin: Int): PrintPiece {
        val rowH = 30f; val rows = 2 + players; val height = (rows * rowH).toInt() + 8
        return PrintPiece(height) { c, y ->
            val nameW = 70f; val cols = pars.size + 1; val cellW = (width - nameW) / cols
            val x0 = margin.toFloat(); val line = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
            val bold = Paint().apply { color = Color.BLACK; strokeWidth = 2.5f }
            val t = paint(13f, headline).apply { textAlign = Paint.Align.CENTER }; val left = paint(13f, headline)
            for (r in 0..rows) c.drawLine(x0, y + r * rowH, x0 + width, y + r * rowH, if (r <= 2 || r == rows) bold else line)
            c.drawLine(x0, y, x0, y + rows * rowH, bold); c.drawLine(x0 + width, y, x0 + width, y + rows * rowH, bold)
            for (k in 0..cols) c.drawLine(x0 + nameW + k * cellW, y, x0 + nameW + k * cellW, y + rows * rowH, if (k == 0 || k == cols - 1) bold else line)
            c.drawText(holeLabel, x0 + 6, y + 20, left); c.drawText(parLabel, x0 + 6, y + rowH + 20, left)
            pars.forEachIndexed { i, par ->
                val cx = x0 + nameW + (i + .5f) * cellW
                c.drawText("${i + 1}", cx, y + 20, t); c.drawText("$par", cx, y + rowH + 20, t)
            }
            val totalX = x0 + nameW + (cols - .5f) * cellW
            t.textSize = 11f; c.drawText(totalLabel, totalX, y + 19, t); t.textSize = 13f
            c.drawText("${pars.sum()}", totalX, y + rowH + 20, t)
            val small = paint(12f, medium)
            for (pl in 1..players) c.drawText("$playerLabel $pl", x0 + 6, y + (pl + 1) * rowH + 19, small)
        }
    }
}

/** Pixel-level canvas for textures; vector marks are drawn afterwards on its bitmap. */
class PixelArt(val width: Int, val height: Int, private val tone: Int) {
    private val px = IntArray(width * height) { Color.WHITE }
    fun fill(mask: BooleanArray, t: Texture) {
        for (i in mask.indices) if (mask[i]) px[i] = if (PrintKit.ink(t, i % width, i / width, tone)) Color.BLACK else Color.WHITE
    }
    fun outline(mask: BooleanArray) {
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            val i = y * width + x
            if (mask[i] && !(mask[i - 1] && mask[i + 1] && mask[i - width] && mask[i + width])) px[i] = Color.BLACK
        }
    }
    fun roundRect(l: Int, t: Int, r: Int, b: Int, radius: Float): BooleanArray {
        val m = BooleanArray(width * height)
        for (y in max(0, t) until min(height, b)) for (x in max(0, l) until min(width, r)) {
            val dx = max(max(l + radius - x - .5f, x + .5f - (r - radius)), 0f)
            val dy = max(max(t + radius - y - .5f, y + .5f - (b - radius)), 0f)
            if (dx * dx + dy * dy <= radius * radius) m[y * width + x] = true
        }
        return m
    }
    fun circle(cx: Float, cy: Float, radius: Float): BooleanArray {
        val m = BooleanArray(width * height)
        for (y in max(0, (cy - radius).toInt()) until min(height, (cy + radius + 1).toInt()))
            for (x in max(0, (cx - radius).toInt()) until min(width, (cx + radius + 1).toInt()))
                if ((x + .5f - cx) * (x + .5f - cx) + (y + .5f - cy) * (y + .5f - cy) <= radius * radius) m[y * width + x] = true
        return m
    }
    fun toBitmap(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, width, 0, 0, width, height) }
}

/** Original vector symbols, centered on (x, y); u = 1 is a 27-dot cell. */
object Glyphs {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val paper = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private fun stroke(w: Float, white: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (white) Color.WHITE else Color.BLACK; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private fun poly(vararg pts: Float) = Path().apply { moveTo(pts[0], pts[1]); for (i in 2 until pts.size step 2) lineTo(pts[i], pts[i + 1]); close() }

    fun dot(c: Canvas, x: Float, y: Float, u: Float) { c.drawCircle(x, y, 3.5f * u, paper); c.drawCircle(x, y, max(1.6f, 1.7f * u), ink) }
    fun whiteDot(c: Canvas, x: Float, y: Float, u: Float) { c.drawCircle(x, y, 3.2f * u, ink); c.drawCircle(x, y, max(1.5f, 1.6f * u), paper) }
    fun halo(c: Canvas, x: Float, y: Float, u: Float, r: Float = 10f) = c.drawCircle(x, y, r * u, paper)

    fun pine(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawPath(poly(x, y - 10 * u, x - 4.5f * u, y - 2 * u, x + 4.5f * u, y - 2 * u), ink)
        c.drawPath(poly(x, y - 6 * u, x - 6.5f * u, y + 3.5f * u, x + 6.5f * u, y + 3.5f * u), ink)
        c.drawRect(x - 1.2f * u, y + 3 * u, x + 1.2f * u, y + 7.5f * u, ink)
    }
    fun roundTree(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawOval(x - 6 * u, y - 10 * u, x + 6 * u, y + 2.5f * u, ink)
        c.drawRect(x - 1.2f * u, y + 1 * u, x + 1.2f * u, y + 7.5f * u, ink)
        c.drawCircle(x - 2.2f * u, y - 5.5f * u, 1.1f * u, paper)
    }
    fun tee(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawCircle(x, y, 9.5f * u, paper); c.drawCircle(x, y, 9f * u, stroke(2f * u)); c.drawCircle(x, y, 4f * u, ink)
    }
    fun flag(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawCircle(x, y, 6.5f * u, paper); c.drawCircle(x, y, 4f * u, ink)
        c.drawRect(x - 1f * u, y - 25 * u, x + 1f * u, y - 2 * u, ink)
        c.drawPath(poly(x + 1f * u, y - 25 * u, x + 14 * u, y - 20 * u, x + 1f * u, y - 15 * u), ink)
    }
    fun flagIcon(c: Canvas, x: Float, y: Float, u: Float) = flag(c, x - 4 * u, y + 11 * u, u * .85f)
    fun ship(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        c.drawPath(poly(x, y + 9 * u, x - 9 * u, y - 6 * u, x - 3 * u, y - 3.5f * u, x, y - 8 * u, x + 3 * u, y - 3.5f * u, x + 9 * u, y - 6 * u), ink)
        c.drawCircle(x, y + 1 * u, 1.8f * u, paper)
    }
    fun crystal(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        c.drawPath(poly(x, y - 10 * u, x + 7.5f * u, y - 2 * u, x, y + 10 * u, x - 7.5f * u, y - 2 * u), ink)
        val s = stroke(1.4f * u, true); c.drawLine(x - 5.5f * u, y - 2 * u, x + 5.5f * u, y - 2 * u, s); c.drawLine(x, y - 7 * u, x, y + 6 * u, s)
    }
    fun station(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawCircle(x, y, 11 * u, paper); c.drawRoundRect(x - 9 * u, y - 9 * u, x + 9 * u, y + 9 * u, 3 * u, 3 * u, ink)
        c.drawCircle(x, y, 5.5f * u, paper); c.drawCircle(x, y, 2.5f * u, ink)
    }
    fun portal(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawCircle(x, y, 11.5f * u, paper)
        for ((r, white) in listOf(10.5f to false, 8f to true, 5.5f to false, 3f to true)) c.drawCircle(x, y, r * u, if (white) paper else ink)
    }
    fun sparkle(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawPath(poly(x, y - 4 * u, x + 1 * u, y - 1 * u, x + 4 * u, y, x + 1 * u, y + 1 * u, x, y + 4 * u, x - 1 * u, y + 1 * u, x - 4 * u, y, x - 1 * u, y - 1 * u), ink)
    }
    fun monster(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 11f)
        val body = Path().apply {
            moveTo(x - 8 * u, y + 7 * u); lineTo(x - 8 * u, y - 2 * u)
            arcTo(x - 8 * u, y - 10 * u, x + 8 * u, y + 6 * u, 180f, 180f, false)
            lineTo(x + 8 * u, y + 7 * u); lineTo(x + 5.3f * u, y + 4.5f * u); lineTo(x + 2.7f * u, y + 7 * u)
            lineTo(x, y + 4.5f * u); lineTo(x - 2.7f * u, y + 7 * u); lineTo(x - 5.3f * u, y + 4.5f * u); close()
        }
        c.drawPath(body, ink)
        c.drawCircle(x - 3 * u, y - 2.5f * u, 2f * u, paper); c.drawCircle(x + 3 * u, y - 2.5f * u, 2f * u, paper)
    }
    fun monsterWithPower(c: Canvas, x: Float, y: Float, u: Float, power: Int) {
        monster(c, x - 4.5f * u, y, u * .8f)
        c.drawRect(x + 2.5f * u, y - 8 * u, x + 13 * u, y + 8 * u, paper)
        val t = PrintKit.paint(17f * u, PrintKit.headline).apply { textAlign = Paint.Align.CENTER }
        c.drawText("$power", x + 7.8f * u, y + 6 * u, t)
    }
    fun chest(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        c.drawRoundRect(x - 8 * u, y - 8 * u, x + 8 * u, y - 1.5f * u, 4 * u, 4 * u, ink)
        c.drawRect(x - 8 * u, y - 0.5f * u, x + 8 * u, y + 7 * u, ink)
        c.drawRect(x - 2 * u, y - 3 * u, x + 2 * u, y + 2 * u, paper); c.drawRect(x - 1 * u, y - 2 * u, x + 1 * u, y + 1 * u, ink)
    }
    fun trap(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        for (dx in listOf(-6f, 0f, 6f)) c.drawPath(poly(x + dx * u, y - 7 * u, x + (dx + 3.2f) * u, y + 4 * u, x + (dx - 3.2f) * u, y + 4 * u), ink)
        c.drawRect(x - 9.5f * u, y + 4 * u, x + 9.5f * u, y + 7 * u, ink)
    }
    fun potion(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        c.drawRect(x - 2.2f * u, y - 9 * u, x + 2.2f * u, y - 3 * u, ink); c.drawRect(x - 3.2f * u, y - 10 * u, x + 3.2f * u, y - 8 * u, ink)
        c.drawCircle(x, y + 2.5f * u, 6.5f * u, ink); c.drawCircle(x - 2.4f * u, y + 0.5f * u, 1.5f * u, paper)
    }
    fun key(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 10.5f)
        c.drawCircle(x - 4.5f * u, y, 4.2f * u, stroke(2.6f * u))
        c.drawRect(x - 0.5f * u, y - 1.3f * u, x + 9 * u, y + 1.3f * u, ink)
        c.drawRect(x + 4.5f * u, y, x + 6.5f * u, y + 4.5f * u, ink); c.drawRect(x + 7.5f * u, y, x + 9 * u, y + 3.5f * u, ink)
    }
    fun door(c: Canvas, x: Float, y: Float, u: Float) {
        c.drawRoundRect(x - 9 * u, y - 11 * u, x + 9 * u, y + 11 * u, 3 * u, 3 * u, ink)
        c.drawCircle(x, y - 2 * u, 2.6f * u, paper); c.drawPath(poly(x, y - 1 * u, x + 2.3f * u, y + 5 * u, x - 2.3f * u, y + 5 * u), paper)
    }
    fun stairs(c: Canvas, x: Float, y: Float, u: Float) {
        halo(c, x, y, u, 11f)
        c.drawPath(poly(x - 9 * u, y + 8 * u, x - 9 * u, y + 3 * u, x - 4 * u, y + 3 * u, x - 4 * u, y - 2 * u, x + 1 * u, y - 2 * u, x + 1 * u, y - 7 * u, x + 9 * u, y - 7 * u, x + 9 * u, y + 8 * u), ink)
    }
    fun star(c: Canvas, x: Float, y: Float, u: Float, outline: Boolean = false) {
        val path = Path()
        for (i in 0 until 10) {
            val a = -PI / 2 + i * PI / 5; val r = if (i % 2 == 0) 9.5f * u else 4f * u
            val px = x + kotlin.math.cos(a).toFloat() * r; val py = y + kotlin.math.sin(a).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close(); halo(c, x, y, u, 10.5f); c.drawPath(path, if (outline) stroke(1.8f * u) else ink)
    }
    fun heartOutline(c: Canvas, x: Float, y: Float, p: Paint) {
        val path = Path().apply {
            moveTo(x, y + 10); cubicTo(x - 14, y, x - 9, y - 12, x, y - 5); cubicTo(x + 9, y - 12, x + 14, y, x, y + 10); close()
        }
        c.drawPath(path, p)
    }
    fun trees(c: Canvas, x: Float, y: Float, u: Float) { pine(c, x - 6 * u, y, u * .9f); roundTree(c, x + 7 * u, y, u * .9f) }
}
