package com.makerpress.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerpress.app.core.updates.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class UpdateDownloadTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun client(bytes: ByteArray, code: Int = 200) = OkHttpClient.Builder().addInterceptor { chain -> Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test").body(bytes.toResponseBody()).build() }.build()
    private fun manifest() = ReleaseManifest("1.1.1", BuildConfig.VERSION_CODE + 1, 26, "https://example.test/update.apk", "a".repeat(64), 3)
    @Test fun corruptDownloadRejectedAndTemporaryFileRemoved() = runTest {
        val result = runCatching { ApkDownloadManager(context, client("abc".toByteArray())).download(manifest()) {} }
        assertTrue(result.isFailure); assertTrue(result.exceptionOrNull()!!.message!!.contains("SHA-256"))
        assertFalse(File(context.cacheDir, "updates/update.part").exists())
    }
    @Test fun wrongSizeAndHttpErrorsRejected() = runTest {
        assertTrue(runCatching { ApkDownloadManager(context, client("abcd".toByteArray())).download(manifest()) {} }.isFailure)
        assertTrue(runCatching { ApkDownloadManager(context, client(byteArrayOf(), 503)).download(manifest()) {} }.isFailure)
    }
    @Test fun unsignedNonApkRejectedEvenWithCorrectChecksum() = runTest {
        val m = manifest().copy(sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        assertTrue(runCatching { ApkDownloadManager(context, client("abc".toByteArray())).download(m) {} }.isFailure)
    }
    @Test fun offlineAndInvalidManifestDoNotTouchLocalJobs() = runTest {
        val offline = OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build()
        assertTrue(runCatching { UpdateRepository(offline).check("https://example.test/latest.json") }.isFailure)
        assertTrue(runCatching { UpdateRepository(client("{}".toByteArray())).check("https://example.test/latest.json") }.isFailure)
    }
    @Test fun incompatibleVersionRejectedBeforeDownload() = runTest {
        assertTrue(runCatching { ApkDownloadManager(context, client(byteArrayOf())).download(manifest().copy(minimumSdk = 99)) {} }.isFailure)
        assertTrue(runCatching { ApkDownloadManager(context, client(byteArrayOf())).download(manifest().copy(versionCode = 1)) {} }.isFailure)
    }
}
