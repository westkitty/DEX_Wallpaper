package com.westkitty.dexwallpaper.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LibraryFilePathTest {
    @Test fun resolvesOnlyDirectPrivateChildren() {
        val root = Files.createTempDirectory("dex-library-path").toFile()
        try {
            listOf("asset-123.img", "-asset.img", "_asset.img").forEach { name ->
                assertEquals(File(root, name).canonicalFile, LibraryFilePath.resolve(root, name))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsAbsoluteTraversalAndNestedNames() {
        val root = Files.createTempDirectory("dex-library-path").toFile()
        try {
            listOf("", ".", "..", "../outside.img", "/outside.img", "folder/asset.img", "folder\\asset.img", "bad name.img")
                .forEach { name ->
                    assertThrows(IllegalArgumentException::class.java) { LibraryFilePath.resolve(root, name) }
                }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rejectsSymlinksThatResolveOutsideRoot() {
        val parent = Files.createTempDirectory("dex-library-symlink")
        val root = Files.createDirectory(parent.resolve("originals")).toFile()
        val outside = Files.createFile(parent.resolve("outside.img")).toFile()
        try {
            Files.createSymbolicLink(File(root, "linked.img").toPath(), outside.toPath())
            assertThrows(IllegalArgumentException::class.java) { LibraryFilePath.resolve(root, "linked.img") }
        } finally {
            root.deleteRecursively()
            outside.delete()
            parent.toFile().deleteRecursively()
        }
    }
}
