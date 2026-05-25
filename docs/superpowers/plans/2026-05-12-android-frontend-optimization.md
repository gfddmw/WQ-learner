# Android Frontend Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Android 端优化成“上传页安静、题库/练习任务化”的真实 App 前端，并让所有可见按钮都有明确功能。

**Architecture:** 保持现有单 Activity + Jetpack Compose 架构，但把可测试的按钮逻辑拆到 `data` 和 `network` 层，把通用 UI 组件拆到 `ui/components`。前端继续只连接函数计算公网 API，上传、确认入库、刷新题库、生成变形题、更新掌握状态都通过云端或明确本地状态完成。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Android Activity Result、JUnit4、现有 `WqLearnerApiClient` HTTP 客户端。

---

## File Structure

- Modify `app/src/main/java/com/example/wq_learner1/network/WqLearnerApiClient.kt`: 增加 `/questions/{id}` PATCH 更新、`/health` 检查能力，并让前端按钮能调用真实云端 API。
- Modify `app/src/test/java/com/example/wq_learner1/network/WqLearnerApiClientTest.kt`: 覆盖 PATCH 请求体、Bearer token、health 解析。
- Create `app/src/main/java/com/example/wq_learner1/data/QuestionInteractionState.kt`: 管理科目列表、掌握状态列表、题库筛选、抽题优先级、更新掌握状态。
- Create `app/src/test/java/com/example/wq_learner1/data/QuestionInteractionStateTest.kt`: 覆盖筛选、抽题、状态更新。
- Create `app/src/main/java/com/example/wq_learner1/ui/components/WqTaskComponents.kt`: 提供任务流卡片、状态标签、页面标题、操作按钮组、空状态。
- Modify `app/src/main/java/com/example/wq_learner1/MainActivity.kt`: 接入新组件和新状态逻辑，逐页重做上传、题库、练习、我的页面按钮行为。
- Modify `docs/superpowers/plans/2026-05-11-remaining-features.md`: 标记 Android 前端优化进展。

---

### Task 1: Android API Client Supports Question Update And Health Check

**Files:**
- Modify: `app/src/main/java/com/example/wq_learner1/network/WqLearnerApiClient.kt`
- Modify: `app/src/test/java/com/example/wq_learner1/network/WqLearnerApiClientTest.kt`

- [ ] **Step 1: Write failing tests for question update and health check**

Add these tests to `WqLearnerApiClientTest`:

```kotlin
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
```

- [ ] **Step 2: Run tests and verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.wq_learner1.network.WqLearnerApiClientTest"`

Expected before implementation: compile failure for missing `updateQuestion`, `healthCheck`, and `ApiHealth`.

- [ ] **Step 3: Implement API methods**

In `WqLearnerApiClient.kt`, add:

```kotlin
data class ApiHealth(
    val status: String,
    val service: String,
    val runtime: String,
)
```

Add methods inside `WqLearnerApiClient`:

```kotlin
fun updateQuestion(
    token: String,
    questionId: String,
    contentMdLatex: String,
    subject: String,
    chapter: String,
    mastery: String,
): ApiQuestion {
    val response = transport.send(
        HttpRequest(
            method = "PATCH",
            path = "/questions/${questionId.urlEncode()}",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = questionUpdateBody(contentMdLatex, subject, chapter, mastery),
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
    return ApiHealth(
        status = response.body.jsonValue("status"),
        service = response.body.jsonValue("service"),
        runtime = response.body.jsonValue("runtime"),
    )
}

private fun questionUpdateBody(
    contentMdLatex: String,
    subject: String,
    chapter: String,
    mastery: String,
): String {
    return """{"content_md_latex":"${contentMdLatex.jsonEscape()}","subject":"${subject.jsonEscape()}","chapter":"${chapter.jsonEscape()}","mastery":"${mastery.jsonEscape()}"}"""
}
```

- [ ] **Step 4: Run tests and verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.wq_learner1.network.WqLearnerApiClientTest"`

Expected: all tests in `WqLearnerApiClientTest` pass.

