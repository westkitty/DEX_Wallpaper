package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class SceneManifestTest {
    @Test fun roundTripPreservesMetadataAndVariables() {
        val scene=SceneManifest(title="Neutral epoch",epoch="User supplied",sequenceIndex=2,layers=listOf(SceneLayer("clock",LayerKind.CLOCK)),variables=mapOf("phase" to .5))
        assertEquals(scene,(SceneCodec.decode(SceneCodec.encode(scene)) as Outcome.Ok).value)
    }
    @Test fun unsupportedVersionRejected() { assertTrue(SceneCodec.decode("{\"schemaVersion\":999}") is Outcome.Error) }
    @Test fun externalAssetPathRejected() { assertTrue(SceneCodec.decode("{\"schemaVersion\":1,\"layers\":[{\"id\":\"a\",\"kind\":\"IMAGE\",\"assetId\":\"../../private\"}]}") is Outcome.Error) }
    @Test fun duplicateLayersAndHugeGeometryRejected() {
        assertTrue(runCatching { SceneCodec.encode(SceneManifest(layers=listOf(SceneLayer("x",LayerKind.TEXT),SceneLayer("x",LayerKind.TEXT)))) }.isFailure)
        assertTrue(runCatching { SceneCodec.encode(SceneManifest(width=8000,height=8000)) }.isFailure)
    }
    @Test fun boundedJsonDepthAndSize() { assertTrue(SceneCodec.decode("[".repeat(17)+"0"+"]".repeat(17)) is Outcome.Error); assertTrue(SceneCodec.decode(" ".repeat(SceneCodec.MAX_BYTES+1)) is Outcome.Error) }
    @Test fun formulaCodeExecutionRejectedAtImport() { assertTrue(runCatching { SceneCodec.encode(SceneManifest(layers=listOf(SceneLayer("x",LayerKind.TEXT,visibleWhen="exec(1)")))) }.isFailure) }
}
