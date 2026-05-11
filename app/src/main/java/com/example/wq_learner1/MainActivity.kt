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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.wq_learner1.data.QuestionBankRepository
import com.example.wq_learner1.data.QuestionBankResult
import com.example.wq_learner1.data.upsertFirstById
import com.example.wq_learner1.domain.SubjectClassifier
import com.example.wq_learner1.network.ApiConfig
import com.example.wq_learner1.network.ApiEndpointState
import com.example.wq_learner1.network.ApiVariantQuestion
import com.example.wq_learner1.network.SessionState
import com.example.wq_learner1.network.WqLearnerApiClient
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
    val sessionState = remember { SessionState() }
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
                        sessionState.clear()
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
    var isUploading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("已生成识别草稿，等待校正") }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            imageState = imageState.select(uri.toString())
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val result = cameraState.complete(success)
        cameraState = result.cameraState
        if (result.imageState != null) {
            imageState = result.imageState
            status = "已拍照并生成图片预览"
        } else {
            status = "已取消拍照"
        }
    }
    var draftContent by remember {
        mutableStateOf("二叉树遍历与哈希查找综合题。请分析遍历过程，并写出平均时间复杂度 ${'$'}O(n)${'$'}。")
    }
    val classification = SubjectClassifier.classify(draftContent)
    var subject by remember { mutableStateOf(classification.subject) }
    var chapter by remember { mutableStateOf(classification.chapter) }

    fun createCameraImageUri(): Uri {
        val imageDir = File(context.cacheDir, "camera-images").apply { mkdirs() }
        val imageFile = File.createTempFile("wq-question-", ".jpg", imageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }

    fun uploadSelectedImage() {
        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            status = "请先在“我的”页面登录后再上传错题。"
            return
        }
        if (isUploading) {
            status = "正在上传，请稍等。"
            return
        }
        val selectedImageUri = imageState.selectedImageUri
        if (selectedImageUri.isNullOrBlank()) {
            status = "请先选择一张错题图片。"
            return
        }

        val imageUri = Uri.parse(selectedImageUri)
        isUploading = true
        status = "正在上传图片并生成错题草稿..."
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
                    draftContent = draft.contentMdLatex
                    subject = draft.subject
                    chapter = draft.chapter
                    onSave(
                        MistakeQuestion(
                            id = draft.id,
                            content = draft.contentMdLatex,
                            subject = draft.subject,
                            chapter = draft.chapter,
                            mastery = draft.mastery,
                        ),
                    )
                    status = "已上传到云端并生成错题草稿：${draft.id}"
                    isUploading = false
                }
            } catch (error: Exception) {
                mainHandler.post {
                    status = "图片上传失败：${error.message}"
                    isUploading = false
                }
            }
        }.start()
    }

    ScreenColumn {
        ScreenTitle(
            title = "上传错题",
            subtitle = "选择图片后会上传到当前后端 API，并返回错题草稿。",
        )

        InfoCard(title = "图片草稿") {
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
                            status = "已清除所选图片"
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

        InfoCard(title = "识别校正") {
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
                onClick = { uploadSelectedImage() },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isUploading) "上传中..." else "上传并入库")
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
    var selectedSubject by remember { mutableStateOf("全部") }
    var status by remember { mutableStateOf("可查看本地样例，也可登录后从后端刷新。") }
    val subjects = listOf("全部", "数据结构", "计算机组成原理", "操作系统", "计算机网络")
    val visibleQuestions = if (selectedSubject == "全部") {
        questions
    } else {
        questions.filter { it.subject == selectedSubject }
    }

    fun refreshFromBackend() {
        status = "正在从后端加载题库..."
        Thread {
            val result = repository.loadQuestions(sessionState, subject = selectedSubject)
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

    ScreenColumn {
        ScreenTitle(
            title = "云端错题库",
            subtitle = "登录后可按科目从后端 /questions 读取 SQLite 中的错题。",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subjects.forEach { subject ->
                TextButton(onClick = { selectedSubject = subject }) {
                    Text(
                        text = subject,
                        fontWeight = if (selectedSubject == subject) FontWeight.Bold else FontWeight.Normal,
                    )
                }
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
            QuestionCard(question)
        }
        if (visibleQuestions.isEmpty()) {
            EmptyState("当前筛选下还没有错题。")
        }
    }
}

@Composable
private fun PracticeScreen(
    questions: List<MistakeQuestion>,
    sessionState: SessionState,
    apiClient: WqLearnerApiClient,
) {
    var mode by remember { mutableStateOf("original") }
    val current = questions.firstOrNull()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf("选择一种练习方式。") }
    var variant by remember { mutableStateOf<ApiVariantQuestion?>(null) }

    fun generateVariant() {
        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            status = "请先在“我的”页面登录后再生成变形题。"
            mode = "variant"
            return
        }
        val source = current
        if (source == null) {
            status = "题库为空，请先上传或刷新错题。"
            mode = "variant"
            return
        }
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

    ScreenColumn {
        ScreenTitle(
            title = "练习复盘",
            subtitle = "支持抽现有错题，也支持由云端大模型生成变形题。",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { mode = "original" }) {
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
            EmptyState("题库为空，请先上传错题。")
        } else if (mode == "original") {
            InfoCard(title = "原题练习") {
                QuestionSummary(current)
                Spacer(Modifier.height(12.dp))
                ReviewButtons()
            }
        } else {
            InfoCard(title = variant?.title ?: "大模型变形题") {
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
                ReviewButtons()
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

    fun loginAfterOptionalRegister(registerFirst: Boolean) {
        status = if (registerFirst) "正在注册并登录..." else "正在登录..."
        Thread {
            try {
                if (registerFirst) {
                    apiClient.register(email, password)
                }
                val session = apiClient.login(email, password)
                sessionState.setSession(session, email)
                tokenPreview = session.accessToken.take(8)
                status = "已连接后端并保存 token"
            } catch (error: Exception) {
                status = "后端连接失败：${error.message}"
            }
        }.start()
    }

    ScreenColumn {
        ScreenTitle(
            title = "我的",
            subtitle = "这里可以切换本地开发地址或函数计算公网 API 地址。",
        )
        InfoCard(title = "后端环境") {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = { endpointDraft = it },
                label = { Text("API 地址") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onBaseUrlChange(endpointDraft)
                        tokenPreview = ""
                        status = "已切换后端地址，请重新登录"
                    },
                ) {
                    Text("应用地址")
                }
                OutlinedButton(
                    onClick = {
                        endpointDraft = ApiConfig.LOCAL_DEVELOPMENT_BASE_URL
                        onBaseUrlChange(ApiConfig.LOCAL_DEVELOPMENT_BASE_URL)
                        tokenPreview = ""
                        status = "已切换为本地开发地址"
                    },
                ) {
                    Text("本地开发")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(endpointState.statusText)
        }
        InfoCard(title = "账号登录") {
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
                        tokenPreview = ""
                        status = "已退出登录"
                    },
                ) {
                    Text("退出")
                }
            }
        }
        InfoCard(title = "连接状态") {
            Text(status, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Token：${tokenPreview.ifBlank { "无" }}")
            Text(endpointState.statusText)
        }
        InfoCard(title = "开发状态") {
            Text("Android：HTTP 客户端和 token 状态已完成")
            Text("后端：FastAPI + 函数计算启动基础")
            Text("下一步：接入云端数据库和 OSS")
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun QuestionCard(question: MistakeQuestion) {
    InfoCard(title = "${question.subject} / ${question.chapter}") {
        QuestionSummary(question)
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
private fun ReviewButtons() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = {}) {
            Text("仍不熟")
        }
        OutlinedButton(onClick = {}) {
            Text("复习中")
        }
        Button(onClick = {}) {
            Text("已掌握")
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WqLearnerAppPreview() {
    WQlearner1Theme {
        WqLearnerApp()
    }
}
