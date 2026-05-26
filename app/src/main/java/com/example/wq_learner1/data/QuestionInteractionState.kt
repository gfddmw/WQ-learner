package com.example.wq_learner1.data

object QuestionFilters {
    const val ALL = "全部"

    val subjects = listOf(
        ALL,
        "数据结构",
        "计算机组成原理",
        "操作系统",
        "计算机网络",
        "高等数学",
        "线性代数",
        "概率统计",
    )

    val masteryStates = listOf(
        ALL,
        "unfamiliar",
        "reviewing",
        "mastered",
    )
}

data class LearningStats(
    val total: Int,
    val unfamiliar: Int,
    val reviewing: Int,
    val mastered: Int,
) {
    val activeCount: Int
        get() = unfamiliar + reviewing

    val activePercent: Int
        get() = if (total == 0) 0 else (((activeCount * 100.0) / total) + 0.5).toInt()

    val nextStepText: String
        get() = when {
            total == 0 -> "先上传一道错题，建立你的复习工作台"
            unfamiliar > 0 -> "优先复盘不熟题，先把薄弱点捞出来"
            reviewing > 0 -> "继续巩固复习中题目，今天适合做一次抽查"
            else -> "题库状态很好，可以生成变形题保持手感"
        }
}

fun List<MistakeQuestion>.learningStats(): LearningStats {
    return LearningStats(
        total = size,
        unfamiliar = count { it.mastery == "unfamiliar" },
        reviewing = count { it.mastery == "reviewing" },
        mastered = count { it.mastery == "mastered" },
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
    val candidates = filter { it.id != previousQuestionId }
    val pool = if (candidates.isEmpty()) this else candidates
    val priority = listOf("unfamiliar", "reviewing", "mastered")
    val bestMastery = pool.map { it.mastery }
        .minByOrNull { priority.indexOf(it).takeIf { idx -> idx >= 0 } ?: priority.size }
        ?: "unfamiliar"
    val bestCandidates = pool.filter { it.mastery == bestMastery }
    return bestCandidates.randomOrNull()
}

fun List<MistakeQuestion>.updateMastery(questionId: String, mastery: String): List<MistakeQuestion> {
    return map { question ->
        if (question.id == questionId) question.copy(mastery = mastery) else question
    }
}
