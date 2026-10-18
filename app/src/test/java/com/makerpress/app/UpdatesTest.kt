package com.makerpress.app

import com.makerpress.app.core.updates.*
import com.makerpress.app.data.DocumentJson
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UpdatesTest {
    private fun valid() = ReleaseManifest("1.0.1", 2, 26, "https://example.test/app.apk", "a".repeat(64), 100)
    @Test fun versionComparisonUsesCode() { assertTrue(valid().isNewer(1)); assertFalse(valid().isNewer(2)); assertFalse(valid().isNewer(3)) }
    @Test fun invalidManifestsRejected() {
        for (m in listOf(valid().copy(apkUrl = "http://example.test/app.apk"), valid().copy(sha256 = "no"), valid().copy(sizeBytes = 0), valid().copy(sizeBytes = 200_000_000))) assertTrue(runCatching { m.validate() }.isFailure)
        assertTrue(runCatching { DocumentJson.decodeFromString<ReleaseManifest>("{}") }.isFailure)
    }
    @Test fun checksumDetectsCorruption() {
        val file = File.createTempFile("makerpress", ".apk")
        try {
            file.writeText("abc"); assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256(file))
            val hash = sha256(file); file.appendText("!"); assertNotEquals(hash, sha256(file))
        } finally { file.delete() }
    }
    @Test fun rejectsWrongPackageSignatureVersionAndSdk() {
        val installed = ApkIdentity("com.makerpress.app", 1, "1.0.0", 26, setOf("stable-cert"))
        val candidate = installed.copy(versionCode = 2, versionName = "1.0.1")
        validateApkIdentity(candidate, installed, valid(), 30)
        for (bad in listOf(candidate.copy(packageName = "other.app"), candidate.copy(certificates = setOf("another-cert")), candidate.copy(certificates = emptySet()), candidate.copy(versionCode = 1), candidate.copy(versionName = "wrong"), candidate.copy(minimumSdk = 35))) {
            assertTrue(runCatching { validateApkIdentity(bad, installed, valid(), 30) }.isFailure)
        }
    }
}
