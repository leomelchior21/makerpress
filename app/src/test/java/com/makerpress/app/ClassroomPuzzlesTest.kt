package com.makerpress.app

import com.makerpress.app.data.DocumentJson
import com.makerpress.app.domain.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class ClassroomPuzzlesTest {
    @Test fun mazesArePerfectWithAValidSolution() {
        for (d in Difficulty.entries) for (seed in 0L..59L) {
            val m = MazeGenerator.generate(d, seed, seed % 2 == 0L, AppLanguage.PT_BR)
            assertEquals(m, MazeGenerator.generate(d, seed, seed % 2 == 0L, AppLanguage.PT_BR))
            val n = m.columns * m.rows
            // A perfect maze is a spanning tree: n - 1 passages, every cell reachable.
            var passages = 0
            for (c in 0 until n) { if (c % m.columns < m.columns - 1 && m.walls[c] and MazeGenerator.E == 0) passages++; if (c / m.columns < m.rows - 1 && m.walls[c] and MazeGenerator.S == 0) passages++ }
            assertEquals(n - 1, passages)
            assertEquals(m.entry, m.solution.first()); assertEquals(m.exit, m.solution.last())
            m.solution.zipWithNext().forEach { (a, b) ->
                val bit = when (b - a) { 1 -> MazeGenerator.E; -1 -> MazeGenerator.W; m.columns -> MazeGenerator.S; else -> MazeGenerator.N }
                assertEquals(0, m.walls[a] and bit)
            }
            assertTrue(m.stars.none { it in m.solution })
            if (seed % 2 == 0L) assertTrue(m.stars.isNotEmpty()) else assertTrue(m.stars.isEmpty())
        }
    }

    @Test fun everyNonogramPictureHasOneLogicalSolution() {
        for ((name, motif) in NonogramGenerator.small + NonogramGenerator.large) {
            val rows = motif.split('|'); val size = rows.size
            assertTrue(name, rows.all { it.length == size } && size in listOf(10, 15))
            val cells = NonogramGenerator.grid(motif)
            val rc = (0 until size).map { r -> NonogramGenerator.clue((0 until size).map { cells[r * size + it] }) }
            val cc = (0 until size).map { c -> NonogramGenerator.clue((0 until size).map { cells[it * size + c] }) }
            assertEquals(name, cells, NonogramGenerator.solve(size, rc, cc))
            assertFalse(NonogramGenerator.label(name, AppLanguage.PT_BR) == name)
        }
        for (d in Difficulty.entries) for (seed in 0L..39L) {
            val p = NonogramGenerator.generate(d, seed, AppLanguage.EN)
            assertEquals(if (d == Difficulty.HARD) 15 else 10, p.size)
            assertEquals(p.cells, NonogramGenerator.solve(p.size, p.rowClues(), p.columnClues()))
            assertTrue(p.given.all { p.cells[it] }); assertEquals(d == Difficulty.EASY, p.given.isNotEmpty())
        }
    }

    @Test fun bingoCardsFollowTheirThemes() {
        val cards = (0L..39L).map { BingoGenerator.generate(BingoTheme.NUMBERS, "", it, AppLanguage.PT_BR) }
        assertEquals(40, cards.map { it.cells }.distinct().size)
        cards.forEach { card ->
            assertEquals(25, card.cells.size); assertEquals(BingoGenerator.FREE, card.cells[12]); assertEquals(75, card.pool.size)
            card.cells.forEachIndexed { i, v -> if (i != 12) assertTrue(v.toInt() in (i % 5) * 15 + 1..(i % 5) * 15 + 15) }
            assertEquals(24, card.cells.filter { it != BingoGenerator.FREE }.distinct().size)
        }
        val times = BingoGenerator.generate(BingoTheme.TIMES, "", 3, AppLanguage.EN)
        assertEquals(24, times.cells.filter { it != BingoGenerator.FREE }.distinct().size)
        times.pool.indices.forEach { i -> val (a, b) = times.calls[i].split(" × ").map { it.toInt() }; assertEquals(times.pool[i].toInt(), a * b) }
        val words = BingoGenerator.generate(BingoTheme.WORDS, BingoGenerator.defaultWords.getValue(AppLanguage.PT_BR), 9, AppLanguage.PT_BR)
        assertEquals(5, words.size)
        assertEquals(3, BingoGenerator.generate(BingoTheme.WORDS, "A,B,C,D,E,F,G,H,I,J", 1, AppLanguage.EN).size)
        assertThrows(IllegalArgumentException::class.java) { BingoGenerator.generate(BingoTheme.WORDS, "A,B,C", 1, AppLanguage.EN) }
    }

    @Test fun shareCodesRoundTripAndNormalize() {
        val r = Random(4)
        for (tool in ShareCode.tools.keys) for (d in Difficulty.entries) repeat(20) {
            val spec = ShareCode.Spec(tool, d, r.nextInt(0, 7), ShareCode.newSeed(r))
            val code = ShareCode.encode(spec)!!
            assertTrue(code, Regex("[A-Z][EMH][0-9A-Z]-[0-9A-Z]{6}").matches(code))
            assertEquals(spec, ShareCode.decode(code))
            assertEquals(spec, ShareCode.decode(code.lowercase().replace("-", " ")))
        }
        val spec = ShareCode.Spec("Golf", Difficulty.MEDIUM, 0, 0)
        assertEquals("GM0-000000", ShareCode.encode(spec)); assertEquals(spec, ShareCode.decode("gm0-ooo0oo"))
        assertNull(ShareCode.encode(spec.copy(seed = ShareCode.SEED_LIMIT)))
        assertNull(ShareCode.encode(spec.copy(tool = "Text Studio")))
        listOf("", "GM0", "QM0-000000", "GZ0-000000", "GM0-00000U").forEach { assertNull(it, ShareCode.decode(it)) }
        val day = LocalDate.of(2026, 11, 17)
        assertEquals(ShareCode.dailySeed("Golf", day), ShareCode.dailySeed("Golf", day))
        assertNotEquals(ShareCode.dailySeed("Golf", day), ShareCode.dailySeed("Golf", day.plusDays(1)))
        assertNotEquals(ShareCode.dailySeed("Golf", day), ShareCode.dailySeed("Maze", day))
        assertTrue(ShareCode.dailySeed("Golf", day) in 0 until ShareCode.SEED_LIMIT)
    }

    @Test fun coursesAndNewBlocksSerialize() {
        for (holes in listOf(3, 6, 9)) {
            val course = PaperGameGenerator.course(Difficulty.MEDIUM, 77, holes, 3)
            assertEquals(holes, course.holes.size); assertEquals(course.holes.sumOf { it.target }, course.par)
            assertEquals(course, PaperGameGenerator.course(Difficulty.MEDIUM, 77, holes, 3))
            assertTrue(course.holes.map { it.rows }.toSet() == PaperGameGenerator.golfLengths.toSet())
        }
        val doc = PrintDocument("All", listOf(
            PrintBlock.Course(PaperGameGenerator.course(Difficulty.EASY, 1, 3, 2), AppLanguage.EN),
            PrintBlock.Game(PaperGameGenerator.generate(PaperGame.DUNGEON, Difficulty.HARD, 2)),
            PrintBlock.Maze(MazeGenerator.generate(Difficulty.EASY, 3, true, AppLanguage.EN), true),
            PrintBlock.Nonogram(NonogramGenerator.generate(Difficulty.HARD, 4, AppLanguage.PT_BR)),
            PrintBlock.Bingo(BingoGenerator.generate(BingoTheme.TIMES, "", 5, AppLanguage.EN), true),
            PrintBlock.Study(StudyGenerator.generate(StudyTopic.MIXED, Difficulty.EASY, 5, 6, AppLanguage.EN), true, true)))
        assertEquals(doc, DocumentJson.decodeFromString<PrintDocument>(DocumentJson.encodeToString(doc)))
        assertTrue(doc.blocks.drop(2).all { it.hasAnswers() })
        assertTrue(doc.blocks.drop(2).all { it.withAnswers(true).showsAnswers() && !it.withAnswers(false).showsAnswers() })
        assertEquals(PrinterProfile(), DocumentJson.decodeFromString<PrinterProfile>("{\"width\":384}"))
        assertThrows(IllegalArgumentException::class.java) { PrinterProfile(tone = 2).validate() }
    }
}
