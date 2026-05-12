package com.example.wq_learner1.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiConfig {
    const val FUNCTION_COMPUTE_BASE_URL = "https://backend-eyigeidcmc.cn-hangzhou.fcapp.run"
    const val DEFAULT_BASE_URL = FUNCTION_COMPUTE_BASE_URL

    fun normalizeBaseUrl(input: String): String {
        val normalized = input.trim().trimEnd('/')
        if (normalized.isBlank()) {
            return DEFAULT_BASE_URL
        }
        return normalized.takeIf { it.startsWith("https://") } ?: DEFAULT_BASE_URL
    }
}

class ApiEndpointState(
    initialBaseUrl: String = ApiConfig.DEFAULT_BASE_URL,
) {
    var baseUrl: String = ApiConfig.normalizeBaseUrl(initialBaseUrl)
        private set

    val environmentLabel: String
        get() = "函数计算公网 API"

    val statusText: String
        get() = "当前后端：$baseUrl（$environmentLabel）"

    fun updateBaseUrl(input: String) {
        baseUrl = ApiConfig.normalizeBaseUrl(input)
    }

    fun withBaseUrl(input: String): ApiEndpointState {
        return ApiEndpointState(input)
    }
}

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val bodyBytes: ByteArray = ByteArray(0),
    val contentType: String = "application/json; charset=utf-8",
) {
    fun payloadBytes(): ByteArray {
        return if (bodyBytes.isNotEmpty()) bodyBytes else body.toByteArray(Charsets.UTF_8)
    }
}

data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

interface HttpTransport {
    fun send(request: HttpRequest): HttpResponse
}

