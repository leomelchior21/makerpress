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
    @Test fun levelsHaveReachableGoalsAndCrystalsAcrossSeeds() {
        for(game in PaperGame.entries) for(difficulty in Difficulty.entries) for(seed in 0L..199L) {
            val level = PaperGameGenerator.generate(game, difficulty, seed)
            assertEquals(level, PaperGameGenerator.generate(game, difficulty, seed))
            val visited = mutableSetOf(level.start); val todo = ArrayDeque<GridPoint>(); todo.add(level.start)
            while(todo.isNotEmpty()) {
                val p=todo.removeFirst()
                for((dx,dy) in listOf(1 to 0,-1 to 0,0 to 1,0 to -1)) {
                    val q=GridPoint(p.x+dx,p.y+dy)
                    if(q.x !in 0 until level.columns || q.y !in 0 until level.rows) continue
                    val cell=level.cells[q.y*level.columns+q.x]
                    if((cell==0 || (game==PaperGame.GALAXY && cell==3)) && visited.add(q)) todo.add(q)
                }
            }
            assertTrue(visited.contains(level.goal))
            if(game==PaperGame.GALAXY) {
                assertEquals(3,level.cells.count { it==3 })
                level.cells.forEachIndexed { i,v -> if(v==3) assertTrue(GridPoint(i%level.columns,i/level.columns) in visited) }
            }
            assertEquals(level.rows*level.columns,level.cells.size)
        }
        assertEquals(200,(0L..199L).map { PaperGameGenerator.generate(PaperGame.GOLF,Difficulty.MEDIUM,it).cells }.distinct().size)
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
