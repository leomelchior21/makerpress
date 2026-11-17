package com.makerpress.app.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.random.Random

// ---------------------------------------------------------------------- maze
/** walls: bitmask per cell, N=1 E=2 S=4 W=8. Perfect maze: exactly one path between any two cells. */
@Serializable data class MazePuzzle(
    val columns: Int, val rows: Int, val walls: List<Int>, val entry: Int, val exit: Int,
    val solution: List<Int>, val stars: List<Int>, val seed: Long, val difficulty: Difficulty, val language: AppLanguage
)

object MazeGenerator {
    const val N = 1; const val E = 2; const val S = 4; const val W = 8
    fun size(d: Difficulty) = listOf(10 to 13, 14 to 19, 18 to 25)[d.ordinal]
    fun generate(difficulty: Difficulty, seed: Long, treasures: Boolean, language: AppLanguage): MazePuzzle {
        val (cols, rows) = size(difficulty); val n = cols * rows
        val r = Random(seed); val walls = IntArray(n) { 15 }; val seen = BooleanArray(n)
        val stack = ArrayDeque<Int>(); val first = r.nextInt(n); stack.addLast(first); seen[first] = true
        while (stack.isNotEmpty()) {
            val c = stack.last(); val x = c % cols; val y = c / cols
            val options = listOf(Triple(0, -1, N), Triple(1, 0, E), Triple(0, 1, S), Triple(-1, 0, W))
                .filter { (dx, dy, _) -> x + dx in 0 until cols && y + dy in 0 until rows && !seen[(y + dy) * cols + x + dx] }
            if (options.isEmpty()) { stack.removeLast(); continue }
            val (dx, dy, bit) = options.random(r); val next = (y + dy) * cols + x + dx
            walls[c] = walls[c] and bit.inv(); walls[next] = walls[next] and opposite(bit).inv()
            seen[next] = true; stack.addLast(next)
        }
        val entry = r.nextInt(cols); val exit = (rows - 1) * cols + r.nextInt(cols)
        walls[entry] = walls[entry] and N.inv(); walls[exit] = walls[exit] and S.inv()
        val solution = path(cols, rows, walls, entry, exit)
        val onPath = solution.toSet()
        val stars = if (!treasures) emptyList() else (0 until n).filter { c -> c !in onPath && Integer.bitCount(walls[c]) == 3 }
            .shuffled(r).fold(mutableListOf<Int>()) { acc, c ->
                if (acc.size < 3 && acc.all { kotlin.math.abs(it % cols - c % cols) + kotlin.math.abs(it / cols - c / cols) > cols / 2 }) acc += c; acc
            }
        return MazePuzzle(cols, rows, walls.toList(), entry, exit, solution, stars, seed, difficulty, language)
    }
    fun opposite(bit: Int) = when (bit) { N -> S; S -> N; E -> W; else -> E }
    fun path(cols: Int, rows: Int, walls: IntArray, from: Int, to: Int): List<Int> {
        val parent = IntArray(cols * rows) { -2 }; parent[from] = -1; val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst(); if (c == to) break
            val x = c % cols; val y = c / cols
            for ((dx, dy, bit) in listOf(Triple(0, -1, N), Triple(1, 0, E), Triple(0, 1, S), Triple(-1, 0, W))) {
                val nx = x + dx; val ny = y + dy
                if (nx !in 0 until cols || ny !in 0 until rows || walls[c] and bit != 0) continue
                val next = ny * cols + nx
                if (parent[next] == -2) { parent[next] = c; queue.add(next) }
            }
        }
        val out = mutableListOf<Int>(); var c = to
        while (c >= 0) { out += c; c = parent[c] }
        return out.reversed()
    }
}

// ---------------------------------------------------------------------- nonogram
@Serializable data class NonogramPuzzle(
    val size: Int, val cells: List<Boolean>, val given: List<Int>, val subject: String,
    val seed: Long, val difficulty: Difficulty, val language: AppLanguage
) {
    fun rowClues() = (0 until size).map { r -> NonogramGenerator.clue((0 until size).map { cells[r * size + it] }) }
    fun columnClues() = (0 until size).map { c -> NonogramGenerator.clue((0 until size).map { cells[it * size + c] }) }
}

