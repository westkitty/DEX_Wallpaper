package com.westkitty.dexwallpaper.core

import kotlin.math.*

/** Numeric expressions, not a scripting runtime. Bounded input/depth/steps; no reflection or I/O. */
object SafeFormula {
    fun evaluate(expression: String, variables: Map<String, Double> = emptyMap()): Outcome<Double> = try {
        require(expression.length in 1..512) { "Expression length must be 1–512 characters" }
        require(variables.size <= 64 && variables.keys.all { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]{0,40}")) }) { "Invalid variable set" }
        val parser = Parser(expression, variables)
        val value = parser.parse()
        Outcome.Ok(value)
    } catch (e: IllegalArgumentException) { Outcome.Error(e.message ?: "Invalid formula") }

    private class Parser(private val input: String, private val variables: Map<String,Double>) {
        private var index = 0
        private var steps = 0
        private var depth = 0
        fun parse(): Double { val result = expression(0); whitespace(); require(index == input.length) { "Unexpected token at position $index" }; return bounded(result) }
        private fun whitespace() { while (index < input.length && input[index].isWhitespace()) index++ }
        private fun expression(min: Int): Double {
            require(++depth <= 32) { "Expression nesting exceeds 32" }
            require(++steps <= 2048) { "Expression step budget exceeded" }
            var left = primary()
            while (true) {
                whitespace()
                val op = operator() ?: break
                val precedence = when (op) { "||" -> 1; "&&" -> 2; "==","!=" -> 3; "<",">","<=",">=" -> 4; "+","-" -> 5; "*","/","%" -> 6; "^" -> 7; else -> 0 }
                if (precedence < min) break
                index += op.length
                val right = expression(if (op == "^") precedence else precedence + 1)
                left = bounded(when(op) {
                    "+" -> left + right; "-" -> left - right; "*" -> left * right
                    "/" -> { require(right != 0.0) { "Division by zero" }; left / right }
                    "%" -> { require(right != 0.0) { "Modulo by zero" }; left % right }
                    "^" -> left.pow(right)
                    "<" -> bool(left < right); ">" -> bool(left > right); "<=" -> bool(left <= right); ">=" -> bool(left >= right)
                    "==" -> bool(left == right); "!=" -> bool(left != right)
                    "&&" -> bool(left != 0.0 && right != 0.0); "||" -> bool(left != 0.0 || right != 0.0)
                    else -> throw IllegalArgumentException("Unknown operator")
                })
            }
            depth--
            return left
        }
        private fun primary(): Double {
            whitespace(); require(index < input.length) { "Expected value" }
            if (input[index] == '+' || input[index] == '-' || input[index] == '!') {
                val op = input[index++]; val value = expression(8)
                return when (op) { '-' -> -value; '!' -> bool(value == 0.0); else -> value }
            }
            if (input[index] == '(') { index++; val result = expression(0); expect(')'); return result }
            if (input[index].isDigit() || input[index] == '.') {
                val start = index
                while (index < input.length && (input[index].isDigit() || input[index] == '.')) index++
                return bounded(input.substring(start,index).toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number"))
            }
            require(input[index].isLetter() || input[index] == '_') { "Forbidden token at position $index" }
            val start = index
            while (index < input.length && (input[index].isLetterOrDigit() || input[index] == '_')) index++
            val name = input.substring(start,index)
            require(name.length <= 41) { "Identifier too long" }
            whitespace()
            if (index < input.length && input[index] == '(') {
                index++
                val args = mutableListOf<Double>()
                whitespace()
                if (index < input.length && input[index] != ')') {
                    do {
                        require(args.size < 3) { "At most three function arguments" }
                        args += expression(0); whitespace()
                        if (index >= input.length || input[index] != ',') break
                        index++
                    } while (true)
                }
                expect(')')
                fun count(n: Int) { require(args.size == n) { "$name expects $n argument(s)" } }
                val result = when(name) {
                    "sin" -> { count(1); sin(args[0]) }; "cos" -> { count(1); cos(args[0]) }
                    "abs" -> { count(1); abs(args[0]) }; "sqrt" -> { count(1); sqrt(args[0]) }
                    "floor" -> { count(1); floor(args[0]) }; "ceil" -> { count(1); ceil(args[0]) }
                    "min" -> { count(2); min(args[0],args[1]) }; "max" -> { count(2); max(args[0],args[1]) }
                    "clamp" -> { count(3); require(args[1] <= args[2]) { "Invalid clamp bounds" }; args[0].coerceIn(args[1],args[2]) }
                    "if" -> { count(3); if (args[0] != 0.0) args[1] else args[2] }
                    else -> throw IllegalArgumentException("Function '$name' is not allowed")
                }
                return bounded(result)
            }
            return bounded(when(name) { "pi" -> PI; "e" -> E; else -> variables[name] ?: throw IllegalArgumentException("Unknown variable '$name'") })
        }
        private fun expect(char: Char) { whitespace(); require(index < input.length && input[index] == char) { "Expected '$char' at position $index" }; index++ }
        private fun operator(): String? = listOf("||","&&","==","!=","<=",">=","+","-","*","/","%","^","<",">").firstOrNull { input.startsWith(it,index) }
        private fun bool(value: Boolean) = if (value) 1.0 else 0.0
        private fun bounded(value: Double): Double { require(value.isFinite() && abs(value) <= 1e9) { "Numeric result out of bounds" }; return value }
    }
}
