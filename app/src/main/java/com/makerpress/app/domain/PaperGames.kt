package com.makerpress.app.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

const val DEFAULT_UPDATE_URL = "https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json"
@Serializable enum class AppLanguage { PT_BR, EN;
    fun text(pt: String, en: String) = if (this == PT_BR) pt else en
}
@Serializable enum class PaperGame { GOLF, GALAXY, DUNGEON }
@Serializable data class GridPoint(val x: Int, val y: Int)

/**
 * A frozen, printable board. Rendering never calls a generator, so history reprints stay exact.
 * generatorVersion 1 = the 1.0/1.1 symbol boards (legacy renderer). Version 2 = terrain maps (1.2+).
 * values: per-cell numbers (dungeon monster strength). expected: golf strokes from the dice odds.
 */
@Serializable data class PaperLevel(
    val game: PaperGame, val seed: Long, val difficulty: Difficulty,
    val columns: Int, val rows: Int, val cells: List<Int>,
    val route: List<GridPoint>, val start: GridPoint, val goal: GridPoint,
    val target: Int, val generatorVersion: Int = 1,
    val values: List<Int> = emptyList(), val expected: Double = 0.0
)
@Serializable data class GolfCourse(val holes: List<PaperLevel>, val seed: Long, val difficulty: Difficulty, val players: Int) {
    val par get() = holes.sumOf { it.target }
}

/** Cell codes keep v1 meanings for 1–3 so both versions read the same at a glance. */
object GolfCell { const val ROUGH = 0; const val TREE = 1; const val SAND = 2; const val WATER = 3; const val FAIRWAY = 4; const val GREEN = 5 }
object GalaxyCell { const val SPACE = 0; const val ASTEROID = 1; const val ENEMY = 2; const val CRYSTAL = 3; const val NEBULA = 4 }
object DungeonCell { const val FLOOR = 0; const val WALL = 1; const val MONSTER = 2; const val CHEST = 3; const val TRAP = 4; const val POTION = 5; const val KEY = 6; const val DOOR = 7 }

/** SplitMix64 step: derives independent, reproducible sub-seeds. */
fun mixSeed(seed: Long, salt: Long): Long {
    var z = seed + salt * -7046029254386353131L
    z = (z xor (z ushr 30)) * -4658895280553007687L
    z = (z xor (z ushr 27)) * -7723592293110705685L
    return z xor (z ushr 31)
}
// StrictMath keeps share codes identical on every phone: tiny libm differences could flip a threshold.
private fun sin(x: Double) = StrictMath.sin(x)
private fun cos(x: Double) = StrictMath.cos(x)
private fun dist(ax: Double, ay: Double, bx: Double, by: Double) = StrictMath.sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by))

object PaperGameGenerator {
    const val COLUMNS = 13
    val golfLengths = listOf(13, 19, 25)

    fun generate(game: PaperGame, difficulty: Difficulty, seed: Long, rows: Int = 25): PaperLevel = when (game) {
        PaperGame.GOLF -> golf(difficulty, seed, rows)
        PaperGame.GALAXY -> galaxy(difficulty, seed)
        PaperGame.DUNGEON -> dungeon(difficulty, seed)
    }

    fun course(difficulty: Difficulty, seed: Long, holes: Int, players: Int): GolfCourse {
        require(holes in listOf(3, 6, 9) && players in 1..4)
        val random = Random(seed)
        val lengths = when (holes) {
            3 -> listOf(19, 13, 25)
            6 -> listOf(19, 13, 25, 19, 25, 13)
            else -> listOf(19, 13, 25, 19, 19, 25, 13, 19, 25)
        }.shuffled(random)
        return GolfCourse(lengths.mapIndexed { i, rows -> golf(difficulty, mixSeed(seed, i + 1L), rows) }, seed, difficulty, players)
    }

    // ------------------------------------------------------------------ golf
    private fun golf(difficulty: Difficulty, seed: Long, rows: Int): PaperLevel {
        require(rows in 11..25)
        for (attempt in 0 until 30) buildGolf(difficulty, seed, rows, attempt)?.let { return it }
        return checkNotNull(buildGolf(difficulty, seed, rows, -1)) { "Golf fallback must be playable." }
    }

