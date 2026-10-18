package com.makerpress.app.core.printing

import com.makerpress.app.core.rendering.*
import com.makerpress.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

class PrintQueueManager(
    private val repository: JobRepository, private val renderer: PrintDocumentRenderer,
    private val engine: EscPosPrintEngine, private val transport: PrinterTransport,
    private val log: (String) -> Unit = {}
) {
    private val lock = Mutex()
    val running = MutableStateFlow(false)
    suspend fun recover() { repository.dao.recoverSending(); repository.dao.recoverRendering() }
    suspend fun run() {
        if (!lock.tryLock()) return
        running.value = true
        try {
            while (currentCoroutineContext().isActive) {
                val job = repository.dao.next() ?: break
                if (repository.dao.claim(job.id) != 1) continue
                try {
                    val profile = repository.profile(job)
                    val raster = withContext(Dispatchers.Default) {
                        val bitmap = renderer.render(repository.document(job), profile)
                        try { EscPosRasterEncoder.encode(BitmapProcessor.pixels(bitmap), bitmap.width, bitmap.height, profile.mode) } finally { bitmap.recycle() }
                    }
                    currentCoroutineContext().ensureActive()
                    repository.dao.status(job.id, "SENDING", 0, "Enviando bytes; saída física ainda não confirmada.")
                    var percent = 0
                    engine.send(raster, profile, job.copies) { value ->
                        if (value != percent) repository.dao.status(job.id, "SENDING", value, "Enviando bytes; saída física ainda não confirmada.")
                        percent = value
                    }
                    repository.dao.status(job.id, "SENT", percent, "Bytes enviados. Confira a impressão no papel.")
                    log("Job ${job.id}: bytes enviados (${raster.size} por cópia); impressão física não verificada.")
                } catch (e: Exception) {
                    val uncertain = e is TransmissionException && e.uncertain
                    withContext(NonCancellable) {
                        repository.dao.status(job.id, if (uncertain) "UNKNOWN" else "FAILED", 0, if (uncertain) "Envio parcial possível. Confira o papel antes de repetir." else (e.message ?: "Falha ao imprimir."))
                        if (uncertain) transport.disconnect()
                    }
                    log("Job ${job.id}: ${if (uncertain) "resultado desconhecido" else "falha"}; ${e.javaClass.simpleName}")
                    break // Explicit user action is needed after failure.
                }
            }
        } finally { running.value = false; lock.unlock() }
    }
}
