package com.westkitty.dexwallpaper.data

import android.content.Context
import android.net.Uri
import com.westkitty.dexwallpaper.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Profiles are reusable snapshots; current authoritative screen/library state remains in Room/DataStore. */
class ProfileRepository(private val context: Context,private val wall: WallRepository) {
    private val directory=File(context.filesDir,"profiles").apply { mkdirs() }
    suspend fun snapshot(name: String,target: Destination,assetId: String?): VisualProfile {
        val p=wall.preferences.flow.first()
        val screen=wall.dao.screen(target.name) ?: ScreenEntity(target.name)
        val edit=assetId?.let { wall.dao.edit(it)?.model() } ?: ImageEdit()
        return VisualProfile(name,screen.collectionId,RotationMode.valueOf(screen.mode),screen.intervalMinutes,edit.dim,edit.saturation,edit.brightness,edit.contrast,
            p.reducedMotion,p.clockOverlay,ShaderEffect.entries.firstOrNull { it.name==p.shaderEffect } ?: ShaderEffect.NONE,p.parallax,p.frameRate)
    }
    suspend fun apply(profile: VisualProfile,target: Destination,assetId: String?) {
        ProfileCodec.encode(profile) // Validate before writing anything; never applies wallpaper.
        val collection=profile.collectionId.takeIf { id -> wall.dao.allCollections().any { it.id==id } }
        wall.updateScreen(target) { it.copy(collectionId=collection ?: it.collectionId,mode=profile.mode.name,intervalMinutes=profile.intervalMinutes,pending="") }
        if(assetId!=null) {
            val old=wall.dao.edit(assetId)?.model() ?: ImageEdit()
            wall.dao.putEdit(EditEntity.from(assetId,old.copy(dim=profile.dim,saturation=profile.saturation,brightness=profile.brightness,contrast=profile.contrast)))
        }
        wall.preferences.flag("reducedMotion",profile.reducedMotion); wall.preferences.flag("clockOverlay",profile.clockOverlay); wall.preferences.flag("parallax",profile.parallax)
        wall.preferences.text("shaderEffect",profile.shader.name); wall.preferences.number("frameRate",profile.frameRate)
    }
    suspend fun save(profile: VisualProfile) = withContext(Dispatchers.IO) {
        val encoded=ProfileCodec.encode(profile)
        val name=java.security.MessageDigest.getInstance("SHA-256").digest(profile.name.toByteArray()).joinToString("") { "%02x".format(it) }
        val target=File(directory,"$name.json"); val temp=File(directory,"$name.partial").apply { writeText(encoded) }
        java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
    suspend fun saved(): List<VisualProfile> = withContext(Dispatchers.IO) { directory.listFiles()?.filter { it.extension=="json" }?.mapNotNull { (ProfileCodec.decode(it.readText()) as? Outcome.Ok)?.value } ?: emptyList() }
    suspend fun export(uri: Uri,profile: VisualProfile) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri,"wt").use { requireNotNull(it).write(ProfileCodec.encode(profile).toByteArray()) }
    }
    suspend fun import(uri: Uri): VisualProfile = withContext(Dispatchers.IO) {
        val output=ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input); val bytes=ByteArray(4096); while(true) { val n=input.read(bytes); if(n<0) break; require(output.size()+n<=16*1024) { "Profile too large" }; output.write(bytes,0,n) } }
        when(val decoded=ProfileCodec.decode(output.toString("UTF-8"))) { is Outcome.Error -> throw IllegalArgumentException(decoded.message); is Outcome.Ok -> decoded.value.also { save(it) } }
    }
}
