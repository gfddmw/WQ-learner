package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MistakeQuestionMergeTest {
    @Test
    fun upsertAddsNewQuestionToFront() {
        val questions = mutableListOf(
            MistakeQuestion(
                id = "Q-001",
                content = "旧题",
                subject = "数据结构",
                chapter = "树与二叉树",
                mastery = "reviewing",
            ),
        )

        questions.upsertFirstById(
            MistakeQuestion(
                id = "Q-002",
                content = "新题",
                subject = "计算机网络",
                chapter = "传输层",
                mastery = "unfamiliar",
            ),
        )

        assertEquals(listOf("Q-002", "Q-001"), questions.map { it.id })
    }

    @Test
    fun upsertReplacesExistingQuestionWithoutDuplicatingIt() {
        val questions = mutableListOf(
            MistakeQuestion(
                id = "Q-001",
                content = "旧内容",
                subject = "数据结构",
                chapter = "树与二叉树",
                mastery = "reviewing",
            ),
        )

        questions.upsertFirstById(
            MistakeQuestion(
                id = "Q-001",
                content = "新内容",
                subject = "数学",
                chapter = "线性代数/矩阵论",
                mastery = "unfamiliar",
            ),
        )

        assertEquals(1, questions.size)
        assertEquals("新内容", questions.first().content)
        assertEquals("数学", questions.first().subject)
    }
}
