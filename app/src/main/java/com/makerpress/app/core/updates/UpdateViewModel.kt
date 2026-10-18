package com.makerpress.app.core.updates

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makerpress.app.BuildConfig
import com.makerpress.app.MakerPressApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MakerPressApp
    var release by mutableStateOf<ReleaseManifest?>(null)
    var status by mutableStateOf("")
    var busy by mutableStateOf(false)
    var progress by mutableIntStateOf(0)
    var apk by mutableStateOf<File?>(null)
    private var download: Job? = null
    init { check(false) }
    fun check(manual: Boolean = true) = viewModelScope.launch {
        if (busy) return@launch
        val settings = app.settings.settings.first()
        if (settings.manifestUrl.isBlank()) { if (manual) status = "Configure o endereço HTTPS do manifesto da sua distribuição."; return@launch }
        if (!manual && System.currentTimeMillis() - settings.lastUpdateCheck < 86_400_000) return@launch
        busy = true
        try {
            app.settings.update { it.copy(lastUpdateCheck = System.currentTimeMillis()) }
            val found = UpdateRepository().check(settings.manifestUrl)
            release = found.takeIf { it.isNewer(BuildConfig.VERSION_CODE) }
            apk = null
            status = if (release == null) "Você está na versão mais recente." else "Atualização disponível: ${found.versionName}"
        } catch (e: Exception) { if (manual) status = "Não foi possível verificar: ${e.message}. A impressão offline continua disponível." }
        finally { busy = false }
    }
    fun download() {
        val manifest = release ?: return
        if (busy) return
        download = viewModelScope.launch {
            busy = true; progress = 0; apk = null
            try { apk = ApkDownloadManager(app).download(manifest) { progress = it }; status = "APK verificado. Pronto para instalar pelo Android." }
            catch (e: CancellationException) { status = "Download cancelado." }
            catch (e: Exception) { status = "Atualização rejeitada: ${e.message}" }
            finally { busy = false }
        }
    }
    fun cancel() { download?.cancel() }
    suspend fun verifiedInstallIntent(): android.content.Intent = withContext(Dispatchers.IO) {
        val file = apk ?: error("Baixe a atualização primeiro.")
        val manifest = release ?: error("Verifique a versão novamente.")
        require(file.length() == manifest.sizeBytes && sha256(file).equals(manifest.sha256, true)) { "APK alterado após download." }
        ApkDownloadManager(app).verifyPackage(file, manifest)
        UpdateManager(app).install(file)
    }
}
