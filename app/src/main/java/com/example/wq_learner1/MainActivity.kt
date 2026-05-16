package com.example.wq_learner1

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import com.yalantis.ucrop.UCrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.wq_learner1.ui.components.QuestionEditDialog
import com.example.wq_learner1.data.CameraCaptureState
import com.example.wq_learner1.data.ImageSelectionState
import com.example.wq_learner1.data.MistakeQuestion
import com.example.wq_learner1.data.QuestionFilters
import com.example.wq_learner1.data.QuestionBankRepository
import com.example.wq_learner1.data.QuestionBankResult
import com.example.wq_learner1.data.compactSubjectLabel
import com.example.wq_learner1.data.drawPracticeQuestion
import com.example.wq_learner1.data.filterQuestions
import com.example.wq_learner1.data.learningStats
import com.example.wq_learner1.data.masteryLabel
import com.example.wq_learner1.data.renderQuestionContent
import com.example.wq_learner1.data.upsertFirstById
import com.example.wq_learner1.data.replaceMasteryById
import com.example.wq_learner1.domain.SubjectClassifier
import com.example.wq_learner1.network.ApiEndpointState
import com.example.wq_learner1.network.ApiVariantQuestion
import com.example.wq_learner1.network.SessionState
import com.example.wq_learner1.network.SharedPreferencesSessionStore
import com.example.wq_learner1.network.WqLearnerApiClient
import com.example.wq_learner1.ui.components.WqActionRow
import com.example.wq_learner1.ui.components.WqEmptyState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.wq_learner1.auth.AliyunPhoneAuthGateway
import com.example.wq_learner1.auth.PhoneAuthGateway
import com.example.wq_learner1.ui.components.WqPageHeader
import com.example.wq_learner1.ui.components.RichQuestionText
import com.example.wq_learner1.ui.components.WqScreen
import com.example.wq_learner1.ui.components.WqStatusPill
import com.example.wq_learner1.ui.components.WqTaskCard
import com.example.wq_learner1.ui.components.WqLearningSummary
import com.example.wq_learner1.ui.components.WqStudyHint
import com.example.wq_learner1.ui.theme.WQlearner1Theme

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

