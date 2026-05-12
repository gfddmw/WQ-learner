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

fun MutableList<MistakeQuestion>.replaceMasteryById(questionId: String, mastery: String): MistakeQuestion? {
    val existingIndex = indexOfFirst { it.id == questionId }
    if (existingIndex < 0) {
        return null
    }
    val updated = this[existingIndex].copy(mastery = mastery)
    this[existingIndex] = updated
    return updated
}