object NonogramGenerator {
    // Original pixel pictures. Every motif is verified by a pure line solver (unique, no guessing).
    val small = linkedMapOf(
        "HEART" to ".##....##.|####..####|##########|##########|##########|.########.|..######..|...####...|....##....|..........",
        "HOUSE" to "....##....|...####...|..######..|.########.|##########|.#......#.|.#.##...#.|.#.##.###.|.#....###.|.########.",
        "TREE" to "....##....|...####...|..######..|...####...|..######..|.########.|..######..|....##....|....##....|..######..",
        "STAR" to "....##....|....##....|...####...|##########|.########.|..######..|..######..|.###..###.|.##....##.|##......##",
        "MUSHROOM" to "...####...|.##.##.##.|###.##.###|##########|##########|...#..#...|...#..#...|...#..#...|...####...|..........",
        "SMILE" to "..######..|.########.|###.##.###|###.##.###|##########|#.######.#|##.####.##|.##....##.|..######..|..........",
        "BOAT" to "....#.....|....##....|....###...|....####..|....#####.|....#.....|##########|.########.|..######..|..........",
        "NOTE" to "...######.|...######.|...#....#.|...#....#.|...#....#.|...#....#.|.###..###.|####.####.|####.####.|.##...##..",
        "TROPHY" to "##########|#.######.#|#.######.#|.########.|..######..|...####...|....##....|....##....|..######..|.########.",
        "ROBOT" to "....##....|..######..|..#.##.#..|..######..|..#....#..|..######..|##########|#.######.#|#.######.#|..##..##..",
        "CAT" to "#........#|##......##|###....###|##########|#.##..##.#|##########|###.##.###|####..####|.########.|..######..",
        "ROCKET" to "....##....|...####...|...#..#...|...####...|...####...|..######..|.###..###.|.##.##.##.|##..##..##|...####..."
    )
    val large = linkedMapOf(
        "TURTLE" to "...............|.....#####.....|....#######....|...##.#.#.##...|..###########..|.#############.|.##.#.###.#.###|.############.#|..###########..|..##.......##..|.###.......###.|...............|...............|...............|...............",
        "ANCHOR" to ".......#.......|......###......|......#.#......|......###......|.......#.......|....#######....|.......#.......|.......#.......|.......#.......|.#.....#.....#.|###....#....###|.##....#....##.|..##...#...##..|...####.####...|.....#####.....",
        "PLANE" to ".......#.......|......###......|......###......|......###......|.....#####.....|...#########...|.#############.|###############|......###......|......###......|......###......|.....#####.....|....#######....|.......#.......|...............",
        "WHALE" to "...............|.........#.#...|..........#....|...............|....#######....|..###########..|.#############.|.##.##########.|.#############.|..############.|...#########.##|.....#####..###|.............##|...............|...............",
        "OWL" to "..##.......##..|..###.....###..|..###########..|.#############.|.##..#####..##.|.#.##.###.##.#.|.##..#####..##.|.######.######.|..###########..|..##.#####.##..|..###########..|...#########...|....#######....|.....#...#.....|....##...##....",
        "CASTLE" to "#.#.#.....#.#.#|#####.....#####|.###...#...###.|.###..###..###.|.###.#####.###.|.#############.|.#############.|.####.###.####.|.####.###.####.|.#############.|.######.######.|.#####...#####.|.#####...#####.|.#####...#####.|###############",
        "BUTTERFLY" to "...............|.###.......###.|#####.....#####|######.#.######|######.#.######|.#####.#.#####.|..####.#.####..|...#########...|..####.#.####..|.#####.#.#####.|.#####.#.#####.|..###..#..###..|...............|...............|..............."
    )
    private val names = mapOf(
        "HEART" to ("Coração" to "Heart"), "HOUSE" to ("Casa" to "House"), "TREE" to ("Árvore" to "Tree"), "STAR" to ("Estrela" to "Star"),
        "MUSHROOM" to ("Cogumelo" to "Mushroom"), "SMILE" to ("Sorriso" to "Smile"), "BOAT" to ("Barco" to "Boat"), "NOTE" to ("Nota musical" to "Music note"),
        "TROPHY" to ("Troféu" to "Trophy"), "ROBOT" to ("Robô" to "Robot"), "CAT" to ("Gato" to "Cat"), "ROCKET" to ("Foguete" to "Rocket"),
        "TURTLE" to ("Tartaruga" to "Turtle"), "ANCHOR" to ("Âncora" to "Anchor"), "PLANE" to ("Avião" to "Plane"), "WHALE" to ("Baleia" to "Whale"),
        "OWL" to ("Coruja" to "Owl"), "CASTLE" to ("Castelo" to "Castle"), "BUTTERFLY" to ("Borboleta" to "Butterfly")
    )
    fun label(subject: String, l: AppLanguage) = names[subject]?.let { l.text(it.first, it.second) } ?: subject
    fun grid(motif: String) = motif.split('|').flatMap { row -> row.map { it == '#' } }