- [ ] **Step 5: Commit Task 1**

```powershell
git add app/src/main/java/com/example/wq_learner1/network/WqLearnerApiClient.kt app/src/test/java/com/example/wq_learner1/network/WqLearnerApiClientTest.kt
git commit -m "feat: add android question update api"
```

---

### Task 2: Extract Question Filtering And Practice State Logic

**Files:**
- Create: `app/src/main/java/com/example/wq_learner1/data/QuestionInteractionState.kt`
- Create: `app/src/test/java/com/example/wq_learner1/data/QuestionInteractionStateTest.kt`

- [ ] **Step 1: Write failing tests for filters, draw logic, and mastery update**

Create `QuestionInteractionStateTest.kt`:

```kotlin
package com.example.wq_learner1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionInteractionStateTest {
    private val questions = listOf(
        MistakeQuestion("Q-1", "one", "数据结构", "树", "mastered"),
        MistakeQuestion("Q-2", "two", "数学", "线性代数/矩阵论", "unfamiliar"),
        MistakeQuestion("Q-3", "three", "数学", "概率", "reviewing"),
    )

    @Test
    fun filtersBySubjectAndMastery() {
        val visible = questions.filterQuestions(subject = "数学", mastery = "unfamiliar")

        assertEquals(listOf("Q-2"), visible.map { it.id })
    }

    @Test
    fun allSubjectAndAllMasteryReturnEveryQuestion() {
        val visible = questions.filterQuestions(subject = QuestionFilters.ALL, mastery = QuestionFilters.ALL)

        assertEquals(listOf("Q-1", "Q-2", "Q-3"), visible.map { it.id })
    }

    @Test
    fun drawPracticeQuestionPrioritizesUnfamiliarThenReviewing() {
        val selected = questions.drawPracticeQuestion(previousQuestionId = null)

        assertEquals("Q-2", selected?.id)
    }

    @Test
    fun drawPracticeQuestionAvoidsRepeatingPreviousWhenPossible() {
        val selected = questions.drawPracticeQuestion(previousQuestionId = "Q-2")

        assertEquals("Q-3", selected?.id)
    }

    @Test
    fun updateMasteryChangesOnlyMatchingQuestion() {
        val updated = questions.updateMastery(questionId = "Q-2", mastery = "mastered")

        assertEquals("mastered", updated.first { it.id == "Q-2" }.mastery)
        assertEquals("mastered", updated.first { it.id == "Q-1" }.mastery)
        assertEquals("reviewing", updated.first { it.id == "Q-3" }.mastery)
    }

    @Test
    fun drawPracticeQuestionReturnsNullForEmptyList() {
        assertNull(emptyList<MistakeQuestion>().drawPracticeQuestion(previousQuestionId = null))
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.wq_learner1.data.QuestionInteractionStateTest"`

Expected before implementation: compile failure for missing helpers.

- [ ] **Step 3: Implement question state helpers**

Create `QuestionInteractionState.kt`:

```kotlin
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
        if (question.id == questionId) {
            question.copy(mastery = mastery)
        } else {
            question
        }
    }
}
```

- [ ] **Step 4: Run test and verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.wq_learner1.data.QuestionInteractionStateTest"`

Expected: tests pass.

- [ ] **Step 5: Commit Task 2**

```powershell
git add app/src/main/java/com/example/wq_learner1/data/QuestionInteractionState.kt app/src/test/java/com/example/wq_learner1/data/QuestionInteractionStateTest.kt
git commit -m "feat: add android question interaction state"
```

---

### Task 3: Extract Reusable Task-Flow UI Components

**Files:**
- Create: `app/src/main/java/com/example/wq_learner1/ui/components/WqTaskComponents.kt`
- Modify: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Create reusable UI components**

Create `WqTaskComponents.kt`:

```kotlin
package com.example.wq_learner1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WqScreen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun WqPageHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WqTaskCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun WqStatusPill(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
fun WqActionRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = { content() })
}

@Composable
fun WqEmptyState(text: String) {
    WqTaskCard(title = "暂无内容") {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WqSpacer() {
    Spacer(Modifier.height(8.dp))
}
```

