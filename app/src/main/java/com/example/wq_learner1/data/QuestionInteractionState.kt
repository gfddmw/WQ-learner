package com.example.wq_learner1.data

object QuestionFilters {
    const val ALL = "全部"

    val subjects = listOf(
        ALL,
        "数据结构",
        "计算机组成原理",
        "操作系统",
        "计算机网络",
        "数学",
    )

    val masteryStates = listOf(
        ALL,
        "unfamiliar",
        "reviewing",
        "mastered",
    )
}

fun masteryLabel(mastery: String): String {
    return when (mastery) {
        "unfamiliar" -> "仍不熟"
        "reviewing" -> "复习中"
        "mastered" -> "已掌握"
        else -> mastery.ifBlank { "未标记" }
    }
}

fun List<MistakeQuestion>.filterQuestions(subject: String, mastery: String): List<MistakeQuestion> {
    return filter { question ->
        val subjectMatches = subject == QuestionFilters.ALL || question.subject == subject
        val masteryMatches = mastery == QuestionFilters.ALL || question.mastery == mastery
        subjectMatches && masteryMatches
    }
}

fun List<MistakeQuestion>.drawPracticeQuestion(previousQuestionId: String?): MistakeQuestion? {
    if (isEmpty()) return null
    val priority = listOf("unfamiliar", "reviewing", "mastered")
    val ordered = sortedWith(
        compareBy<MistakeQuestion> { priority.indexOf(it.mastery).takeIf { index -> index >= 0 } ?: priority.size }
            .thenBy { it.id },
    )
    return ordered.firstOrNull { it.id != previousQuestionId } ?: ordered.first()
}

fun List<MistakeQuestion>.updateMastery(questionId: String, mastery: String): List<MistakeQuestion> {
    return map { question ->
        if (question.id == questionId) question.copy(mastery = mastery) else question
    }
}