    private fun buildGolf(d: Difficulty, seed: Long, rows: Int, attempt: Int): PaperLevel? {
        val r = Random(if (attempt == 0) seed else mixSeed(seed, 1000L + attempt))
        val cols = COLUMNS; val g = IntArray(cols * rows); val long = rows >= 16
        fun idx(x: Int, y: Int) = y * cols + x
        val tee = GridPoint(r.nextInt(4, 9), rows - 2)
        val hole = GridPoint(r.nextInt(3, 10), if (long) r.nextInt(2, 4) else 2)
        val side = if (r.nextBoolean()) 1 else -1
        val ctrlX = 6.0 + side * (if (long) r.nextDouble(2.5, 5.5) else r.nextDouble(1.0, 3.0))
        val ctrlY = rows * r.nextDouble(0.4, 0.6)
        fun bez(t: Double): Pair<Double, Double> {
            val a = (1 - t) * (1 - t); val b = 2 * (1 - t) * t; val c = t * t
            return (a * tee.x + b * ctrlX + c * hole.x) to (a * tee.y + b * ctrlY + c * hole.y)
        }
        val path = Array(81) { bez(it / 80.0) }
        val k1 = r.nextDouble(0.0, 9.0); val k2 = r.nextDouble(0.0, 9.0)
        fun wob(x: Int, y: Int, k: Double) = sin(x * 1.7 + k) * cos(y * 1.3 + k * 2)
        val nearT = DoubleArray(cols * rows); val nearD = DoubleArray(cols * rows)
        for (y in 0 until rows) for (x in 0 until cols) {
            var best = 0; var bd = Double.MAX_VALUE
            path.forEachIndexed { i, (px, py) -> val dd = dist(px, py, x.toDouble(), y.toDouble()); if (dd < bd) { bd = dd; best = i } }
            nearT[idx(x, y)] = best / 80.0; nearD[idx(x, y)] = bd
        }
        // Fairway corridor with carries (rough gaps) on longer holes.
        val gaps = mutableListOf(0.3, 0.42, 0.55, 0.66).shuffled(r).take(if (!long) 0 else if (d == Difficulty.EASY) 1 else 2)
        for (y in 0 until rows) for (x in 0 until cols) {
            val t = nearT[idx(x, y)]
            val width = 1.75 + 0.55 * sin(t * 7 + k1) + 0.35 * wob(x, y, k2) - d.ordinal * 0.15
            if (t > 0.12 && t < 0.86 && nearD[idx(x, y)] < width && gaps.all { abs(t - it) > 0.03 }) g[idx(x, y)] = GolfCell.FAIRWAY
            if (dist(x.toDouble(), y.toDouble(), tee.x.toDouble(), tee.y.toDouble()) <= 1.1) g[idx(x, y)] = GolfCell.FAIRWAY
        }
        val sx = r.nextDouble(1.6, 2.4); val sy = r.nextDouble(1.3, 2.0)
        for (y in 0 until rows) for (x in 0 until cols) {
            val ex = (x - hole.x) / sx; val ey = (y - hole.y) / sy
            if (ex * ex + ey * ey + 0.25 * wob(x, y, k1) <= 1.05) g[idx(x, y)] = GolfCell.GREEN
        }
        fun blob(cx: Double, cy: Double, radius: Double, kind: Int) {
            for (y in 0 until rows) for (x in 0 until cols)
                if (dist(x.toDouble(), y.toDouble(), cx, cy) + 0.35 * wob(x, y, cx + cy) <= radius && g[idx(x, y)] in listOf(GolfCell.ROUGH, GolfCell.FAIRWAY)) g[idx(x, y)] = kind
        }
        repeat(1 + d.ordinal) {
            val a = r.nextDouble(0.0, 2 * Math.PI)
            blob(hole.x + cos(a) * 2.8, hole.y + abs(sin(a)) * 2.4, r.nextDouble(0.9, 1.4), GolfCell.SAND)
        }
        if (long) { val (px, py) = bez(r.nextDouble(0.45, 0.6)); blob(px - side * 2.2, py, 1.2, GolfCell.SAND) }
        if (attempt >= 0) {
            if (long && d != Difficulty.EASY) {
                val wy = bez(r.nextDouble(0.3, 0.42)).second
                for (y in 0 until rows) for (x in 0 until cols)
                    if (abs(y - (wy + 1.2 * sin(x * 0.55 + k2))) < 0.75 + 0.3 * wob(x, y, k1) && g[idx(x, y)] in listOf(GolfCell.ROUGH, GolfCell.FAIRWAY)) g[idx(x, y)] = GolfCell.WATER
            } else if (long || d == Difficulty.HARD) {
                val (px, py) = bez(if (long) 0.35 else 0.5); blob(px + side * 3, py, if (long) 1.8 else 1.3, GolfCell.WATER)
            }
            val clusters = ((4 + d.ordinal) * rows / 25.0).roundToInt().coerceAtLeast(2)
            repeat(clusters) {
                var cx = -1; var cy = -1
                for (tries in 0 until 40) {
                    val x = r.nextInt(cols); val y = r.nextInt(2, rows - 1)
                    if (g[idx(x, y)] == GolfCell.ROUGH && nearD[idx(x, y)] > 2.6) { cx = x; cy = y; break }
                }
                if (cx < 0) return@repeat
                val radius = r.nextDouble(1.4, 2.4); val kk = r.nextDouble(0.0, 9.0)
                for (y in 1 until rows) for (x in 0 until cols)
                    if (dist(x.toDouble(), y.toDouble(), cx.toDouble(), cy.toDouble()) + 0.6 * wob(x, y, kk) <= radius &&
                        g[idx(x, y)] == GolfCell.ROUGH && nearD[idx(x, y)] > 1.9) g[idx(x, y)] = GolfCell.TREE
            }
        }
        g[idx(tee.x, tee.y)] = GolfCell.FAIRWAY; g[idx(hole.x, hole.y)] = GolfCell.GREEN
        val expected = GolfPar.expected(cols, rows, g, tee, hole)
        if (expected >= 25.0) return null
        return PaperLevel(PaperGame.GOLF, seed, d, cols, rows, g.toList(), emptyList(), tee, hole,
            max(2, expected.roundToInt()), 2, expected = (expected * 10).roundToInt() / 10.0)
    }

