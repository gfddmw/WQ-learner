package com.example.wq_learner1.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WqLearnerApiClientTest {
    @Test
    fun loginSendsCredentialsAndReturnsToken() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """{"access_token":"abc123","token_type":"bearer"}""",
            ),
        )
        val client = WqLearnerApiClient(transport)

        val session = client.login("demo@example.com", "secret123")

        assertEquals("abc123", session.accessToken)
        assertEquals("bearer", session.tokenType)
        assertEquals("POST", transport.lastRequest.method)
        assertEquals("/auth/login", transport.lastRequest.path)
        assertTrue(transport.lastRequest.body.contains("demo@example.com"))
    }

    @Test
    fun listQuestionsSendsBearerTokenAndParsesQuestions() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    [
                      {
                        "id":"Q-001",
                        "user_id":"U-1",
                        "image_url":"/uploads/question.png",
                        "content_md_latex":"Binary tree costs ${'$'}O(n)${'$'}.",
                        "subject":"数据结构",
                        "chapter":"树与二叉树",
                        "status":"confirmed",
                        "mastery":"reviewing"
                      }
                    ]
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val questions = client.listQuestions("abc123", subject = "数据结构")

        assertEquals("Bearer abc123", transport.lastRequest.headers["Authorization"])
        assertEquals("/questions?subject=%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84", transport.lastRequest.path)
        assertEquals(1, questions.size)
        assertEquals("Q-001", questions.first().id)
        assertEquals("树与二叉树", questions.first().chapter)
    }

    @Test
    fun sessionStateStoresAndClearsLogin() {
        val state = SessionState()

        state.setSession(AuthSession(accessToken = "abc123", tokenType = "bearer"), "demo@example.com")

        assertTrue(state.isLoggedIn)
        assertEquals("abc123", state.accessToken)
        assertEquals("demo@example.com", state.email)

        state.clear()

        assertEquals(false, state.isLoggedIn)
        assertEquals(null, state.accessToken)
        assertEquals(null, state.email)
    }
}

private class FakeTransport(
    private val response: HttpResponse,
) : HttpTransport {
    lateinit var lastRequest: HttpRequest

    override fun send(request: HttpRequest): HttpResponse {
        lastRequest = request
        return response
    }
}
