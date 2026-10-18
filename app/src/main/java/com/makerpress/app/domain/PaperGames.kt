package com.makerpress.app.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.math.abs

const val DEFAULT_UPDATE_URL = "https://github.com/leomelchior21/makerpress/releases/latest/download/latest.json"
@Serializable enum class AppLanguage { PT_BR, EN;
    fun text(pt: String, en: String) = if (this == PT_BR) pt else en
}
@Serializable enum class PaperGame { GOLF, GALAXY }
@Serializable data class GridPoint(val x: Int, val y: Int)
@Serializable data class PaperLevel(
    val game: PaperGame, val seed: Long, val difficulty: Difficulty,
    val columns: Int, val rows: Int, val cells: List<Int>,
    val route: List<GridPoint>, val start: GridPoint, val goal: GridPoint,
    val target: Int, val generatorVersion: Int = 1
)

/** 0=open, 1=tree/asteroid, 2=sand/enemy, 3=water/crystal.
 * Reserve a connected route before placing hazards. Galaxy's route contains its crystals.
 * Frozen cells are stored in history; rendering never calls the generator.
 */
object PaperGameGenerator {
    fun generate(game: PaperGame, difficulty: Difficulty, seed: Long): PaperLevel {
        val random = Random(seed); val cols = 13; val rows = 25
        val route = mutableListOf<GridPoint>()
        var x = random.nextInt(3, cols - 3)
        for (y in rows - 1 downTo 0) {
            route += GridPoint(x, y)
            if (y > 0 && y < rows - 1 && y % 3 == 0) {
                val next = (x + random.nextInt(-3, 4)).coerceIn(1, cols - 2)
                while (x != next) { x += if (next > x) 1 else -1; route += GridPoint(x, y) }
            }
        }
        val safe = route.toSet()
        val cells = MutableList(cols * rows) { 0 }
        // Clustered terrain with clean outlines on narrow thermal paper.
        repeat(18 + difficulty.ordinal * 8) {
            val cx = random.nextInt(cols); val cy = random.nextInt(1, rows - 1)
            val terrain = random.nextInt(1, 4)
            for (dy in -1..1) for (dx in -1..1) {
                val p = GridPoint(cx + dx, cy + dy)
                if (p.x in 0 until cols && p.y in 0 until rows && p !in safe && abs(dx) + abs(dy) <= 1) {
                    cells[p.y * cols + p.x] = if (game == PaperGame.GALAXY) 1 else terrain
                }
            }
        }
        if (game == PaperGame.GALAXY) {
            for (row in listOf(5, 12, 19)) {
                val p = route.first { it.y == row }; cells[p.y * cols + p.x] = 3
            }
            val open = cells.indices.filter { cells[it] == 0 && GridPoint(it % cols, it / cols) !in safe }.shuffled(random)
            open.take(5 + difficulty.ordinal * 3).forEach { cells[it] = 2 }
        }
        return PaperLevel(game, seed, difficulty, cols, rows, cells, route, route.first(), route.last(),
            if (game == PaperGame.GOLF) 8 + difficulty.ordinal * 2 else 3)
    }

    fun rules(game: PaperGame, language: AppLanguage): String = when (game) {
        PaperGame.GOLF -> language.text(
            "Lápis + dado de 6 lados. Comece em S. A cada tacada, role e avance de 1 até o valor do dado em linha reta, horizontal, vertical ou diagonal. Não atravesse árvores (triângulos) nem saia da grade. Água (~): volte ao ponto anterior e some 1 tacada. Areia (hachuras): pare ao entrar. Alcance H; não precisa usar todo o dado. Conte as tacadas; menos é melhor. Meta sugerida, não par certificado.",
            "Pencil + six-sided die. Start at S. Each stroke: roll and move 1 to the rolled number of dots in a straight horizontal, vertical or diagonal line. Do not cross trees (triangles) or leave the grid. Entering water (~): return to your previous point and add 1 stroke. Sand (hatching): stop on entry. Reach H; you may use less than the roll. Count strokes; fewer is better. Suggested target, not a certified par.")
        PaperGame.GALAXY -> language.text(
            "Lápis + dado de 6 lados. Comece em S com 3 escudos. Role e avance de 1 até o valor do dado; cada passo vai a um ponto vizinho na horizontal ou vertical, podendo virar. Não atravesse asteroides (hexágonos). Ao entrar num inimigo (triângulo), risque-o e perca 1 escudo; ele não ataca novamente. Recolha os 3 cristais (losangos), riscando cada um. Vença chegando a H com pelo menos 1 escudo. Com 0 escudos, perdeu. Conte os turnos; menos é melhor.",
            "Pencil + six-sided die. Start at S with 3 shields. Roll and move 1 to the rolled number of steps; each step goes to a horizontal or vertical neighbor and you may turn. Do not cross asteroids (hexagons). Entering an enemy (triangle) crosses it out and costs 1 shield; it never attacks again. Collect all 3 crystals (diamonds), crossing each out. Win by reaching H with at least 1 shield. At 0 shields you lose. Count turns; fewer is better.")
    }
}
