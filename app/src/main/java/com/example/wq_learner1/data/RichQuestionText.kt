package com.example.wq_learner1.data

fun renderQuestionContent(raw: String): String {
    var text = raw.replace("\r\n", "\n").replace("\r", "\n")
    text = text.replace(Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)) { match ->
        renderLatex(match.groupValues[1])
    }
    text = text.replace(Regex("""\$(.*?)\$""", RegexOption.DOT_MATCHES_ALL)) { match ->
        renderLatex(match.groupValues[1])
    }
    text = text.replace(Regex("""```(.*?)```""", RegexOption.DOT_MATCHES_ALL)) { match ->
        match.groupValues[1].trim()
    }
    text = text.replace(Regex("""`([^`]*)`""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""!\[([^]]*)]\([^)]*\)""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""\[([^]]+)]\([^)]*\)""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""\*\*([^*]+)\*\*""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""__([^_]+)__""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""(?<!_)_([^_\n]+)_(?!_)""")) { match -> match.groupValues[1] }
    text = text.lines()
        .map { line ->
            line
                .replace(Regex("""^\s{0,6}#{1,6}\s*"""), "")
                .replace(Regex("""^\s{0,6}[-*+]\s+"""), "")
                .replace(Regex("""^\s{0,6}\d+[.)]\s+"""), "")
                .trimEnd()
        }
        .joinToString("\n")
    return text
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

fun String.hasRawQuestionMarkup(): Boolean {
    return contains("**") ||
        contains("```") ||
        contains(Regex("""(?<!\\)\$""")) ||
        contains(Regex("""\\[A-Za-z]+"""))
}

fun compactSubjectLabel(subject: String): String {
    return when (subject.trim()) {
        "全部" -> "全部"
        "数据结构" -> "数据"
        "计算机组成原理" -> "计组"
        "操作系统" -> "操作"
        "计算机网络" -> "网络"
        "数学" -> "数学"
        else -> subject
    }
}

private fun renderLatex(raw: String): String {
    var text = raw.trim()
    text = replaceCommandWithTwoGroups(text, "\\frac") { numerator, denominator ->
        "${renderLatex(numerator)}/${renderLatex(denominator)}"
    }
    text = replaceCommandWithOneGroup(text, "\\sqrt") { value ->
        "√(${renderLatex(value)})"
    }
    latexSymbols.forEach { (latex, rendered) ->
        text = text.replace(latex, rendered)
    }
    text = replaceScript(text, '^', superscriptDigits + superscriptLetters)
    text = replaceScript(text, '_', subscriptDigits + subscriptLetters)
    return text
        .replace(Regex("""\\([A-Za-z]+)""")) { match -> match.groupValues[1] }
        .replace(Regex("""\s+"""), " ")
        .replace("{", "")
        .replace("}", "")
        .trim()
}

private fun replaceCommandWithTwoGroups(
    source: String,
    command: String,
    replacement: (String, String) -> String,
): String {
    var text = source
    while (true) {
        val commandIndex = text.indexOf(command)
        if (commandIndex < 0) return text
        val firstStart = text.indexOf('{', startIndex = commandIndex + command.length)
        if (firstStart < 0) return text
        val firstEnd = matchingBraceIndex(text, firstStart)
        if (firstEnd < 0) return text
        val secondStart = text.indexOf('{', startIndex = firstEnd + 1)
        if (secondStart < 0) return text
        val secondEnd = matchingBraceIndex(text, secondStart)
        if (secondEnd < 0) return text
        val rendered = replacement(
            text.substring(firstStart + 1, firstEnd),
            text.substring(secondStart + 1, secondEnd),
        )
        text = text.replaceRange(commandIndex, secondEnd + 1, rendered)
    }
}

private fun replaceCommandWithOneGroup(
    source: String,
    command: String,
    replacement: (String) -> String,
): String {
    var text = source
    while (true) {
        val commandIndex = text.indexOf(command)
        if (commandIndex < 0) return text
        val groupStart = text.indexOf('{', startIndex = commandIndex + command.length)
        if (groupStart < 0) return text
        val groupEnd = matchingBraceIndex(text, groupStart)
        if (groupEnd < 0) return text
        val rendered = replacement(text.substring(groupStart + 1, groupEnd))
        text = text.replaceRange(commandIndex, groupEnd + 1, rendered)
    }
}

private fun replaceScript(source: String, marker: Char, map: Map<Char, Char>): String {
    val result = StringBuilder()
    var index = 0
    while (index < source.length) {
        if (source[index] != marker || index == source.lastIndex) {
            result.append(source[index])
            index += 1
            continue
        }

        val nextIndex = index + 1
        val token = if (source[nextIndex] == '{') {
            val end = matchingBraceIndex(source, nextIndex)
            if (end < 0) {
                result.append(source[index])
                index += 1
                continue
            }
            index = end + 1
            source.substring(nextIndex + 1, end)
        } else {
            index += 2
            source[nextIndex].toString()
        }
        result.append(token.map { map[it] ?: it }.joinToString(""))
    }
    return result.toString()
}

private fun matchingBraceIndex(text: String, openIndex: Int): Int {
    var depth = 0
    for (index in openIndex until text.length) {
        when (text[index]) {
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth == 0) return index
            }
        }
    }
    return -1
}

private val latexSymbols = mapOf(
    "\\alpha" to "α",
    "\\beta" to "β",
    "\\gamma" to "γ",
    "\\delta" to "δ",
    "\\theta" to "θ",
    "\\lambda" to "λ",
    "\\mu" to "μ",
    "\\pi" to "π",
    "\\sigma" to "σ",
    "\\omega" to "ω",
    "\\times" to "×",
    "\\cdot" to "·",
    "\\leq" to "≤",
    "\\le" to "≤",
    "\\geq" to "≥",
    "\\ge" to "≥",
    "\\neq" to "≠",
    "\\infty" to "∞",
    "\\sum" to "Σ",
    "\\rightarrow" to "→",
    "\\to" to "→",
)

private val superscriptDigits = mapOf(
    '0' to '⁰',
    '1' to '¹',
    '2' to '²',
    '3' to '³',
    '4' to '⁴',
    '5' to '⁵',
    '6' to '⁶',
    '7' to '⁷',
    '8' to '⁸',
    '9' to '⁹',
    '+' to '⁺',
    '-' to '⁻',
    '=' to '⁼',
    '(' to '⁽',
    ')' to '⁾',
)

private val subscriptDigits = mapOf(
    '0' to '₀',
    '1' to '₁',
    '2' to '₂',
    '3' to '₃',
    '4' to '₄',
    '5' to '₅',
    '6' to '₆',
    '7' to '₇',
    '8' to '₈',
    '9' to '₉',
    '+' to '₊',
    '-' to '₋',
    '=' to '₌',
    '(' to '₍',
    ')' to '₎',
)

private val superscriptLetters = mapOf(
    'n' to 'ⁿ',
    'i' to 'ⁱ',
)

private val subscriptLetters = mapOf(
    'a' to 'ₐ',
    'e' to 'ₑ',
    'h' to 'ₕ',
    'i' to 'ᵢ',
    'j' to 'ⱼ',
    'k' to 'ₖ',
    'l' to 'ₗ',
    'm' to 'ₘ',
    'n' to 'ₙ',
    'o' to 'ₒ',
    'p' to 'ₚ',
    'r' to 'ᵣ',
    's' to 'ₛ',
    't' to 'ₜ',
    'u' to 'ᵤ',
    'v' to 'ᵥ',
    'x' to 'ₓ',
)