- [ ] **Step 2: Replace old local components with imports**

In `MainActivity.kt`, import:

```kotlin
import com.example.wq_learner1.ui.components.WqActionRow
import com.example.wq_learner1.ui.components.WqEmptyState
import com.example.wq_learner1.ui.components.WqPageHeader
import com.example.wq_learner1.ui.components.WqScreen
import com.example.wq_learner1.ui.components.WqSpacer
import com.example.wq_learner1.ui.components.WqStatusPill
import com.example.wq_learner1.ui.components.WqTaskCard
```

Then replace usages:
- `ScreenColumn` -> `WqScreen`
- `ScreenTitle` -> `WqPageHeader`
- `InfoCard` -> `WqTaskCard`
- `EmptyState` -> `WqEmptyState`

Remove the old local composables only after all call sites compile.

- [ ] **Step 3: Run compile-focused test**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests pass and `MainActivity.kt` compiles with extracted components.

- [ ] **Step 4: Commit Task 3**

```powershell
git add app/src/main/java/com/example/wq_learner1/ui/components/WqTaskComponents.kt app/src/main/java/com/example/wq_learner1/MainActivity.kt
git commit -m "refactor: extract android task ui components"
```

---

### Task 4: Rebuild Upload Page With Quiet Workbench Flow

**Files:**
- Modify: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Change upload state to separate draft and confirm actions**

Inside `UploadScreen`, keep these state values:

```kotlin
var draftQuestionId by remember { mutableStateOf<String?>(null) }
var imageState by remember { mutableStateOf(ImageSelectionState()) }
var cameraState by remember { mutableStateOf(CameraCaptureState()) }
var isUploading by remember { mutableStateOf(false) }
var isConfirming by remember { mutableStateOf(false) }
var status by remember { mutableStateOf("选择图片后会自动上传云端识别") }
var draftContent by remember { mutableStateOf("") }
var subject by remember { mutableStateOf("数据结构") }
var chapter by remember { mutableStateOf("待分类") }
var mastery by remember { mutableStateOf("unfamiliar") }
```

- [ ] **Step 2: Replace upload function with URI-based auto recognition**

Use this function shape:

```kotlin
fun recognizeSelectedImage(selectedImageUri: String) {
    val token = sessionState.accessToken
    if (token.isNullOrBlank()) {
        status = "请先在“我的”页登录后再上传错题。"
        return
    }
    if (isUploading) {
        status = "正在识别，请稍等。"
        return
    }
    val imageUri = Uri.parse(selectedImageUri)
    imageState = imageState.select(selectedImageUri)
    isUploading = true
    status = "正在上传图片并识别题干..."
    Thread {
        try {
            val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { input ->
                input.readBytes()
            } ?: throw IllegalStateException("无法读取所选图片")
            val detectedContentType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val fileName = imageUri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "question-upload.jpg" }
                ?: "question-upload.jpg"
            val draft = apiClient.uploadQuestion(token, imageBytes, fileName, detectedContentType)
            mainHandler.post {
                draftQuestionId = draft.id
                draftContent = draft.contentMdLatex
                subject = draft.subject
                chapter = draft.chapter
                mastery = draft.mastery
                onSave(MistakeQuestion(draft.id, draft.contentMdLatex, draft.subject, draft.chapter, draft.mastery))
                status = "识别完成，请校正后确认入库。"
                isUploading = false
            }
        } catch (error: Exception) {
            mainHandler.post {
                status = "识别失败：${error.message}"
                isUploading = false
            }
        }
    }.start()
}
```

- [ ] **Step 3: Wire camera and gallery buttons to auto recognition**

Gallery launcher callback:

```kotlin
) { uri ->
    if (uri != null) {
        recognizeSelectedImage(uri.toString())
    }
}
```

Camera launcher success branch:

```kotlin
if (result.imageState != null) {
    val selected = result.imageState.selectedImageUri
    if (!selected.isNullOrBlank()) {
        recognizeSelectedImage(selected)
    }
} else {
    status = "已取消拍照"
}
```

