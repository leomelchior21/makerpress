package com.makerpress.app.core.rendering

import android.graphics.*
import android.text.Layout
import com.makerpress.app.core.rendering.PrintKit.LegendItem
import com.makerpress.app.core.rendering.PrintKit.Mark
import com.makerpress.app.core.rendering.PrintKit.Swatch
import com.makerpress.app.core.rendering.PrintKit.Track
import com.makerpress.app.domain.*
import kotlin.math.max
import kotlin.math.min

/** Printable art for 1.2 activities. Every function lays out at the printer's native dot width. */
object GameArt {
    private class Grid(width: Int, cols: Int, rows: Int) {
        val step = ((width - 36) / (cols - 1)).toFloat()
        val ox = (width - step * (cols - 1)) / 2f
        val oy = 15f
        val height = ((rows - 1) * step + 30).toInt()
        val u = step / 27f
        fun x(c: Int) = ox + c * step
        fun y(r: Int) = oy + r * step
    }
    private fun clipFor(width: Int, g: Grid) = Rect((g.ox - 16 * g.u).toInt(), 0, (width - g.ox + 16 * g.u).toInt() + 1, g.height)
    private fun pine(seed: Long, cell: Int) = Math.floorMod(mixSeed(seed, cell.toLong()), 100L) < 55

    /** Full single-board pieces: header, map, key, tracker and rules. */
    fun level(level: PaperLevel, l: AppLanguage, width: Int, margin: Int, tone: Int): List<PrintPiece> = buildList {
        when (level.game) {
            PaperGame.GOLF -> {
                add(PrintKit.header("GOLF", lengthName(level.rows, l), "PAR ${level.target}", width, margin))
                add(golfMap(level, width, margin, tone))
                add(PrintKit.legend(l.text("Legenda", "Key"), golfLegend(l), width, margin, tone))
                add(PrintKit.tracker(listOf(listOf(Track(l.text("Tacadas", "Strokes"), 12, Mark.CIRCLE, level.target, true))), width, margin,
                    l.text("par calculado %.1f".format(java.util.Locale.forLanguageTag("pt-BR"), level.expected), "calculated par %.1f".format(java.util.Locale.US, level.expected))))
            }
            PaperGame.GALAXY -> {
                add(PrintKit.header("GALAXY", l.text("Missão", "Mission"), l.text("META ${level.target}", "TARGET ${level.target}"), width, margin))
                add(galaxyMap(level, width, margin, tone))
                add(PrintKit.legend(l.text("Legenda", "Key"), galaxyLegend(l), width, margin, tone))
                add(PrintKit.tracker(listOf(
                    listOf(Track(l.text("Escudos", "Shields"), 3, Mark.CIRCLE), Track(l.text("Cristais", "Crystals"), 3, Mark.DIAMOND)),
                    listOf(Track(l.text("Turnos", "Turns"), 16, Mark.CIRCLE, level.target, true))), width, margin))
            }
            PaperGame.DUNGEON -> {
                add(PrintKit.header("DUNGEON", l.text("Masmorra", "Crawl"), l.text("${level.target} BAÚS", "${level.target} CHESTS"), width, margin))
                add(dungeonMap(level, width, margin, tone))
                add(PrintKit.legend(l.text("Legenda", "Key"), dungeonLegend(l), width, margin, tone))
                add(PrintKit.tracker(listOf(
                    listOf(Track(l.text("Corações", "Hearts"), 6, Mark.HEART), Track(l.text("Chave", "Key"), 1, Mark.SQUARE)),
                    listOf(Track(l.text("Tesouros", "Treasure"), level.target, Mark.CIRCLE))), width, margin,
                    l.text("pontos: ______", "score: ______")))
            }
        }
        add(PrintKit.text(PaperGameGenerator.rules(level.game, l, level.generatorVersion), 15f, width, margin))
        add(PrintKit.text(l.text("Design original MakerPress • inspiração: Gladden Design", "Original MakerPress design • inspired by Gladden Design"), 12f, width, margin))
    }

    fun lengthName(rows: Int, l: AppLanguage) = when {
        rows <= 14 -> l.text("Buraco curto", "Short hole")
        rows <= 20 -> l.text("Buraco médio", "Medium hole")
        else -> l.text("Buraco longo", "Long hole")
    }

