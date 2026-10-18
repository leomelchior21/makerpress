package com.makerpress.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.makerpress.app.core.printing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
class BluetoothClassicTransport(context: Context) : PrinterTransport {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    override val state = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    @Volatile private var socket: BluetoothSocket? = null
    private val lock = Mutex()
    private suspend fun socketOperation(current: BluetoothSocket, block: () -> Unit) = coroutineScope {
        val timeout = launch { delay(15_000); runCatching { current.close() } }
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                continuation.invokeOnCancellation { runCatching { current.close() } }
                try { block(); continuation.resume(Unit) } catch (e: Exception) { continuation.resumeWithException(e) }
            }
        } finally { timeout.cancel() }
    }
    override suspend fun connect(address: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            socket?.close(); socket = null
            state.value = PrinterConnectionState.Connecting
            try {
                check(adapter != null && adapter.isEnabled) { "Ative o Bluetooth nas configurações do Android." }
                val next = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket = next
                // Closing the socket interrupts the platform's blocking connect call.
                socketOperation(next) { next.connect() }
                state.value = PrinterConnectionState.Connected(address)
            } catch (e: Exception) {
                runCatching { socket?.close() }; socket = null
                state.value = PrinterConnectionState.Error("Falha RFCOMM/SPP. Confira pareamento, energia e se outro aplicativo está conectado. ${e.javaClass.simpleName}")
                throw e
            }
        }
    }
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { socket?.close() }; socket = null; state.value = PrinterConnectionState.Disconnected
    }
    override suspend fun write(bytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val current = socket ?: error("Impressora desconectada.")
            socketOperation(current) { current.outputStream.write(bytes); current.outputStream.flush() }
        } catch (e: Exception) {
            runCatching { socket?.close() }; socket = null
            state.value = PrinterConnectionState.Error("Transmissão interrompida; confira o papel antes de repetir.")
            throw e
        }
    }
}
data class PairedPrinter(val name: String, val address: String)
@SuppressLint("MissingPermission")
class BluetoothPrinterManager(context: Context) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    val transport = BluetoothClassicTransport(context)
    val logs = CopyOnWriteArrayList<String>()
    fun log(message: String) {
        logs += "${java.time.Instant.now()} $message"
        while (logs.size > 300) logs.removeAt(0)
    }
    fun paired(): List<PairedPrinter> {
        check(adapter != null) { "Este aparelho não possui Bluetooth." }
        check(adapter.isEnabled) { "Ative o Bluetooth e faça o pareamento nas configurações do Android." }
        return adapter.bondedDevices.map { PairedPrinter(it.name ?: "Dispositivo Bluetooth", it.address) }.sortedBy { it.name }
    }
}
