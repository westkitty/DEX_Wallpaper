package com.westkitty.dexwallpaper
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.westkitty.dexwallpaper.security.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
@RunWith(AndroidJUnit4::class)
class CredentialVaultTest {
    @Test fun keystoreRoundTripNeverStoresPlaintext() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val vault=CredentialVault(context);val id="synthetic-test-source"
        val fixture=RemoteCredentials("synthetic-test-user","synthetic-not-a-real-password")
        try {
            vault.put(id,fixture);assertEquals(fixture,vault.get(id));assertTrue(vault.has(id))
            val bytes=File(context.filesDir,"credentials/$id.enc").readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(bytes.contains(fixture.password));assertFalse(bytes.contains(fixture.username))
        } finally { vault.remove(id) }
    }
}