- [ ] **Step 4: Add confirm draft button**

Add:

```kotlin
fun confirmDraft() {
    val token = sessionState.accessToken
    val questionId = draftQuestionId
    if (token.isNullOrBlank()) {
        status = "请先登录后再确认入库。"
        return
    }
    if (questionId.isNullOrBlank()) {
        status = "请先拍照或选图识别题干。"
        return
    }
    isConfirming = true
    status = "正在确认入库..."
    Thread {
        try {
            val updated = apiClient.updateQuestion(token, questionId, draftContent, subject, chapter, mastery)
            mainHandler.post {
                onSave(MistakeQuestion(updated.id, updated.contentMdLatex, updated.subject, updated.chapter, updated.mastery))
                status = "已确认入库：${updated.id}"
                isConfirming = false
            }
        } catch (error: Exception) {
            mainHandler.post {
                status = "确认入库失败：${error.message}"
                isConfirming = false
            }
        }
    }.start()
}
```

The visible button text is:

```kotlin
Button(
    onClick = { confirmDraft() },
    enabled = !isUploading && !isConfirming,
    modifier = Modifier.fillMaxWidth(),
) {
    Text(if (isConfirming) "确认中..." else "确认入库")
}
```

- [ ] **Step 5: Make clear button clear the full upload state**

Clear action:

```kotlin
imageState = imageState.clear()
draftQuestionId = null
draftContent = ""
subject = "数据结构"
chapter = "待分类"
mastery = "unfamiliar"
status = "已清除图片和识别草稿"
```

- [ ] **Step 6: Run Android unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests pass.

- [ ] **Step 7: Commit Task 4**

```powershell
git add app/src/main/java/com/example/wq_learner1/MainActivity.kt
git commit -m "feat: improve android upload workflow"
```

---

### Task 5: Rebuild Question Bank Page With Filters And Expandable Cards

**Files:**
- Modify: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Add subject and mastery filter state**

Inside `QuestionBankScreen`, use:

```kotlin
var selectedSubject by remember { mutableStateOf(QuestionFilters.ALL) }
var selectedMastery by remember { mutableStateOf(QuestionFilters.ALL) }
var expandedQuestionId by remember { mutableStateOf<String?>(null) }
val visibleQuestions = questions.filterQuestions(selectedSubject, selectedMastery)
```

Import:

```kotlin
import com.example.wq_learner1.data.QuestionFilters
import com.example.wq_learner1.data.filterQuestions
import com.example.wq_learner1.data.masteryLabel
```

- [ ] **Step 2: Replace subject buttons with selected pills**

Render:

```kotlin
WqActionRow {
    QuestionFilters.subjects.forEach { subject ->
        WqStatusPill(
            text = subject,
            selected = selectedSubject == subject,
            onClick = { selectedSubject = subject },
        )
    }
}
```

Render mastery row:

```kotlin
WqActionRow {
    QuestionFilters.masteryStates.forEach { mastery ->
        WqStatusPill(
            text = if (mastery == QuestionFilters.ALL) QuestionFilters.ALL else masteryLabel(mastery),
            selected = selectedMastery == mastery,
            onClick = { selectedMastery = mastery },
        )
    }
}
```

- [ ] **Step 3: Refresh from cloud using selected subject**

Keep `refreshFromBackend`, but pass no subject for `全部`:

```kotlin
val subjectQuery = selectedSubject.takeUnless { it == QuestionFilters.ALL }
val result = repository.loadQuestions(sessionState, subject = subjectQuery)
```

- [ ] **Step 4: Make question cards expandable**

Change `QuestionCard` signature:

```kotlin
@Composable
private fun QuestionCard(
    question: MistakeQuestion,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
)
```

Card content:

```kotlin
WqTaskCard(title = "${question.subject} / ${question.chapter}") {
    Text(masteryLabel(question.mastery), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Text(if (expanded) question.content else question.content.take(96))
    OutlinedButton(onClick = onToggleExpanded) {
        Text(if (expanded) "收起详情" else "查看详情")
    }
}
```

