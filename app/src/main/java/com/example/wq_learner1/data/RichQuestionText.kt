package com.example.wq_learner1.data

fun renderQuestionContent(raw: String): String {
    var text = raw.replace("\r\n", "\n").replace("\r", "\n")
    
    // 1. Block LaTeX
    text = text.replace(Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)) { match ->
        "\n" + renderLatex(match.groupValues[1]) + "\n"
    }
    
    // 2. Inline LaTeX (more robust regex to avoid greedy matching)
    text = text.replace(Regex("""(?<!\\)\$(.*?)(?<!\\)\$""", RegexOption.DOT_MATCHES_ALL)) { match ->
        renderLatex(match.groupValues[1])
    }
    
    // 3. Markdown Code (preserve inline backticks for UI styling)
    text = text.replace(Regex("""```(.*?)```""", RegexOption.DOT_MATCHES_ALL)) { match ->
        "\n" + match.groupValues[1].trim() + "\n"
    }
    
    // 4. Markdown links/images
    text = text.replace(Regex("""!\[([^]]*)]\([^)]*\)""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""\[([^]]+)]\([^)]*\)""")) { match -> match.groupValues[1] }
    
    // 5. Markdown emphasis (preserve ** for UI styling, strip others for simplicity)
    text = text.replace(Regex("""\*\*\*([^*]+)\*\*\*""")) { match -> "**" + match.groupValues[1] + "**" }
    text = text.replace(Regex("""__([^_]+)__""")) { match -> "**" + match.groupValues[1] + "**" }
    text = text.replace(Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")) { match -> match.groupValues[1] }
    text = text.replace(Regex("""(?<!_)_([^_\n]+)_(?!_)""")) { match -> match.groupValues[1] }
    
    // 6. Lists and Headings cleanup
    text = text.lines()
        .map { line ->
            line
                .replace(Regex("""^\s{0,6}#{1,6}\s*"""), "")
                .replace(Regex("""^\s{0,6}[-*+]\s+"""), "• ")
                .replace(Regex("""^\s{0,6}\d+[.)]\s+""")) { it.value.trim() + " " }
                .trimEnd()
        }
        .joinToString("\n")
        
    return text
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

fun prepareQuestionMarkdown(raw: String): String {
    return raw
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\\n", "\n")
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
        "高等数学" -> "高数"
        "线性代数" -> "线代"
        "概率统计" -> "概统"
        else -> subject
    }
}

