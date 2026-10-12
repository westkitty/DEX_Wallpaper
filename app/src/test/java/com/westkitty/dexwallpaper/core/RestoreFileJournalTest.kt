package com.westkitty.dexwallpaper.core

import com.westkitty.dexwallpaper.data.RestoreFileJournal
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RestoreFileJournalTest {
    @Test fun rollbackRemovesOnlyNewOriginalAndPreservesOlderFiles() {
        val root=Files.createTempDirectory("dex-restore-journal").toFile()
        try {
            val old=root.resolve("existing.img").apply { writeText("personal artwork") }
            val incoming=root.resolve("source.img").apply { writeText("verified archive bytes") }
            val destination=root.resolve("fresh-id.img")
            val journal=RestoreFileJournal()
            journal.installVerified(incoming,destination)
            assertEquals("verified archive bytes",destination.readText())
            journal.rollbackIfUncommitted()
            assertFalse(destination.exists())
            assertEquals("personal artwork",old.readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun committedInstallRemainsAvailable() {
        val root=Files.createTempDirectory("dex-restore-commit").toFile()
        try {
            val incoming=root.resolve("source.img").apply { writeText("restored") }
            val target=root.resolve("new.img")
            val journal=RestoreFileJournal()
            journal.installVerified(incoming,target)
            journal.committed()
            journal.rollbackIfUncommitted()
            assertEquals("restored",target.readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun overwriteAttemptIsRejected() {
        val root=Files.createTempDirectory("dex-restore-collision").toFile()
        try {
            val existing=root.resolve("existing.img").apply { writeText("keep") }
            val incoming=root.resolve("source.img").apply { writeText("new") }
            val journal=RestoreFileJournal()
            assertTrue(runCatching { journal.installVerified(incoming,existing) }.isFailure)
            assertEquals("keep",existing.readText())
            assertEquals("new",incoming.readText())
        } finally { root.deleteRecursively() }
    }
}
