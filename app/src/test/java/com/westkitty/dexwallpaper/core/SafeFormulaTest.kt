package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class SafeFormulaTest {
    private fun result(expr: String, vars: Map<String,Double> = emptyMap()) = (SafeFormula.evaluate(expr,vars) as Outcome.Ok).value
    @Test fun arithmeticPrecedence() { assertEquals(14.0,result("2 + 3 * 4"),0.0); assertEquals(20.0,result("(2 + 3) * 4"),0.0) }
    @Test fun conditionalAndVariables() { assertEquals(.4,result("if(battery > 20 && charging == 1, x * 2, 0)",mapOf("battery" to 50.0,"charging" to 1.0,"x" to .2)),0.0) }
    @Test fun boundedFunctions() { assertEquals(1.0,result("clamp(sin(pi / 2), 0, 1)"),1e-9) }
    @Test fun reflectionAndNetworkAreForbidden() { listOf("Runtime.exec(1)","http(1)","File(1)","eval(1)","java.lang.System.exit(0)","x=1").forEach { assertTrue(it,SafeFormula.evaluate(it) is Outcome.Error) } }
    @Test fun divisionAndNonfiniteNumbersRejected() { listOf("1/0","1%0","sqrt(-1)","999999999^9","NaN").forEach { assertTrue(SafeFormula.evaluate(it) is Outcome.Error) } }
    @Test fun inputAndNestingBudget() { assertTrue(SafeFormula.evaluate("1".repeat(513)) is Outcome.Error); assertTrue(SafeFormula.evaluate("(".repeat(40)+"1"+")".repeat(40)) is Outcome.Error) }
    @Test fun unknownVariablesFail() { assertTrue(SafeFormula.evaluate("private_data") is Outcome.Error) }
    @Test fun noUnboundedFunctionArguments() { assertTrue(SafeFormula.evaluate("min(1,2,3,4)") is Outcome.Error) }
}