    // ------------------------------------------------------------------ galaxy
    private fun windingRoute(random: Random, cols: Int, rows: Int): List<GridPoint> {
        val route = mutableListOf<GridPoint>()
        var x = random.nextInt(3, cols - 3)
        for (y in rows - 1 downTo 0) {
            route += GridPoint(x, y)
            if (y > 0 && y < rows - 1 && y % 3 == 0) {
                val next = (x + random.nextInt(-3, 4)).coerceIn(1, cols - 2)
                while (x != next) { x += if (next > x) 1 else -1; route += GridPoint(x, y) }
            }
        }
        return route
    }

    private fun galaxy(d: Difficulty, seed: Long): PaperLevel {
        val r = Random(seed); val cols = COLUMNS; val rows = 25
        fun idx(x: Int, y: Int) = y * cols + x
        val route = windingRoute(r, cols, rows); val safe = route.toSet()
        val g = IntArray(cols * rows)
        val k = r.nextDouble(0.0, 9.0)
        fun wob(x: Int, y: Int, s: Double) = sin(x * 1.9 + s + k) * cos(y * 1.1 + s * 1.7)
        fun blob(radius: ClosedFloatingPointRange<Double>, kind: Int, allowRoute: Boolean) {
            val cx = r.nextInt(cols).toDouble(); val cy = r.nextInt(1, rows - 1).toDouble(); val rad = r.nextDouble(radius.start, radius.endInclusive)
            for (y in 0 until rows) for (x in 0 until cols) {
                val p = GridPoint(x, y)
                if (dist(x.toDouble(), y.toDouble(), cx, cy) + 0.45 * wob(x, y, cx + cy) <= rad && (allowRoute || p !in safe)) g[idx(x, y)] = kind
            }
        }
        repeat(2 + (d.ordinal + 1) / 2) { blob(1.4..2.4, GalaxyCell.NEBULA, true) }
        repeat(7 + 2 * d.ordinal) { blob(0.9..2.0, GalaxyCell.ASTEROID, false) }
        val start = route.first(); val goal = route.last()
        g[idx(start.x, start.y)] = GalaxyCell.SPACE; g[idx(goal.x, goal.y)] = GalaxyCell.SPACE
        for (row in listOf(5, 12, 19)) { val p = route.first { it.y == row }; g[idx(p.x, p.y)] = GalaxyCell.CRYSTAL }
        val open = g.indices.filter { g[it] == GalaxyCell.SPACE && GridPoint(it % cols, it / cols) !in safe &&
            abs(it % cols - start.x) + abs(it / cols - start.y) > 2 }.shuffled(r)
        open.take(4 + 3 * d.ordinal).forEach { g[it] = GalaxyCell.ENEMY }
        // Target turns: the shortest enemy-free tour through every crystal, at an average roll of 3.5.
        val crystals = g.indices.filter { g[it] == GalaxyCell.CRYSTAL }
        fun bfs(from: Int): IntArray {
            val dist = IntArray(g.size) { -1 }; dist[from] = 0; val queue = ArrayDeque(listOf(from))
            while (queue.isNotEmpty()) {
                val c = queue.removeFirst(); val x = c % cols; val y = c / cols
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until cols || ny !in 0 until rows) continue
                    val n = idx(nx, ny)
                    if (dist[n] < 0 && g[n] != GalaxyCell.ASTEROID && g[n] != GalaxyCell.ENEMY) { dist[n] = dist[c] + 1; queue.add(n) }
                }
            }
            return dist
        }
        val stops = listOf(idx(start.x, start.y)) + crystals + idx(goal.x, goal.y)
        val table = stops.map { bfs(it) }
        val tour = listOf(listOf(1, 2, 3), listOf(1, 3, 2), listOf(2, 1, 3), listOf(2, 3, 1), listOf(3, 1, 2), listOf(3, 2, 1)).minOf { order ->
            val seq = listOf(0) + order + 4
            seq.zipWithNext().sumOf { (a, b) -> table[a][stops[b]] }
        }
        val nebulaStops = route.count { g[idx(it.x, it.y)] == GalaxyCell.NEBULA } / 3
        return PaperLevel(PaperGame.GALAXY, seed, d, cols, rows, g.toList(), route, start, goal,
            ceil(tour / 3.5).toInt() + 1 + nebulaStops, 2)
    }

    // ------------------------------------------------------------------ dungeon
    private fun dungeon(d: Difficulty, seed: Long): PaperLevel {
        for (attempt in 0 until 200) buildDungeon(d, seed, attempt)?.let { return it }
        error("Could not build a dungeon.")
    }

    private class Room(val x: Int, val y: Int, val w: Int, val h: Int) {
        val cx get() = x + w / 2; val cy get() = y + h / 2
        fun contains(px: Int, py: Int) = px in x until x + w && py in y until y + h
        fun near(o: Room) = x - 1 < o.x + o.w && o.x - 1 < x + w && y - 1 < o.y + o.h && o.y - 1 < y + h
    }

    private fun buildDungeon(d: Difficulty, seed: Long, attempt: Int): PaperLevel? {
        val r = Random(if (attempt == 0) seed else mixSeed(seed, 5000L + attempt))
        val cols = COLUMNS; val rows = 25
        fun idx(x: Int, y: Int) = y * cols + x
        val g = IntArray(cols * rows) { DungeonCell.WALL }
        val rooms = mutableListOf<Room>()
        fun tryAdd(room: Room) = if (room.x >= 1 && room.y >= 1 && room.x + room.w <= cols - 1 && room.y + room.h <= rows - 1 && rooms.none { it.near(room) }) { rooms += room; true } else false
        val sw = r.nextInt(3, 5); if (!tryAdd(Room(r.nextInt(1, cols - sw), rows - 4, sw, 3))) return null
        val gw = r.nextInt(3, 5); if (!tryAdd(Room(r.nextInt(1, cols - gw), 1, gw, 3))) return null
        val wanted = 6 + d.ordinal
        repeat(80) { if (rooms.size < wanted) { val w = r.nextInt(3, 6); val h = r.nextInt(3, 5); tryAdd(Room(r.nextInt(1, cols - w), r.nextInt(1, rows - h), w, h)) } }
        if (rooms.size < 5) return null
        rooms.forEach { room -> for (y in room.y until room.y + room.h) for (x in room.x until room.x + room.w) g[idx(x, y)] = DungeonCell.FLOOR }
        val startRoom = rooms[0]; val goalRoom = rooms[1]
        fun corridor(a: Room, b: Room) {
            var x = a.cx; var y = a.cy
            val horizontalFirst = r.nextBoolean()
            fun carve() { if (!goalRoom.contains(x, y) || a === goalRoom || b === goalRoom) g[idx(x, y)] = DungeonCell.FLOOR }
            if (horizontalFirst) { while (x != b.cx) { x += if (b.cx > x) 1 else -1; carve() }; while (y != b.cy) { y += if (b.cy > y) 1 else -1; carve() } }
            else { while (y != b.cy) { y += if (b.cy > y) 1 else -1; carve() }; while (x != b.cx) { x += if (b.cx > x) 1 else -1; carve() } }
        }
        val others = rooms.filter { it !== goalRoom }.sortedByDescending { it.cy }
        others.zipWithNext().forEach { (a, b) -> corridor(a, b) }
        repeat(1 + d.ordinal) { val a = others.random(r); val b = others.random(r); if (a !== b) corridor(a, b) }
        corridor(goalRoom, others.filter { it !== startRoom }.minBy { abs(it.cx - goalRoom.cx) + abs(it.cy - goalRoom.cy) })
        // The stairs room must have exactly one entrance, which becomes the locked door.
        val entrances = (0 until cols * rows).filter { c ->
            val x = c % cols; val y = c / cols
            g[c] == DungeonCell.FLOOR && !goalRoom.contains(x, y) &&
                listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).any { (dx, dy) -> goalRoom.contains(x + dx, y + dy) }
        }
        if (entrances.size != 1) return null
        val door = entrances.single(); g[door] = DungeonCell.DOOR
        fun reach(from: Int, doorOpen: Boolean): IntArray {
            val dist = IntArray(g.size) { -1 }; dist[from] = 0; val queue = ArrayDeque(listOf(from))
            while (queue.isNotEmpty()) {
                val c = queue.removeFirst(); val x = c % cols; val y = c / cols
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until cols || ny !in 0 until rows) continue
                    val n = idx(nx, ny)
                    if (dist[n] < 0 && g[n] != DungeonCell.WALL && (doorOpen || g[n] != DungeonCell.DOOR)) { dist[n] = dist[c] + 1; queue.add(n) }
                }
            }
            return dist
        }
        val start = GridPoint(startRoom.cx, startRoom.cy); val goal = GridPoint(goalRoom.cx, goalRoom.cy)
        val closed = reach(idx(start.x, start.y), false)
        if (closed[idx(goal.x, goal.y)] >= 0) return null
        if (rooms.any { it !== goalRoom && closed[idx(it.cx, it.cy)] < 0 }) return null
        fun inRoom(c: Int) = rooms.any { it.contains(c % cols, c / cols) }
        val reserved = mutableSetOf(idx(start.x, start.y), idx(goal.x, goal.y), door)
        val keyRoom = rooms.filter { it !== startRoom && it !== goalRoom }.maxBy { closed[idx(it.cx, it.cy)] }
        val keyCell = (keyRoom.y until keyRoom.y + keyRoom.h).flatMap { y -> (keyRoom.x until keyRoom.x + keyRoom.w).map { x -> idx(x, y) } }.random(r)
        g[keyCell] = DungeonCell.KEY; reserved += keyCell
        val values = IntArray(g.size)
        fun place(kind: Int, count: Int, where: (Int) -> Boolean) {
            g.indices.filter { g[it] == DungeonCell.FLOOR && it !in reserved && where(it) }.shuffled(r).take(count).forEach { g[it] = kind; reserved += it }
        }
        val awayFromStart: (Int) -> Boolean = { !startRoom.contains(it % cols, it / cols) }
        place(DungeonCell.MONSTER, 4 + 2 * d.ordinal) { awayFromStart(it) }
        g.indices.filter { g[it] == DungeonCell.MONSTER }.forEach { values[it] = r.nextInt(2, min(6, 4 + d.ordinal) + 1) }
        place(DungeonCell.CHEST, 5) { awayFromStart(it) && inRoom(it) }
        place(DungeonCell.TRAP, 2 + 2 * d.ordinal) { !inRoom(it) }
        place(DungeonCell.POTION, 3 - d.ordinal) { awayFromStart(it) && inRoom(it) }
        // A guard stands in front of the door on medium and hard.
        if (d != Difficulty.EASY) {
            val dx = door % cols; val dy = door / cols
            listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1).map { (ox, oy) -> dx + ox to dy + oy }
                .firstOrNull { (x, y) -> x in 0 until cols && y in 0 until rows && g[idx(x, y)] == DungeonCell.FLOOR && !goalRoom.contains(x, y) && idx(x, y) !in reserved }
                ?.let { (x, y) -> g[idx(x, y)] = DungeonCell.MONSTER; values[idx(x, y)] = 4 + d.ordinal.coerceAtMost(1) }
        }
        return PaperLevel(PaperGame.DUNGEON, seed, d, cols, rows, g.toList(), emptyList(), start, goal,
            g.count { it == DungeonCell.CHEST }, 2, values.toList())
    }

    // ------------------------------------------------------------------ rules
    fun rules(game: PaperGame, language: AppLanguage, version: Int = 2): String = when {
        game == PaperGame.GOLF && version >= 2 -> language.text(
            "Lápis + 1 dado. Comece no tee (bola no círculo). A cada tacada, role e mova de 1 até o valor do dado em linha reta, em 8 direções, de ponto a ponto. A bola voa sobre tudo, menos árvores, e não sai do mapa. O terreno onde ela cai limita a próxima tacada (veja a legenda). Caiu na água? Volte ao ponto anterior e some 1 tacada. Termine exatamente no buraco (bandeira). Risque uma bolinha por tacada.",
            "Pencil + 1 die. Start on the tee (ball in a ring). Each stroke: roll and move 1 up to the roll in a straight line, in 8 directions, dot to dot. The ball flies over everything except trees and never leaves the map. Where it lands limits your next stroke (see the key). Landed in water? Go back to your previous dot and add 1 stroke. Finish exactly on the cup (flag). Tick one circle per stroke.")
        game == PaperGame.GALAXY && version >= 2 -> language.text(
            "Lápis + 1 dado. Saia da base com 3 escudos. A cada turno, role e ande até esse número de passos na horizontal ou vertical, podendo virar. Asteroides bloqueiam. Entrou numa nebulosa? Pare ali. Ao entrar num inimigo, risque-o e perca 1 escudo. Pegue os 3 cristais e chegue ao portal com pelo menos 1 escudo. Menos turnos é melhor.",
            "Pencil + 1 die. Leave the base with 3 shields. Each turn, roll and take up to that many steps horizontally or vertically; you may turn. Asteroids block you. Entered a nebula? Stop there. Entering an enemy crosses it out and costs 1 shield. Collect all 3 crystals and reach the portal with at least 1 shield. Fewer turns is better.")
        game == PaperGame.DUNGEON -> language.text(
            "Lápis + 1 dado. Comece na estrela com 6 corações. Role e ande até esse número de passos na horizontal ou vertical, podendo virar; paredes bloqueiam. Monstro: pare nele e role; tirou o número dele ou mais? Risque-o. Senão, perca 1 coração e volte 1 passo. Armadilha: perca 1 coração e risque-a. Baú: +1 tesouro. Poção: +1 coração (máx. 6). Pegue a chave, abra a porta e fuja pela escada. Pontos: tesouros × 2 + corações.",
            "Pencil + 1 die. Start on the star with 6 hearts. Roll and take up to that many steps horizontally or vertically; you may turn, walls block. Monster: stop on it and roll; rolled its number or more? Cross it out. Otherwise lose 1 heart and step back. Trap: lose 1 heart and cross it out. Chest: +1 treasure. Potion: +1 heart (max 6). Take the key, open the door and escape down the stairs. Score: treasures × 2 + hearts.")
        game == PaperGame.GOLF -> language.text(
            "Lápis + dado de 6 lados. Comece em S. A cada tacada, role e avance de 1 até o valor do dado em linha reta, horizontal, vertical ou diagonal. Não atravesse árvores (triângulos) nem saia da grade. Água (~): volte ao ponto anterior e some 1 tacada. Areia (hachuras): pare ao entrar. Alcance H; não precisa usar todo o dado. Conte as tacadas; menos é melhor. Meta sugerida, não par certificado.",
            "Pencil + six-sided die. Start at S. Each stroke: roll and move 1 to the rolled number of dots in a straight horizontal, vertical or diagonal line. Do not cross trees (triangles) or leave the grid. Entering water (~): return to your previous point and add 1 stroke. Sand (hatching): stop on entry. Reach H; you may use less than the roll. Count strokes; fewer is better. Suggested target, not a certified par.")
        else -> language.text(
            "Lápis + dado de 6 lados. Comece em S com 3 escudos. Role e avance de 1 até o valor do dado; cada passo vai a um ponto vizinho na horizontal ou vertical, podendo virar. Não atravesse asteroides (hexágonos). Ao entrar num inimigo (triângulo), risque-o e perca 1 escudo; ele não ataca novamente. Recolha os 3 cristais (losangos), riscando cada um. Vença chegando a H com pelo menos 1 escudo. Com 0 escudos, perdeu. Conte os turnos; menos é melhor.",
            "Pencil + six-sided die. Start at S with 3 shields. Roll and move 1 to the rolled number of steps; each step goes to a horizontal or vertical neighbor and you may turn. Do not cross asteroids (hexagons). Entering an enemy (triangle) crosses it out and costs 1 shield; it never attacks again. Collect all 3 crystals (diamonds), crossing each out. Win by reaching H with at least 1 shield. At 0 shields you lose. Count turns; fewer is better.")
    }
}

