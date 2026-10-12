package com.westkitty.dexwallpaper.core

/** Restore merges must never overwrite a user's existing record or original image bytes. */
object BackupMergePolicy {
    enum class OriginalAction { KEEP_EXISTING, INSTALL_VERIFIED, QUARANTINE }

    /** A record marked healthy is trusted only while its local original is still present. */
    fun originalAction(hasLocalRecord: Boolean, locallyQuarantined: Boolean, localFileExists: Boolean,
        verifiedBackupOriginal: Boolean): OriginalAction = when {
        hasLocalRecord && !locallyQuarantined && localFileExists -> OriginalAction.KEEP_EXISTING
        verifiedBackupOriginal -> OriginalAction.INSTALL_VERIFIED
        else -> OriginalAction.QUARANTINE
    }

    /** Reserve IDs for every restored row, remapping collisions without changing local rows. */
    fun distinctImportedId(preferred: String, reserved: MutableSet<String>, generate: () -> String): String {
        if (reserved.add(preferred)) return preferred
        repeat(64) {
            val candidate = generate()
            if (candidate.isNotBlank() && reserved.add(candidate)) return candidate
        }
        error("Could not allocate a distinct restored identity")
    }
}
