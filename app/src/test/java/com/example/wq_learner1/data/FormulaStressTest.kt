package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaStressTest {

    @Test
    fun testAdvancedMathematicsFormulas() {
        // 1. Limits
        val limit = renderQuestionContent("${'$'}\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1${'$'}")
        assertEquals("limₓ→₀ sin x/x = 1", limit)

        // 2. Derivatives
        val derivative = renderQuestionContent("${'$'}\\frac{\\mathrm{d}y}{\\mathrm{d}x} = f'(x)${'$'}")
        assertEquals("dy/dx = f'(x)", derivative)

        // 3. Integrals (Requires \int -> ∫)
        val integral = renderQuestionContent("${'$'}\\int_a^b f(x) \\mathrm{d}x${'$'}")
        assertEquals("∫ₐᵇ f(x) dx", integral)

        // 4. Double Integrals (Requires \iint -> ∬)
        val doubleIntegral = renderQuestionContent("${'$'}\\iint_D f(x, y) \\mathrm{d}x\\mathrm{d}y${'$'}")
        assertEquals("∬_D f(x, y) dxdy", doubleIntegral)

        // 5. Infinite Series (Requires \sum -> ∑, \infty -> ∞)
        val series = renderQuestionContent("${'$'}\\sum_{n=1}^{\\infty} x^n${'$'}")
        assertEquals("∑ₙ₌₁^∞ xⁿ", series)
    }

    @Test
    fun testLinearAlgebraFormulas() {
        // 1. Transpose and Inverse
        val matrixOps = renderQuestionContent("${'$'}(A^\\top)^{-1} = (A^{-1})^\\top${'$'}")
        assertEquals("(Aᵀ)⁻¹ = (A⁻¹)ᵀ", matrixOps)

        // 2. Matrix Multiplication / Eigenvalues
        val eigen = renderQuestionContent("${'$'}A \\mathbf{x} = \\lambda \\mathbf{x}${'$'}")
        assertEquals("A x = λ x", eigen)

        // 3. Determinant
        val det = renderQuestionContent("${'$'}\\det(A) \\neq 0${'$'}")
        assertEquals("det(A) ≠ 0", det)
    }

    @Test
    fun testProbabilityFormulas() {
        // 1. Conditional Probability
        val condProb = renderQuestionContent("${'$'}P(A|B) = \\frac{P(AB)}{P(B)}${'$'}")
        assertEquals("P(A|B) = P(AB)/P(B)", condProb)

        // 2. Normal Distribution (Requires \sim -> ∼)
        val normal = renderQuestionContent("${'$'}X \\sim N(\\mu, \\sigma^2)${'$'}")
        assertEquals("X ∼ N(μ, σ²)", normal)
        
        // 3. Expectation with Summation (Requires \sum -> ∑)
        val expVal = renderQuestionContent("${'$'}E(X) = \\sum x_i p_i${'$'}")
        assertEquals("E(X) = ∑ xᵢ pᵢ", expVal)
    }

    @Test
    fun testComputerScience408Formulas() {
        // 1. Shannon's Formula
        val shannon = renderQuestionContent("${'$'}C = W \\log_2 (1 + \\frac{S}{N})${'$'}")
        assertEquals("C = W log₂ (1 + S/N)", shannon)

        // 2. Nyquist's Theorem
        val nyquist = renderQuestionContent("${'$'}C = 2W \\log_2 L${'$'}")
        assertEquals("C = 2W log₂ L", nyquist)

        // 3. Complexity (Requires \Theta -> Θ)
        val complexity = renderQuestionContent("${'$'}T(n) = \\Theta(n^2)${'$'}")
        assertEquals("T(n) = Θ(n²)", complexity)

        // 4. IEEE 754 Float representation
        val ieeeFloat = renderQuestionContent("${'$'}(-1)^S \\times (1 + M) \\times 2^{E - 127}${'$'}")
        assertEquals("(-1)ˢ × (1 + M) × 2ᴱ⁻¹²⁷", ieeeFloat)
    }
}