/**
 * Expected strokes from every dot, solved by value iteration over the real dice odds:
 * a roll caps the distance by the lie (fairway 6, rough 4, sand 2, green 2); trees block the line;
 * landing in water costs a stroke and returns the ball. The tee value is the calibrated par.
 */
object GolfPar {
    fun cap(cell: Int) = when (cell) { GolfCell.FAIRWAY -> 6; GolfCell.ROUGH -> 4; else -> 2 }
    fun expected(cols: Int, rows: Int, g: IntArray, tee: GridPoint, hole: GridPoint): Double {
        val n = cols * rows; val holeIndex = hole.y * cols + hole.x
        val playable = BooleanArray(n) { g[it] != GolfCell.TREE && g[it] != GolfCell.WATER }
        val dest = arrayOfNulls<IntArray>(n); val far = arrayOfNulls<IntArray>(n)
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
        for (c in 0 until n) if (playable[c]) {
            val d = mutableListOf<Int>(); val k = mutableListOf<Int>()
            for ((dx, dy) in dirs) for (step in 1..6) {
                val x = c % cols + dx * step; val y = c / cols + dy * step
                if (x !in 0 until cols || y !in 0 until rows || g[y * cols + x] == GolfCell.TREE) break
                d += y * cols + x; k += step
            }
            dest[c] = d.toIntArray(); far[c] = k.toIntArray()
        }
        val e = DoubleArray(n) { if (it == holeIndex) 0.0 else 30.0 }
        val bestAt = DoubleArray(7)
        repeat(300) {
            var delta = 0.0
            for (c in 0 until n) {
                if (!playable[c] || c == holeIndex) continue
                bestAt.fill(Double.MAX_VALUE)
                val ds = dest[c]!!; val ks = far[c]!!
                for (i in ds.indices) {
                    val cost = if (g[ds[i]] == GolfCell.WATER) 1 + e[c] else e[ds[i]]
                    if (cost < bestAt[ks[i]]) bestAt[ks[i]] = cost
                }
                for (k in 2..6) bestAt[k] = min(bestAt[k], bestAt[k - 1])
                val capped = cap(g[c]); var total = 0.0
                for (roll in 1..6) { val b = bestAt[min(roll, capped)]; total += if (b == Double.MAX_VALUE) 1 + e[c] else b }
                val v = min(30.0, 1 + total / 6)
                delta = max(delta, abs(v - e[c])); e[c] = v
            }
            if (delta < 1e-4) return e[tee.y * cols + tee.x]
        }
        return e[tee.y * cols + tee.x]
    }
}
