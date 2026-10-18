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
    private fun checkBitmap(b:Bitmap) {
        assertEquals(384,b.width); assertTrue(b.height in 100..16000)
        for(y in 0 until b.height step 7) for(x in 0 until b.width step 3) assertTrue(b.getPixel(x,y) in listOf(Color.BLACK,Color.WHITE))
    }
}
