package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichQuestionTextTest {
    @Test
    fun rendersMarkdownAndInlineLatexAsReadableText() {
        val rendered = renderQuestionContent("**关键点**：计算 ${'$'}x_{i}^{2} + \\frac{1}{n}${'$'} 的值。")

        assertEquals("关键点：计算 xᵢ² + 1/n 的值。", rendered)
        assertFalse(rendered.contains("**"))
        assertFalse(rendered.contains("$"))
        assertFalse(rendered.contains("\\frac"))
    }

    @Test
    fun rendersBlockLatexAndCommonSymbols() {
        val rendered = renderQuestionContent("$$\\sqrt{x} \\leq \\alpha + \\beta$$")

        assertEquals("√(x) ≤ α + β", rendered)
    }

    @Test
    fun keepsListItemsReadableWithoutMarkdownBullets() {
        val rendered = renderQuestionContent("- TCP 拥塞窗口\n- 时间复杂度 `O(n)`")

        assertEquals("TCP 拥塞窗口\n时间复杂度 O(n)", rendered)
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
        assertFalse(rendered.hasRawQuestionMarkup())
    }
}
