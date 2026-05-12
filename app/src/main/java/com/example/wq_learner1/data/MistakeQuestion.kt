package com.example.wq_learner1.data

data class MistakeQuestion(
    val id: String,
    val content: String,
    val subject: String,
    val chapter: String,
    val mastery: String,
    val answer: String = "",
    val explanation: String = "",
)

fun MutableList<MistakeQuestion>.upsertFirstById(question: MistakeQuestion) {
    val existingIndex = indexOfFirst { it.id == question.id }
    if (existingIndex >= 0) {
        removeAt(existingIndex)
    }
    add(0, question)
}
