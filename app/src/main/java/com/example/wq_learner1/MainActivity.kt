package com.example.wq_learner1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wq_learner1.domain.SubjectClassifier
import com.example.wq_learner1.ui.theme.WQlearner1Theme

private enum class MainTab(val label: String) {
    Upload("上传"),
    Bank("题库"),
    Practice("练习"),
    Me("我的"),
}

private data class MistakeQuestion(
    val id: String,
    val content: String,
    val subject: String,
    val chapter: String,
    val mastery: String,
)

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
                MainTab.Bank -> QuestionBankScreen(questions = questions)
                MainTab.Practice -> PracticeScreen(questions = questions)
                MainTab.Me -> MeScreen()
            }
        }
    }
}

@Composable
private fun UploadScreen(
    onSave: (content: String, subject: String, chapter: String) -> Unit,
) {
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
            subtitle = "第一版模拟拍照上传后的识别草稿；后端 API 已提供真实上传入口。",
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
                Text("拍照 / 相册图片预览")
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { status = "已模拟拍照并上传到识别服务" }) {
                    Text("拍照")
                }
                OutlinedButton(onClick = { status = "已模拟从相册选择图片" }) {
                    Text("相册")
                }
            }
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
                    status = "已保存到云端错题库"
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
private fun QuestionBankScreen(questions: List<MistakeQuestion>) {
    var selectedSubject by remember { mutableStateOf("全部") }
    val subjects = listOf("全部", "数据结构", "计算机组成原理", "操作系统", "计算机网络")
    val visibleQuestions = if (selectedSubject == "全部") {
        questions
    } else {
        questions.filter { it.subject == selectedSubject }
    }

    ScreenColumn {
        ScreenTitle(
            title = "云端错题库",
            subtitle = "按 11408 科目和章节筛选，查看 Markdown + LaTeX 识别结果。",
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
private fun MeScreen() {
    ScreenColumn {
        ScreenTitle(
            title = "我的",
            subtitle = "账号和云端同步入口。第一版使用演示账号展示登录状态。",
        )
        InfoCard(title = "账号") {
            Text("demo@example.com", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("云端错题库同步：已连接")
        }
        InfoCard(title = "开发状态") {
            Text("Android：本地 MVP 界面")
            Text("后端：FastAPI 内存版 API")
            Text("变形题：模拟返回，保留大模型接口")
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
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