Call site:

```kotlin
visibleQuestions.forEach { question ->
    QuestionCard(
        question = question,
        expanded = expandedQuestionId == question.id,
        onToggleExpanded = {
            expandedQuestionId = if (expandedQuestionId == question.id) null else question.id
        },
    )
}
```

- [ ] **Step 5: Run Android unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests pass.

- [ ] **Step 6: Commit Task 5**

```powershell
git add app/src/main/java/com/example/wq_learner1/MainActivity.kt
git commit -m "feat: improve android question bank ui"
```

---

### Task 6: Complete Practice Page Buttons

**Files:**
- Modify: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Add practice state**

Inside `PracticeScreen`, use:

```kotlin
var mode by remember { mutableStateOf("original") }
var currentQuestionId by remember { mutableStateOf<String?>(null) }
val current = questions.firstOrNull { it.id == currentQuestionId } ?: questions.drawPracticeQuestion(null)
var status by remember { mutableStateOf("选择一种练习方式。") }
var variant by remember { mutableStateOf<ApiVariantQuestion?>(null) }
```

Import:

```kotlin
import com.example.wq_learner1.data.drawPracticeQuestion
import com.example.wq_learner1.data.updateMastery
```

- [ ] **Step 2: Implement draw original button**

Add:

```kotlin
fun drawOriginalQuestion() {
    val next = questions.drawPracticeQuestion(previousQuestionId = currentQuestionId)
    if (next == null) {
        status = "题库为空，请先上传或刷新错题。"
        mode = "original"
        return
    }
    currentQuestionId = next.id
    variant = null
    mode = "original"
    status = "已抽取：${next.subject} / ${next.chapter}"
}
```

Button:

```kotlin
Button(onClick = { drawOriginalQuestion() }) {
    Text("抽现有错题")
}
```

- [ ] **Step 3: Make variant generation use current or drawn question**

At the start of `generateVariant`:

```kotlin
val source = current ?: questions.drawPracticeQuestion(previousQuestionId = null)
if (source == null) {
    status = "题库为空，请先上传或刷新错题。"
    mode = "variant"
    return
}
currentQuestionId = source.id
```

Keep the existing `/practice/variant` call, using `source.id` and `source.chapter`.

- [ ] **Step 4: Implement mastery feedback buttons**

Add:

```kotlin
fun updateCurrentMastery(nextMastery: String) {
    val token = sessionState.accessToken
    val source = current
    if (source == null) {
        status = "请先抽取一道题。"
        return
    }
    val localUpdated = questions.updateMastery(source.id, nextMastery)
    questions.clear()
    questions.addAll(localUpdated)
    status = "已标记为：${masteryLabel(nextMastery)}"
    if (token.isNullOrBlank()) {
        status = "已在本地标记为：${masteryLabel(nextMastery)}；登录后可同步云端。"
        return
    }
    Thread {
        try {
            apiClient.updateQuestion(token, source.id, source.content, source.subject, source.chapter, nextMastery)
        } catch (error: Exception) {
            mainHandler.post {
                status = "本地已更新，云端同步失败：${error.message}"
            }
        }
    }.start()
}
```

Change `ReviewButtons` signature:

```kotlin
@Composable
private fun ReviewButtons(onReview: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onReview("unfamiliar") }) { Text("仍不熟") }
        OutlinedButton(onClick = { onReview("reviewing") }) { Text("复习中") }
        Button(onClick = { onReview("mastered") }) { Text("已掌握") }
    }
}
```

Call:

```kotlin
ReviewButtons(onReview = ::updateCurrentMastery)
```

- [ ] **Step 5: Run Android unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests pass.

- [ ] **Step 6: Commit Task 6**

```powershell
git add app/src/main/java/com/example/wq_learner1/MainActivity.kt
git commit -m "feat: complete android practice actions"
```

---

### Task 7: Polish My Page And Cloud Status Actions

**Files:**
- Modify: `app/src/main/java/com/example/wq_learner1/MainActivity.kt`

