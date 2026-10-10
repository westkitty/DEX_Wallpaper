package com.westkitty.dexwallpaper.core

import java.io.File

/** Resolves an internal asset/cache entry without allowing a database value to escape its root. */
object LibraryFilePath {
    private val directChildName = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}")

    /**
     * Library and thumbnail entries are deliberately flat, private files. Canonicalization also
     * rejects a direct-child symlink that resolves outside the owned directory.
     */
    fun resolve(root: File, relativeName: String): File {
        require(relativeChild(relativeName)) { "Invalid private library path" }
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, relativeName).canonicalFile
        require(target.parentFile == canonicalRoot) { "Private library path escaped its storage directory" }
        return target
    }

    private fun relativeChild(value: String): Boolean =
        value != "." && value != ".." && directChildName.matches(value)
}
