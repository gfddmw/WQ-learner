package com.example.wq_learner1.data

import com.example.wq_learner1.network.ApiQuestion
import com.example.wq_learner1.network.QuestionApi
import com.example.wq_learner1.network.SessionState

sealed class QuestionBankResult {
    data object LoginRequired : QuestionBankResult()
    data class Loaded(val questions: List<MistakeQuestion>) : QuestionBankResult()
    data class Failed(val message: String) : QuestionBankResult()
}

class QuestionBankRepository(
    private val api: QuestionApi,
) {
    fun loadQuestions(
        sessionState: SessionState,
        subject: String? = null,
        chapter: String? = null,
    ): QuestionBankResult {
        val token = sessionState.accessToken ?: return QuestionBankResult.LoginRequired
        return try {
            val normalizedSubject = subject?.takeUnless { it == "全部" }
            val questions = api.listQuestions(
                token = token,
                subject = normalizedSubject,
                chapter = chapter,
            ).map { it.toMistakeQuestion() }
            QuestionBankResult.Loaded(questions)
        } catch (error: Exception) {
            QuestionBankResult.Failed(error.message ?: "题库加载失败")
        }
    }
}

private fun ApiQuestion.toMistakeQuestion(): MistakeQuestion {
    return MistakeQuestion(
        id = id,
        content = contentMdLatex,
        subject = subject,
        chapter = chapter,
        mastery = mastery,
    )
}
