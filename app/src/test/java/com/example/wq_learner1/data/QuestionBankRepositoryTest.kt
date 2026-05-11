package com.example.wq_learner1.data

import com.example.wq_learner1.network.ApiQuestion
import com.example.wq_learner1.network.AuthSession
import com.example.wq_learner1.network.QuestionApi
import com.example.wq_learner1.network.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionBankRepositoryTest {
    @Test
    fun returnsLoginRequiredWithoutTokenAndDoesNotCallApi() {
        val api = FakeQuestionApi()
        val repository = QuestionBankRepository(api)
        val sessionState = SessionState()

        val result = repository.loadQuestions(sessionState, subject = "数据结构")

        assertTrue(result is QuestionBankResult.LoginRequired)
        assertEquals(0, api.callCount)
    }

    @Test
    fun loadsQuestionsWithBearerTokenAndMapsApiFields() {
        val api = FakeQuestionApi(
            questions = listOf(
                ApiQuestion(
                    id = "Q-001",
                    userId = "U-1",
                    imageUrl = "/uploads/question.png",
                    contentMdLatex = "二叉树遍历的时间复杂度是 ${'$'}O(n)${'$'}。",
                    subject = "数据结构",
                    chapter = "树与二叉树",
                    status = "confirmed",
                    mastery = "reviewing",
                ),
            ),
        )
        val repository = QuestionBankRepository(api)
        val sessionState = SessionState()
        sessionState.setSession(AuthSession(accessToken = "abc123", tokenType = "bearer"), "demo@example.com")

        val result = repository.loadQuestions(sessionState, subject = "数据结构")

        assertTrue(result is QuestionBankResult.Loaded)
        val loaded = result as QuestionBankResult.Loaded
        assertEquals("abc123", api.lastToken)
        assertEquals("数据结构", api.lastSubject)
        assertEquals(1, loaded.questions.size)
        assertEquals("Q-001", loaded.questions.first().id)
        assertEquals("二叉树遍历的时间复杂度是 ${'$'}O(n)${'$'}。", loaded.questions.first().content)
        assertEquals("树与二叉树", loaded.questions.first().chapter)
    }
}

private class FakeQuestionApi(
    private val questions: List<ApiQuestion> = emptyList(),
) : QuestionApi {
    var callCount = 0
    var lastToken: String? = null
    var lastSubject: String? = null

    override fun listQuestions(token: String, subject: String?, chapter: String?): List<ApiQuestion> {
        callCount += 1
        lastToken = token
        lastSubject = subject
        return questions
    }
}
