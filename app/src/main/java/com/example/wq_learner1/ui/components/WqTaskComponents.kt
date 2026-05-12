package com.example.wq_learner1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wq_learner1.data.MistakeQuestion
import com.example.wq_learner1.data.masteryLabel
import com.example.wq_learner1.data.renderQuestionContent

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
fun WqPageHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun WqTaskCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WqActionRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
fun WqEmptyState(text: String) {
    WqTaskCard(title = "列表为空") {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WqSpacer() {
    Spacer(Modifier.height(8.dp))
}

@Composable
fun QuestionEditDialog(
    question: MistakeQuestion,
    onDismiss: () -> Unit,
    onSave: (MistakeQuestion) -> Unit
) {
    var content by remember { mutableStateOf(question.content) }
    var subject by remember { mutableStateOf(question.subject) }
    var chapter by remember { mutableStateOf(question.chapter) }
    var mastery by remember { mutableStateOf(question.mastery) }
    var answer by remember { mutableStateOf(question.answer) }
    var explanation by remember { mutableStateOf(question.explanation) }
    var showAnswer by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "题目详情",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Mastery Selector
                Text("掌握状态", style = MaterialTheme.typography.labelLarge)
                WqActionRow {
                    listOf("unfamiliar", "reviewing", "mastered").forEach { m ->
                        WqStatusPill(
                            text = masteryLabel(m),
                            selected = mastery == m,
                            onClick = { mastery = m }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("题目内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("科目") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("章节") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!showAnswer) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showAnswer = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("查看答案与解析")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("答案") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        OutlinedTextField(
                            value = explanation,
                            onValueChange = { explanation = it },
                            label = { Text("解析") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            onSave(question.copy(
                                content = content,
                                subject = subject,
                                chapter = chapter,
                                mastery = mastery,
                                answer = answer,
                                explanation = explanation,
                            ))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存修改")
                    }
                }
            }
        }
    }
}

@Composable
fun RichQuestionText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val renderedText = remember(text) { renderQuestionContent(text) }
    val annotatedString = remember(renderedText) { parseRichText(renderedText) }
    
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text = annotatedString,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun parseRichText(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentIndex = 0
        
        val patterns = listOf(
            Regex("""\*\*([^*]+)\*\*"""),
            Regex("""`([^`]*)`""")
        )
        
        val matches = patterns.flatMap { it.findAll(text) }.sortedBy { it.range.first }
        
        for (match in matches) {
            if (match.range.first < currentIndex) continue
            
            append(text.substring(currentIndex, match.range.first))
            
            val isBold = match.value.startsWith("**")
            val content = match.groupValues[1]
            
            if (isBold) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(content)
                pop()
            } else {
                pushStyle(androidx.compose.ui.text.SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f)
                ))
                append(content)
                pop()
            }
            
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
