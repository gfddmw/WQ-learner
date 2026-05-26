package com.example.wq_learner1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wq_learner1.data.LearningStats
import com.example.wq_learner1.data.MistakeQuestion
import com.example.wq_learner1.data.masteryLabel
import com.example.wq_learner1.data.renderQuestionContent
import com.example.wq_learner1.ui.theme.GradientStart
import com.example.wq_learner1.ui.theme.GradientEnd
import com.example.wq_learner1.ui.theme.ColorUnfamiliar
import com.example.wq_learner1.ui.theme.ColorReviewing
import com.example.wq_learner1.ui.theme.ColorMastered

@Composable
fun WqScreen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
fun WqPageHeader(
    title: String,
    subtitle: String = "",
    meta: String = "",
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title, 
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        // 简洁分割线
        Box(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun WqTaskCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(accentColor, RoundedCornerShape(2.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun WqStatusPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.05f else 1.0f, label = "pill_scale")
    val pillShape = RoundedCornerShape(20.dp)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = FontWeight.Bold) },
        shape = pillShape,
        modifier = Modifier.scale(scale),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.outline,
            borderWidth = 1.dp
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WqActionRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
fun WqEmptyState(text: String) {
    WqTaskCard(
        title = "暂无错题内容",
        subtitle = "建议开始拍照上传",
        accentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "提示",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun WqStudyHint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "TIP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WqLearningSummary(stats: LearningStats) {
    WqTaskCard(
        title = "今日学习概览",
        subtitle = stats.nextStepText,
        accentColor = MaterialTheme.colorScheme.secondary,
    ) {
        val progress = stats.activePercent / 100f
        val progressShape = RoundedCornerShape(4.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("错题复盘总进度", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${stats.activePercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, progressShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, progressShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WqMetricTile("总题数", stats.total.toString(), Icons.AutoMirrored.Rounded.MenuBook, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            WqMetricTile("不熟", stats.unfamiliar.toString(), Icons.Rounded.Warning, ColorUnfamiliar, Modifier.weight(1f))
            WqMetricTile("复习中", stats.reviewing.toString(), Icons.AutoMirrored.Rounded.Help, ColorReviewing, Modifier.weight(1f))
            WqMetricTile("已掌握", stats.mastered.toString(), Icons.Rounded.CheckCircle, ColorMastered, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WqMetricTile(
    label: String, 
    value: String, 
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier,
        shape = tileShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
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
    var content by remember(question.id) { mutableStateOf(question.content) }
    var subject by remember(question.id) { mutableStateOf(question.subject) }
    var chapter by remember(question.id) { mutableStateOf(question.chapter) }
    var mastery by remember(question.id) { mutableStateOf(question.mastery) }
    var answer by remember(question.id) { mutableStateOf(question.answer) }
    var explanation by remember(question.id) { mutableStateOf(question.explanation) }
    var showAnswer by remember(question.id) { mutableStateOf(question.answer.isBlank() && question.explanation.isBlank()) }

    val dialogShape = RoundedCornerShape(16.dp)
    val buttonShape = RoundedCornerShape(8.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = dialogShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp,
        ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "错题档案校正",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = question.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Mastery Selector
                    Text("掌握状态", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    WqActionRow {
                        listOf("unfamiliar", "reviewing", "mastered").forEach { m ->
                            val isSelected = mastery == m
                            val accentColor = when (m) {
                                "unfamiliar" -> ColorUnfamiliar
                                "reviewing" -> ColorReviewing
                                "mastered" -> ColorMastered
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            FilterChip(
                                selected = isSelected,
                                onClick = { mastery = m },
                                label = { Text(masteryLabel(m), fontWeight = FontWeight.Bold) },
                                shape = buttonShape,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.outline,
                                    borderWidth = 1.dp
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("题目题干") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("科目") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = chapter,
                            onValueChange = { chapter = it },
                            label = { Text("章节") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!showAnswer) {
                        androidx.compose.material3.Button(
                            onClick = { showAnswer = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = buttonShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("查看 / 补充答案解析")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = answer,
                                onValueChange = { answer = it },
                                label = { Text("答案") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            OutlinedTextField(
                                value = explanation,
                                onValueChange = { explanation = it },
                                label = { Text("详细解析") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.weight(1.5f),
                            shape = buttonShape
                        ) {
                            Text("保存归档")
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
                lineHeight = 24.sp
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
