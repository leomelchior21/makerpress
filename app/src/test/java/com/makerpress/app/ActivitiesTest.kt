package com.makerpress.app

import com.makerpress.app.domain.*
import com.makerpress.app.data.DocumentJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ActivitiesTest {
    @Test fun numberBondsHaveExactlyOneEquationAndDotsStayPrintable() {
        for(d in Difficulty.entries) for(seed in 0L..99L) {
            val p=KakoomaGenerator.generate(d,15,seed,AppLanguage.EN)
            p.groups.forEach { g ->
                assertEquals(g.numbers.size,g.numbers.distinct().size)
                val solutions=KakoomaGenerator.solutions(g.numbers)
                assertEquals(1,solutions.size)
                assertEquals(g.sum,g.numbers[solutions.single().third])
                assertEquals(g.sum,g.first+g.second)
            }
            for(subject in DotSubject.entries) {
                val dots=DotGenerator.generate(subject,d,seed,AppLanguage.EN)
                assertEquals(dots,DotGenerator.generate(dots.requestedSubject,d,seed,AppLanguage.EN))
                assertTrue(dots.subject!=DotSubject.SURPRISE)
                assertTrue(dots.points.size in 10..65)
                assertEquals(dots.points.size,dots.points.distinct().size)
                assertTrue(dots.points.all { it.x in 30f..330f && it.y in 20f..440f })
            }
        }
    }
    private fun reach(level: PaperLevel, from: GridPoint, passable: (Int) -> Boolean): Set<GridPoint> {
        val visited = mutableSetOf(from); val todo = ArrayDeque(listOf(from))
        while (todo.isNotEmpty()) {
            val p = todo.removeFirst()
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val q = GridPoint(p.x + dx, p.y + dy)
                if (q.x !in 0 until level.columns || q.y !in 0 until level.rows) continue
                if (passable(level.cells[q.y * level.columns + q.x]) && visited.add(q)) todo.add(q)
            }
        }
        return visited
    }
    @Test fun golfHolesArePlayableWithCalibratedPar() {
        for (difficulty in Difficulty.entries) for (seed in 0L..39L) for (rows in PaperGameGenerator.golfLengths) {
            val level = PaperGameGenerator.generate(PaperGame.GOLF, difficulty, seed, rows)
            assertEquals(level, PaperGameGenerator.generate(PaperGame.GOLF, difficulty, seed, rows))
            assertEquals(2, level.generatorVersion); assertEquals(rows, level.rows); assertEquals(rows * level.columns, level.cells.size)
            assertEquals(GolfCell.FAIRWAY, level.cells[level.start.y * level.columns + level.start.x])
            assertEquals(GolfCell.GREEN, level.cells[level.goal.y * level.columns + level.goal.x])
            assertTrue("seed $seed: ${level.expected}", level.expected in 1.5..24.9)
            assertTrue(kotlin.math.abs(kotlin.math.max(2, kotlin.math.round(level.expected).toInt()) - level.target) <= 1)
            assertTrue(level.cells.any { it == GolfCell.FAIRWAY } && level.cells.count { it == GolfCell.GREEN } >= 5)
        }
        assertEquals(100, (0L..99L).map { PaperGameGenerator.generate(PaperGame.GOLF, Difficulty.MEDIUM, it).cells }.distinct().size)
        // Longer holes need more strokes on average.
        val pars = PaperGameGenerator.golfLengths.map { rows -> (0L..19L).map { PaperGameGenerator.generate(PaperGame.GOLF, Difficulty.MEDIUM, it, rows).expected }.average() }
        assertTrue(pars.toString(), pars[0] < pars[1] && pars[1] < pars[2])
    }
    @Test fun galaxyAndDungeonAreSolvable() {
        for (difficulty in Difficulty.entries) for (seed in 0L..149L) {
            val galaxy = PaperGameGenerator.generate(PaperGame.GALAXY, difficulty, seed)
            assertEquals(galaxy, PaperGameGenerator.generate(PaperGame.GALAXY, difficulty, seed))
            val open = reach(galaxy, galaxy.start) { it != GalaxyCell.ASTEROID && it != GalaxyCell.ENEMY }
            assertTrue(galaxy.goal in open)
            assertEquals(3, galaxy.cells.count { it == GalaxyCell.CRYSTAL })
            galaxy.cells.forEachIndexed { i, v -> if (v == GalaxyCell.CRYSTAL) assertTrue(GridPoint(i % galaxy.columns, i / galaxy.columns) in open) }
            assertTrue(galaxy.target in 4..30)

            val dungeon = PaperGameGenerator.generate(PaperGame.DUNGEON, difficulty, seed)
            assertEquals(dungeon, PaperGameGenerator.generate(PaperGame.DUNGEON, difficulty, seed))
            assertEquals(1, dungeon.cells.count { it == DungeonCell.KEY }); assertEquals(1, dungeon.cells.count { it == DungeonCell.DOOR })
            val closed = reach(dungeon, dungeon.start) { it != DungeonCell.WALL && it != DungeonCell.DOOR }
            val keyAt = dungeon.cells.indexOf(DungeonCell.KEY)
            assertTrue("key reachable before the door", GridPoint(keyAt % dungeon.columns, keyAt / dungeon.columns) in closed)
            assertFalse("the door guards the stairs", dungeon.goal in closed)
            assertTrue(dungeon.goal in reach(dungeon, dungeon.start) { it != DungeonCell.WALL })
            dungeon.cells.forEachIndexed { i, v -> if (v == DungeonCell.MONSTER) assertTrue(dungeon.values[i] in 2..6) }
            assertEquals(dungeon.target, dungeon.cells.count { it == DungeonCell.CHEST })
        }
    }
    @Test fun legacyLevelsStillDecode() {
        val legacy = """{"game":"GOLF","seed":5,"difficulty":"MEDIUM","columns":2,"rows":2,"cells":[0,1,2,3],"route":[],"start":{"x":0,"y":1},"goal":{"x":0,"y":0},"target":8}"""
        val level = DocumentJson.decodeFromString<PaperLevel>(legacy)
        assertEquals(1, level.generatorVersion); assertEquals(emptyList<Int>(), level.values)
    }
    @Test fun graphEquationsAndMarkedRootsAreConsistent() {
        for(seed in 0L..99L) for(topic in listOf(StudyTopic.LINEAR,StudyTopic.QUADRATIC)) {
            val sheet=StudyGenerator.generate(topic,Difficulty.HARD,5,seed,AppLanguage.EN)
            sheet.questions.forEachIndexed { i,q ->
                val f=q.figure!!; assertEquals(i%2==1,f.plotted)
                val (a,b,c)=f.values
                if(topic==StudyTopic.QUADRATIC) {
                    val delta=b*b-4*a*c; assertTrue(delta>0)
                    val root=(-b+kotlin.math.sqrt(delta))/(2*a)
                    assertEquals(0.0,a*root*root+b*root+c,0.000001)
                    assertEquals(root.toInt().toDouble(),root,0.000001)
                    assertTrue(c in -12.0..12.0); assertTrue("The third point must not duplicate a root", c != 0.0)
                } else { assertEquals(0.0,a,0.0); assertTrue(b!=0.0); assertTrue(b+c in -12.0..12.0) }
                assertFalse(q.prompt.contains(" = ${q.solution}"))
            }
        }
    }
    @Test fun languageAndHistoryRoundTripAndLegacyDefaults() {
        val old=DocumentJson.decodeFromString<AppSettings>("{\"copies\":2}")
        assertEquals(AppLanguage.PT_BR,old.language); assertEquals(DEFAULT_UPDATE_URL,old.manifestUrl)
        for(l in AppLanguage.entries) for(topic in StudyTopic.entries) {
            val s=StudyGenerator.generate(topic,Difficulty.MEDIUM,10,42,l)
            val doc=PrintDocument("Test",listOf(PrintBlock.Study(s),PrintBlock.Game(PaperGameGenerator.generate(PaperGame.GALAXY,Difficulty.EASY,8),l)))
            assertEquals(doc,DocumentJson.decodeFromString<PrintDocument>(DocumentJson.encodeToString(doc)))
            assertEquals(s,StudyGenerator.generate(topic,Difficulty.MEDIUM,10,42,l))
        }
        assertEquals("Preview print",UiStrings.translate("Visualizar impressão",AppLanguage.EN))
        assertEquals("Visualizar impressão",UiStrings.translate("Visualizar impressão",AppLanguage.PT_BR))
    }
}
