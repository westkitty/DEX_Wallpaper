package com.westkitty.dexwallpaper.core

import com.westkitty.dexwallpaper.data.OriginalIntegrity
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class OriginalIntegrityTest {
    private fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun matchingExistingOriginalIsTrusted() {
        val file=Files.createTempFile("dex-intact", ".img").toFile()
        try {
            val data="original art".toByteArray()
            file.writeBytes(data)
            assertTrue(OriginalIntegrity.matches(file,hash(data),data.size.toLong()))
        } finally { file.delete() }
    }

    @Test fun modifiedBytesOfSameLengthFailVerification() {
        val file=Files.createTempFile("dex-tampered", ".img").toFile()
        try {
            val data="original art".toByteArray()
            file.writeBytes(data)
            file.writeBytes("corruptd art".toByteArray())
            assertFalse(OriginalIntegrity.matches(file,hash(data),data.size.toLong()))
        } finally { file.delete() }
    }

    @Test fun backupIncludesOnlyVerifiedHealthyOriginals() {
        val file=Files.createTempFile("dex-export-verified", ".img").toFile()
        try {
            val data="untouched original".toByteArray()
            file.writeBytes(data)
            assertTrue(OriginalIntegrity.eligibleForBackup(file, hash(data), data.size.toLong(), false))
            assertFalse(OriginalIntegrity.eligibleForBackup(file, hash(data), data.size.toLong(), true))
        } finally { file.delete() }
    }

    @Test fun backupOmitsDecodableButModifiedOriginals() {
        val file=Files.createTempFile("dex-export-modified", ".img").toFile()
        try {
            val data="image payload".toByteArray()
            file.writeBytes(data)
            file.writeBytes("image payloae".toByteArray())
            assertFalse(OriginalIntegrity.eligibleForBackup(file, hash(data), data.size.toLong(), false))
        } finally { file.delete() }
    }

    @Test fun wrongLengthAndMissingFilesAreRejected() {
        val file=Files.createTempFile("dex-short", ".img").toFile()
        try {
            val data="original art".toByteArray()
            file.writeBytes(data)
            assertFalse(OriginalIntegrity.matches(file,hash(data),data.size.toLong()+1))
        } finally { file.delete() }
        assertFalse(OriginalIntegrity.matches(file,hash("original art".toByteArray()),12))
    }
}