    fun course(course: GolfCourse, l: AppLanguage, width: Int, margin: Int, tone: Int): List<PrintPiece> = buildList {
        add(PrintKit.header(l.text("CAMPO", "COURSE"), l.text("${course.holes.size} buracos", "${course.holes.size} holes"), "PAR ${course.par}", width, margin))
        add(PrintKit.legend(l.text("Legenda", "Key"), golfLegend(l), width, margin, tone))
        add(PrintKit.text(PaperGameGenerator.rules(PaperGame.GOLF, l, 2), 15f, width, margin, spacing = 14))
        course.holes.forEachIndexed { i, hole ->
            add(PrintKit.header(l.text("BURACO ${i + 1}", "HOLE ${i + 1}"), lengthName(hole.rows, l), "PAR ${hole.target}", width, margin))
            add(golfMap(hole, width, margin, tone))
        }
        add(PrintKit.text(l.text("Cartão de pontos", "Scorecard"), 16f, width, margin, PrintKit.headline, 6))
        add(PrintKit.scorecard(course.holes.map { it.target }, course.players, l.text("Buraco", "Hole"), "Par", l.text("Jogador", "Player"), "Total", width, margin))
        add(PrintKit.text(l.text("Design original MakerPress • inspiração: Gladden Design", "Original MakerPress design • inspired by Gladden Design"), 12f, width, margin))
    }

    // ------------------------------------------------------------------ golf
    fun golfLegend(l: AppLanguage) = listOf(
        LegendItem(Swatch.Tex(Texture.FAIRWAY), "Fairway", l.text("dado livre (1–6)", "full roll (1–6)")),
        LegendItem(Swatch.Tex(null), "Rough", l.text("máx. 4", "max 4")),
        LegendItem(Swatch.Tex(Texture.SAND), l.text("Areia", "Sand"), l.text("máx. 2", "max 2")),
        LegendItem(Swatch.Tex(Texture.GREEN), "Green", l.text("máx. 2, até o buraco", "max 2, to the cup")),
        LegendItem(Swatch.Tex(Texture.WATER, 2), l.text("Água", "Water"), l.text("volte e +1 tacada", "go back, +1 stroke")),
        LegendItem(Swatch.Icon(Glyphs::trees), l.text("Árvores", "Trees"), l.text("bloqueiam a bola", "block the ball")),
        LegendItem(Swatch.Icon(Glyphs::tee), "Tee", l.text("comece aqui", "start here")),
        LegendItem(Swatch.Icon(Glyphs::flagIcon), l.text("Buraco", "Cup"), l.text("termine exatamente", "finish exactly"))
    )

    fun golfMap(level: PaperLevel, width: Int, margin: Int, tone: Int): PrintPiece {
        val g = Grid(width, level.columns, level.rows)
        return PrintPiece(g.height) { c, y ->
            val art = PixelArt(width, g.height, tone); val clip = clipFor(width, g)
            for ((kind, tex) in listOf(GolfCell.FAIRWAY to Texture.FAIRWAY, GolfCell.GREEN to Texture.GREEN, GolfCell.SAND to Texture.SAND, GolfCell.WATER to Texture.WATER)) {
                val mask = PrintKit.blobMask(width, g.height, level.columns, level.rows, g.ox, g.oy, g.step, clip) { level.cells[it] == kind }
                art.fill(mask, tex); if (kind != GolfCell.FAIRWAY) art.outline(mask)
            }
            val bmp = art.toBitmap(); val cv = Canvas(bmp)
            for (cell in level.cells.indices) {
                val x = g.x(cell % level.columns); val yy = g.y(cell / level.columns)
                when (level.cells[cell]) {
                    GolfCell.TREE -> { Glyphs.halo(cv, x, yy - 1.5f * g.u, g.u, 10f); if (pine(level.seed, cell)) Glyphs.pine(cv, x, yy, g.u) else Glyphs.roundTree(cv, x, yy, g.u) }
                    GolfCell.WATER -> Glyphs.whiteDot(cv, x, yy, g.u)
                    else -> Glyphs.dot(cv, x, yy, g.u)
                }
            }
            Glyphs.tee(cv, g.x(level.start.x), g.y(level.start.y), g.u)
            Glyphs.flag(cv, g.x(level.goal.x), g.y(level.goal.y), g.u)
            c.drawBitmap(bmp, margin.toFloat(), y, null); bmp.recycle()
        }
    }

