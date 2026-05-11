# WQ Learner

WQ Learner 是一个面向 11408 考研复习的 Android 错题学习应用。当前版本包含：

- Android Jetpack Compose MVP 界面
- FastAPI 后端
- SQLite 持久化
- 11408 科目/章节关键词分类
- 错题上传草稿、确认入库、题库查询、练习接口
- Android HTTP 客户端与登录 token 状态管理

## 项目结构

```text
app/        Android 应用
backend/    FastAPI 后端
docs/       设计文档和实施计划
gradle/     Gradle wrapper 配置
```

## Android 构建

项目需要 Java 17。当前本机可使用：

```powershell
$env:JAVA_HOME="E:\DevEco Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

运行测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

构建 debug APK：

```powershell
.\gradlew.bat assembleDebug
```

## 后端运行

```powershell
uvicorn app.main:app --app-dir backend --reload
```

后端默认监听 `http://127.0.0.1:8000`。Android 模拟器访问开发机后端时使用：

```text
http://10.0.2.2:8000
```

## 后端测试

```powershell
pytest backend/tests -v
```

## 当前能力

- 注册、登录、token 认证
- SQLite 保存用户、token、错题、练习记录
- 图片上传接口返回模拟 OCR 草稿
- 识别结果以 Markdown + LaTeX 保存
- 按 11408 科目和章节分类
- 抽取已有错题练习
- 返回模拟变形题
- Android 端可登录后端并保存 token 状态

## 后续计划

后续功能按文档 `docs/superpowers/plans/2026-05-11-remaining-features.md` 逐步开发：

1. Android 题库页接入真实 `/questions` 数据
2. Android 相册选图
3. Android 拍照
4. 后端上传图片文件存储
5. OCR/公式识别服务适配
6. 大模型变形题服务适配