- [ ] **Step 1: Add cloud status check state**

Inside `MeScreen`, add:

```kotlin
var cloudStatus by remember { mutableStateOf("未检查") }
var isCheckingCloud by remember { mutableStateOf(false) }
```

- [ ] **Step 2: Add health check action**

Add:

```kotlin
fun checkCloudStatus() {
    if (isCheckingCloud) return
    isCheckingCloud = true
    cloudStatus = "正在检查云端服务..."
    Thread {
        try {
            val health = apiClient.healthCheck()
            cloudStatus = "FC 正常：${health.service} / ${health.runtime}"
        } catch (error: Exception) {
            cloudStatus = "云端检查失败：${error.message}"
        } finally {
            isCheckingCloud = false
        }
    }.start()
}
```

- [ ] **Step 3: Hide development wording and show real cloud state**

Replace the old “开发状态” card with:

```kotlin
WqTaskCard(title = "云端状态") {
    Text(endpointState.statusText)
    Text("账号：${sessionState.email ?: "未登录"}")
    Text("连接：$cloudStatus", fontWeight = FontWeight.Bold)
    Button(
        onClick = { checkCloudStatus() },
        enabled = !isCheckingCloud,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isCheckingCloud) "检查中..." else "检查云端状态")
    }
}
```

- [ ] **Step 4: Keep API address visible but non-disruptive**

Keep the API address field and “应用地址” button, but place it below account controls. When the user applies a new address, keep current behavior:

```kotlin
onBaseUrlChange(endpointDraft)
tokenPreview = ""
endpointDraft = endpointState.withBaseUrl(endpointDraft).baseUrl
status = "已应用云端 API 地址，请重新登录"
```

- [ ] **Step 5: Run Android unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: tests pass.

- [ ] **Step 6: Commit Task 7**

```powershell
git add app/src/main/java/com/example/wq_learner1/MainActivity.kt
git commit -m "feat: polish android account page"
```

---

### Task 8: Final Verification And Documentation Update

**Files:**
- Modify: `docs/superpowers/plans/2026-05-11-remaining-features.md`

- [x] **Step 1: Update progress document**

Add or update checklist lines:

```markdown
- [x] Android 前端完成任务流卡片化优化。
- [x] 上传页支持拍照/相册后自动识别，并可确认入库。
- [x] 题库页支持云端刷新、科目筛选、掌握状态筛选和详情展开。
- [x] 练习页支持抽题、变形题和掌握状态按钮。
- [x] 我的页隐藏开发痕迹，并支持云端状态检查。
```

- [x] **Step 2: Run all Android unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat assembleDebug`

Expected in a JDK 17 environment: `BUILD SUCCESSFUL`.

If the local shell still uses Java 11, record this exact limitation in the final report:

```text
assembleDebug 未在当前 shell 完成：Android Gradle plugin requires Java 17；当前 JAVA_HOME 指向 Java 11。
```

- [x] **Step 4: Run backend regression tests**

Run: `pytest backend\tests -v`

Expected: all backend tests pass.

- [x] **Step 5: Commit final documentation**

```powershell
git add docs/superpowers/plans/2026-05-11-remaining-features.md
git commit -m "docs: update android frontend progress"
```

- [x] **Step 6: Ask user before pushing**

Report completed features and verification. Ask: `是否现在提交并上传到 GitHub？`

---

## Self-Review

Spec coverage:
- 上传页安静工作台：Task 4。
- 题库任务流卡片、筛选、详情：Task 5。
- 练习抽题、变形题、掌握状态：Task 2 and Task 6。
- 我的页账号和云端状态：Task 1 and Task 7。
- 所有按钮有行为：Task 4 through Task 7。
- 测试和验收：Task 1, Task 2, Task 8。

Placeholder scan:
- This plan intentionally contains no placeholder markers and no empty implementation steps.

Type consistency:
- `ApiHealth`, `updateQuestion`, `healthCheck`, `QuestionFilters`, `filterQuestions`, `drawPracticeQuestion`, `updateMastery`, and `masteryLabel` are introduced before their later use.
