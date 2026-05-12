package com.example.wq_learner1

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wq_learner1.data.CameraCaptureState
import com.example.wq_learner1.data.ImageSelectionState
import com.example.wq_learner1.data.MistakeQuestion
import com.example.wq_learner1.data.QuestionFilters
import com.example.wq_learner1.data.QuestionBankRepository
import com.example.wq_learner1.data.QuestionBankResult
import com.example.wq_learner1.data.drawPracticeQuestion
import com.example.wq_learner1.data.filterQuestions
import com.example.wq_learner1.data.masteryLabel
import com.example.wq_learner1.data.updateMastery
import com.example.wq_learner1.data.upsertFirstById
import com.example.wq_learner1.domain.SubjectClassifier
import com.example.wq_learner1.network.ApiConfig
import com.example.wq_learner1.network.ApiEndpointState
import com.example.wq_learner1.network.ApiVariantQuestion
import com.example.wq_learner1.network.SessionState
import com.example.wq_learner1.network.SharedPreferencesSessionStore
import com.example.wq_learner1.network.WqLearnerApiClient
import com.example.wq_learner1.ui.components.WqActionRow
import com.example.wq_learner1.ui.components.WqEmptyState
import com.example.wq_learner1.ui.components.WqPageHeader
import com.example.wq_learner1.ui.components.WqScreen
import com.example.wq_learner1.ui.components.WqStatusPill
import com.example.wq_learner1.ui.components.WqTaskCard
import com.example.wq_learner1.ui.theme.WQlearner1Theme

private enum class MainTab(val label: String) {
    Upload("上传"),
    Bank("题库"),
    Practice("练习"),
    Me("我的"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WQlearner1Theme {
                WqLearnerApp()
            }
        }
    }
}

@Composable
private fun WqLearnerApp() {
    var selectedTab by remember { mutableStateOf(MainTab.Upload) }
    val context = LocalContext.current
    val sessionStore = remember { SharedPreferencesSessionStore(context.applicationContext) }
    val sessionState = remember {
        SessionState().apply {
            restore(sessionStore.load())
        }
    }
    var endpointState by remember { mutableStateOf(ApiEndpointState()) }
    val apiClient = remember(endpointState.baseUrl) { WqLearnerApiClient(endpointState.baseUrl) }
    val questionBankRepository = remember(apiClient) { QuestionBankRepository(apiClient) }
    val questions = remember { mutableStateListOf<MistakeQuestion>() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.label.take(1)) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (selectedTab) {
                MainTab.Upload -> UploadScreen(
                    sessionState = sessionState,
                    apiClient = apiClient,
                    onSave = { question ->
                        questions.upsertFirstById(question)
                    },
                )
                MainTab.Bank -> QuestionBankScreen(
                    questions = questions,
                    sessionState = sessionState,
                    repository = questionBankRepository,
                )
                MainTab.Practice -> PracticeScreen(
                    questions = questions,
                    sessionState = sessionState,
                    apiClient = apiClient,
                )
                MainTab.Me -> MeScreen(
                    sessionState = sessionState,
                    apiClient = apiClient,
                    endpointState = endpointState,
                    onBaseUrlChange = { nextBaseUrl ->
                        endpointState = endpointState.withBaseUrl(nextBaseUrl)
                        sessionStore.clear()
                        sessionState.clear()
                    },
                    onSessionSaved = {
                        sessionState.snapshot()?.let(sessionStore::save)
                    },
                    onSessionCleared = {
                        sessionStore.clear()
                    },
                )
            }
        }
    }
}