private fun renderLatex(raw: String): String {
    var text = raw.trim()
    
    // Handle escaped braces
    text = text.replace("\\{", "{").replace("\\}", "}")
    
    // Explicitly handle A^* before other scripts to ensure it's perfect
    text = text.replace("A^*", "A﹡")
    text = text.replace("A^{*}", "A﹡")
    
    // Handle \text{...}
    text = replaceCommandWithOneGroup(text, "\\text") { it }
    text = replaceCommandWithOneGroup(text, "\\mathrm") { it }
    text = replaceCommandWithOneGroup(text, "\\mathbf") { it }
    
    // Handle fractions and binomials
    text = replaceCommandWithTwoGroups(text, "\\frac") { n, d -> 
        val rn = renderLatex(n)
        val rd = renderLatex(d)
        if (rn.length + rd.length > 10) "($rn)/($rd)" else "$rn/$rd"
    }
    text = replaceCommandWithTwoGroups(text, "\\binom") { n, k -> "(${renderLatex(n)} choose ${renderLatex(k)})" }
    
    // Handle sqrt
    text = replaceCommandWithOneGroup(text, "\\sqrt") { "√(${renderLatex(it)})" }
    
    // Handle accents
    text = replaceCommandWithOneGroup(text, "\\bar") { "${renderLatex(it)}\u0304" }
    text = replaceCommandWithOneGroup(text, "\\vec") { "${renderLatex(it)}\u20D7" }
    text = replaceCommandWithOneGroup(text, "\\hat") { "${renderLatex(it)}\u0302" }
    text = replaceCommandWithOneGroup(text, "\\tilde") { "${renderLatex(it)}\u0303" }

    text = text.replace(matrixEnvironmentPattern) { match ->
        renderSquareBracketMatrix(match.groupValues[2])
    }

    // Handle Matrix environments (p, b, B, v, V matrices)
    val matrixPattern = Regex("""\\begin\{([pbBvV]?)matrix\}(.*?)\\end\{\1matrix\}""", RegexOption.DOT_MATCHES_ALL)
    text = text.replace(matrixPattern) { match ->
        val type = match.groupValues[1]
        val content = match.groupValues[2].trim()
        
        val rows = content.split("\\\\")
        val renderedRows = rows.filter { it.isNotBlank() }.map { row ->
            row.trim().split("&").joinToString("  ") { it.trim() }
        }
        
        if (renderedRows.isEmpty()) return@replace ""
        
        val maxW = renderedRows.maxOf { it.length }
        val padded = renderedRows.map { it.padEnd(maxW) }
        
        val (topOpen, extOpen, midOpen, botOpen) = when(type) {
            "p" -> listOf("⎛", "⎜", "⎜", "⎝")
            "b" -> listOf("⎡", "⎢", "⎢", "⎣")
            "B" -> listOf("⎧", "⎪", "⎨", "⎩")
            "v" -> listOf("⎪", "⎪", "⎪", "⎪")
            "V" -> listOf("‖", "‖", "‖", "‖")
            else -> listOf("", "", "", "")
        }
        val (topClose, extClose, midClose, botClose) = when(type) {
            "p" -> listOf("⎞", "⎟", "⎟", "⎠")
            "b" -> listOf("⎤", "⎥", "⎥", "⎦")
            "B" -> listOf("⎫", "⎪", "⎬", "⎭")
            "v" -> listOf("⎪", "⎪", "⎪", "⎪")
            "V" -> listOf("‖", "‖", "‖", "‖")
            else -> listOf("", "", "", "")
        }

        if (padded.size == 1) {
            val open = when(type) { "p"->"("; "b"->"["; "B"->"{"; "v"->"|"; "V"->"‖"; else->"" }
            val close = when(type) { "p"->")"; "b"->"]"; "B"->"}"; "v"->"|"; "V"->"‖"; else->"" }
            "$open ${padded[0]} $close"
        } else {
            val sb = StringBuilder("\n")
            padded.forEachIndexed { i, row ->
                val left = when {
                    i == 0 -> topOpen
                    i == padded.lastIndex -> botOpen
                    type == "B" && i == padded.size / 2 -> midOpen
                    else -> extOpen
                }
                val right = when {
                    i == 0 -> topClose
                    i == padded.lastIndex -> botClose
                    type == "B" && i == padded.size / 2 -> midClose
                    else -> extClose
                }
                sb.append("$left $row $right\n")
            }
            sb.toString()
        }
    }

    // Handle cases environment
    val casesPattern = Regex("""\\begin\{cases\}(.*?)\\end\{cases\}""", RegexOption.DOT_MATCHES_ALL)
    text = text.replace(casesPattern) { match ->
        val content = match.groupValues[1].trim()
        val rows = content.split("\\\\").filter { it.isNotBlank() }
        val sb = StringBuilder("\n")
        rows.forEachIndexed { i, row ->
            val left = when {
                i == 0 -> "⎧"
                i == rows.lastIndex -> "⎩"
                i == rows.size / 2 -> "⎨"
                else -> "⎪"
            }
            sb.append("$left ${row.trim().replace("&", "  ")}\n")
        }
        sb.toString()
    }

    // Handle \underline{\hspace{...}} or just \underline{...}
    text = replaceCommandWithOneGroup(text, "\\underline") { content ->
        if (content.contains("\\hspace")) {
            "_____"
        } else {
            "_$content"
        }
    }
    text = text.replace(Regex("""\\hspace\{[^}]*\}"""), "  ")

    // Symbols replacement (sorted by length descending to prevent prefix conflicts)
    sortedLatexSymbols.forEach { entry ->
        text = text.replace(entry.key, entry.value)
    }
    
    // Handle scripts
    text = replaceScript(text, '^', superscriptMap)
    text = replaceScript(text, '_', subscriptMap)
    
    // Final cleanup: strip remaining backslash commands if they are not common functions
    text = text.replace(Regex("""\\([A-Za-z]+)""")) { match ->
        val cmd = match.groupValues[1]
        if (cmd in mathFunctions) cmd else ""
    }
    
    return text
        .replace("{", "")
        .replace("}", "")
        .lines()
        .joinToString("\n") { line -> line.replace(Regex("""[ \t]{3,}"""), " ").trimEnd() }
        .trim()
}

