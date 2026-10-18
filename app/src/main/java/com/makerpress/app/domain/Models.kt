package com.makerpress.app.domain

import kotlinx.serialization.Serializable

@Serializable enum class Alignment { LEFT, CENTER, RIGHT }
@Serializable enum class Dither { THRESHOLD, FLOYD_STEINBERG, ATKINSON }
@Serializable enum class RasterMode { GS_V_0, ESC_STAR }
@Serializable enum class Difficulty { EASY, MEDIUM, HARD }
@Serializable enum class Operation { ADD, SUBTRACT, MULTIPLY, DIVIDE, MIXED }
@Serializable data class PrinterProfile(
    val width: Int = 384, val chunkSize: Int = 512, val delayMs: Long = 30,
    val feed: Int = 3, val margin: Int = 12, val mode: RasterMode = RasterMode.GS_V_0
) {
    fun validate() {
        require(width in 192..832 && width % 8 == 0) { "Largura: 192 a 832 pontos, múltiplo de 8." }
        require(chunkSize in 64..4096 && delayMs in 0..500 && feed in 0..10 && margin in 0..32)
    }
}
@Serializable data class AppSettings(
    val printerAddress: String = "", val profile: PrinterProfile = PrinterProfile(),
    val theme: String = "system", val copies: Int = 1, val dither: Dither = Dither.FLOYD_STEINBERG,
    val manifestUrl: String = DEFAULT_UPDATE_URL, val lastUpdateCheck: Long = 0,
    val language: AppLanguage = AppLanguage.PT_BR
)
@Serializable data class SudokuPuzzle(val size: Int, val cells: List<Int>, val solution: List<Int>, val seed: Long, val difficulty: Difficulty, val logicalSingles: Int)
@Serializable data class WordPlacement(val word: String, val row: Int, val col: Int, val dr: Int, val dc: Int)
@Serializable data class WordSearchPuzzle(val size: Int, val cells: List<String>, val words: List<WordPlacement>, val seed: Long)
@Serializable data class MathQuestion(val a: Int, val b: Int, val operation: Operation, val answer: Int)
@Serializable data class MathChallenge(val questions: List<MathQuestion>, val seed: Long)
@Serializable sealed class PrintBlock {
    @Serializable data class Text(val value: String, val size: Float = 24f, val bold: Boolean = false, val alignment: Alignment = Alignment.LEFT, val spacing: Int = 8) : PrintBlock()
    @Serializable data class Rule(val spacing: Int = 12) : PrintBlock()
    @Serializable data class Image(val asset: String, val rotation: Int = 0, val zoom: Float = 1f, val cropX: Float = .5f, val cropY: Float = .5f, val brightness: Float = 0f, val contrast: Float = 1f, val dither: Dither = Dither.FLOYD_STEINBERG) : PrintBlock()
    @Serializable data class Sudoku(val puzzle: SudokuPuzzle, val answers: Boolean = false) : PrintBlock()
    @Serializable data class WordSearch(val puzzle: WordSearchPuzzle, val answers: Boolean = false) : PrintBlock()
    @Serializable data class Math(val challenge: MathChallenge, val answers: Boolean = false) : PrintBlock()
    @Serializable data class Study(val sheet: StudySheet, val answers: Boolean = false) : PrintBlock()
    @Serializable data class Game(val level: PaperLevel, val language: AppLanguage = AppLanguage.PT_BR) : PrintBlock()
    @Serializable data class Kakooma(val puzzle: KakoomaPuzzle, val answers: Boolean = false) : PrintBlock()
    @Serializable data class Dots(val puzzle: DotPuzzle, val answers: Boolean = false) : PrintBlock()
    @Serializable data class Qr(val value: String, val size: Int = 320) : PrintBlock()
    @Serializable data class Pattern(val kind: String) : PrintBlock()
}
@Serializable data class PrintDocument(val title: String, val blocks: List<PrintBlock>, val createdAt: Long = System.currentTimeMillis())
enum class JobState { SAVED, PENDING, RENDERING, SENDING, SENT, FAILED, CANCELLED, UNKNOWN }
