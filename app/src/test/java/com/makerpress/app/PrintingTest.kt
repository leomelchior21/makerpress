package com.makerpress.app

import com.makerpress.app.core.printing.*
import com.makerpress.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FakePrinterTransport : PrinterTransport {
    override val state = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val writes = mutableListOf<ByteArray>()
    var failAt = Int.MAX_VALUE
    var failConnect = false
    override suspend fun connect(address: String) {
        state.value = PrinterConnectionState.Connecting
        if (failConnect) { state.value = PrinterConnectionState.Error("Falha simulada"); throw IOException("connect") }
        state.value = PrinterConnectionState.Connected(address)
    }
    override suspend fun disconnect() { state.value = PrinterConnectionState.Disconnected }
    override suspend fun write(bytes: ByteArray) { if (writes.size >= failAt) throw IOException("partial"); writes += bytes }
}
class PrintingTest {
    @Test fun rowPackingUsesMostSignificantBitFirst() {
        val bytes = EscPosRasterEncoder.encode(booleanArrayOf(true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false), 8, 2, RasterMode.GS_V_0)
        assertArrayEquals(byteArrayOf(0x1d, 0x76, 0x30, 0, 1, 0, 2, 0, 0x81.toByte(), 0x40), bytes)
    }
    @Test fun rasterBandsAndBitImageHaveCorrectLengths() {
        assertEquals(2 * 8 + 48 * 129, EscPosRasterEncoder.encode(BooleanArray(384 * 129), 384, 129, RasterMode.GS_V_0).size)
        val bits = EscPosRasterEncoder.encode(BooleanArray(8 * 25) { it == 0 }, 8, 25, RasterMode.ESC_STAR)
        assertEquals(3 + 2 * (5 + 8 * 3 + 1) + 2, bits.size)
        assertEquals(0x80, bits[8].toInt() and 255)
    }
    @Test fun chunkingAndCopiesAreSequential() = runTest {
        val fake = FakePrinterTransport(); fake.connect("test")
        val progress = mutableListOf<Int>()
        EscPosPrintEngine(fake).send(ByteArray(200) { it.toByte() }, PrinterProfile(chunkSize = 64, delayMs = 0), 2) { progress += it }
        assertEquals(8, fake.writes.size); assertTrue(fake.writes.all { it.size <= 64 }); assertEquals(100, progress.last())
        assertArrayEquals(fake.writes.take(4).flatMap { it.toList() }.toByteArray(), fake.writes.drop(4).flatMap { it.toList() }.toByteArray())
    }
    @Test fun partialFailureIsUncertainAndNeverRetried() = runTest {
        val fake = FakePrinterTransport(); fake.connect("test"); fake.failAt = 1
        try { EscPosPrintEngine(fake).send(ByteArray(200), PrinterProfile(chunkSize = 64, delayMs = 0), 1) {}; fail("Expected failure") }
        catch (e: TransmissionException) { assertTrue(e.uncertain); assertEquals(1, fake.writes.size) }
    }
    @Test fun firstWriteFailureIsAlsoUncertain() = runTest {
        val fake = FakePrinterTransport(); fake.connect("test"); fake.failAt = 0
        try { EscPosPrintEngine(fake).send(ByteArray(10), PrinterProfile(delayMs = 0), 1) {}; fail() } catch (e: TransmissionException) { assertTrue(e.uncertain) }
    }
    @Test fun connectionAndFailureStates() = runTest {
        val fake = FakePrinterTransport(); fake.connect("test"); assertTrue(fake.state.value is PrinterConnectionState.Connected)
        fake.disconnect(); assertTrue(fake.state.value is PrinterConnectionState.Disconnected)
        fake.failConnect = true; runCatching { fake.connect("test") }; assertTrue(fake.state.value is PrinterConnectionState.Error)
    }
}