class PracticeState(
    private val apiClient: WqLearnerApiClient,
    private val questions: SnapshotStateList<MistakeQuestion>,
    private val sessionState: SessionState,
) {
    var mode by mutableStateOf("original")
    var currentQuestionId by mutableStateOf<String?>(null)
    var variant by mutableStateOf<ApiVariantQuestion?>(null)
    var isGenerating by mutableStateOf(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    val current: MistakeQuestion?
        get() = questions.firstOrNull { it.id == currentQuestionId } ?: questions.drawPracticeQuestion(null)

    val stats
        get() = questions.learningStats()

    fun drawOriginalQuestion(context: android.content.Context) {
        val next = questions.drawPracticeQuestion(previousQuestionId = currentQuestionId)
        if (next == null) {
            android.widget.Toast.makeText(context, "题库为空", android.widget.Toast.LENGTH_SHORT).show()
            mode = "original"
            return
        }
        currentQuestionId = next.id
        variant = null
        mode = "original"
    }

    fun generateVariant(context: android.content.Context) {
        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            android.widget.Toast.makeText(context, "请先登录", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val source = current
        if (source == null) {
            android.widget.Toast.makeText(context, "题库为空", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        currentQuestionId = source.id
        mode = "variant"
        if (isGenerating) return

        isGenerating = true
        Thread {
            try {
                val practice = apiClient.createVariantPractice(
                    token = token,
                    sourceQuestionId = source.id,
                    topic = source.chapter,
                )
                mainHandler.post {
                    variant = practice.variant
                    isGenerating = false
                }
            } catch (error: Exception) {
                mainHandler.post {
                    android.widget.Toast.makeText(context, "生成失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                    isGenerating = false
                }
            }
        }.start()
    }

    fun markCurrentMastery(mastery: String): MistakeQuestion? {
        val source = current ?: return null
        currentQuestionId = source.id
        return questions.replaceMasteryById(source.id, mastery)
    }
}

private enum class MainTab(val label: String) {
    Upload("上传"),
    Bank("题库"),
    Practice("练习"),
    Me("我的"),
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
    val endpointState = remember { ApiEndpointState() }
    val apiClient = remember(endpointState.baseUrl) { WqLearnerApiClient(endpointState.baseUrl) }
    val phoneAuthGateway = remember { AliyunPhoneAuthGateway(BuildConfig.ALIYUN_PHONE_AUTH_SECRET) }
    val questionBankRepository = remember(apiClient) { QuestionBankRepository(apiClient) }
    val questions = remember { mutableStateListOf<MistakeQuestion>() }
    val practiceState = remember(apiClient, questions, sessionState) {
        PracticeState(apiClient, questions, sessionState)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
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
                    apiClient = apiClient,
                )
                MainTab.Practice -> PracticeScreen(
                    sessionState = sessionState,
                    apiClient = apiClient,
                    practiceState = practiceState,
                )
                MainTab.Me -> MeScreen(
                    sessionState = sessionState,
                    apiClient = apiClient,
                    phoneAuthGateway = phoneAuthGateway,
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
    var draftContent by remember { mutableStateOf("") }
    var draftAnswer by remember { mutableStateOf("") }
    var draftExplanation by remember { mutableStateOf("") }
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
            android.widget.Toast.makeText(context, "请先登录", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (isUploading) return

        val imageUri = Uri.parse(selectedImageUri)
        imageState = imageState.select(selectedImageUri)
        isUploading = true
        Thread {
            try {
                val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.readBytes()
                } ?: throw IllegalStateException("无法读取图片")
                val detectedContentType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val fileName = imageUri.lastPathSegment ?: "question.jpg"
                val draft = apiClient.uploadQuestion(
                    token = token,
                    imageBytes = imageBytes,
                    fileName = fileName,
                    contentType = detectedContentType,
                )

                mainHandler.post {
                    draftQuestionId = draft.id
                    draftContent = draft.contentMdLatex
                    draftAnswer = draft.answerMdLatex
                    draftExplanation = draft.explanationMdLatex
                    subject = draft.subject
                    chapter = draft.chapter
                    mastery = draft.mastery
                    isUploading = false
                }
            } catch (error: Exception) {
                mainHandler.post {
                    android.widget.Toast.makeText(context, "识别失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
            }
        }.start()
    }

    fun confirmDraft() {
        val token = sessionState.accessToken
        val questionId = draftQuestionId
        if (token.isNullOrBlank()) {
            android.widget.Toast.makeText(context, "请先登录", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (questionId.isNullOrBlank()) {
            android.widget.Toast.makeText(context, "请先拍照识别", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        isConfirming = true
        Thread {
            try {
                val updated = apiClient.updateQuestion(
                    token = token,
                    questionId = questionId,
                    contentMdLatex = draftContent,
                    subject = subject,
                    chapter = chapter,
                    mastery = mastery,
                    answerMdLatex = draftAnswer,
                    explanationMdLatex = draftExplanation,
                )
                mainHandler.post {
                    onSave(
                        MistakeQuestion(
                            id = updated.id,
                            content = updated.contentMdLatex,
                            subject = updated.subject,
                            chapter = updated.chapter,
                            mastery = updated.mastery,
                            answer = updated.answerMdLatex,
                            explanation = updated.explanationMdLatex,
                        ),
                    )
                    android.widget.Toast.makeText(context, "已入库", android.widget.Toast.LENGTH_SHORT).show()
                    isConfirming = false
                }
            } catch (error: Exception) {
                mainHandler.post {
                    android.widget.Toast.makeText(context, "入库失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                    isConfirming = false
                }
            }
        }.start()
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data
                if (data != null) {
                    val resultUri = UCrop.getOutput(data)
                    if (resultUri != null) {
                        recognizeSelectedImage(resultUri.toString())
                    }
                }
            }
            UCrop.RESULT_ERROR -> {
                val data = result.data
                val cropError = data?.let { UCrop.getError(it) }
                android.widget.Toast.makeText(context, "裁剪失败: ${cropError?.message ?: "未知错误"}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(80)
            setFreeStyleCropEnabled(true)
        }
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(0f, 0f) // 自由比例

        cropLauncher.launch(uCrop.getIntent(context))
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val result = cameraState.complete(success)
        cameraState = result.cameraState
        val selectedImageUri = result.imageState?.selectedImageUri
        if (!selectedImageUri.isNullOrBlank()) {
            startCrop(Uri.parse(selectedImageUri))
        }
    }

    WqScreen {
        WqPageHeader(
            title = "错题工作台",
            subtitle = "拍照或选图后先校正题干，再确认入库。",
            meta = "UPLOAD",
        )
        WqStudyHint("建议流程：上传图片 -> 校正 OCR 题干和答案 -> 确认入库。每道题入库后会自动进入题库和练习复盘。")

        WqTaskCard(title = "图片草稿", subtitle = "保留原图，方便对照校正") {
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
                    fallbackLabel = if (isUploading) "正在识别..." else "待选择",
                )
            }
            Spacer(Modifier.height(12.dp))
            WqActionRow {
                Button(
                    onClick = {
                        runCatching {
                            val cameraUri = createCameraImageUri()
                            cameraState = cameraState.prepare(cameraUri.toString())
                            cameraLauncher.launch(cameraUri)
                        }.onFailure { error ->
                            android.widget.Toast.makeText(context, "启动失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
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
                            draftAnswer = ""
                            draftExplanation = ""
                            subject = "数据结构"
                            chapter = "待分类"
                            mastery = "unfamiliar"
                        },
                    ) {
                        Text("清除")
                    }
                }
            }
        }

        WqTaskCard(
            title = "识别校正",
            subtitle = if (draftContent.isBlank()) "等待上传后自动填充" else "确认题干、科目和章节",
            accentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            OutlinedTextField(
                value = draftContent,
                onValueChange = {
                    draftContent = it
                    val next = SubjectClassifier.classify(it)
                    subject = next.subject
                    chapter = next.chapter
                },
                label = { Text("题干内容") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            if (draftContent.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("预览", fontWeight = FontWeight.Bold)
                RichQuestionText(draftContent)
            }
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
            OutlinedTextField(
                value = draftAnswer,
                onValueChange = { draftAnswer = it },
                label = { Text("答案") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draftExplanation,
                onValueChange = { draftExplanation = it },
                label = { Text("解析") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
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
                val uri = Uri.parse(uriText)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    // First decode with inJustDecodeBounds=true to check dimensions
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(input, null, options)

                    // Calculate inSampleSize
                    options.inSampleSize = calculateInSampleSize(options, 512, 512)
                    options.inJustDecodeBounds = false

                    // Decode bitmap with inSampleSize set
                    // Must reopen stream because it was consumed
                    context.contentResolver.openInputStream(uri)?.use { input2 ->
                        BitmapFactory.decodeStream(input2, null, options)
                    }
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

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@Composable
private fun QuestionBankScreen(
    questions: MutableList<MistakeQuestion>,
    sessionState: SessionState,
    repository: QuestionBankRepository,
    apiClient: WqLearnerApiClient,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var selectedSubject by remember { mutableStateOf(QuestionFilters.ALL) }
    var selectedMastery by remember { mutableStateOf(QuestionFilters.ALL) }
    var editingQuestion by remember { mutableStateOf<MistakeQuestion?>(null) }
    val visibleQuestions = questions.filterQuestions(selectedSubject, selectedMastery)
    val stats = questions.learningStats()

    fun refreshFromBackend() {
        Thread {
            val subjectQuery = selectedSubject.takeUnless { it == QuestionFilters.ALL }
            val result = repository.loadQuestions(sessionState, subject = subjectQuery)
            mainHandler.post {
                when (result) {
                    is QuestionBankResult.Loaded -> {
                        questions.clear()
                        questions.addAll(result.questions)
                    }
                    is QuestionBankResult.Failed -> {
                        android.widget.Toast.makeText(context, "刷新失败: ${result.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }.start()
    }

    fun updateQuestionOnBackend(updated: MistakeQuestion) {
        val token = sessionState.accessToken ?: return
        Thread {
            try {
                val saved = apiClient.updateQuestion(
                    token = token,
                    questionId = updated.id,
                    contentMdLatex = updated.content,
                    subject = updated.subject,
                    chapter = updated.chapter,
                    mastery = updated.mastery,
                    answerMdLatex = updated.answer,
                    explanationMdLatex = updated.explanation,
                )
                mainHandler.post {
                    questions.upsertFirstById(
                        MistakeQuestion(
                            id = saved.id,
                            content = saved.contentMdLatex,
                            subject = saved.subject,
                            chapter = saved.chapter,
                            mastery = saved.mastery,
                            answer = saved.answerMdLatex,
                            explanation = saved.explanationMdLatex,
                        ),
                    )
                    android.widget.Toast.makeText(context, "已更新", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    android.widget.Toast.makeText(context, "更新失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshFromBackend()
    }

    androidx.compose.runtime.LaunchedEffect(selectedSubject) {
        refreshFromBackend()
    }

    WqScreen {
        WqPageHeader(
            title = "云端题库",
            subtitle = "按科目和掌握度整理错题，像复习笔记一样维护。",
            meta = "BANK",
        )
        WqLearningSummary(stats)
        WqActionRow {
            QuestionFilters.subjects.forEach { subject ->
                WqStatusPill(
                    text = compactSubjectLabel(subject),
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

        visibleQuestions.forEach { question ->
            QuestionCard(
                question = question,
                onClick = { editingQuestion = question }
            )
        }
        if (visibleQuestions.isEmpty()) {
            WqEmptyState("暂无错题，请上传。")
        }
    }

    editingQuestion?.let { q ->
        QuestionEditDialog(
            question = q,
            onDismiss = { editingQuestion = null },
            onSave = { updated -> updateQuestionOnBackend(updated) }
        )
    }
}

@Composable
private fun PracticeScreen(
    sessionState: SessionState,
    apiClient: WqLearnerApiClient,
    practiceState: PracticeState,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val current = practiceState.current

    fun updateCurrentMastery(nextMastery: String) {
        val updated = practiceState.markCurrentMastery(nextMastery) ?: return

        val token = sessionState.accessToken
        if (token.isNullOrBlank()) {
            android.widget.Toast.makeText(context, "已在本地标记", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                apiClient.updateQuestion(
                    token = token,
                    questionId = updated.id,
                    contentMdLatex = updated.content,
                    subject = updated.subject,
                    chapter = updated.chapter,
                    mastery = nextMastery,
                    answerMdLatex = updated.answer,
                    explanationMdLatex = updated.explanation,
                )
            } catch (error: Exception) {
                mainHandler.post {
                    android.widget.Toast.makeText(context, "云端同步失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    WqScreen {
        WqPageHeader(
            title = "练习复盘",
            subtitle = "先处理最需要复盘的题，再用变形题检查迁移能力。",
            meta = "PRACTICE",
        )
        WqLearningSummary(practiceState.stats)
        WqActionRow {
            Button(onClick = { practiceState.drawOriginalQuestion(context) }) {
                Text("抽原题")
            }
            OutlinedButton(onClick = { practiceState.generateVariant(context) }) {
                Text(if (practiceState.isGenerating) "生成中..." else "变形题")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (current == null) {
            WqEmptyState("题库为空，请先上传。")
        } else if (practiceState.mode == "original") {
            var showOriginalAnswer by remember(current.id) { mutableStateOf(false) }
            WqTaskCard(
                title = "原题练习",
                subtitle = "先回忆解法，再展开答案解析",
            ) {
                QuestionSummary(current)
                if (!showOriginalAnswer) {
                    androidx.compose.material3.TextButton(onClick = { showOriginalAnswer = true }) {
                        Text("查看答案与解析")
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("答案", fontWeight = FontWeight.Bold)
                    RichQuestionText(current.answer.ifBlank { "暂无答案" })
                    Spacer(Modifier.height(8.dp))
                    Text("解析", fontWeight = FontWeight.Bold)
                    RichQuestionText(current.explanation.ifBlank { "暂无解析" })
                }
                Spacer(Modifier.height(12.dp))
                ReviewButtons(onReview = ::updateCurrentMastery)
            }
        } else {
            var showVariantAnswer by remember(practiceState.variant?.sourceQuestionId) { mutableStateOf(false) }
            WqTaskCard(
                title = practiceState.variant?.title ?: "变形练习",
                subtitle = "围绕当前错题生成同知识点练习",
                accentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                if (practiceState.variant == null) {
                    Text(
                        text = if (practiceState.isGenerating) "正在生成..." else "基于当前题目生成变形题。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    RichQuestionText(practiceState.variant?.contentMdLatex.orEmpty())
                    if (!showVariantAnswer) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = { showVariantAnswer = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("查看变形题答案")
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("答案", fontWeight = FontWeight.Bold)
                        RichQuestionText(practiceState.variant?.answerMdLatex.orEmpty())
                        Spacer(Modifier.height(8.dp))
                        Text("解析", fontWeight = FontWeight.Bold)
                        RichQuestionText(practiceState.variant?.explanationMdLatex.orEmpty())
                    }
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
    phoneAuthGateway: PhoneAuthGateway,
    onSessionSaved: () -> Unit,
    onSessionCleared: () -> Unit,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (sessionState.isLoggedIn) "已登录" else "未登录") }

    fun loginWithOneClickPhone() {
        val activity = context.findActivity()
        if (activity == null || isLoggingIn) return
        isLoggingIn = true
        status = "正在唤起本机号码认证..."
        phoneAuthGateway.requestLoginToken(
            activity = activity,
            onSuccess = { aliyunToken ->
                mainHandler.post { status = "已授权，正在登录..." }
                Thread {
                    try {
                        val session = apiClient.loginWithAliyunPhoneAuthToken(aliyunToken)
                        sessionState.setSession(session, session.account.ifBlank { "手机号账号" })
                        onSessionSaved()
                        mainHandler.post {
                            status = "已登录"
                            isLoggingIn = false
                        }
                    } catch (error: Exception) {
                        mainHandler.post {
                            status = "登录失败：${error.message}"
                            isLoggingIn = false
                        }
                    }
                }.start()
            },
            onError = { message ->
                mainHandler.post {
                    status = message.ifBlank { "号码认证失败" }
                    isLoggingIn = false
                }
            },
        )
    }

    val isLoggedIn = sessionState.isLoggedIn
    val accountName = sessionState.email ?: "未登录"

    WqScreen {
        MeTopBar(isLoggedIn = isLoggedIn)
        AccountProfileCard(
            isLoggedIn = isLoggedIn,
            isLoggingIn = isLoggingIn,
            accountName = accountName,
            status = status,
            onLogin = { loginWithOneClickPhone() },
        )
        AccountOverviewPanel(isLoggedIn = isLoggedIn)
        SettingsSection(
            isLoggedIn = isLoggedIn,
            status = status,
            accountName = accountName,
            onLogout = {
                sessionState.clear()
                onSessionCleared()
                status = "已退出登录"
            },
        )
    }
}

@Composable
private fun MeTopBar(isLoggedIn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        AccountStateBadge(isLoggedIn = isLoggedIn)
    }
}

@Composable
private fun AccountProfileCard(
    isLoggedIn: Boolean,
    isLoggingIn: Boolean,
    accountName: String,
    status: String,
    onLogin: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                UserAvatar(isLoggedIn = isLoggedIn)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isLoggedIn) accountName else "立即登录",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (isLoggedIn) "云端题库已开启" else "登录后同步题库、上传记录和练习进度",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isLoggedIn) {
                Button(
                    onClick = onLogin,
                    enabled = !isLoggingIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (isLoggingIn) "正在登录..." else "本机号码一键登录")
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(isLoggedIn: Boolean) {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = CircleShape,
        color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (isLoggedIn) "学" else "未",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (isLoggedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountOverviewPanel(isLoggedIn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OverviewTile(
            title = "题库",
            value = if (isLoggedIn) "云端" else "本地",
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            title = "上传",
            value = if (isLoggedIn) "同步" else "待登录",
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            title = "练习",
            value = if (isLoggedIn) "记录中" else "本机",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSection(
    isLoggedIn: Boolean,
    status: String,
    accountName: String,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            SettingsRow("账", "账号与安全", accountName)
            SettingsDivider()
            SettingsRow("云", "云端同步", if (isLoggedIn) "已开启" else "未开启")
            SettingsDivider()
            SettingsRow("状", "登录状态", status)
            SettingsDivider()
            SettingsRow("帮", "帮助与反馈", "使用问题与建议")
            if (isLoggedIn) {
                SettingsDivider()
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("退出登录")
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    marker: String,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = marker,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
}

@Composable
private fun AccountStateBadge(isLoggedIn: Boolean) {
    val badgeColor = if (isLoggedIn) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    }
    val borderColor = if (isLoggedIn) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = badgeColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                    ),
            )
            Text(
                text = if (isLoggedIn) "已登录" else "未登录",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun QuestionCard(
    question: MistakeQuestion,
    onClick: () -> Unit,
) {
    WqTaskCard(
        title = "${question.subject} / ${question.chapter}",
        modifier = Modifier.clickable { onClick() },
        subtitle = masteryLabel(question.mastery),
        accentColor = when (question.mastery) {
            "mastered" -> MaterialTheme.colorScheme.secondary
            "reviewing" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                masteryLabel(question.mastery),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "点击查看详情",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        RichQuestionText(renderQuestionContent(question.content).take(128) + if (question.content.length > 128) "..." else "")
    }
}

@Composable
private fun QuestionSummary(question: MistakeQuestion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${question.subject} / ${question.chapter}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            masteryLabel(question.mastery),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    RichQuestionText(question.content)
}

@Composable
private fun ReviewButtons(onReview: (String) -> Unit) {
    WqActionRow {
        OutlinedButton(onClick = { onReview("unfamiliar") }) {
            Text("不熟")
        }
        OutlinedButton(onClick = { onReview("reviewing") }) {
            Text("复习")
        }
        Button(onClick = { onReview("mastered") }) {
            Text("掌握")
        }
    }
}
