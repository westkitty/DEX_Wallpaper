package com.westkitty.dexwallpaper.data

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

/** Copy-on-write installation for verified backup images before the metadata transaction. */
class RestoreFileJournal {
    private val installed=mutableListOf<File>()
    private var committed=false

    fun installVerified(staged: File, target: File) {
        check(!committed) { "Cannot install after restore commit" }
        require(staged.isFile) { "Verified backup original is missing" }
        require(!Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS)) { "Restore target already exists" }
        // The target is a new app-private opaque name, never an existing artwork path.
        Files.move(staged.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE)
        installed.add(target)
    }

    fun committed() { committed=true }

    /** Only rollback uncommitted NEW files; never delete or replace a previous original. */
    fun rollbackIfUncommitted() {
        if(!committed) installed.asReversed().forEach { Files.deleteIfExists(it.toPath()) }
    }
}
