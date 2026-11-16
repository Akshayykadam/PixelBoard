package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

internal object RuntimeCallEmitter {
    fun invoke(call: RuntimeCallId, registers: String): String =
        invoke(RuntimeAbiCatalog.abi(call), registers)

    fun invoke(abi: RuntimeAbi, registers: String): String {
        val expression = RegisterExpression.parse(registers)
        val expectedWidths = buildList {
            if (!abi.isStatic) add(1)
            abi.parameters.forEach { type -> add(if (type == "J" || type == "D") 2 else 1) }
        }
        require(expression.words.size == expectedWidths.sum()) {
            "${abi.reference} requires ${expectedWidths.sum()} register words, " +
                "but '$registers' supplies ${expression.words.size}"
        }
        expression.requireContiguousWideArguments(expectedWidths, abi)

        val rendered = expression.forInvocation()
        val opcode = when {
            abi.isStatic && rendered.isRange -> "invoke-static/range"
            abi.isStatic -> "invoke-static"
            rendered.isRange -> "invoke-virtual/range"
            else -> "invoke-virtual"
        }
        return "$opcode {${rendered.registers}}, ${abi.reference}"
    }
}

private sealed interface RegisterExpression {
    val words: List<RegisterWord>

    fun forInvocation(): RenderedRegisters

    fun requireContiguousWideArguments(widths: List<Int>, abi: RuntimeAbi) {
        var wordIndex = 0
        widths.forEach { width ->
            if (width == 2 && !words[wordIndex].isImmediatelyBefore(words[wordIndex + 1])) {
                throw IllegalArgumentException(
                    "Wide argument in ${abi.reference} must use contiguous register words"
                )
            }
            wordIndex += width
        }
    }

    data class Direct(override val words: List<RegisterWord>) : RegisterExpression {
        override fun forInvocation(): RenderedRegisters {
            val fits35c = words.size <= MAX_35C_WORDS && words.all { word ->
                word.index == null || word.index <= MAX_35C_REGISTER
            }
            if (fits35c) {
                return RenderedRegisters(words.joinToString(", ") { word -> word.source }, false)
            }
            require(words.isNumericContiguous()) {
                "Register expression '${words.joinToString(", ") { it.source }}' " +
                    "does not fit invoke-35c and cannot be encoded as a contiguous range"
            }
            return RenderedRegisters("${words.first().source} .. ${words.last().source}", true)
        }
    }

    data class Range(override val words: List<RegisterWord>) : RegisterExpression {
        override fun forInvocation(): RenderedRegisters =
            RenderedRegisters("${words.first().source} .. ${words.last().source}", true)
    }

    companion object {
        fun parse(source: String): RegisterExpression {
            if (source.isBlank()) return Direct(emptyList())
            if (".." in source) {
                val match = RANGE_PATTERN.matchEntire(source.trim())
                    ?: throw IllegalArgumentException("Malformed register range: '$source'")
                val start = RegisterWord.numeric(match.groupValues[1])
                val end = RegisterWord.numeric(match.groupValues[2])
                require(start.prefix == end.prefix && start.index!! <= end.index!!) {
                    "Register range must be ascending within one register family: '$source'"
                }
                return Range(
                    (start.index..end.index).map { index ->
                        RegisterWord("${start.prefix}$index", start.prefix, index)
                    }
                )
            }
            return Direct(source.split(',').map { token -> RegisterWord.parse(token.trim()) })
        }
    }
}

private data class RegisterWord(
    val source: String,
    val prefix: Char?,
    val index: Int?,
) {
    fun isImmediatelyBefore(other: RegisterWord): Boolean =
        prefix != null && prefix == other.prefix && index != null && other.index == index + 1

    companion object {
        fun parse(source: String): RegisterWord {
            NUMERIC_REGISTER.matchEntire(source)?.let { match ->
                return RegisterWord(source, match.groupValues[1].single(), match.groupValues[2].toInt())
            }
            require(SYMBOLIC_REGISTER.matches(source)) { "Malformed register word: '$source'" }
            return RegisterWord(source, null, null)
        }

        fun numeric(source: String): RegisterWord = parse(source).also { word ->
            require(word.index != null) { "Register ranges require concrete registers: '$source'" }
        }
    }
}

private data class RenderedRegisters(val registers: String, val isRange: Boolean)

private fun List<RegisterWord>.isNumericContiguous(): Boolean =
    isNotEmpty() && all { word -> word.index != null } &&
        zipWithNext().all { (left, right) -> left.isImmediatelyBefore(right) }

private const val MAX_35C_WORDS = 5
private const val MAX_35C_REGISTER = 15
private val NUMERIC_REGISTER = Regex("([vp])(\\d+)")
private val SYMBOLIC_REGISTER = Regex("(?:%s|[vp][A-Za-z0-9_]+)")
private val RANGE_PATTERN = Regex("([vp]\\d+)\\s*\\.\\.\\s*([vp]\\d+)")
