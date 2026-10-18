package com.makerpress.app

import com.makerpress.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class GeneratorsTest {
    @Test fun sudokuHasOneSolutionAndReproduces() {
        for (size in listOf(4, 9)) for (difficulty in Difficulty.entries) for (seed in 0L..4L) {
            val puzzle = SudokuGenerator.generate(size, difficulty, seed)
            assertEquals(1, SudokuGenerator.countSolutions(puzzle.cells, size))
            assertEquals(1, SudokuGenerator.countSolutions(puzzle.solution, size))
            assertTrue(puzzle.cells.indices.all { puzzle.cells[it] == 0 || puzzle.cells[it] == puzzle.solution[it] })
            assertEquals(puzzle, SudokuGenerator.generate(size, difficulty, seed))
        }
    }
    @Test fun invalidSudokuIsRejected() { assertEquals(0, SudokuGenerator.countSolutions(listOf(1, 1) + List(14) { 0 }, 4)); assertEquals(2, SudokuGenerator.countSolutions(List(16) { 0 }, 4)) }
    @Test fun wordsMatchEveryRecordedCoordinate() {
        for (size in listOf(10, 12)) for (seed in 1L..20L) {
            val p = WordSearchGenerator.generate(listOf("coração", "água", "árvore", "ação"), size, true, true, seed)
            assertEquals(4, p.words.size)
            p.words.forEach { word -> word.word.forEachIndexed { i, letter ->
                val row = word.row + i * word.dr; val col = word.col + i * word.dc
                assertTrue(row in 0 until size && col in 0 until size)
                assertEquals(letter.toString(), p.cells[row * size + col])
            } }
            assertEquals(p, WordSearchGenerator.generate(listOf("coração", "água", "árvore", "ação"), size, true, true, seed))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun longWordRejected() { WordSearchGenerator.generate(listOf("ABCDEFGHIJKLMNOP"), 10, false, false, 1) }
    @Test fun mathAnswersAndRules() {
        for (difficulty in Difficulty.entries) for (op in Operation.entries) for (seed in 1L..30L) {
            val p = MathGenerator.generate(op, difficulty, 15, seed)
            assertEquals(15, p.questions.distinct().size)
            assertEquals(p, MathGenerator.generate(op, difficulty, 15, seed))
            p.questions.forEach { q ->
                val expected = when (q.operation) { Operation.ADD -> q.a + q.b; Operation.SUBTRACT -> q.a - q.b; Operation.MULTIPLY -> q.a * q.b; Operation.DIVIDE -> { assertTrue(q.b != 0); assertEquals(0, q.a % q.b); q.a / q.b }; else -> error("mixed") }
                assertEquals(expected, q.answer)
                if (difficulty == Difficulty.EASY) assertTrue(q.answer >= 0)
            }
        }
    }
}