class UrlConnectionTransport(
    baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
) : HttpTransport {
    private val baseUrl = ApiConfig.normalizeBaseUrl(baseUrl)

    override fun send(request: HttpRequest): HttpResponse {
        val url = URL(baseUrl + request.path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            connectTimeout = 120_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", request.contentType)
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        val payloadBytes = request.payloadBytes()
        if (payloadBytes.isNotEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { stream ->
                stream.write(payloadBytes)
            }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val responseBody = stream?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: ""

        connection.disconnect()
        return HttpResponse(statusCode = statusCode, body = responseBody)
    }
}

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
)

data class StoredSession(
    val accessToken: String,
    val tokenType: String,
    val email: String,
) {
    val isComplete: Boolean
        get() = accessToken.isNotBlank() && tokenType.isNotBlank() && email.isNotBlank()
}

data class ApiQuestion(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val contentMdLatex: String,
    val subject: String,
    val chapter: String,
    val status: String,
    val mastery: String,
    val answerMdLatex: String = "",
    val explanationMdLatex: String = "",
)

data class ApiVariantQuestion(
    val sourceQuestionId: String,
    val title: String,
    val contentMdLatex: String,
    val answerMdLatex: String,
    val explanationMdLatex: String,
)

data class ApiPractice(
    val id: String,
    val mode: String,
    val variant: ApiVariantQuestion? = null,
)

data class ApiHealth(
    val status: String,
    val service: String,
    val runtime: String,
)

class SessionState {
    var accessToken: String? = null
        private set
    var tokenType: String? = null
        private set
    var email: String? = null
        private set

    val isLoggedIn: Boolean
        get() = accessToken != null

    fun setSession(session: AuthSession, email: String) {
        accessToken = session.accessToken
        tokenType = session.tokenType
        this.email = email
    }

    fun restore(session: StoredSession?) {
        if (session == null || !session.isComplete) {
            clear()
            return
        }
        accessToken = session.accessToken
        tokenType = session.tokenType
        email = session.email
    }

    fun snapshot(): StoredSession? {
        val currentAccessToken = accessToken
        val currentTokenType = tokenType
        val currentEmail = email
        if (currentAccessToken.isNullOrBlank() || currentTokenType.isNullOrBlank() || currentEmail.isNullOrBlank()) {
            return null
        }
        return StoredSession(
            accessToken = currentAccessToken,
            tokenType = currentTokenType,
            email = currentEmail,
        )
    }

    fun clear() {
        accessToken = null
        tokenType = null
        email = null
    }
}

class ApiException(message: String) : RuntimeException(message)

interface QuestionApi {
    fun listQuestions(token: String, subject: String? = null, chapter: String? = null): List<ApiQuestion>
}

class WqLearnerApiClient(
    private val transport: HttpTransport = UrlConnectionTransport(),
) : QuestionApi {
    constructor(baseUrl: String) : this(UrlConnectionTransport(baseUrl))

    fun register(email: String, password: String) {
        val response = transport.send(
            HttpRequest(
                method = "POST",
                path = "/auth/register",
                body = authBody(email, password),
            ),
        )
        requireSuccess(response)
    }

    fun login(email: String, password: String): AuthSession {
        val response = transport.send(
            HttpRequest(
                method = "POST",
                path = "/auth/login",
                body = authBody(email, password),
            ),
        )
        requireSuccess(response)
        return AuthSession(
            accessToken = response.body.jsonValue("access_token"),
            tokenType = response.body.jsonValue("token_type"),
        )
    }

    override fun listQuestions(token: String, subject: String?, chapter: String?): List<ApiQuestion> {
        val query = buildList {
            if (!subject.isNullOrBlank()) add("subject=${subject.urlEncode()}")
            if (!chapter.isNullOrBlank()) add("chapter=${chapter.urlEncode()}")
        }.joinToString(separator = "&")
        val path = if (query.isBlank()) "/questions" else "/questions?$query"
        val response = transport.send(
            HttpRequest(
                method = "GET",
                path = path,
                headers = mapOf("Authorization" to "Bearer $token"),
            ),
        )
        requireSuccess(response)
        return response.body.jsonObjects().map { it.toQuestion() }
    }

    fun uploadQuestion(
        token: String,
        imageBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): ApiQuestion {
        val boundary = "----WqLearnerUploadBoundary"
        val response = transport.send(
            HttpRequest(
                method = "POST",
                path = "/questions/upload",
                headers = mapOf("Authorization" to "Bearer $token"),
                bodyBytes = multipartImageBody(
                    boundary = boundary,
                    imageBytes = imageBytes,
                    fileName = fileName,
                    contentType = contentType,
                ),
                contentType = "multipart/form-data; boundary=$boundary",
            ),
        )
        requireSuccess(response)
        return response.body.toQuestion()
    }

    fun updateQuestion(
        token: String,
        questionId: String,
        contentMdLatex: String,
        subject: String,
        chapter: String,
        mastery: String,
        answerMdLatex: String = "",
        explanationMdLatex: String = "",
    ): ApiQuestion {
        val response = transport.send(
            HttpRequest(
                method = "PATCH",
                path = "/questions/${questionId.urlEncode()}",
                headers = mapOf("Authorization" to "Bearer $token"),
                body = questionUpdateBody(
                    contentMdLatex = contentMdLatex,
                    subject = subject,
                    chapter = chapter,
                    mastery = mastery,
                    answerMdLatex = answerMdLatex,
                    explanationMdLatex = explanationMdLatex,
                ),
            ),
        )
        requireSuccess(response)
        return response.body.toQuestion()
    }

    fun healthCheck(): ApiHealth {
        val response = transport.send(
            HttpRequest(
                method = "GET",
                path = "/health",
            ),
        )
        requireSuccess(response)
        return response.body.toHealth()
    }

    fun createVariantPractice(
        token: String,
        sourceQuestionId: String,
        topic: String,
    ): ApiPractice {
        val response = transport.send(
            HttpRequest(
                method = "POST",
                path = "/practice/variant",
                headers = mapOf("Authorization" to "Bearer $token"),
                body = variantPracticeBody(sourceQuestionId, topic),
            ),
        )
        requireSuccess(response)
        return response.body.toPractice()
    }

    private fun authBody(email: String, password: String): String {
        return """{"email":"${email.jsonEscape()}","password":"${password.jsonEscape()}"}"""
    }

    private fun variantPracticeBody(sourceQuestionId: String, topic: String): String {
        return """{"source_question_id":"${sourceQuestionId.jsonEscape()}","topic":"${topic.jsonEscape()}"}"""
    }

    private fun questionUpdateBody(
        contentMdLatex: String,
        subject: String,
        chapter: String,
        mastery: String,
        answerMdLatex: String,
        explanationMdLatex: String,
    ): String {
        return """{"content_md_latex":"${contentMdLatex.jsonEscape()}","subject":"${subject.jsonEscape()}","chapter":"${chapter.jsonEscape()}","mastery":"${mastery.jsonEscape()}","answer_md_latex":"${answerMdLatex.jsonEscape()}","explanation_md_latex":"${explanationMdLatex.jsonEscape()}"}"""
    }

    private fun multipartImageBody(
        boundary: String,
        imageBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): ByteArray {
        val safeFileName = fileName.ifBlank { "question.jpg" }.replace("\"", "")
        val prefix = buildString {
            append("--")
            append(boundary)
            append("\r\n")
            append("Content-Disposition: form-data; name=\"image\"; filename=\"")
            append(safeFileName)
            append("\"\r\n")
            append("Content-Type: ")
            append(contentType.ifBlank { "image/jpeg" })
            append("\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val suffix = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        return prefix + imageBytes + suffix
    }

    private fun requireSuccess(response: HttpResponse) {
        if (response.statusCode !in 200..299) {
            throw ApiException("HTTP ${response.statusCode}: ${response.body}")
        }
    }
}

private fun String.toQuestion(): ApiQuestion {
    return ApiQuestion(
        id = jsonValue("id"),
        userId = jsonValue("user_id"),
        imageUrl = jsonValue("image_url"),
        contentMdLatex = jsonValue("content_md_latex"),
        subject = jsonValue("subject"),
        chapter = jsonValue("chapter"),
        status = jsonValue("status"),
        mastery = jsonValue("mastery"),
        answerMdLatex = jsonValue("answer_md_latex"),
        explanationMdLatex = jsonValue("explanation_md_latex"),
    )
}

private fun String.toPractice(): ApiPractice {
    val id = jsonValue("id")
    val mode = jsonValue("mode")
    val variantTitle = jsonValue("title")
    
    // Only parse variant if title is found, indicating a non-null variant object
    val variant = if (variantTitle.isNotBlank()) {
        ApiVariantQuestion(
            sourceQuestionId = jsonValue("source_question_id"),
            title = variantTitle,
            contentMdLatex = jsonValue("content_md_latex"),
            answerMdLatex = jsonValue("answer_md_latex"),
            explanationMdLatex = jsonValue("explanation_md_latex"),
        )
    } else {
        null
    }
    
    return ApiPractice(id = id, mode = mode, variant = variant)
}

private fun String.toHealth(): ApiHealth {
    return ApiHealth(
        status = jsonValue("status"),
        service = jsonValue("service"),
        runtime = jsonValue("runtime"),
    )
}

private fun String.jsonObjects(): List<String> {
    val objects = mutableListOf<String>()
    var start = -1
    var depth = 0
    var inString = false
    var escaped = false

    forEachIndexed { index, char ->
        if (escaped) {
            escaped = false
            return@forEachIndexed
        }
        if (char == '\\' && inString) {
            escaped = true
            return@forEachIndexed
        }
        if (char == '"') {
            inString = !inString
            return@forEachIndexed
        }
        if (inString) {
            return@forEachIndexed
        }

        when (char) {
            '{' -> {
                if (depth == 0) {
                    start = index
                }
                depth += 1
            }
            '}' -> {
                depth -= 1
                if (depth == 0 && start >= 0) {
                    objects.add(substring(start, index + 1))
                    start = -1
                }
            }
        }
    }
    return objects
}

private fun String.jsonValue(key: String): String {
    val pattern = Regex(""""$key"\s*:\s*"((?:\\.|[^"])*)"""")
    return pattern.find(this)?.groupValues?.get(1)?.jsonUnescape().orEmpty()
}

private fun String.jsonEscape(): String {
    return replace("\\", "\\\\").replace("\"", "\\\"")
}

private fun String.jsonUnescape(): String {
    return replace("\\\"", "\"").replace("\\\\", "\\")
}

private fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8").replace("+", "%20")
}
