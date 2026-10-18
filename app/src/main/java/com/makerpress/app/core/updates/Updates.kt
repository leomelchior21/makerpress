package com.makerpress.app.core.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.makerpress.app.BuildConfig
import com.makerpress.app.data.DocumentJson
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable data class ReleaseManifest(
    val versionName: String, val versionCode: Int, val minimumSdk: Int,
    val apkUrl: String, val sha256: String, val sizeBytes: Long,
    val changelog: List<String> = emptyList()
) {
    fun validate() {
        require(versionCode > 0 && versionName.isNotBlank() && minimumSdk >= 26) { "Manifesto de versão inválido." }
        require(apkUrl.startsWith("https://") && sha256.matches(Regex("[a-fA-F0-9]{64}")) && sizeBytes in 1..150_000_000) { "URL, tamanho ou SHA-256 inválido." }
        require(changelog.size <= 100 && changelog.all { it.length <= 2000 })
    }
    fun isNewer(installed: Int) = versionCode > installed
}
fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> val buffer = ByteArray(32768); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
data class ApkIdentity(val packageName: String, val versionCode: Long, val versionName: String?, val minimumSdk: Int, val certificates: Set<String>)
fun validateApkIdentity(candidate: ApkIdentity, installed: ApkIdentity, manifest: ReleaseManifest, sdk: Int) {
    require(candidate.packageName == installed.packageName) { "APK de outro aplicativo rejeitado." }
    require(candidate.versionCode == manifest.versionCode.toLong() && candidate.versionCode > installed.versionCode && candidate.versionName == manifest.versionName) { "Versão do APK não corresponde ao manifesto." }
    require(candidate.minimumSdk <= sdk) { "APK incompatível com esta versão do Android." }
    require(installed.certificates.isNotEmpty() && candidate.certificates.isNotEmpty() && installed.certificates == candidate.certificates) { "Assinatura diferente da versão instalada. Atualização rejeitada." }
}
class UpdateRepository(private val client: OkHttpClient = updateClient()) {
    suspend fun check(url: String): ReleaseManifest = withContext(Dispatchers.IO) {
        require(url.startsWith("https://")) { "Configure um endereço HTTPS público para o manifesto." }
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { "Servidor de atualizações: HTTP ${response.code}." }
            val body = response.body ?: error("Manifesto vazio.")
            val bytes = body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
                while (out.size() <= 65536) { val n = input.read(buffer, 0, minOf(buffer.size, 65537 - out.size())); if (n <= 0) break; out.write(buffer, 0, n) }
                out.toByteArray()
            }
            require(bytes.size <= 65536) { "Manifesto muito grande." }
            DocumentJson.decodeFromString<ReleaseManifest>(bytes.decodeToString()).also { it.validate() }
        }
    }
}
fun updateClient() = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(5, TimeUnit.MINUTES)
    .followSslRedirects(false).addNetworkInterceptor { chain ->
        require(chain.request().url.isHttps) { "Atualizações exigem HTTPS." }; chain.proceed(chain.request())
    }.build()

class ApkDownloadManager(private val context: Context, private val client: OkHttpClient = updateClient()) {
    suspend fun download(manifest: ReleaseManifest, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        manifest.validate()
        require(manifest.isNewer(BuildConfig.VERSION_CODE) && manifest.minimumSdk <= Build.VERSION.SDK_INT) { "Versão incompatível com este aparelho." }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        require(dir.usableSpace > manifest.sizeBytes + 20_000_000) { "Espaço insuficiente para baixar a atualização." }
        val part = File(dir, "update.part"); val target = File(dir, "update.apk")
        try {
            client.newCall(Request.Builder().url(manifest.apkUrl).build()).execute().use { response ->
                check(response.isSuccessful) { "Download: HTTP ${response.code}." }
                val body = response.body ?: error("Download vazio.")
                val length = body.contentLength()
                require(length == -1L || length == manifest.sizeBytes) { "Tamanho de download incorreto." }
                var total = 0L
                body.byteStream().use { input -> part.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer); if (n < 0) break
                        total += n; require(total <= manifest.sizeBytes) { "Download excedeu o tamanho declarado." }
                        output.write(buffer, 0, n); progress((100 * total / manifest.sizeBytes).toInt())
                    }
                } }
                require(total == manifest.sizeBytes) { "Download incompleto." }
            }
            require(sha256(part).equals(manifest.sha256, true)) { "SHA-256 não confere. Download rejeitado." }
            verifyPackage(part, manifest)
            if (target.exists()) check(target.delete())
            check(part.renameTo(target)) { "Não foi possível salvar o APK." }
            target
        } catch (e: Exception) { part.delete(); throw e }
    }
    @Suppress("DEPRECATION")
    fun verifyPackage(file: File, manifest: ReleaseManifest) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.path, flags) ?: error("APK inválido ou não assinado.")
        val installed = pm.getPackageInfo(context.packageName, flags)
        val code = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        val expected = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
        val actual = if (Build.VERSION.SDK_INT >= 28) candidate.signingInfo?.apkContentsSigners else candidate.signatures
        validateApkIdentity(
            ApkIdentity(candidate.packageName, code, candidate.versionName, candidate.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE, actual?.map { it.toCharsString() }?.toSet() ?: emptySet()),
            ApkIdentity(context.packageName, BuildConfig.VERSION_CODE.toLong(), BuildConfig.VERSION_NAME, 26, expected?.map { it.toCharsString() }?.toSet() ?: emptySet()),
            manifest, Build.VERSION.SDK_INT
        )
    }
}
class UpdateManager(private val context: Context) {
    fun install(file: File): Intent {
        require(context.packageManager.canRequestPackageInstalls()) { "Autorize a instalação por este aplicativo nas configurações do Android." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
