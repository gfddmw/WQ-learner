package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichQuestionTextTest {
    @Test
    fun rendersMarkdownAndInlineLatexAsReadableText() {
        // Now preserves ** for UI styling
        val rendered = renderQuestionContent("**关键点**：计算 ${'$'}x_{i}^{2} + \\frac{1}{n}${'$'} 的值。")

        assertEquals("**关键点**：计算 xᵢ² + 1/n 的值。", rendered)
        assertTrue(rendered.contains("**"))
        assertFalse(rendered.contains("$"))
        assertFalse(rendered.contains("\\frac"))
    }

    @Test
    fun rendersBlockLatexAndCommonSymbols() {
        val rendered = renderQuestionContent("$$\\sqrt{x} \\leq \\alpha + \\beta$$")

        assertEquals("√(x) ≤ α + β", rendered.trim())
    }
    
    @Test
    fun rendersComplexLatexFor11408() {
        val rendered = renderQuestionContent("设 ${'$'}S = \\{1, 2, \\dots, n\\}${'$'}，则 ${'$'}|S| = n${'$'}。")
        // Note: \dots is handled by symbols map, { } are stripped at the end
        assertTrue(rendered.contains("S = 1, 2, ..., n"))
    }

    @Test
    fun keepsListItemsReadableWithBullets() {
        // Now preserves backticks for UI styling
        val rendered = renderQuestionContent("- TCP 拥塞窗口\n- 时间复杂度 `O(n)`")

        assertEquals("• TCP 拥塞窗口\n• 时间复杂度 `O(n)`", rendered)
    }

    @Test
    fun abbreviatesLongSubjectsForCompactControls() {
        assertEquals("计组", compactSubjectLabel("计算机组成原理"))
        assertEquals("全部", compactSubjectLabel("全部"))
        assertEquals("数学", compactSubjectLabel("数学"))
    }

    @Test
    fun detectsRawMarkupAfterRendering() {
        val rendered = renderQuestionContent("若 ${'$'}a_i \\geq 0${'$'}，证明 **单调性**。")

        assertTrue(rendered.isNotBlank())
        // hasRawQuestionMarkup checks for $ and \, so it should be false if LaTeX is rendered
        assertFalse(rendered.contains("$"))
        assertFalse(rendered.contains("\\geq"))
    }
}
