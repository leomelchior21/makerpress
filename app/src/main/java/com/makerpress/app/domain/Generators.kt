package com.makerpress.app.domain

import java.text.Normalizer
import kotlin.math.sqrt
import kotlin.random.Random

object SudokuGenerator {
    fun countSolutions(cells: List<Int>, size: Int, limit: Int = 2, nodeLimit: Int = 500_000): Int {
        require(size == 4 || size == 9)
        require(cells.size == size * size && cells.all { it in 0..size })
        val board = cells.toIntArray()
        val box = sqrt(size.toDouble()).toInt()
        fun candidates(i: Int) = (1..size).filter { v ->
            (0 until size).none { board[i / size * size + it] == v || board[it * size + i % size] == v } &&
                (0 until size).none { board[(i / size / box * box + it / box) * size + i % size / box * box + it % box] == v }
        }
        for (i in board.indices) if (board[i] != 0) {
            val v = board[i]; board[i] = 0
            if (v !in candidates(i)) return 0
            board[i] = v
        }
        var nodes = 0
        fun solve(): Int {
            check(++nodes < nodeLimit) { "Limite de busca do Sudoku atingido." }
            var index = -1; var options = emptyList<Int>()
            for (i in board.indices) if (board[i] == 0) {
                val c = candidates(i)
                if (c.isEmpty()) return 0
                if (index < 0 || c.size < options.size) { index = i; options = c }
                if (c.size == 1) break
            }
            if (index < 0) return 1
            var count = 0
            for (v in options) {
                board[index] = v; count += solve(); board[index] = 0
                if (count >= limit) return limit
            }
            return count
        }
        return solve()
    }
    fun generate(size: Int, difficulty: Difficulty, seed: Long): SudokuPuzzle {
        require(size == 4 || size == 9)
        val random = Random(seed); val box = sqrt(size.toDouble()).toInt()
        fun axis() = (0 until box).shuffled(random).flatMap { group -> (0 until box).shuffled(random).map { group * box + it } }
        val rows = axis(); val cols = axis(); val digits = (1..size).shuffled(random)
        val solution = rows.flatMap { r -> cols.map { c -> digits[(r * box + r / box + c) % size] } }
        val cells = solution.toMutableList()
        val clues = if (size == 4) listOf(9, 7, 5)[difficulty.ordinal] else listOf(42, 34, 27)[difficulty.ordinal]
        for (index in cells.indices.shuffled(random)) {
            if (cells.count { it != 0 } <= clues) break
            val old = cells[index]; cells[index] = 0
            if (runCatching { countSolutions(cells, size, nodeLimit = 20_000) }.getOrDefault(2) != 1) cells[index] = old
        }
        // Report logical naked-single coverage alongside the clue-removal difficulty heuristic.
        val work = cells.toMutableList(); var singles = 0
        do {
            var changed = false
            for (i in work.indices) if (work[i] == 0) {
                val opts = (1..size).filter { v -> (0 until size).none { k ->
                    work[i / size * size + k] == v || work[k * size + i % size] == v ||
                        work[(i / size / box * box + k / box) * size + i % size / box * box + k % box] == v
                } }
                if (opts.size == 1) { work[i] = opts[0]; singles++; changed = true }
            }
        } while (changed)
        return SudokuPuzzle(size, cells, solution, seed, difficulty, singles)
    }
}