private val matrixEnvironmentPattern =
    Regex("""\\begin\{([pbBvV]?)matrix\}(.*?)\\end\{\1matrix\}""", RegexOption.DOT_MATCHES_ALL)

private fun renderSquareBracketMatrix(rawContent: String): String {
    val renderedRows = rawContent
        .trim()
        .split("\\\\")
        .map { row -> row.trim() }
        .filter { it.isNotBlank() }
        .map { row ->
            row.split("&").joinToString("  ") { cell -> renderLatex(cell.trim()) }
        }

    if (renderedRows.isEmpty()) {
        return ""
    }
    if (renderedRows.size == 1) {
        return "[ ${renderedRows.first()} ]"
    }

    val width = renderedRows.maxOf { it.length }
    val paddedRows = renderedRows.map { it.padEnd(width) }
    return buildString {
        append('\n')
        paddedRows.forEachIndexed { index, row ->
            val left = if (index == 0) "\u23a1" else if (index == paddedRows.lastIndex) "\u23a3" else "\u23a2"
            val right = if (index == 0) "\u23a4" else if (index == paddedRows.lastIndex) "\u23a6" else "\u23a5"
            append(left).append(' ').append(row).append(' ').append(right).append('\n')
        }
    }
}

private fun replaceCommandWithTwoGroups(
    source: String,
    command: String,
    replacement: (String, String) -> String,
): String {
    var text = source
    var lastFound = 0
    while (true) {
        val commandIndex = text.indexOf(command, lastFound)
        if (commandIndex < 0) return text
        
        val firstStart = text.indexOf('{', startIndex = commandIndex + command.length)
        if (firstStart < 0 || (firstStart - (commandIndex + command.length) > 2)) {
            lastFound = commandIndex + 1
            continue
        }
        
        val firstEnd = matchingBraceIndex(text, firstStart)
        if (firstEnd < 0) return text
        
        val secondStart = text.indexOf('{', startIndex = firstEnd + 1)
        if (secondStart < 0 || (secondStart - firstEnd > 2)) {
            lastFound = commandIndex + 1
            continue
        }
        
        val secondEnd = matchingBraceIndex(text, secondStart)
        if (secondEnd < 0) return text
        
        val rendered = replacement(
            text.substring(firstStart + 1, firstEnd),
            text.substring(secondStart + 1, secondEnd),
        )
        text = text.replaceRange(commandIndex, secondEnd + 1, rendered)
        lastFound = commandIndex + rendered.length
    }
}

