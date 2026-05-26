---
name: WQ Learner Design System
version: 2.0
colors:
  primary: "#0F766E"
  primaryContainer: "#CCFBF1"
  secondary: "#0EA5E9"
  secondaryContainer: "#E0F2FE"
  tertiary: "#8B5CF6"
  tertiaryContainer: "#EDE9FE"
  error: "#E11D48"
  errorContainer: "#FFE4E6"
  success: "#16A34A"
  successContainer: "#DCFCE7"
  warning: "#D97706"
  warningContainer: "#FEF3C7"
  background: "#FAFAFA"
  surface: "#FFFFFF"
  surfaceVariant: "#F4F4F5"
  outline: "#E4E4E7"
  outlineVariant: "#D4D4D8"
  onPrimary: "#FFFFFF"
  onBackground: "#18181B"
  onSurface: "#18181B"
  onSurfaceVariant: "#71717A"
  muted: "#A1A1AA"
typography:
  display:
    fontFamily: system-ui
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 36sp
    letterSpacing: -0.5sp
  headline:
    fontFamily: system-ui
    fontSize: 22sp
    fontWeight: 600
    lineHeight: 28sp
    letterSpacing: -0.3sp
  title:
    fontFamily: system-ui
    fontSize: 16sp
    fontWeight: 600
    lineHeight: 24sp
  body:
    fontFamily: system-ui
    fontSize: 14sp
    fontWeight: 400
    lineHeight: 20sp
  label:
    fontFamily: system-ui
    fontSize: 12sp
    fontWeight: 500
    lineHeight: 16sp
  caption:
    fontFamily: system-ui
    fontSize: 11sp
    fontWeight: 500
    lineHeight: 14sp
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 20dp
  2xl: 24dp
  3xl: 32dp
radius:
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 20dp
  full: 999dp
elevation:
  none: 0dp
  xs: 0.5dp
  sm: 1dp
  md: 2dp
---

# WQ Learner Design System v2.0

> 基于 nexu-io/open-design skills 方法论重设计：
> - **platform-design**: Material Design 3 + Apple HIG 合规
> - **taste-skill**: 反 AI Slop，有温度的设计细节
> - **color-expert**: OKLCH 感知均匀色彩
> - **plan-design-review**: gstack 0-10 评分维度
> - **web-design-guidelines**: Vercel 产品 UI 标准

## 1. Visual Theme & Atmosphere

**极简教育工具** — 清晰、安静、专注。

- 不追求"酷"，追求"好用"
- 信息密度适中：每屏一个核心任务
- 留白是设计语言的一部分
- 动效克制：仅在状态转换时使用

**反 AI Slop 规则**:
- ❌ 没有渐变按钮（除非是主CTA）
- ❌ 没有装饰性投影
- ❌ 没有纯装饰 accent bar（除非传递信息）
- ❌ 没有过度圆角（pill shape 仅用于筛选器）
- ✅ 用留白代替分割线
- ✅ 用颜色层级代替描边

## 2. Colors

### Primary — Teal 700
`#0F766E` → 专注、学术、不张扬。比墨绿更现代。

### Semantic Colors
| 用途 | 颜色 | 容器色 |
|---|---|---|
| 不熟 | `#E11D48` Rose-600 | `#FFE4E6` |
| 复习中 | `#D97706` Amber-600 | `#FEF3C7` |
| 已掌握 | `#16A34A` Green-600 | `#DCFCE7` |

### 规则
- 背景始终 `#FAFAFA`（zinc-50），不用象牙白
- 卡片始终纯白 `#FFFFFF`
- 边框仅在需要分隔时使用，默认用背景色差替代

## 3. Typography

- **唯一字体族**: `system-ui`（SansSerif）
- **层级**: Display > Headline > Title > Body > Label > Caption
- **标题负字距**: `-0.3sp ~ -0.5sp`
- **正文正字距**: `0sp ~ 0.1sp`

**禁止**:
- Serif 字体
- Monospace 字体（代码块除外）
- `FontWeight.Black`（最重为 Bold/700）
- 全大写英文标题

## 4. Spacing

采用 4dp 基础网格：

| Token | 值 | 用途 |
|---|---|---|
| `xs` | 4dp | 紧凑内间距 |
| `sm` | 8dp | 元素间微距 |
| `md` | 12dp | 组内间距 |
| `lg` | 16dp | 卡片内边距、组间距 |
| `xl` | 20dp | 页面水平边距 |
| `2xl` | 24dp | 区块间距 |
| `3xl` | 32dp | 页面顶部留白 |

## 5. Components

### Card（卡片）
- 圆角: `12dp`
- 背景: `surface`（纯白）
- 边框: **无**（用 `0.5dp elevation` 微阴影替代）
- 内边距: `16dp`
- 间距: `12dp`（组件间）

### Button（按钮）
- Primary: 填充色 `primary`，圆角 `10dp`
- Secondary: 描边 `outline`，圆角 `10dp`
- Text: 无背景，文字色 `primary`
- 高度: `44dp`（触控安全区域）

### Chip / Pill（筛选标签）
- 圆角: `full`（999dp）
- 选中: 填充 `primary`
- 未选: 填充 `surfaceVariant`
- 描边: 不使用（颜色对比足够）

### TextField（输入框）
- 圆角: `10dp`
- 边框: `1dp` `outline`
- 焦点边框: `1.5dp` `primary`

### NavigationBar
- 高度: 80dp（含 safe area）
- 背景: `surface`
- 分割线: `0.5dp` `outlineVariant`

## 6. Elevation & Shadow

**极简阴影策略**:
- Level 0: 无阴影（默认平面元素）
- Level 1: `0.5dp` shadowElevation（卡片）
- Level 2: `2dp` shadowElevation（浮层/对话框）

**禁止**: 硬阴影、偏置阴影、装饰性 glow

## 7. Motion

- 时长: `200ms`（微交互）/ `300ms`（转场）
- 缓动: `EaseOutCubic`
- 触控反馈: scale 0.97x → 1.0x
- 禁止: 弹跳动效、长时间 loading 动画

## 8. Layout

- 页面水平边距: `20dp`
- 区块间距: `24dp`
- 卡片内间距: `16dp`
- 底部安全区: `innerPadding` + `16dp`

## 9. Accessibility

- 文字对比度 ≥ 4.5:1（WCAG AA）
- 最小触控目标: `44dp × 44dp`
- 所有图标配 `contentDescription`
- 焦点顺序符合视觉顺序
