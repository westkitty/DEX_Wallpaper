package com.westkitty.dexwallpaper.data

import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** A local image is healthy only if the entire on-disk content matches the manifest. */
object OriginalIntegrity {
    /** Never export unverified originals or files already marked quarantined. */
    fun eligibleForBackup(file: File, sha256: String, bytes: Long, quarantined: Boolean): Boolean =
        !quarantined && matches(file, sha256, bytes)

    fun matches(file: File, sha256: String, bytes: Long): Boolean {
        if (!file.isFile || bytes < 1L || file.length() != bytes) return false
        return try {
            val digest=MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer=ByteArray(64*1024)
                while (true) {
                    val n=input.read(buffer)
                    if (n<0) break
                    digest.update(buffer,0,n)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }==sha256
        } catch (_: IOException) {
            // A vanished or unreadable file is not a trusted original. The caller
            // may repair it from verified new bytes without destroying the old path.
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
