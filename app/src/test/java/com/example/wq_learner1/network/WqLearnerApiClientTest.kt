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
    fun uploadQuestionSendsMultipartImageAndParsesDraft() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    {
                      "id":"Q-UPLOAD",
                      "user_id":"U-1",
                      "image_url":"/uploads/users/U-1/questions/image.jpg",
                      "content_md_latex":"Uploaded draft ${'$'}O(n)${'$'}.",
                      "subject":"数据结构",
                      "chapter":"树与二叉树",
                      "status":"draft",
                      "mastery":"unfamiliar"
                    }
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val draft = client.uploadQuestion(
            token = "abc123",
            imageBytes = byteArrayOf(0x01, 0x23, 0x45),
            fileName = "mistake.jpg",
            contentType = "image/jpeg",
        )

        assertEquals("Q-UPLOAD", draft.id)
        assertEquals("draft", draft.status)
        assertEquals("POST", transport.lastRequest.method)
        assertEquals("/questions/upload", transport.lastRequest.path)
        assertEquals("Bearer abc123", transport.lastRequest.headers["Authorization"])
        assertTrue(transport.lastRequest.contentType.startsWith("multipart/form-data; boundary="))
        val bodyText = transport.lastRequest.bodyBytes.toString(Charsets.ISO_8859_1)
        assertTrue(bodyText.contains("""name="image"; filename="mistake.jpg""""))
        assertTrue(bodyText.contains("Content-Type: image/jpeg"))
        assertTrue(transport.lastRequest.bodyBytes.containsSubsequence(byteArrayOf(0x01, 0x23, 0x45)))
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

private fun ByteArray.containsSubsequence(expected: ByteArray): Boolean {
    return indices.any { start ->
        start + expected.size <= size && expected.indices.all { offset ->
            this[start + offset] == expected[offset]
        }
    }
}