object WordSearchGenerator {
    val categories = linkedMapOf(
        "Animais" to "GATO,CACHORRO,TIGRE,LEAO,ZEBRA,PANDA",
        "Espaço" to "LUA,SOL,PLANETA,ESTRELA,COMETA,ORBITA",
        "Tecnologia" to "ROBO,SENSOR,TELA,REDE,DADOS,CHIP",
        "Ciência" to "ATOMO,CELULA,ENERGIA,LUZ,FORCA,MASSA",
        "Sustentabilidade" to "AGUA,RECICLAR,VERDE,CLIMA,SOLAR,TERRA",
        "Esportes" to "BOLA,TENIS,NATACAO,CORRIDA,JUDO,GOL",
        "Matemática" to "SOMA,ANGULO,NUMERO,FRACAO,RAIZ,AREA",
        "Programação" to "CODIGO,FUNCAO,CLASSE,LOOP,LISTA,TESTE"
    )
    val english = listOf("CAT,DOG,TIGER,LION,ZEBRA,PANDA", "MOON,SUN,PLANET,STAR,COMET,ORBIT", "ROBOT,SENSOR,SCREEN,NETWORK,DATA,CHIP", "ATOM,CELL,ENERGY,LIGHT,FORCE,MASS", "WATER,RECYCLE,GREEN,CLIMATE,SOLAR,EARTH", "BALL,TENNIS,SWIMMING,RUNNING,JUDO,GOAL", "SUM,ANGLE,NUMBER,FRACTION,ROOT,AREA", "CODE,FUNCTION,CLASS,LOOP,LIST,TEST")
    fun normalize(word: String) = Normalizer.normalize(word.trim().uppercase(java.util.Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    fun generate(words: List<String>, size: Int, diagonal: Boolean, reversed: Boolean, seed: Long): WordSearchPuzzle {
        require(size in listOf(10, 12))
        val normalized = words.map(::normalize).filter { it.isNotBlank() }.distinct().sortedByDescending { it.length }
        require(normalized.isNotEmpty() && normalized.size <= 30) { "Informe de 1 a 30 palavras." }
        require(normalized.all { it.length in 2..size && it.matches(Regex("[A-Z]+")) }) { "Use palavras de 2 a $size letras, sem espaços ou símbolos." }
        val dirs = mutableListOf(0 to 1, 1 to 0)
        if (diagonal) dirs.addAll(listOf(1 to 1, 1 to -1))
        if (reversed) dirs.addAll(dirs.toList().map { -it.first to -it.second })
        val random = Random(seed)
        repeat(80) {
            val grid = CharArray(size * size) { ' ' }; val placed = mutableListOf<WordPlacement>()
            for (word in normalized) {
                val candidates = (0 until size * size).flatMap { i -> dirs.map { d -> WordPlacement(word, i / size, i % size, d.first, d.second) } }.shuffled(random)
                val p = candidates.firstOrNull { p -> word.indices.all { k ->
                    val r = p.row + k * p.dr; val c = p.col + k * p.dc
                    r in 0 until size && c in 0 until size && (grid[r * size + c] == ' ' || grid[r * size + c] == word[k])
                } } ?: break
                word.forEachIndexed { k, letter -> grid[(p.row + k * p.dr) * size + p.col + k * p.dc] = letter }
                placed += p
            }
            if (placed.size == normalized.size) return WordSearchPuzzle(size, grid.map { (if (it == ' ') 'A' + random.nextInt(26) else it).toString() }, placed, seed)
        }
        error("As palavras não cabem. Reduza a lista ou escolha 12 × 12.")
    }
}

object MathGenerator {
    fun generate(operation: Operation, difficulty: Difficulty, count: Int, seed: Long): MathChallenge {
        require(count in listOf(5, 10, 15))
        val random = Random(seed); val max = listOf(10, 50, 100)[difficulty.ordinal]
        val questions = linkedSetOf<MathQuestion>()
        var attempts = 0
        while (questions.size < count && attempts++ < 10_000) {
            val op = if (operation == Operation.MIXED) Operation.entries[random.nextInt(4)] else operation
            var a = random.nextInt(1, max + 1); var b = random.nextInt(1, max + 1)
            if (op == Operation.DIVIDE) { b = random.nextInt(1, if (difficulty == Difficulty.HARD) 21 else 11); a *= b }
            if (op == Operation.SUBTRACT && difficulty == Difficulty.EASY && a < b) { val t = a; a = b; b = t }
            val answer = when (op) { Operation.ADD -> a + b; Operation.SUBTRACT -> a - b; Operation.MULTIPLY -> a * b; Operation.DIVIDE -> a / b; else -> error("Operação inválida") }
            questions += MathQuestion(a, b, op, answer)
        }
        check(questions.size == count)
        return MathChallenge(questions.toList(), seed)
    }
}
