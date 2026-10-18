package com.makerpress.app.core.printing

import com.makerpress.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data class Connected(val address: String) : PrinterConnectionState
    data class Error(val message: String) : PrinterConnectionState
}
interface PrinterTransport {
    val state: StateFlow<PrinterConnectionState>
    suspend fun connect(address: String)
    suspend fun disconnect()
    suspend fun write(bytes: ByteArray)
}
class TransmissionException(val uncertain: Boolean, cause: Throwable) : IOException(cause)

object EscPosRasterEncoder {
    fun encode(pixels: BooleanArray, width: Int, height: Int, mode: RasterMode): ByteArray {
        require(width > 0 && width % 8 == 0 && height in 1..16_000 && pixels.size == width * height)
        val out = java.io.ByteArrayOutputStream()
        if (mode == RasterMode.GS_V_0) {
            // Bounded raster bands avoid overflowing small printer buffers.
            for (top in 0 until height step 128) {
                val rows = minOf(128, height - top); val bytes = width / 8
                out.write(byteArrayOf(0x1d, 0x76, 0x30, 0, (bytes % 256).toByte(), (bytes / 256).toByte(), rows.toByte(), 0))
                for (y in top until top + rows) for (x in 0 until width step 8) {
                    var packed = 0
                    for (bit in 0..7) if (pixels[y * width + x + bit]) packed = packed or (0x80 shr bit)
                    out.write(packed)
                }
            }
        } else {
            out.write(byteArrayOf(0x1b, 0x33, 24))
            for (top in 0 until height step 24) {
                out.write(byteArrayOf(0x1b, 0x2a, 33, (width % 256).toByte(), (width / 256).toByte()))
                for (x in 0 until width) for (band in 0..2) {
                    var packed = 0
                    for (bit in 0..7) { val y = top + band * 8 + bit; if (y < height && pixels[y * width + x]) packed = packed or (0x80 shr bit) }
                    out.write(packed)
                }
                out.write(10)
            }
            out.write(byteArrayOf(0x1b, 0x32))
        }
        return out.toByteArray()
    }
}
class EscPosPrintEngine(private val transport: PrinterTransport) {
    private val mutex = Mutex()
    suspend fun send(raster: ByteArray, profile: PrinterProfile, copies: Int, progress: suspend (Int) -> Unit) = mutex.withLock {
        profile.validate(); require(copies in 1..20)
        check(transport.state.value is PrinterConnectionState.Connected) { "Conecte uma impressora antes de imprimir." }
        val data = byteArrayOf(0x1b, 0x40) + raster + byteArrayOf(0x1b, 0x64, profile.feed.toByte())
        var attempted = false; var written = 0
        try {
            repeat(copies) {
                for (offset in data.indices step profile.chunkSize) {
                    currentCoroutineContext().ensureActive()
                    val end = minOf(offset + profile.chunkSize, data.size)
                    attempted = true // write may throw after delivering some bytes.
                    transport.write(data.copyOfRange(offset, end))
                    written += end - offset; progress((written.toLong() * 100 / (data.size.toLong() * copies)).toInt())
                    delay(profile.delayMs)
                }
            }
        } catch (e: Exception) { throw TransmissionException(attempted, e) }
    }
}
