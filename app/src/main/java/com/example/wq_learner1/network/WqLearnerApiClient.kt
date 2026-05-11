package com.example.wq_learner1.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiConfig {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
}

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val contentType: String = "application/json; charset=utf-8",
)

data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

interface HttpTransport {
    fun send(request: HttpRequest): HttpResponse
}

class UrlConnectionTransport(
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
) : HttpTransport {
    override fun send(request: HttpRequest): HttpResponse {
        val url = URL(baseUrl.trimEnd('/') + request.path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", request.contentType)
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        if (request.body.isNotEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { stream ->
                stream.write(request.body.toByteArray(Charsets.UTF_8))
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

data class ApiQuestion(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val contentMdLatex: String,
    val subject: String,
    val chapter: String,
    val status: String,
    val mastery: String,
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

    fun clear() {
        accessToken = null
        tokenType = null
        email = null
    }
}

class ApiException(message: String) : RuntimeException(message)

class WqLearnerApiClient(
    private val transport: HttpTransport = UrlConnectionTransport(),
) {
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

    fun listQuestions(token: String, subject: String? = null, chapter: String? = null): List<ApiQuestion> {
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

    private fun authBody(email: String, password: String): String {
        return """{"email":"${email.jsonEscape()}","password":"${password.jsonEscape()}"}"""
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
    )
}

private fun String.jsonObjects(): List<String> {
    return Regex("\\{[^{}]*}").findAll(this).map { it.value }.toList()
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
