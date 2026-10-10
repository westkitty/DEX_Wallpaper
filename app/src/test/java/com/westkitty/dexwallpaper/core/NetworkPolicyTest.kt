package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.InetAddress
class NetworkPolicyTest {
    @Test fun httpsWithoutEmbeddedSecretsRequired() { listOf("http://example.com/a","https://user:secret@example.com/a","https://example.com/a?token=x","https://localhost/a","file:///private").forEach { assertTrue(it,NetworkPolicy.endpoint(it) is Outcome.Error) } }
    @Test fun redirectsAndDavLinksCannotEscapeAllowlist() { val base=(NetworkPolicy.endpoint("https://example.com/art/") as Outcome.Ok).value; assertTrue(NetworkPolicy.requestAllowed(base,URI("https://example.com/art/a.jpg")));assertFalse(NetworkPolicy.requestAllowed(base,URI("https://evil.example/art/a.jpg")));assertFalse(NetworkPolicy.requestAllowed(base,URI("https://example.com/private/a.jpg")));assertFalse(NetworkPolicy.requestAllowed(base,URI("https://example.com/art/%2e%2e/private")));assertFalse(NetworkPolicy.requestAllowed(base,URI("https://example.com/art/%252e%252e/private"))) }
    @Test fun privateAndMetadataIpsFailUnlessExplicitlyAllowed() { assertFalse(NetworkPolicy.addressAllowed(InetAddress.getByAddress(byteArrayOf(127,0,0,1)),true));assertFalse(NetworkPolicy.addressAllowed(InetAddress.getByAddress(byteArrayOf(169.toByte(),254.toByte(),169.toByte(),254.toByte())),true));assertFalse(NetworkPolicy.addressAllowed(InetAddress.getByAddress(byteArrayOf(192.toByte(),168.toByte(),1,2)),false));assertTrue(NetworkPolicy.addressAllowed(InetAddress.getByAddress(byteArrayOf(192.toByte(),168.toByte(),1,2)),true)) }
    @Test fun literalIpsCannotBypassTheDnsPolicy() {
        listOf("https://127.0.0.1/a","https://169.254.169.254/a","https://192.168.1.2/a","https://[fd00::1]/a","https://2130706433/a","https://127.00.0.1/a").forEach { assertTrue(it,NetworkPolicy.endpoint(it) is Outcome.Error) }
        assertTrue(NetworkPolicy.endpoint("https://192.168.1.2/a",true) is Outcome.Ok)
        assertTrue(NetworkPolicy.endpoint("https://127.0.0.1/a",true) is Outcome.Error)
    }
    @Test fun backoffIsBoundedAndExponential() { assertEquals(2000,NetworkPolicy.retryDelayMillis(0));assertEquals(4000,NetworkPolicy.retryDelayMillis(1));assertEquals(60_000,NetworkPolicy.retryDelayMillis(100,10000)) }
    @Test fun httpDateDelaysAreHonoredRatherThanRetriedEarly() {
        val now=java.time.Instant.parse("2026-10-10T00:00:00Z")
        assertEquals(30L,NetworkPolicy.retryAfterSeconds("Sat, 10 Oct 2026 00:00:30 GMT",now))
        assertEquals(300L,NetworkPolicy.retryAfterSeconds("300",now))
        assertNull(NetworkPolicy.retryAfterSeconds(null,now))
    }
    @Test fun budgetsRejectMeteredAndOversizedRequests() { assertFalse(NetworkPolicy.mayTransfer(true,false,false,false,0,1,100));assertFalse(NetworkPolicy.mayTransfer(false,false,true,true,0,1,100));assertFalse(NetworkPolicy.mayTransfer(false,true,true,true,50,51,100));assertTrue(NetworkPolicy.mayTransfer(true,false,true,false,50,50,100)) }
}
