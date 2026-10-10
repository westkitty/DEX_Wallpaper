package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class NumericDataCodecTest {
    @Test fun authorizedFiniteFieldsDecodeWithoutFabricatedValues() { val fields=(NumericDataCodec.decode("{\"temperature\":18.5,\"cloud\":0}") as Outcome.Ok).value;assertEquals(18.5,fields.getValue("temperature"),0.0);assertEquals(2,fields.size) }
    @Test fun stringsNestedValuesAndExecutableKeysFail() { listOf("{\"x\":\"12\"}","{\"x\":{\"y\":1}}","{\"Runtime.exec\":1}","{\"x\":null}","{\"x\":1e308}").forEach { assertTrue(it,NumericDataCodec.decode(it) is Outcome.Error) } }
    @Test fun byteAndFieldBudgetsFailClosed() { assertTrue(NumericDataCodec.decode(" ".repeat(65537)) is Outcome.Error);assertTrue(NumericDataCodec.decode((1..65).joinToString(",","{","}") { "\"v$it\":1" }) is Outcome.Error) }
}