private fun replaceCommandWithOneGroup(
    source: String,
    command: String,
    replacement: (String) -> String,
): String {
    var text = source
    var lastFound = 0
    while (true) {
        val commandIndex = text.indexOf(command, lastFound)
        if (commandIndex < 0) return text
        
        val groupStart = text.indexOf('{', startIndex = commandIndex + command.length)
        if (groupStart < 0 || (groupStart - (commandIndex + command.length) > 2)) {
            lastFound = commandIndex + 1
            continue
        }
        
        val groupEnd = matchingBraceIndex(text, groupStart)
        if (groupEnd < 0) return text
        
        val rendered = replacement(text.substring(groupStart + 1, groupEnd))
        text = text.replaceRange(commandIndex, groupEnd + 1, rendered)
        lastFound = commandIndex + rendered.length
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
            val content = source.substring(nextIndex + 1, end).replace(" ", "")
            index = end + 1
            content
        } else {
            val char = source[nextIndex]
            index += 2
            char.toString()
        }
        
        val mapped = token.map { map[it] }
        if (mapped.all { it != null }) {
            result.append(mapped.filterNotNull().joinToString(""))
        } else {
            if (token.length > 1) {
                result.append(token.map { map[it] ?: it }.joinToString(""))
            } else {
                result.append(marker).append(token)
            }
        }
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

private val mathFunctions = setOf(
    "log", "ln", "lg", "exp", "sin", "cos", "tan", "min", "max", "lim", "inf", "sup", "det", "arg"
)

private val latexSymbols = mapOf(
    "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
    "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
    "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
    "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
    "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
    "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
    "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
    "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
    "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω",
    "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\div" to "÷",
    "\\cdot" to "·", "\\cdots" to "···", "\\dots" to "...", "\\vdots" to "⋮", "\\ddots" to "⋱",
    "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥",
    "\\neq" to "≠", "\\ne" to "≠", "\\approx" to "≈", "\\equiv" to "≡",
    "\\sim" to "∼", "\\propto" to "∝", "\\ll" to "≪", "\\gg" to "≫",
    "\\sum" to "∑", "\\prod" to "∏",
    "\\subset" to "⊂", "\\supset" to "⊃", "\\subseteq" to "⊆", "\\supseteq" to "⊇",
    "\\iint" to "∬", "\\int" to "∫",
    "\\in" to "∈", "\\notin" to "∉", "\\cup" to "∪", "\\cap" to "∩",
    "\\setminus" to "∖", "\\forall" to "∀", "\\exists" to "∃", "\\neg" to "¬",
    "\\vee" to "∨", "\\land" to "∧", "\\oplus" to "⊕", "\\otimes" to "⊗",
    "\\perp" to "⊥", "\\angle" to "∠", "\\infty" to "∞", "\\partial" to "∂",
    "\\nabla" to "∇", "\\leftarrow" to "←", "\\rightarrow" to "→", "\\top" to "T", "\\to" to "→",
    "\\leftrightarrow" to "↔", "\\Leftarrow" to "⇐", "\\Rightarrow" to "⇒",
    "\\Leftrightarrow" to "⇔", "\\iff" to "⟺", "\\uparrow" to "↑", "\\downarrow" to "↓",
    "\\lfloor" to "⌊", "\\rfloor" to "⌋", "\\lceil" to "⌈", "\\rceil" to "⌉",
    "\\emptyset" to "∅"
)

private val sortedLatexSymbols = latexSymbols.entries.sortedByDescending { it.key.length }

private val superscriptMap = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
    'n' to 'ⁿ', 'i' to 'ⁱ', 'j' to 'ʲ', 'k' to 'ᵏ', 'm' to 'ᵐ',
    'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ', 'a' to 'ᵃ', 'b' to 'ᵇ',
    'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'r' to 'ʳ', 's' to 'ˢ', 't' to 'ᵗ',
    'T' to 'ᵀ', '*' to '﹡', 'H' to 'ᴴ',
    'A' to 'ᴬ', 'B' to 'ᴮ', 'C' to 'ᶜ', 'D' to 'ᴰ', 'E' to 'ᴱ',
    'F' to 'ᶠ', 'G' to 'ᴳ', 'I' to 'ᴵ', 'J' to 'ᴶ', 'K' to 'ᴷ',
    'L' to 'ᴸ', 'M' to 'ᴹ', 'N' to 'ᴺ', 'O' to 'ᴼ', 'P' to 'ᴾ',
    'R' to 'ᴿ', 'S' to 'ˢ', 'U' to 'ᵁ', 'V' to 'ⱽ', 'W' to 'ᵂ'
)

private val subscriptMap = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
    '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
    'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
    'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
    'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
    'v' to 'ᵥ', 'x' to 'ₓ', '*' to '⁎'
)
