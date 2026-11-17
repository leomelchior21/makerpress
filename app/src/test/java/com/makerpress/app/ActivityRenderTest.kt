package com.makerpress.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.makerpress.app.domain.*
import com.makerpress.app.core.rendering.PrintDocumentRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActivityRenderTest {
    @Test fun printableActivitiesStayWithinLimitsAndExportSamples() {
        val renderer=PrintDocumentRenderer(ApplicationProvider.getApplicationContext<Context>())
        val out=File("build/activity-samples").apply { mkdirs() }
        for(l in AppLanguage.entries) {
            val extras = listOf(PrintBlock.Kakooma(KakoomaGenerator.generate(Difficulty.HARD,5,77,l))) +
                DotSubject.entries.filter { it!=DotSubject.SURPRISE }.flatMap { subject ->
                    val p=DotGenerator.generate(subject,Difficulty.HARD,77,l)
                    listOf(PrintBlock.Dots(p),PrintBlock.Dots(p,true))
                }
            extras.forEachIndexed { i,block ->
                val image=renderer.render(PrintDocument("Extra",listOf(block)),PrinterProfile())
                checkBitmap(image)
                File(out,"extra_${l}_$i.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
            }
            for(game in PaperGame.entries) {
                val doc=PrintDocument("MakerPress $game",listOf(PrintBlock.Game(PaperGameGenerator.generate(game,Difficulty.MEDIUM,1024),l)))
                val image=renderer.render(doc,PrinterProfile())
                checkBitmap(image)
                File(out,"${game}_${l}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
            }
            for(topic in StudyTopic.entries) {
                val s=StudyGenerator.generate(topic,Difficulty.HARD,15,1024,l)
                for(answers in listOf(false,true)) {
                    val image=renderer.render(PrintDocument("Study",listOf(PrintBlock.Study(s,answers))),PrinterProfile())
                    checkBitmap(image)
                    if(topic in listOf(StudyTopic.MIXED,StudyTopic.QUADRATIC,StudyTopic.PHYSICS)) File(out,"${topic}_${l}_$answers.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
                    image.recycle()
                }
            }
        }
    }
    @Test fun newActivitiesRenderAtNativeWidthAndExportSamples() {
        val renderer=PrintDocumentRenderer(ApplicationProvider.getApplicationContext<Context>())
        val out=File("build/activity-samples").apply { mkdirs() }
        fun save(name: String, blocks: List<PrintBlock>, profile: PrinterProfile = PrinterProfile()) {
            val image=renderer.render(PrintDocument(name,blocks),profile)
            checkBitmap(image, profile.width)
            File(out,"$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
        }
        for(l in AppLanguage.entries) {
            for(rows in PaperGameGenerator.golfLengths) save("golf_${rows}_$l", listOf(PrintBlock.Game(PaperGameGenerator.generate(PaperGame.GOLF,Difficulty.MEDIUM,1024,rows),l)))
            save("galaxy_v2_$l", listOf(PrintBlock.Game(PaperGameGenerator.generate(PaperGame.GALAXY,Difficulty.MEDIUM,1024),l)))
            save("dungeon_$l", listOf(PrintBlock.Game(PaperGameGenerator.generate(PaperGame.DUNGEON,Difficulty.MEDIUM,1024),l)))
            save("course_$l", listOf(PrintBlock.Course(PaperGameGenerator.course(Difficulty.MEDIUM,7,3,2),l)))
            val maze=MazeGenerator.generate(Difficulty.MEDIUM,11,true,l)
            save("maze_$l", listOf(PrintBlock.Maze(maze))); save("maze_answer_$l", listOf(PrintBlock.Maze(maze,true)))
            for(d in Difficulty.entries) { val n=NonogramGenerator.generate(d,5,l); save("nonogram_${d}_$l", listOf(PrintBlock.Nonogram(n))); save("nonogram_${d}_answer_$l", listOf(PrintBlock.Nonogram(n,true))) }
            for(theme in BingoTheme.entries) {
                val card=BingoGenerator.generate(theme,BingoGenerator.defaultWords.getValue(l),3,l)
                save("bingo_${theme}_$l", listOf(PrintBlock.Bingo(card))); save("bingo_${theme}_caller_$l", listOf(PrintBlock.Bingo(card,true)))
            }
            save("calibration_$l", listOf(PrintBlock.Pattern(if(l==AppLanguage.EN) "DENSITY_EN" else "DENSITY")))
        }
        // Wider printers, lighter/darker textures and legacy 1.1 boards keep rendering.
        save("golf_wide_dark", listOf(PrintBlock.Game(PaperGameGenerator.generate(PaperGame.GOLF,Difficulty.HARD,9),AppLanguage.EN)), PrinterProfile(width=576, tone=1))
        save("dungeon_light", listOf(PrintBlock.Game(PaperGameGenerator.generate(PaperGame.DUNGEON,Difficulty.EASY,9),AppLanguage.EN)), PrinterProfile(tone=-1))
        val legacyCells = List(13*25) { if (it % 7 == 0) (it / 7) % 4 else 0 }
        save("legacy_golf", listOf(PrintBlock.Game(PaperLevel(PaperGame.GOLF, 3, Difficulty.MEDIUM, 13, 25, legacyCells, emptyList(), GridPoint(6,24), GridPoint(6,0), 10))))
    }
    private fun checkBitmap(b:Bitmap, width: Int = 384) {
        assertEquals(width,b.width); assertTrue(b.height in 100..16000)
        for(y in 0 until b.height step 7) for(x in 0 until b.width step 3) assertTrue(b.getPixel(x,y) in listOf(Color.BLACK,Color.WHITE))
    }
}
