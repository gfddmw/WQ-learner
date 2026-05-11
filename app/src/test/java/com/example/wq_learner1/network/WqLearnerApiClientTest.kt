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
    fun listQuestionsParsesMultipleObjectsWithoutRegexSyntaxError() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    [
                      {
                        "id":"Q-001",
                        "user_id":"U-1",
                        "image_url":"oss://wq-learner/q1.jpg",
                        "content_md_latex":"第一题",
                        "subject":"数据结构",
                        "chapter":"树与二叉树",
                        "status":"draft",
                        "mastery":"unfamiliar"
                      },
                      {
                        "id":"Q-002",
                        "user_id":"U-1",
                        "image_url":"oss://wq-learner/q2.jpg",
                        "content_md_latex":"第二题",
                        "subject":"计算机网络",
                        "chapter":"传输层",
                        "status":"draft",
                        "mastery":"reviewing"
                      }
                    ]
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val questions = client.listQuestions("abc123")

        assertEquals(2, questions.size)
        assertEquals("Q-001", questions[0].id)
        assertEquals("Q-002", questions[1].id)
    }

    @Test
    fun listQuestionsParsesLatexBracesInsideStringValues() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    [
                      {
                        "id":"Q-LATEX",
                        "user_id":"U-1",
                        "image_url":"oss://wq-learner/q.jpg",
                        "content_md_latex":"证明 ${'$'}A^{2}=A${'$'} 且 ${'$'}x_{i}^{T}x_i=1${'$'}。",
                        "subject":"数学",
                        "chapter":"线性代数/矩阵论",
                        "status":"draft",
                        "mastery":"unfamiliar"
                      }
                    ]
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val questions = client.listQuestions("abc123")

        assertEquals(1, questions.size)
        assertEquals("Q-LATEX", questions.first().id)
        assertEquals("证明 ${'$'}A^{2}=A${'$'} 且 ${'$'}x_{i}^{T}x_i=1${'$'}。", questions.first().contentMdLatex)
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
    fun createVariantPracticeSendsSourceQuestionAndParsesVariant() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    {
                      "id":"P-001",
                      "mode":"variant",
                      "questions":[],
                      "variant":{
                        "source_question_id":"Q-001",
                        "title":"二叉树遍历变式题",
                        "content_md_latex":"若输入规模变为 ${'$'}2n${'$'}，分析复杂度。",
                        "answer_md_latex":"仍为 ${'$'}O(n)${'$'}。",
                        "explanation_md_latex":"每个结点访问一次。"
                      }
                    }
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val practice = client.createVariantPractice(
            token = "abc123",
            sourceQuestionId = "Q-001",
            topic = "树与二叉树",
        )

        assertEquals("POST", transport.lastRequest.method)
        assertEquals("/practice/variant", transport.lastRequest.path)
        assertEquals("Bearer abc123", transport.lastRequest.headers["Authorization"])
        assertTrue(transport.lastRequest.body.contains("Q-001"))
        assertTrue(transport.lastRequest.body.contains("树与二叉树"))
        assertEquals("P-001", practice.id)
        assertEquals("variant", practice.mode)
        assertEquals("二叉树遍历变式题", practice.variant?.title)
        assertEquals("仍为 ${'$'}O(n)${'$'}。", practice.variant?.answerMdLatex)
    }

    @Test
    fun updateQuestionSendsPatchAndParsesUpdatedQuestion() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """
                    {
                      "id":"Q-001",
                      "user_id":"U-1",
                      "image_url":"oss://wq-learner/q1.jpg",
                      "content_md_latex":"Updated ${'$'}A^2=A${'$'}.",
                      "subject":"数学",
                      "chapter":"线性代数/矩阵论",
                      "status":"confirmed",
                      "mastery":"mastered"
                    }
                """.trimIndent(),
            ),
        )
        val client = WqLearnerApiClient(transport)

        val updated = client.updateQuestion(
            token = "abc123",
            questionId = "Q-001",
            contentMdLatex = "Updated ${'$'}A^2=A${'$'}.",
            subject = "数学",
            chapter = "线性代数/矩阵论",
            mastery = "mastered",
        )

        assertEquals("PATCH", transport.lastRequest.method)
        assertEquals("/questions/Q-001", transport.lastRequest.path)
        assertEquals("Bearer abc123", transport.lastRequest.headers["Authorization"])
        assertTrue(transport.lastRequest.body.contains("Updated ${'$'}A^2=A${'$'}."))
        assertTrue(transport.lastRequest.body.contains("mastered"))
        assertEquals("Q-001", updated.id)
        assertEquals("mastered", updated.mastery)
    }

    @Test
    fun healthCheckCallsCloudHealthEndpoint() {
        val transport = FakeTransport(
            HttpResponse(
                statusCode = 200,
                body = """{"status":"ok","service":"wq-learner-api","runtime":"fastapi"}""",
            ),
        )
        val client = WqLearnerApiClient(transport)

        val health = client.healthCheck()

        assertEquals("GET", transport.lastRequest.method)
        assertEquals("/health", transport.lastRequest.path)
        assertEquals("ok", health.status)
        assertEquals("wq-learner-api", health.service)
        assertEquals("fastapi", health.runtime)
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
