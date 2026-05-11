package com.example.wq_learner1

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.example.wq_learner1.data.ImageSelectionState
import com.example.wq_learner1.data.MistakeQuestion
import com.example.wq_learner1.data.QuestionBankRepository
import com.example.wq_learner1.data.QuestionBankResult
import com.example.wq_learner1.domain.SubjectClassifier
import com.example.wq_learner1.network.ApiConfig
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
    val apiClient = remember { WqLearnerApiClient() }
    val questionBankRepository = remember { QuestionBankRepository(apiClient) }
    val questions = remember {
        mutableStateListOf(
            MistakeQuestion(
                id = "Q-001",
                content = "二叉树遍历的时间复杂度为 ${'$'}O(n)${'$'}，请说明先序遍历过程。",
                subject = "数据结构",
                chapter = "树与二叉树",
                mastery = "reviewing",
            ),
            MistakeQuestion(
                id = "Q-002",
                content = "TCP 拥塞控制中慢开始阈值如何变化？",
                subject = "计算机网络",
                chapter = "传输层",
                mastery = "unfamiliar",
            ),
        )
    }

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
                    onSave = { content, subject, chapter ->
                        questions.add(
                            0,
                            MistakeQuestion(
                                id = "Q-${(questions.size + 1).toString().padStart(3, '0')}",
                                content = content,
                                subject = subject,
                                chapter = chapter,
                                mastery = "reviewing",
                            ),
                        )
                    },
                )
                MainTab.Bank -> QuestionBankScreen(
                    questions = questions,
                    sessionState = sessionState,
                    repository = questionBankRepository,
                )
                MainTab.Practice -> PracticeScreen(questions = questions)
                MainTab.Me -> MeScreen(sessionState = sessionState, apiClient = apiClient)
            }
        }
    }
}

@Composable
private fun UploadScreen(
    onSave: (content: String, subject: String, chapter: String) -> Unit,
) {
    val context = LocalContext.current
    var imageState by remember { mutableStateOf(ImageSelectionState()) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            imageState = imageState.select(uri.toString())
        }
    }
    var draftContent by remember {
        mutableStateOf("二叉树遍历与哈希查找综合题。请分析遍历过程，并写出平均时间复杂度 ${'$'}O(n)${'$'}。")
    }
    val classification = SubjectClassifier.classify(draftContent)
    var subject by remember { mutableStateOf(classification.subject) }
    var chapter by remember { mutableStateOf(classification.chapter) }
    var status by remember { mutableStateOf("已生成识别草稿，等待校正") }

    ScreenColumn {
        ScreenTitle(
            title = "上传错题",
            subtitle = "当前仍是本地草稿流；后续功能会把这里接到真实上传 API。",
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
                Button(onClick = { status = "已模拟拍照并生成识别草稿" }) {
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
                onClick = {
                    onSave(draftContent, subject, chapter)
                    status = "已保存到本地错题列表"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("确认入库")
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
private fun PracticeScreen(questions: List<MistakeQuestion>) {
    var mode by remember { mutableStateOf("original") }
    val current = questions.firstOrNull()

    ScreenColumn {
        ScreenTitle(
            title = "练习复盘",
            subtitle = "支持抽现有错题，也支持预留的大模型变形题模式。",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { mode = "original" }) {
                Text("抽现有错题")
            }
            OutlinedButton(onClick = { mode = "variant" }) {
                Text("生成变形题")
            }
        }
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
            InfoCard(title = "模拟变形题") {
                Text(
                    text = "基于「${current.chapter}」生成：如果将原题条件改为另一种输入规模，时间复杂度如何变化？",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "第一版由后端模拟返回，后续可替换为真实大模型。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
) {
    var email by remember { mutableStateOf(sessionState.email ?: "demo@example.com") }
    var password by remember { mutableStateOf("secret123") }
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
            subtitle = "这里已经接入 Android HTTP 客户端。开发机后端地址：${ApiConfig.DEFAULT_BASE_URL}",
        )
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
            Text("后端：${ApiConfig.DEFAULT_BASE_URL}")
        }
        InfoCard(title = "开发状态") {
            Text("Android：HTTP 客户端和 token 状态已完成")
            Text("后端：FastAPI + SQLite API")
            Text("下一步：上传页接入真实图片选择")
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