@Composable
private fun UploadScreen(
    sessionState: SessionState,
    apiClient: WqLearnerApiClient,
    onSave: (MistakeQuestion) -> Unit,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var imageState by remember { mutableStateOf(ImageSelectionState()) }
    var cameraState by remember { mutableStateOf(CameraCaptureState()) }
    var draftQuestionId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("选择图片后会自动上传云端识别") }
    var draftContent by remember { mutableStateOf("") }
    val classification = SubjectClassifier.classify(draftContent)
    var subject by remember { mutableStateOf(classification.subject.ifBlank { "数据结构" }) }
    var chapter by remember { mutableStateOf(classification.chapter.ifBlank { "待分类" }) }
    var mastery by remember { mutableStateOf("unfamiliar") }

    fun createCameraImageUri(): Uri {
        val imageDir = File(context.cacheDir, "camera-images").apply { mkdirs() }
        val imageFile = File.createTempFile("wq-question-", ".jpg", imageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }

    fun recognizeSelectedImage(selectedImageUri: String) {
        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            status = "请先在“我的”页面登录后再上传错题。"
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
                val fileName = imageUri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.ifBlank { "question-upload.jpg" }
                    ?: "question-upload.jpg"
                val draft = apiClient.uploadQuestion(
                    token = token,
                    imageBytes = imageBytes,
                    fileName = fileName,
                    contentType = detectedContentType,
                )

                mainHandler.post {
                    draftQuestionId = draft.id
                    draftContent = draft.contentMdLatex
                    subject = draft.subject
                    chapter = draft.chapter
                    mastery = draft.mastery
                    onSave(
                        MistakeQuestion(
                            id = draft.id,
                            content = draft.contentMdLatex,
                            subject = draft.subject,
                            chapter = draft.chapter,
                            mastery = draft.mastery,
                        ),
                    )
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
                val updated = apiClient.updateQuestion(
                    token = token,
                    questionId = questionId,
                    contentMdLatex = draftContent,
                    subject = subject,
                    chapter = chapter,
                    mastery = mastery,
                )
                mainHandler.post {
                    onSave(
                        MistakeQuestion(
                            id = updated.id,
                            content = updated.contentMdLatex,
                            subject = updated.subject,
                            chapter = updated.chapter,
                            mastery = updated.mastery,
                        ),
                    )
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            recognizeSelectedImage(uri.toString())
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val result = cameraState.complete(success)
        cameraState = result.cameraState
        val selectedImageUri = result.imageState?.selectedImageUri
        if (!selectedImageUri.isNullOrBlank()) {
            recognizeSelectedImage(selectedImageUri)
        } else {
            status = "已取消拍照"
        }
    }

    WqScreen {
        WqPageHeader(
            title = "上传错题",
            subtitle = "选择图片后会上传到当前后端 API，并返回错题草稿。",
        )

        WqTaskCard(title = "图片草稿") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SelectedImagePreview(
                    selectedImageUri = imageState.selectedImageUri,
                    fallbackLabel = imageState.previewLabel,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        runCatching {
                            val cameraUri = createCameraImageUri()
                            cameraState = cameraState.prepare(cameraUri.toString())
                            status = "正在打开系统相机..."
                            cameraLauncher.launch(cameraUri)
                        }.onFailure { error ->
                            status = "无法打开相机：${error.message}"
                        }
                    },
                ) {
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                        status = "正在打开系统相册..."
                    },
                ) {
                    Text("相册")
                }
                if (imageState.hasImage) {
                    OutlinedButton(
                        onClick = {
                            imageState = imageState.clear()
                            draftQuestionId = null
                            draftContent = ""
                            subject = "数据结构"
                            chapter = "待分类"
                            mastery = "unfamiliar"
                            status = "已清除图片和识别草稿"
                        },
                    ) {
                        Text("清除")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = imageState.previewLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        WqTaskCard(title = "识别校正") {
            OutlinedTextField(
                value = draftContent,
                onValueChange = {
                    draftContent = it
                    val next = SubjectClassifier.classify(it)
                    subject = next.subject
                    chapter = next.chapter
                },
                label = { Text("Markdown + LaTeX 题干") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("章节") },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { confirmDraft() },
                enabled = !isUploading && !isConfirming,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        isUploading -> "识别中..."
                        isConfirming -> "确认中..."
                        else -> "确认入库"
                    },
                )
            }
        }

        Text(
            text = status,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SelectedImagePreview(
    selectedImageUri: String?,
    fallbackLabel: String,
) {
    val context = LocalContext.current
    val bitmap = remember(selectedImageUri) {
        selectedImageUri?.let { uriText ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriText))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "已选择的错题图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Text(fallbackLabel)
    }
}

@Composable
private fun QuestionBankScreen(
    questions: MutableList<MistakeQuestion>,
    sessionState: SessionState,
    repository: QuestionBankRepository,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var selectedSubject by remember { mutableStateOf(QuestionFilters.ALL) }
    var selectedMastery by remember { mutableStateOf(QuestionFilters.ALL) }
    var expandedQuestionId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("登录后可从云端刷新错题。") }
    val visibleQuestions = questions.filterQuestions(selectedSubject, selectedMastery)

    fun refreshFromBackend() {
        status = "正在从后端加载题库..."
        Thread {
            val subjectQuery = selectedSubject.takeUnless { it == QuestionFilters.ALL }
            val result = repository.loadQuestions(sessionState, subject = subjectQuery)
            mainHandler.post {
                when (result) {
                    QuestionBankResult.LoginRequired -> {
                        status = "请先在“我的”页面登录后再刷新题库。"
                    }
                    is QuestionBankResult.Loaded -> {
                        questions.clear()
                        questions.addAll(result.questions)
                        status = "已从后端加载 ${result.questions.size} 道错题。"
                    }
                    is QuestionBankResult.Failed -> {
                        status = "题库加载失败：${result.message}"
                    }
                }
            }
        }.start()
    }

    WqScreen {
        WqPageHeader(
            title = "云端错题库",
            subtitle = "登录后可按科目从云端 /questions 读取错题。",
        )
        WqActionRow {
            QuestionFilters.subjects.forEach { subject ->
                WqStatusPill(
                    text = subject,
                    selected = selectedSubject == subject,
                    onClick = { selectedSubject = subject },
                )
            }
        }
        WqActionRow {
            QuestionFilters.masteryStates.forEach { mastery ->
                WqStatusPill(
                    text = if (mastery == QuestionFilters.ALL) QuestionFilters.ALL else masteryLabel(mastery),
                    selected = selectedMastery == mastery,
                    onClick = { selectedMastery = mastery },
                )
            }
        }
        Button(onClick = { refreshFromBackend() }, modifier = Modifier.fillMaxWidth()) {
            Text("从后端刷新题库")
        }
        Text(
            text = status,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        visibleQuestions.forEach { question ->
            QuestionCard(
                question = question,
                expanded = expandedQuestionId == question.id,
                onToggleExpanded = {
                    expandedQuestionId = if (expandedQuestionId == question.id) null else question.id
                },
            )
        }
        if (visibleQuestions.isEmpty()) {
            WqEmptyState("当前筛选下还没有错题。")
        }
    }
}

@Composable
private fun PracticeScreen(
    questions: MutableList<MistakeQuestion>,
    sessionState: SessionState,
    apiClient: WqLearnerApiClient,
) {
    var mode by remember { mutableStateOf("original") }
    var currentQuestionId by remember { mutableStateOf<String?>(null) }
    val current = questions.firstOrNull { it.id == currentQuestionId } ?: questions.drawPracticeQuestion(null)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf("选择一种练习方式。") }
    var variant by remember { mutableStateOf<ApiVariantQuestion?>(null) }

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

    fun generateVariant() {
        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            status = "请先在“我的”页面登录后再生成变形题。"
            mode = "variant"
            return
        }
        val source = current ?: questions.drawPracticeQuestion(previousQuestionId = null)
        if (source == null) {
            status = "题库为空，请先上传或刷新错题。"
            mode = "variant"
            return
        }
        currentQuestionId = source.id
        mode = "variant"
        status = "正在生成变形题..."
        Thread {
            try {
                val practice = apiClient.createVariantPractice(
                    token = token,
                    sourceQuestionId = source.id,
                    topic = source.chapter,
                )
                mainHandler.post {
                    variant = practice.variant
                    status = "已生成变形题：${practice.id}"
                }
            } catch (error: Exception) {
                mainHandler.post {
                    status = "变形题生成失败：${error.message}"
                }
            }
        }.start()
    }

    fun updateCurrentMastery(nextMastery: String) {
        val source = current
        if (source == null) {
            status = "请先抽取一道题。"
            return
        }
        val localUpdated = questions.updateMastery(source.id, nextMastery)
        questions.clear()
        questions.addAll(localUpdated)
        status = "已标记为：${masteryLabel(nextMastery)}"

        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            status = "已在本地标记为：${masteryLabel(nextMastery)}；登录后可同步云端。"
            return
        }
        Thread {
            try {
                apiClient.updateQuestion(
                    token = token,
                    questionId = source.id,
                    contentMdLatex = source.content,
                    subject = source.subject,
                    chapter = source.chapter,
                    mastery = nextMastery,
                )
            } catch (error: Exception) {
                mainHandler.post {
                    status = "本地已更新，云端同步失败：${error.message}"
                }
            }
        }.start()
    }

    WqScreen {
        WqPageHeader(
            title = "练习复盘",
            subtitle = "支持抽现有错题，也支持由云端大模型生成变形题。",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { drawOriginalQuestion() }) {
                Text("抽现有错题")
            }
            OutlinedButton(onClick = { generateVariant() }) {
                Text("生成变形题")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = status,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        if (current == null) {
            WqEmptyState("题库为空，请先上传错题。")
        } else if (mode == "original") {
            WqTaskCard(title = "原题练习") {
                QuestionSummary(current)
                Spacer(Modifier.height(12.dp))
                ReviewButtons(onReview = ::updateCurrentMastery)
            }
        } else {
            WqTaskCard(title = variant?.title ?: "大模型变形题") {
                if (variant == null) {
                    Text(
                        text = "点击“生成变形题”后，将基于「${current.chapter}」从云端生成。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = variant?.contentMdLatex.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "答案：${variant?.answerMdLatex.orEmpty()}",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "解析：${variant?.explanationMdLatex.orEmpty()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                ReviewButtons(onReview = ::updateCurrentMastery)
            }
        }
    }
}

@Composable
private fun MeScreen(
    sessionState: SessionState,
    apiClient: WqLearnerApiClient,
    endpointState: ApiEndpointState,
    onBaseUrlChange: (String) -> Unit,
    onSessionSaved: () -> Unit,
    onSessionCleared: () -> Unit,
) {
    var email by remember { mutableStateOf(sessionState.email ?: "demo@example.com") }
    var password by remember { mutableStateOf("secret123") }
    var endpointDraft by remember(endpointState.baseUrl) { mutableStateOf(endpointState.baseUrl) }
    var status by remember {
        mutableStateOf(if (sessionState.isLoggedIn) "已登录" else "未登录")
    }
    var tokenPreview by remember {
        mutableStateOf(sessionState.accessToken?.take(8).orEmpty())
    }
    var cloudStatus by remember { mutableStateOf("未检查") }
    var isCheckingCloud by remember { mutableStateOf(false) }

    fun loginAfterOptionalRegister(registerFirst: Boolean) {
        status = if (registerFirst) "正在注册并登录..." else "正在登录..."
        Thread {
            try {
                if (registerFirst) {
                    apiClient.register(email, password)
                }
                val session = apiClient.login(email, password)
                sessionState.setSession(session, email)
                onSessionSaved()
                tokenPreview = session.accessToken.take(8)
                status = "已连接后端并保存 token"
            } catch (error: Exception) {
                status = "后端连接失败：${error.message}"
            }
        }.start()
    }

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

    WqScreen {
        WqPageHeader(
            title = "我的",
            subtitle = "管理账号登录和云端服务连接。",
        )
        WqTaskCard(title = "账号登录") {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { loginAfterOptionalRegister(registerFirst = false) }) {
                    Text("登录")
                }
                OutlinedButton(onClick = { loginAfterOptionalRegister(registerFirst = true) }) {
                    Text("注册并登录")
                }
                OutlinedButton(
                    onClick = {
                        sessionState.clear()
                        onSessionCleared()
                        tokenPreview = ""
                        status = "已退出登录"
                    },
                ) {
                    Text("退出")
                }
            }
        }
        WqTaskCard(title = "连接状态") {
            Text(status, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Token：${tokenPreview.ifBlank { "无" }}")
            Text("账号：${sessionState.email ?: "未登录"}")
        }
        WqTaskCard(title = "云端状态") {
            Text(endpointState.statusText)
            Text("连接：$cloudStatus", fontWeight = FontWeight.Bold)
            Button(
                onClick = { checkCloudStatus() },
                enabled = !isCheckingCloud,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isCheckingCloud) "检查中..." else "检查云端状态")
            }
        }
        WqTaskCard(title = "云端 API") {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = { endpointDraft = it },
                label = { Text("API 地址") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onBaseUrlChange(endpointDraft)
                    tokenPreview = ""
                    endpointDraft = endpointState.withBaseUrl(endpointDraft).baseUrl
                    status = "已应用云端 API 地址，请重新登录"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("应用地址")
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: MistakeQuestion,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    WqTaskCard(title = "${question.subject} / ${question.chapter}") {
        Text(masteryLabel(question.mastery), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(if (expanded) question.content else question.content.take(96))
        if (onToggleExpanded != null) {
            OutlinedButton(onClick = onToggleExpanded) {
                Text(if (expanded) "收起详情" else "查看详情")
            }
        }
    }
}

@Composable
private fun QuestionSummary(question: MistakeQuestion) {
    Text(question.id, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Text(question.content)
    Spacer(Modifier.height(6.dp))
    Text("掌握状态：${question.mastery}", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReviewButtons(onReview: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onReview("unfamiliar") }) {
            Text("仍不熟")
        }
        OutlinedButton(onClick = { onReview("reviewing") }) {
            Text("复习中")
        }
        Button(onClick = { onReview("mastered") }) {
            Text("已掌握")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WqLearnerAppPreview() {
    WQlearner1Theme {
        WqLearnerApp()
    }
}
