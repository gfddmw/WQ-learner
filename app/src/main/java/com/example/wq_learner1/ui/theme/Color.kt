package com.example.wq_learner1.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary ───
val PrimaryTeal = Color(0xFF0F766E)       // Teal-700 主色
val PrimaryTealDark = Color(0xFF14B8A6)   // Teal-500 暗色模式主色
val PrimaryContainer = Color(0xFFCCFBF1)  // Teal-100

// ─── Neutral ───
val NeutralInk = Color(0xFF18181B)        // Zinc-900 文字
val NeutralBg = Color(0xFFFAFAFA)         // Zinc-50 背景
val NeutralSurface = Color(0xFFFFFFFF)    // 纯白卡片
val NeutralVariant = Color(0xFFF4F4F5)    // Zinc-100 次级面
val NeutralOutline = Color(0xFFE4E4E7)    // Zinc-200 边框
val NeutralMuted = Color(0xFF71717A)      // Zinc-500 副文本
val NeutralSlate = Color(0xFFA1A1AA)      // Zinc-400
val NeutralDivider = Color(0xFFD4D4D8)    // Zinc-300

// ─── Semantic ───
val SemanticSuccess = Color(0xFF16A34A)   // Green-600
val SemanticWarning = Color(0xFFD97706)   // Amber-600
val SemanticError = Color(0xFFE11D48)     // Rose-600
val SemanticInfo = Color(0xFF0EA5E9)      // Sky-500

// ─── Semantic Containers ───
val SuccessContainer = Color(0xFFDCFCE7)
val WarningContainer = Color(0xFFFEF3C7)
val ErrorContainer = Color(0xFFFFE4E6)
val InfoContainer = Color(0xFFE0F2FE)

// ─── Mastery (mapped to semantic) ───
val ColorUnfamiliar = SemanticError
val ColorReviewing = SemanticWarning
val ColorMastered = SemanticSuccess

// ─── Dark Mode Mastery ───
val ColorUnfamiliarDark = Color(0xFFFDA4AF)   // Rose-300
val ColorReviewingDark = Color(0xFFFCD34D)    // Amber-300
val ColorMasteredDark = Color(0xFF86EFAC)     // Green-300

// ─── Dark Mode Neutral ───
val NightPrimary = Color(0xFF5EEAD4)      // Teal-300
val NightSurface = Color(0xFF18181B)      // Zinc-900
val NightBackground = Color(0xFF09090B)   // Zinc-950
val NightVariant = Color(0xFF27272A)      // Zinc-800
val NightOutline = Color(0xFF3F3F46)      // Zinc-700
val NightMuted = Color(0xFFA1A1AA)        // Zinc-400

// ─── Gradients (保留兼容) ───
val GradientStart = PrimaryTeal
val GradientEnd = Color(0xFF14B8A6)
val GradientBlueStart = Color(0xFF1E293B)
val GradientBlueEnd = Color(0xFF334155)
val GradientOrangeStart = Color(0xFFB7791F)
val GradientOrangeEnd = Color(0xFFD98A29)

// ─── Legacy compat (渐进移除) ───
@Deprecated("Use NeutralOutline") val WorkbenchLine = NeutralOutline
@Deprecated("Use NeutralMuted") val WorkbenchMuted = NeutralMuted
