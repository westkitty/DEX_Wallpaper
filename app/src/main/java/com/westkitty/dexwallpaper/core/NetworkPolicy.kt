package com.westkitty.dexwallpaper.core

import java.net.URI
import java.net.InetAddress
import kotlin.math.min

data class TrustedEndpoint(val uri: URI,val allowPrivateNetwork: Boolean=false) {
    val origin: String get()="https://${uri.host.lowercase()}:${if(uri.port<0) 443 else uri.port}"
}
object NetworkPolicy {
    fun endpoint(input: String,allowPrivate: Boolean=false): Outcome<TrustedEndpoint> = try {
        require(input.length in 1..2048) { "Endpoint length outside limits" }
        val uri=URI(input).normalize()
        require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null && uri.fragment==null && uri.rawQuery==null) { "HTTPS endpoint without embedded credentials/query/fragment required" }
        require(uri.port==-1 || uri.port in 1..65535) { "Invalid endpoint port" }
        val host=uri.host.lowercase().removePrefix("[").removeSuffix("]")
        require(host!="localhost" && !host.endsWith(".localhost") && '%' !in host) { "Localhost and scoped addresses forbidden" }
        if(':' in host) require(addressAllowed(InetAddress.getByName(host),allowPrivate)) { "Prohibited literal endpoint address" }
        else if(host.matches(Regex("[0-9.]+"))) {
            val parts=host.split('.')
            require(parts.size==4 && parts.all { it.isNotEmpty() && (it.toIntOrNull()?.let { n -> n in 0..255 }==true) && (it.length==1 || !it.startsWith('0')) }) { "Noncanonical literal address forbidden" }
            require(addressAllowed(InetAddress.getByAddress(parts.map { it.toInt().toByte() }.toByteArray()),allowPrivate)) { "Prohibited literal endpoint address" }
        }
        require(!uri.rawPath.orEmpty().contains(Regex("%2f|%5c|%2e|%25",RegexOption.IGNORE_CASE)) && !uri.path.orEmpty().contains('\\')) { "Encoded traversal/separator forbidden" }
        Outcome.Ok(TrustedEndpoint(uri,allowPrivate))
    } catch(e: IllegalArgumentException) { Outcome.Error(e.message ?: "Invalid endpoint") }
      catch(_: java.net.UnknownHostException) { Outcome.Error("Invalid endpoint address") }
      catch(_: java.net.URISyntaxException) { Outcome.Error("Malformed HTTPS endpoint") }
    fun requestAllowed(base: TrustedEndpoint,request: URI): Boolean {
        val candidate=endpoint(request.toString(),base.allowPrivateNetwork) as? Outcome.Ok ?: return false
        val prefix=base.uri.path.orEmpty().let { if(it.endsWith('/')) it else it.substringBeforeLast('/',"")+"/" }
        return candidate.value.origin==base.origin && candidate.value.uri.path.orEmpty().ifEmpty { "/" }.startsWith(prefix)
    }
    fun addressAllowed(address: InetAddress,allowPrivate: Boolean): Boolean =
        !address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress && !address.isMulticastAddress && (allowPrivate || (!address.isSiteLocalAddress && !restrictedV4(address) && !(address.address.size==16 && (address.address[0].toInt() and 0xfe)==0xfc)))
    private fun restrictedV4(address: InetAddress): Boolean {
        val bytes=address.address
        if(bytes.size!=4) return false
        val a=bytes[0].toInt() and 255;val b=bytes[1].toInt() and 255
        return a==0 || a==100 && b in 64..127 || a==198 && b in 18..19
    }
    fun retryAfterSeconds(header: String?,now: java.time.Instant=java.time.Instant.now()): Long? {
        if(header==null) return null
        val seconds=header.toLongOrNull() ?: java.time.Duration.between(now,java.time.ZonedDateTime.parse(header,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).seconds
        return seconds.coerceAtLeast(0)
    }
    fun retryDelayMillis(attempt: Int,retryAfterSeconds: Long?=null): Long =
        (retryAfterSeconds?.coerceIn(1,60)?.times(1000) ?: min(60_000L,2000L*(1L shl attempt.coerceIn(0,5))))
    fun mayTransfer(wifiOnly: Boolean,allowMetered: Boolean,wifi: Boolean,metered: Boolean,used: Long,incoming: Long,budget: Long): Boolean =
        (!wifiOnly || wifi) && (allowMetered || !metered) && incoming>=0 && used>=0 && budget>=0 && incoming<=budget-used
}
