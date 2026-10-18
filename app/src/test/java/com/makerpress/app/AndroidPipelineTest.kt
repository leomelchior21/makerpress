package com.makerpress.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.makerpress.app.core.rendering.*
import com.makerpress.app.core.printing.*
import com.makerpress.app.data.*
import com.makerpress.app.domain.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidPipelineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun rendererWrapsAndProducesExactMonochromeWidth() {
        val renderer = PrintDocumentRenderer(context)
        val short = renderer.render(PrintDocument("test", listOf(PrintBlock.Text("Olá"))), PrinterProfile())
        val long = renderer.render(PrintDocument("test", listOf(PrintBlock.Text("Criação, ação e coração. ".repeat(30)))), PrinterProfile())
        assertEquals(384, long.width); assertTrue(long.height > short.height)
        for (y in 0 until long.height) for (x in 0 until long.width) assertTrue(long.getPixel(x, y) in listOf(Color.BLACK, Color.WHITE))
    }
    @Test fun renderedQrDecodes() {
        val value = "https://makerpress.test/ação"
        val b = PrintDocumentRenderer(context).render(PrintDocument("QR", listOf(PrintBlock.Qr(value))), PrinterProfile())
        val pixels = IntArray(b.width * b.height); b.getPixels(pixels, 0, b.width, 0, 0, b.width, b.height)
        assertEquals(value, MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(b.width, b.height, pixels)))).text)
    }
    @Test fun allDithersCompositeTransparencyOnWhite() {
        val src = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888); src.eraseColor(Color.TRANSPARENT)
        for (mode in Dither.entries) { val out = BitmapProcessor.monochrome(src, mode); assertEquals(Color.WHITE, out.getPixel(3, 3)) }
    }
    @Test fun imagesPreserveAspectRatioAndRotateWithoutClipping() {
        val name = "${java.util.UUID.randomUUID()}.png"
        val file = java.io.File(context.filesDir, "images/$name").apply { parentFile?.mkdirs() }
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        try {
            val renderer = PrintDocumentRenderer(context)
            val normal = renderer.render(PrintDocument("image", listOf(PrintBlock.Image(name))), PrinterProfile())
            val rotated = renderer.render(PrintDocument("image", listOf(PrintBlock.Image(name, rotation = 90))), PrinterProfile())
            assertEquals(180 + 24 + 8, normal.height)
            assertEquals(720 + 24 + 8, rotated.height)
            assertEquals(Color.WHITE, normal.getPixel(0, 30)); assertEquals(Color.BLACK, normal.getPixel(12, 30))
        } finally { file.delete() }
    }
    @Test fun persistedDocumentsQueueOrderAndRecovery() = runTest {
        val name = "test-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, MakerDatabase::class.java, name).allowMainThreadQueries().addMigrations(MakerDatabase.MIGRATION_1_2).build()
        val repo = JobRepository(db.jobs())
        val doc = PrintDocument("Sudoku", listOf(PrintBlock.Sudoku(SudokuGenerator.generate(4, Difficulty.EASY, 7))))
        val first = repo.save(doc, PrinterProfile(), 1, true); val second = repo.save(doc, PrinterProfile(), 2, true)
        assertEquals(first, db.jobs().next()!!.id)
        assertEquals(1, db.jobs().claim(first)); assertEquals(0, db.jobs().claim(first))
        db.jobs().status(first, "SENDING", 20, "")
        db.close()
        db = Room.databaseBuilder(context, MakerDatabase::class.java, name).allowMainThreadQueries().addMigrations(MakerDatabase.MIGRATION_1_2).build()
        db.jobs().recoverSending(); db.jobs().recoverRendering()
        assertEquals(second, db.jobs().next()!!.id)
        assertEquals(doc, JobRepository(db.jobs()).document(db.jobs().next()!!))
        db.jobs().cancel(second); assertNull(db.jobs().next())
        db.close(); context.deleteDatabase(name)
    }
    @Test fun queueStopsAfterPartialFailureWithoutRetryingNextJob() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, MakerDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = JobRepository(db.jobs()); val fake = FakePrinterTransport()
            val doc = PrintDocument("Test", listOf(PrintBlock.Text("HELLO")))
            val first = repo.save(doc, PrinterProfile(chunkSize = 64, delayMs = 0), 1, true)
            val second = repo.save(doc, PrinterProfile(delayMs = 0), 1, true)
            fake.connect("test"); fake.failAt = 1
            PrintQueueManager(repo, PrintDocumentRenderer(context), EscPosPrintEngine(fake), fake).run()
            val rows = db.jobs().observe().first()
            assertEquals("UNKNOWN", rows.first { it.id == first }.state)
            assertEquals("PENDING", rows.first { it.id == second }.state)
            assertEquals(1, fake.writes.size)
            assertTrue(fake.state.value is PrinterConnectionState.Disconnected)
        } finally { db.close() }
    }
    @Test fun explicitMigrationPreservesOldRows() = runTest {
        val name = "migration-${System.nanoTime()}.db"
        context.openOrCreateDatabase(name, 0, null).use { db ->
            db.execSQL("CREATE TABLE print_jobs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, documentJson TEXT NOT NULL, profileJson TEXT NOT NULL, copies INTEGER NOT NULL, createdAt INTEGER NOT NULL, state TEXT NOT NULL, progress INTEGER NOT NULL)")
            db.execSQL("INSERT INTO print_jobs VALUES(1, 'Preservado', '{}', '{}', 1, 100, 'SAVED', 0)")
            db.version = 1
        }
        val db = Room.databaseBuilder(context, MakerDatabase::class.java, name).allowMainThreadQueries().addMigrations(MakerDatabase.MIGRATION_1_2).build()
        try { val row = db.jobs().observe().first().single(); assertEquals("Preservado", row.title); assertEquals("", row.message) }
        finally { db.close(); context.deleteDatabase(name) }
    }
}