    // ------------------------------------------------------------------ galaxy
    fun galaxyLegend(l: AppLanguage) = listOf(
        LegendItem(Swatch.Tex(null), l.text("Espaço", "Space"), l.text("ande livre", "move freely")),
        LegendItem(Swatch.Tex(Texture.NEBULA), l.text("Nebulosa", "Nebula"), l.text("pare ao entrar", "stop on entry")),
        LegendItem(Swatch.Tex(Texture.ASTEROID, 0), l.text("Asteroides", "Asteroids"), l.text("bloqueiam", "block you")),
        LegendItem(Swatch.Icon(Glyphs::ship), l.text("Inimigo", "Enemy"), l.text("−1 escudo, risque", "−1 shield, cross out")),
        LegendItem(Swatch.Icon(Glyphs::crystal), l.text("Cristal", "Crystal"), l.text("pegue os 3", "collect all 3")),
        LegendItem(Swatch.Icon(Glyphs::station), "Base", l.text("início", "start")),
        LegendItem(Swatch.Icon(Glyphs::portal), "Portal", l.text("chegada", "finish"))
    )

    fun galaxyMap(level: PaperLevel, width: Int, margin: Int, tone: Int): PrintPiece {
        val g = Grid(width, level.columns, level.rows)
        return PrintPiece(g.height) { c, y ->
            val art = PixelArt(width, g.height, tone); val clip = clipFor(width, g)
            val nebula = PrintKit.blobMask(width, g.height, level.columns, level.rows, g.ox, g.oy, g.step, clip) { level.cells[it] == GalaxyCell.NEBULA }
            art.fill(nebula, Texture.NEBULA)
            val rocks = PrintKit.blobMask(width, g.height, level.columns, level.rows, g.ox, g.oy, g.step, clip) { level.cells[it] == GalaxyCell.ASTEROID }
            art.fill(rocks, Texture.ASTEROID); art.outline(rocks)
            val bmp = art.toBitmap(); val cv = Canvas(bmp)
            val crater = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.5f }
            val cols = level.columns
            for (cell in level.cells.indices) {
                val x = g.x(cell % cols); val yy = g.y(cell / cols)
                when (level.cells[cell]) {
                    GalaxyCell.ASTEROID -> if (Math.floorMod(mixSeed(level.seed, cell.toLong()), 3L) == 0L) cv.drawCircle(x, yy, 3.5f * g.u, crater)
                    GalaxyCell.ENEMY -> Glyphs.ship(cv, x, yy, g.u)
                    GalaxyCell.CRYSTAL -> Glyphs.crystal(cv, x, yy, g.u)
                    else -> Glyphs.dot(cv, x, yy, g.u)
                }
            }
            // Decorative sparkles sit between dots, only in open space, so they never read as cells.
            for (cy in 0 until level.rows - 1) for (cx in 0 until cols - 1) {
                val corners = listOf(cy * cols + cx, cy * cols + cx + 1, (cy + 1) * cols + cx, (cy + 1) * cols + cx + 1)
                if (corners.all { level.cells[it] == GalaxyCell.SPACE } && Math.floorMod(mixSeed(level.seed, 9000L + cy * cols + cx), 9L) == 0L)
                    Glyphs.sparkle(cv, g.x(cx) + g.step / 2, g.y(cy) + g.step / 2, g.u)
            }
            Glyphs.station(cv, g.x(level.start.x), g.y(level.start.y), g.u)
            Glyphs.portal(cv, g.x(level.goal.x), g.y(level.goal.y), g.u)
            c.drawBitmap(bmp, margin.toFloat(), y, null); bmp.recycle()
        }
    }

    // ------------------------------------------------------------------ dungeon
    fun dungeonLegend(l: AppLanguage) = listOf(
        LegendItem(Swatch.Tex(null), l.text("Chão", "Floor"), l.text("ande", "walk")),
        LegendItem(Swatch.Tex(Texture.ROCK, 0), l.text("Parede", "Wall"), l.text("bloqueia", "blocks")),
        LegendItem(Swatch.Icon { c, x, y, u -> Glyphs.monsterWithPower(c, x, y, u, 4) }, l.text("Monstro", "Monster"), l.text("role ≥ o número", "roll ≥ its number")),
        LegendItem(Swatch.Icon(Glyphs::trap), l.text("Armadilha", "Trap"), l.text("−1 coração", "−1 heart")),
        LegendItem(Swatch.Icon(Glyphs::chest), l.text("Baú", "Chest"), l.text("+1 tesouro", "+1 treasure")),
        LegendItem(Swatch.Icon(Glyphs::potion), l.text("Poção", "Potion"), l.text("+1 coração", "+1 heart")),
        LegendItem(Swatch.Icon(Glyphs::key), l.text("Chave", "Key"), l.text("abre a porta", "opens the door")),
        LegendItem(Swatch.Icon(Glyphs::door), l.text("Porta", "Door"), l.text("precisa da chave", "needs the key")),
        LegendItem(Swatch.Icon { c, x, y, u -> Glyphs.star(c, x, y, u) }, l.text("Início", "Start"), l.text("comece aqui", "start here")),
        LegendItem(Swatch.Icon(Glyphs::stairs), l.text("Escada", "Stairs"), l.text("saída", "exit"))
    )

    fun dungeonMap(level: PaperLevel, width: Int, margin: Int, tone: Int): PrintPiece {
        val g = Grid(width, level.columns, level.rows)
        return PrintPiece(g.height) { c, y ->
            val art = PixelArt(width, g.height, tone); val clip = clipFor(width, g)
            val walls = PrintKit.blobMask(width, g.height, level.columns, level.rows, g.ox, g.oy, g.step, clip) { level.cells[it] == DungeonCell.WALL }
            art.fill(walls, Texture.ROCK); art.outline(walls)
            val bmp = art.toBitmap(); val cv = Canvas(bmp)
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
            cv.drawRoundRect(RectF(clip), 10f, 10f, frame)
            val cols = level.columns
            for (cell in level.cells.indices) {
                val x = g.x(cell % cols); val yy = g.y(cell / cols)
                when (level.cells[cell]) {
                    DungeonCell.WALL -> Unit
                    DungeonCell.MONSTER -> Glyphs.monsterWithPower(cv, x, yy, g.u, level.values.getOrElse(cell) { 3 })
                    DungeonCell.CHEST -> Glyphs.chest(cv, x, yy, g.u)
                    DungeonCell.TRAP -> Glyphs.trap(cv, x, yy, g.u)
                    DungeonCell.POTION -> Glyphs.potion(cv, x, yy, g.u)
                    DungeonCell.KEY -> Glyphs.key(cv, x, yy, g.u)
                    DungeonCell.DOOR -> Glyphs.door(cv, x, yy, g.u * .9f)
                    else -> Glyphs.dot(cv, x, yy, g.u)
                }
            }
            Glyphs.star(cv, g.x(level.start.x), g.y(level.start.y), g.u)
            Glyphs.stairs(cv, g.x(level.goal.x), g.y(level.goal.y), g.u)
            c.drawBitmap(bmp, margin.toFloat(), y, null); bmp.recycle()
        }
    }

    // ------------------------------------------------------------------ maze
    fun maze(p: MazePuzzle, answers: Boolean, width: Int, margin: Int): PrintPiece {
        val cell = ((width - 8) / p.columns).toFloat(); val ox = ((width - cell * p.columns) / 2).toInt().toFloat(); val oy = 24f
        val height = (cell * p.rows + 2 * oy).toInt()
        return PrintPiece(height) { c, y ->
            c.save(); c.translate(margin.toFloat(), y)
            // Aliasing off: walls land on whole dots and stay crisp after the 1-bit conversion.
            val wall = Paint().apply { color = Color.BLACK; strokeWidth = if (cell < 24) 2f else 3f; strokeCap = Paint.Cap.SQUARE }
            fun cx(i: Int) = ox + (i % p.columns + .5f) * cell
            fun cy(i: Int) = oy + (i / p.columns + .5f) * cell
            if (answers) {
                val path = Path(); path.moveTo(cx(p.entry), oy - 10)
                p.solution.forEach { path.lineTo(cx(it), cy(it)) }
                path.lineTo(cx(p.exit), oy + cell * p.rows + 10)
                c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = max(3f, cell / 5); strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; pathEffect = DashPathEffect(floatArrayOf(cell / 3, cell / 5), 0f) })
            }
            for (i in p.walls.indices) {
                val x = ox + i % p.columns * cell; val yy = oy + i / p.columns * cell; val w = p.walls[i]
                if (w and MazeGenerator.N != 0) c.drawLine(x, yy, x + cell, yy, wall)
                if (w and MazeGenerator.W != 0) c.drawLine(x, yy, x, yy + cell, wall)
                if (w and MazeGenerator.E != 0 && i % p.columns == p.columns - 1) c.drawLine(x + cell, yy, x + cell, yy + cell, wall)
                if (w and MazeGenerator.S != 0 && i / p.columns == p.rows - 1) c.drawLine(x, yy + cell, x + cell, yy + cell, wall)
            }
            val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            fun down(x: Float, top: Float) = c.drawPath(Path().apply { moveTo(x - 7, top); lineTo(x + 7, top); lineTo(x, top + 10); close() }, arrow)
            down(cx(p.entry), 4f); down(cx(p.exit), oy + cell * p.rows + 6)
            p.stars.forEach { Glyphs.star(c, cx(it), cy(it), cell / 27f * .8f) }
            c.restore()
        }
    }

    // ------------------------------------------------------------------ nonogram
    fun nonogram(p: NonogramPuzzle, answers: Boolean, width: Int, margin: Int): PrintPiece {
        val rows = p.rowClues(); val cols = p.columnClues()
        val maxRow = max(1, rows.maxOf { it.size }); val maxCol = max(1, cols.maxOf { it.size })
        val cell = ((width - 4) / (p.size + maxRow * .72f)).toInt().toFloat()
        val clue = cell * .72f; val left = (width - cell * p.size - 2).coerceAtLeast(maxRow * clue); val top = maxCol * clue + 4
        val height = (top + cell * p.size + 8).toInt()
        return PrintPiece(height) { c, y ->
            c.save(); c.translate(margin.toFloat(), y)
            val t = PrintKit.paint(min(cell * .64f, 16f), PrintKit.bold).apply { textAlign = Paint.Align.CENTER }
            val ink = Paint().apply { color = Color.BLACK }
            for (r in 0 until p.size) {
                val shown = rows[r].ifEmpty { listOf(0) }
                shown.reversed().forEachIndexed { k, v -> c.drawText("$v", left - (k + .5f) * clue, top + (r + .5f) * cell + t.textSize * .36f, t) }
            }
            for (col in 0 until p.size) {
                val shown = cols[col].ifEmpty { listOf(0) }
                shown.reversed().forEachIndexed { k, v -> c.drawText("$v", left + (col + .5f) * cell, top - 4 - k * clue - clue * .25f, t) }
            }
            for (i in 0 until p.size * p.size) {
                if ((answers && p.cells[i]) || i in p.given) {
                    val x = left + i % p.size * cell; val yy = top + i / p.size * cell
                    c.drawRect(x + 2, yy + 2, x + cell - 2, yy + cell - 2, ink)
                }
            }
            for (k in 0..p.size) {
                ink.strokeWidth = if (k % 5 == 0) 2.5f else 1f
                c.drawLine(left + k * cell, top, left + k * cell, top + p.size * cell, ink)
                c.drawLine(left, top + k * cell, left + p.size * cell, top + k * cell, ink)
                if (k % 5 == 0) { ink.strokeWidth = 1f; c.drawLine(left + k * cell, 0f, left + k * cell, top, ink); c.drawLine(0f, top + k * cell, left, top + k * cell, ink) }
            }
            c.restore()
        }
    }

    // ------------------------------------------------------------------ bingo
    fun bingoCard(card: BingoCard, width: Int, margin: Int): PrintPiece {
        val n = card.size; val cell = ((width - 4) / n).toFloat(); val band = if (card.theme == BingoTheme.NUMBERS) 44f else 0f
        val height = (band + cell * n + 6).toInt()
        return PrintPiece(height) { c, y ->
            c.save(); c.translate(margin.toFloat() + (width - cell * n) / 2f, y)
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            if (band > 0) {
                c.drawRoundRect(0f, 0f, cell * n, band - 4, 10f, 10f, ink)
                val h = PrintKit.paint(32f, PrintKit.headline).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
                "BINGO".forEachIndexed { i, ch -> c.drawText("$ch", (i + .5f) * cell, band - 14, h) }
            }
            val t = PrintKit.paint(1f, PrintKit.bold).apply { textAlign = Paint.Align.CENTER }
            card.cells.forEachIndexed { i, v ->
                val cx = (i % n + .5f) * cell; val cy = band + (i / n + .5f) * cell
                if (v == BingoGenerator.FREE) {
                    Glyphs.star(c, cx, cy - cell * .1f, cell / 27f * .9f)
                    t.textSize = cell * .16f; c.drawText(card.language.text("LIVRE", "FREE"), cx, cy + cell * .36f, t)
                } else {
                    t.textSize = if (v.length <= 2) cell * .42f else cell * .3f
                    while (t.measureText(v) > cell - 10 && t.textSize > 9f) t.textSize -= .5f
                    c.drawText(v, cx, cy + t.textSize * .35f, t)
                }
            }
            for (k in 0..n) {
                ink.strokeWidth = if (k == 0 || k == n) 3f else 1.5f
                c.drawLine(k * cell, band, k * cell, band + n * cell, ink); c.drawLine(0f, band + k * cell, n * cell, band + k * cell, ink)
            }
            c.restore()
        }
    }

    fun bingoCaller(card: BingoCard, width: Int, margin: Int): PrintPiece {
        val columns = if (card.theme == BingoTheme.WORDS) 2 else 3; val rowH = 24f
        val lines = (card.pool.size + columns - 1) / columns; val height = (lines * rowH + 6).toInt()
        return PrintPiece(height) { c, y ->
            val colW = width / columns.toFloat()
            val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
            val t = PrintKit.paint(14f, PrintKit.medium)
            card.pool.indices.forEach { i ->
                val x = margin + (i / lines) * colW; val yy = y + (i % lines) * rowH
                c.drawRect(x + 1, yy + 5, x + 14, yy + 18, box)
                val label = if (card.theme == BingoTheme.TIMES) "${card.calls[i]} = ${card.pool[i]}" else card.calls[i]
                t.textSize = 14f; while (t.measureText(label) > colW - 22 && t.textSize > 9f) t.textSize -= .5f
                c.drawText(label, x + 20, yy + 17, t)
            }
        }
    }

    // ------------------------------------------------------------------ calibration
    fun calibration(width: Int, margin: Int, l: AppLanguage): List<PrintPiece> = buildList {
        add(PrintKit.header(l.text("CALIBRAR", "CALIBRATE"), l.text("texturas", "textures"), "", width, margin))
        add(PrintKit.text(l.text("Compare as três linhas. Escolha a que mostra todas as texturas distintas, sem manchar, e salve em Impressora → Perfil → Texturas.",
            "Compare the three rows. Pick the one where every texture is distinct without smudging, then save it in Printer → Profile → Textures."), 14f, width, margin))
        val textures = Texture.entries
        val names = listOf("Fairway", "Green", l.text("Areia", "Sand"), l.text("Água", "Water"), l.text("Nebulosa", "Nebula"), l.text("Asteroide", "Asteroid"), l.text("Parede", "Wall"), "50%")
        for ((tone, label) in listOf(-1 to l.text("Claras (−1)", "Lighter (−1)"), 0 to l.text("Normais (0)", "Normal (0)"), 1 to l.text("Escuras (+1)", "Darker (+1)"))) {
            val sw = (width - 7 * 4) / 8
            add(PrintPiece(sw + 40) { c, y ->
                val h = sw + 40; val art = PixelArt(width, h, tone)
                textures.forEachIndexed { i, t -> art.fill(art.roundRect(i * (sw + 4), 18, i * (sw + 4) + sw, 18 + sw, 6f), t) }
                val bmp = art.toBitmap(); val cv = Canvas(bmp)
                cv.drawText(label, 0f, 13f, PrintKit.paint(13f, PrintKit.headline))
                val small = PrintKit.paint(10f, PrintKit.medium).apply { textAlign = Paint.Align.CENTER }
                textures.indices.forEach { i -> cv.drawText(names[i], i * (sw + 4) + sw / 2f, 18f + sw + 13, small) }
                c.drawBitmap(bmp, margin.toFloat(), y, null); bmp.recycle()
            })
        }
        add(PrintPiece(150) { c, y ->
            val t = PrintKit.paint(12f); val p = Paint().apply { color = Color.BLACK }
            c.drawText(l.text("Linhas: 1, 2, 3 e 4 pontos", "Lines: 1, 2, 3 and 4 dots"), margin.toFloat(), y + 12, t)
            for (w in 1..4) { val yy = y + 18 + w * 12f; c.drawRect(margin.toFloat(), yy, margin + width.toFloat(), yy + w, p) }
            c.drawText(l.text("Pontos da grade", "Grid dots"), margin.toFloat(), y + 92, t)
            for (k in 0 until 12) Glyphs.dot(c, margin + 14f + k * 27f, y + 108, 1f)
            c.drawRect(margin.toFloat(), y + 124, margin + width.toFloat(), y + 144, p)
        })
        for (size in listOf(11f, 13f, 16f, 20f, 26f)) add(PrintKit.text("${size.toInt()} px · Ação criação 0123456789", size, width, margin, spacing = 4))
        add(PrintKit.text("MakerPress", 30f, width, margin, PrintKit.headline, align = Layout.Alignment.ALIGN_CENTER))
    }
}