    fun generate(difficulty: Difficulty, seed: Long, language: AppLanguage): NonogramPuzzle {
        val r = Random(seed)
        val pool = if (difficulty == Difficulty.HARD) large else small
        val subject = pool.keys.toList().random(r)
        val size = if (difficulty == Difficulty.HARD) 15 else 10
        val base = grid(pool.getValue(subject))
        val mirror = r.nextBoolean()
        val cells = List(size * size) { i -> base[i / size * size + if (mirror) size - 1 - i % size else i % size] }
        val given = if (difficulty == Difficulty.EASY) cells.indices.filter { cells[it] }.shuffled(r).take(6).sorted() else emptyList()
        return NonogramPuzzle(size, cells, given, subject, seed, difficulty, language)
    }

    fun clue(line: List<Boolean>): List<Int> {
        val out = mutableListOf<Int>(); var run = 0
        for (v in line) { if (v) run++ else if (run > 0) { out += run; run = 0 } }
        if (run > 0) out += run
        return out
    }

    private fun placements(clue: List<Int>, n: Int): List<BooleanArray> {
        if (clue.isEmpty()) return listOf(BooleanArray(n))
        val first = clue.first(); val rest = clue.drop(1); val need = rest.sum() + rest.size
        val out = mutableListOf<BooleanArray>()
        for (s in 0..n - need - first) {
            val tails = if (rest.isEmpty()) listOf(BooleanArray(n - s - first)) else placements(rest, n - s - first - 1).map { BooleanArray(1) + it }
            for (tail in tails) out += BooleanArray(s) + BooleanArray(first) { true } + tail
        }
        return out
    }

    /** Pure line logic. Returns the solved grid, or null if logic alone cannot finish it. */
    fun solve(size: Int, rows: List<List<Int>>, cols: List<List<Int>>): List<Boolean>? {
        val g = arrayOfNulls<Boolean>(size * size)
        val rowOptions = rows.map { placements(it, size) }; val colOptions = cols.map { placements(it, size) }
        var changed = true
        while (changed) {
            changed = false
            for (axis in 0..1) for (i in 0 until size) {
                fun at(j: Int) = if (axis == 0) i * size + j else j * size + i
                val options = (if (axis == 0) rowOptions[i] else colOptions[i]).filter { p -> (0 until size).all { j -> g[at(j)] == null || g[at(j)] == p[j] } }
                if (options.isEmpty()) return null
                for (j in 0 until size) if (g[at(j)] == null) {
                    val v = options[0][j]
                    if (options.all { it[j] == v }) { g[at(j)] = v; changed = true }
                }
            }
        }
        return if (g.all { it != null }) g.map { it!! } else null
    }
}

// ---------------------------------------------------------------------- bingo
@Serializable enum class BingoTheme { NUMBERS, TIMES, WORDS;
    fun label(l: AppLanguage) = l.text(listOf("Números 1–75", "Tabuada", "Palavras")[ordinal], listOf("Numbers 1–75", "Times tables", "Words")[ordinal])
}
/** One card plus the caller pool, so the caller sheet can be printed from any card. */
@Serializable data class BingoCard(
    val theme: BingoTheme, val size: Int, val cells: List<String>, val pool: List<String>, val calls: List<String>,
    val seed: Long, val language: AppLanguage
)

object BingoGenerator {
    const val FREE = "★"
    val defaultWords = mapOf(
        AppLanguage.PT_BR to "ÁTOMO,PLANETA,ENERGIA,CÉLULA,ROBÔ,SENSOR,FRAÇÃO,ÂNGULO,ÓRBITA,COMETA,FORÇA,MASSA,CLIMA,SOLAR,ÁGUA,LUZ,SOM,ÍMÃ,VULCÃO,FÓSSIL,VÍRUS,DADOS,CÓDIGO,FUNÇÃO,LOOP,MAPA,ESCALA,ÁREA,VOLUME,TEMPO",
        AppLanguage.EN to "ATOM,PLANET,ENERGY,CELL,ROBOT,SENSOR,FRACTION,ANGLE,ORBIT,COMET,FORCE,MASS,CLIMATE,SOLAR,WATER,LIGHT,SOUND,MAGNET,VOLCANO,FOSSIL,VIRUS,DATA,CODE,FUNCTION,LOOP,MAP,SCALE,AREA,VOLUME,TIME"
    )
    fun words(text: String) = text.split(',', '\n', ';').map { it.trim().uppercase().take(14) }.filter { it.isNotBlank() }.distinct()

