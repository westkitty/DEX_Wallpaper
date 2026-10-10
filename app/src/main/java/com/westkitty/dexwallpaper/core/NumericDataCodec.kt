package com.westkitty.dexwallpaper.core

import org.json.JSONObject

/** A source snapshot is data, not executable expressions, nested objects, or coercible strings. */
object NumericDataCodec {
    fun decode(input: String): Outcome<Map<String,Double>> = try {
        require(input.toByteArray().size<=64*1024 && input.count { it=='{' || it=='[' }<=1) { "Numeric data must be a bounded flat object" }
        val objectData=JSONObject(input)
        require(objectData.length()<=64) { "At most 64 numeric fields" }
        val fields=objectData.keys().asSequence().associateWith { key ->
            require(key.matches(Regex("[A-Za-z_][A-Za-z0-9_]{0,40}"))) { "Invalid source variable name" }
            val value=objectData.get(key)
            require(value is Number) { "Source fields must be numeric, not strings or objects" }
            value.toDouble().also { require(it.isFinite() && kotlin.math.abs(it)<=1e9) { "Source value exceeds numeric bounds" } }
        }
        Outcome.Ok(fields)
    } catch(e: Exception) { Outcome.Error(e.message?.take(180) ?: "Invalid numeric data") }
}
