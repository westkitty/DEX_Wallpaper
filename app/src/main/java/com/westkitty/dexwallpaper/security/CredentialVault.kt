package com.westkitty.dexwallpaper.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

data class RemoteCredentials(val username: String,val password: String)
/** Keystore AES-GCM; ciphertext stays app-private and is excluded from all exports/backups. */
class CredentialVault(context: Context) {
    private val directory=File(context.filesDir,"credentials").apply { mkdirs() }
    private val alias="dex.remote.credentials.v1"
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing=store.getKey(alias,null) as? SecretKey
        if(existing!=null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun file(id: String): File { require(id.matches(Regex("[A-Za-z0-9_-]{1,80}")));return File(directory,"$id.enc") }
    @Synchronized fun put(id: String,credentials: RemoteCredentials) {
        require(credentials.username.length<=500 && credentials.password.length<=2000)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val plain=JSONObject().put("username",credentials.username).put("password",credentials.password).toString().toByteArray()
        val encrypted=try { cipher.doFinal(plain) } finally { plain.fill(0) }
        val target=file(id);val temp=File(directory,"$id.partial")
        temp.outputStream().use { it.write(byteArrayOf(cipher.iv.size.toByte()));it.write(cipher.iv);it.write(encrypted) }
        java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
    @Synchronized fun get(id: String): RemoteCredentials? {
        val file=file(id);if(!file.isFile) return null
        val bytes=file.readBytes();require(bytes.size in 30..16384 && bytes[0].toInt()==12) { "Invalid encrypted credential" }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(1,13))) }
        val plain=cipher.doFinal(bytes.copyOfRange(13,bytes.size))
        return try { JSONObject(String(plain)).let { RemoteCredentials(it.getString("username"),it.getString("password")) } } finally { plain.fill(0);bytes.fill(0) }
    }
    fun has(id: String)=file(id).isFile
    fun remove(id: String) { file(id).delete() }
}