    fun generate(theme: BingoTheme, wordText: String, seed: Long, language: AppLanguage): BingoCard {
        val r = Random(seed)
        return when (theme) {
            BingoTheme.NUMBERS -> {
                val letters = "BINGO"
                val columns = (0 until 5).map { c -> (c * 15 + 1..c * 15 + 15).shuffled(r).take(5) }
                val cells = (0 until 25).map { i -> if (i == 12) FREE else columns[i % 5][i / 5].toString() }
                val pool = (1..75).map { it.toString() }
                BingoCard(theme, 5, cells, pool, pool.map { "${letters[(it.toInt() - 1) / 15]}-$it" }, seed, language)
            }
            BingoTheme.TIMES -> {
                val facts = (2..9).flatMap { a -> (a..9).map { b -> a * b to "$a × $b" } }.groupBy({ it.first }, { it.second }).toSortedMap()
                val pool = facts.keys.map { it.toString() }
                val calls = facts.values.map { it.first() }
                val picks = pool.shuffled(r).take(24).toMutableList(); picks.add(12, FREE)
                BingoCard(theme, 5, picks, pool, calls, seed, language)
            }
            BingoTheme.WORDS -> {
                val pool = words(wordText)
                require(pool.size >= 9) { language.text("Use pelo menos 9 palavras diferentes.", "Use at least 9 different words.") }
                val size = if (pool.size >= 24) 5 else 3
                val picks = pool.shuffled(r).take(if (size == 5) 24 else 9).toMutableList()
                if (size == 5) picks.add(12, FREE)
                BingoCard(theme, size, picks, pool, pool, seed, language)
            }
        }
    }
}

// ---------------------------------------------------------------------- share codes
/**
 * Short codes such as GM0-7K2QX9 rebuild an identical print on any phone:
 * tool letter, difficulty (E/M/H), one option character, then a 30-bit seed in Crockford base32.
 */
object ShareCode {
    const val SEED_LIMIT = 1L shl 30
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    data class Spec(val tool: String, val difficulty: Difficulty, val option: Int, val seed: Long)
    val tools = linkedMapOf("Golf" to 'G', "Galaxy" to 'X', "Dungeon" to 'D', "Maze" to 'L', "Nonogram" to 'N',
        "Sudoku" to 'S', "Kakooma" to 'K', "Connect the Dots" to 'P', "Bingo" to 'B')
    fun newSeed(random: Random = Random.Default) = random.nextLong(SEED_LIMIT)
    fun encode(spec: Spec): String? {
        val letter = tools[spec.tool] ?: return null
        if (spec.seed !in 0 until SEED_LIMIT || spec.option !in 0..35) return null
        val seed = (0 until 6).map { ALPHABET[((spec.seed shr (5 * (5 - it))) and 31).toInt()] }.joinToString("")
        return "$letter${"EMH"[spec.difficulty.ordinal]}${spec.option.toString(36).uppercase()}-$seed"
    }
    fun decode(text: String): Spec? {
        val clean = text.uppercase().filter { it.isLetterOrDigit() }
        if (clean.length != 9) return null
        val tool = tools.entries.firstOrNull { it.value == clean[0] }?.key ?: return null
        val difficulty = when (clean[1]) { 'E' -> Difficulty.EASY; 'M' -> Difficulty.MEDIUM; 'H' -> Difficulty.HARD; else -> return null }
        val option = clean[2].digitToIntOrNull(36) ?: return null
        var seed = 0L
        for (ch in clean.substring(3)) {
            val normalized = when (ch) { 'O' -> '0'; 'I', 'L' -> '1'; else -> ch }
            val v = ALPHABET.indexOf(normalized); if (v < 0) return null
            seed = seed * 32 + v
        }
        return Spec(tool, difficulty, option, seed)
    }
    /** Everyone gets the same daily seed for a tool, with no server involved. */
    fun dailySeed(tool: String, date: LocalDate): Long {
        var h = 1125899906842597L
        "$tool|$date".forEach { h = 31 * h + it.code }
        return Math.floorMod(mixSeed(h, 77), SEED_LIMIT)
    }
}
