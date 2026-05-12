package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionInteractionStateTest {
    private val questions = listOf(
        MistakeQuestion("Q-1", "one", "数据结构", "树", "mastered", "", ""),
        MistakeQuestion("Q-2", "two", "数学", "线性代数/矩阵论", "unfamiliar", "", ""),
        MistakeQuestion("Q-3", "three", "数学", "概率", "reviewing", "", ""),
    )

    @Test
    fun filtersBySubjectAndMastery() {
        val visible = questions.filterQuestions(subject = "数学", mastery = "unfamiliar")

        assertEquals(listOf("Q-2"), visible.map { it.id })
    }

    @Test
    fun allSubjectAndAllMasteryReturnEveryQuestion() {
        val visible = questions.filterQuestions(subject = QuestionFilters.ALL, mastery = QuestionFilters.ALL)

        assertEquals(listOf("Q-1", "Q-2", "Q-3"), visible.map { it.id })
    }

    @Test
    fun drawPracticeQuestionPrioritizesUnfamiliarThenReviewing() {
        val selected = questions.drawPracticeQuestion(previousQuestionId = null)

        assertEquals("Q-2", selected?.id)
    }

    @Test
    fun drawPracticeQuestionAvoidsRepeatingPreviousWhenPossible() {
        val selected = questions.drawPracticeQuestion(previousQuestionId = "Q-2")

        assertEquals("Q-3", selected?.id)
    }

    @Test
    fun updateMasteryChangesOnlyMatchingQuestion() {
        val updated = questions.updateMastery(questionId = "Q-2", mastery = "mastered")

        assertEquals("mastered", updated.first { it.id == "Q-2" }.mastery)
        assertEquals("mastered", updated.first { it.id == "Q-1" }.mastery)
        assertEquals("reviewing", updated.first { it.id == "Q-3" }.mastery)
    }

    @Test
    fun learningStatsCountMasteryStatesForWorkspaceSummary() {
        val stats = questions.learningStats()

        assertEquals(3, stats.total)
        assertEquals(1, stats.unfamiliar)
        assertEquals(1, stats.reviewing)
        assertEquals(1, stats.mastered)
        assertEquals(67, stats.activePercent)
    }

    @Test
    fun learningStatsHandleEmptyQuestionBank() {
        val stats = emptyList<MistakeQuestion>().learningStats()

        assertEquals(0, stats.total)
        assertEquals(0, stats.activePercent)
        assertEquals("先上传一道错题，建立你的复习工作台", stats.nextStepText)
    }

    @Test
    fun drawPracticeQuestionReturnsNullForEmptyList() {
        assertNull(emptyList<MistakeQuestion>().drawPracticeQuestion(previousQuestionId = null))
    }
}
