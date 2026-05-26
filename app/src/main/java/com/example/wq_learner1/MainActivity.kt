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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import com.example.wq_learner1.ui.theme.ColorUnfamiliar
import com.example.wq_learner1.ui.theme.ColorReviewing
import com.example.wq_learner1.ui.theme.ColorMastered
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
                tonalElevation = 0.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val icon = when (tab) {
                        MainTab.Upload -> if (isSelected) Icons.Filled.CloudUpload else Icons.Outlined.CloudUpload
                        MainTab.Bank -> if (isSelected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
                        MainTab.Practice -> if (isSelected) Icons.Filled.School else Icons.Outlined.School
                        MainTab.Me -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            androidx.compose.material3.Icon(
                                imageVector = icon,
                                contentDescription = tab.label
                            )
                        },
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
            
            // WQ Learner v2.0 Design Tokens (Zinc Neutral & Teal Primary)
            val brandTeal = android.graphics.Color.parseColor("#0F766E")       // Teal-700 主色
            val brandTealDark = android.graphics.Color.parseColor("#0D5E58")   // Teal-800 状态栏
            val deepDark = android.graphics.Color.parseColor("#09090B")        // Zinc-950 裁剪区背景，沉浸式编辑
            
            setToolbarColor(brandTeal)
            setStatusBarColor(brandTealDark)
            setToolbarWidgetColor(android.graphics.Color.WHITE)
            setToolbarTitle("裁剪与校正")
            
            // 裁剪网格与编辑背景
            setRootViewBackgroundColor(deepDark)
            setCropFrameColor(brandTeal)
            setCropGridColor(brandTeal)
            
            // 活动控制高亮色
            setActiveControlsWidgetColor(brandTeal)
            
            // 确保底部控制栏可见
            setHideBottomControls(false)
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
            val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            val hasImage = imageState.hasImage
            val previewShape = RoundedCornerShape(12.dp)
            val actionButtonShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(previewShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .drawBehind {
                        if (!hasImage) {
                            drawRoundRect(
                                color = strokeColor,
                                style = Stroke(
                                    width = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (hasImage) {
                    SelectedImagePreview(
                        selectedImageUri = imageState.selectedImageUri,
                        fallbackLabel = if (isUploading) "正在上传识别..." else "图片解析错误",
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = "上传",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isUploading) "正在云端识别中..." else "待上传错题图片",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "支持拍照或从相册选择",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            WqActionRow {
                androidx.compose.material3.Button(
                    onClick = {
                        runCatching {
                            val cameraUri = createCameraImageUri()
                            cameraState = cameraState.prepare(cameraUri.toString())
                            cameraLauncher.launch(cameraUri)
                        }.onFailure { error ->
                            android.widget.Toast.makeText(context, "启动相机失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = actionButtonShape,
            
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "拍照",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("拍照拍摄")
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    shape = actionButtonShape,
            
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Collections,
                            contentDescription = "相册",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("本地相册")
                    }
                }
                if (hasImage) {
                    androidx.compose.material3.OutlinedButton(
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
                        shape = actionButtonShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "清除",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("清除")
                        }
                    }
                }
            }
        }

        WqTaskCard(
            title = "识别校正工作台",
            subtitle = if (draftContent.isBlank()) "等待上传识别后自动校正" else "请确认OCR结果、答案及分类",
            accentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            val textFieldsShape = RoundedCornerShape(10.dp)
            val previewShape = RoundedCornerShape(12.dp)
            val actionButtonShape = RoundedCornerShape(10.dp)
            
            OutlinedTextField(
                value = draftContent,
                onValueChange = {
                    draftContent = it
                    val next = SubjectClassifier.classify(it)
                    subject = next.subject
                    chapter = next.chapter
                },
                label = { Text("题目题干内容 (支持 Markdown + LaTeX)") },
                shape = textFieldsShape,
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = "题干",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            
            AnimatedVisibility(
                visible = draftContent.isNotBlank(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(6.dp))
                    Text("题干实时预览", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = previewShape,
                
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            RichQuestionText(draftContent)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目") },
                    shape = textFieldsShape,
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = "科目",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("章节") },
                    shape = textFieldsShape,
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = "章节",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = draftAnswer,
                onValueChange = { draftAnswer = it },
                label = { Text("参考答案") },
                shape = textFieldsShape,
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "答案",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = draftExplanation,
                onValueChange = { draftExplanation = it },
                label = { Text("答案解析") },
                shape = textFieldsShape,
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = "解析",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            
            androidx.compose.material3.Button(
                onClick = { confirmDraft() },
                enabled = !isUploading && !isConfirming && draftContent.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = actionButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = when {
                        isUploading -> "智能 OCR 识别中..."
                        isConfirming -> "正在同步入库..."
                        else -> "确认归档入库"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
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
        val buttonShape = RoundedCornerShape(10.dp)
        WqActionRow {
            androidx.compose.material3.Button(
                onClick = { practiceState.drawOriginalQuestion(context) },
                shape = buttonShape,
        
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "抽原题",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("抽取原题")
                }
            }
            androidx.compose.material3.OutlinedButton(
                onClick = { practiceState.generateVariant(context) },
                shape = buttonShape,
        
                enabled = !practiceState.isGenerating
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = "变形题",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(if (practiceState.isGenerating) "智能生成中..." else "生成变形题")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (current == null) {
            WqEmptyState("题库为空，请先上传。")
        } else if (practiceState.mode == "original") {
            var showOriginalAnswer by remember(current.id) { mutableStateOf(false) }
            WqTaskCard(
                title = "原题练习",
                subtitle = "先回忆解法，再展开答案解析",
            ) {
                QuestionSummary(current)
                
                Spacer(Modifier.height(10.dp))
                
                if (!showOriginalAnswer) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showOriginalAnswer = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = buttonShape,
                
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = "查看",
                                modifier = Modifier.size(16.dp)
                              )
                            Text("查看参考答案与详细解析", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = showOriginalAnswer,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = if (isSystemInDarkTheme()) Color(0xFF2C281A) else Color(0xFFFFFDE2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("参考答案", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                    RichQuestionText(current.answer.ifBlank { "暂无参考答案" })
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text("详细解析", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                    RichQuestionText(current.explanation.ifBlank { "暂无解析内容" })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ReviewButtons(onReview = ::updateCurrentMastery)
            }
        } else {
            var showVariantAnswer by remember(practiceState.variant?.sourceQuestionId) { mutableStateOf(false) }
            val variant = practiceState.variant
            WqTaskCard(
                title = variant?.title ?: "变形练习",
                subtitle = "围绕当前错题生成同知识点练习",
                accentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                if (variant == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "大模型智能生成变式题中，请稍候...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    val variantBoxShape = RoundedCornerShape(10.dp)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        shape = variantBoxShape,
                
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            RichQuestionText(variant.contentMdLatex)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (!showVariantAnswer) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showVariantAnswer = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = buttonShape,
                    
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = "查看",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("查看变形题答案与详细解析", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        AnimatedVisibility(
                            visible = showVariantAnswer,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    color = if (isSystemInDarkTheme()) Color(0xFF2C281A) else Color(0xFFFFFDE2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("参考答案", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                        RichQuestionText(variant.answerMdLatex)
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                        Text("详细解析", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                        RichQuestionText(variant.explanationMdLatex)
                                    }
                                }
                            }
                        }
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
            fontWeight = FontWeight.SemiBold,
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
    val cardShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                            text = if (isLoggedIn) accountName else "未登录账号",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLoggedIn) "云端同步服务正常运行中" else "登录后同步您的错题本与练习进度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!isLoggedIn) {
                    val loginButtonShape = RoundedCornerShape(10.dp)
                    androidx.compose.material3.Button(
                        onClick = onLogin,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth(),
                        shape = loginButtonShape,
                
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoggingIn) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isLoggingIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isLoggingIn) "正在安全验证登录..." else "本机号码一键登录",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

@Composable
private fun UserAvatar(isLoggedIn: Boolean) {
    val avatarShape = CircleShape
    Surface(
        modifier = Modifier.size(48.dp),
        shape = avatarShape,
        color = MaterialTheme.colorScheme.surface,

    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .clip(avatarShape)
                .background(if (isLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "Avatar",
                tint = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(42.dp)
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
            title = "同步题库",
            value = if (isLoggedIn) "云端同步" else "离线本地",
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            title = "备份状态",
            value = if (isLoggedIn) "已备份" else "未备份",
            modifier = Modifier.weight(1f),
        )
        OverviewTile(
            title = "记录方式",
            value = if (isLoggedIn) "多端共享" else "单机保存",
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
    val tileShape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = tileShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
    ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
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
    val cardShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
                SettingsRow(
                    icon = Icons.Rounded.Lock,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "账号与安全",
                    value = accountName
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Sync,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = "云端同步",
                    value = if (isLoggedIn) "已开启自动备份" else "未开启（登录后开启）"
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.SignalCellularAlt,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = "登录状态",
                    value = status
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.Help,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "帮助与反馈",
                    value = "使用问题、功能建议与反馈"
                )
                if (isLoggedIn) {
                    SettingsDivider()
                    val logoutButtonShape = RoundedCornerShape(10.dp)
                    androidx.compose.material3.OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = logoutButtonShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = "退出",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("退出当前账号", fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
) {
    val rowShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = rowShape,
            color = iconColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, iconColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            )
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "详情",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}

@Composable
private fun AccountStateBadge(isLoggedIn: Boolean) {
    val badgeColor = if (isLoggedIn) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
    }
    val borderColor = if (isLoggedIn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val badgeShape = RoundedCornerShape(10.dp)

    Surface(
        shape = badgeShape,
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
                    .size(6.dp)
                    .background(
                        color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            Text(
                text = if (isLoggedIn) "云端已同步" else "本地离线",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
    val masteryColor = when (question.mastery) {
        "mastered" -> ColorMastered
        "reviewing" -> ColorReviewing
        else -> ColorUnfamiliar
    }
    val masteryBg = masteryColor.copy(alpha = 0.1f)
    val pillShape = RoundedCornerShape(10.dp)
    val boxShape = RoundedCornerShape(10.dp)
    
    WqTaskCard(
        title = question.chapter.ifBlank { "未分类章节" },
        subtitle = "科目：${question.subject}",
        modifier = Modifier.clickable { onClick() },
        accentColor = masteryColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = pillShape,
                color = masteryBg,
                border = BorderStroke(1.dp, masteryColor)
            ) {
                Text(
                    text = masteryLabel(question.mastery),
                    color = masteryColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看修改",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "查看",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(Modifier.height(2.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            shape = boxShape,
    
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                val previewText = renderQuestionContent(question.content)
                val truncated = if (previewText.length > 96) previewText.take(96) + "..." else previewText
                RichQuestionText(truncated)
            }
        }
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
    val buttonShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedButton(
            onClick = { onReview("unfamiliar") },
            shape = buttonShape,
            border = BorderStroke(1.dp, ColorUnfamiliar),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ColorUnfamiliar.copy(alpha = 0.05f),
                contentColor = ColorUnfamiliar
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "不熟",
                    modifier = Modifier.size(14.dp)
                )
                Text("仍不熟", fontWeight = FontWeight.Bold)
            }
        }
        
        androidx.compose.material3.OutlinedButton(
            onClick = { onReview("reviewing") },
            shape = buttonShape,
            border = BorderStroke(1.dp, ColorReviewing),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ColorReviewing.copy(alpha = 0.05f),
                contentColor = ColorReviewing
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Help,
                    contentDescription = "复习",
                    modifier = Modifier.size(14.dp)
                )
                Text("复习中", fontWeight = FontWeight.Bold)
            }
        }
        
        androidx.compose.material3.Button(
            onClick = { onReview("mastered") },
            shape = buttonShape,
            border = BorderStroke(1.dp, ColorMastered),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorMastered,
                contentColor = Color.White
            ),
            modifier = Modifier.weight(1.2f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "掌握",
                    modifier = Modifier.size(14.dp)
                )
                Text("已掌握", fontWeight = FontWeight.Bold)
            }
        }
    }
}
