# WQ Learner MVP 实施计划

> **给自动化开发者的要求：** 实施本计划时，应使用逐任务执行流程。步骤使用复选框跟踪，按测试先行、实现、验证的顺序推进。

**目标：** 构建一个可运行的 WQ Learner MVP：FastAPI 后端提供账号、错题、练习 API；Android Jetpack Compose App 提供上传校正、题库、练习和账号页面。

**架构：** 第一版先用可测试的后端 API 和 Android 本地 UI 闭环跑通产品结构。后端随后升级为 SQLite 持久化，Android 再逐步接入真实 HTTP、相册、相机、OCR 和大模型服务。

**技术栈：** Android Kotlin、Jetpack Compose Material 3、JUnit 4、Python 3、FastAPI、Pydantic、pytest。

---

## 文件结构

- 新增 `backend/app/__init__.py`：后端包标记。
- 新增 `backend/app/models.py`：Pydantic 请求/响应模型。
- 新增 `backend/app/classifier.py`：11408 科目/章节关键词分类器。
- 新增 `backend/app/store.py`：用户、token、错题和练习记录存储。
- 新增 `backend/app/main.py`：FastAPI 路由。
- 新增 `backend/tests/test_classifier.py`：分类器行为测试。
- 新增 `backend/tests/test_api.py`：API 契约测试。
- 新增 `backend/README.md`：后端运行和测试说明。
- 新增 `app/src/main/java/com/example/wq_learner1/domain/SubjectClassifier.kt`：Android 端分类器。
- 新增 `app/src/test/java/com/example/wq_learner1/domain/SubjectClassifierTest.kt`：Android 分类器测试。
- 替换 `app/src/main/java/com/example/wq_learner1/MainActivity.kt`：Compose MVP 页面。

## 任务 1：后端分类器

**文件：**

- 新增：`backend/app/classifier.py`
- 新增：`backend/tests/test_classifier.py`

步骤：

- [x] 编写失败测试：验证二叉树/哈希题目分类为“数据结构 / 树与二叉树”，未知题目返回“待分类 / 待选择”。
- [x] 运行测试，确认因为 `app.classifier` 不存在而失败。
- [x] 实现 `ClassificationResult` 和 `classify_question(text: str)`，使用关键词评分识别数据结构、计算机组成原理、操作系统、计算机网络。
- [x] 运行分类器测试，确认通过。

## 任务 2：后端 API

**文件：**

- 新增：`backend/app/models.py`
- 新增：`backend/app/store.py`
- 新增：`backend/app/main.py`
- 新增：`backend/tests/test_api.py`
- 新增：`backend/README.md`

步骤：

- [x] 编写失败 API 测试：注册、登录、上传草稿、确认入库、按科目查询、抽原题练习、请求模拟变形题。
- [x] 运行测试，确认因为 API 文件不存在而失败。
- [x] 实现模型、存储和路由，包括认证、上传草稿、确认错题、查询错题、原题练习、模拟变形题和复盘记录。
- [x] 运行 API 测试，确认通过。

## 任务 3：Android 端分类器

**文件：**

- 新增：`app/src/main/java/com/example/wq_learner1/domain/SubjectClassifier.kt`
- 新增：`app/src/test/java/com/example/wq_learner1/domain/SubjectClassifierTest.kt`

步骤：

- [x] 编写失败 Android 单元测试：验证 TCP 题目分类为“计算机网络 / 传输层”，未知题目返回“待分类 / 待选择”。
- [x] 运行测试，确认因为 `SubjectClassifier` 不存在而失败。
- [x] 实现 Android 端分类器，词表与后端保持一致。
- [x] 运行 Android 单元测试，确认通过。

## 任务 4：Android Compose MVP

**文件：**

- 替换：`app/src/main/java/com/example/wq_learner1/MainActivity.kt`

步骤：

- [x] 实现四个入口：上传、题库、练习、我的。
- [x] 使用本地样例状态展示 Markdown + LaTeX 识别结果、科目/章节建议、错题列表、原题练习和模拟变形题。
- [x] 运行 Android 单元测试，确认通过。
- [x] 构建 debug APK，确认通过。

## 自审结果

- 覆盖了认证、上传草稿、Markdown + LaTeX 内容、科目/章节分类、题库、原题练习、模拟变形题和 Android 页面骨架。
- 第一版刻意简化了数据库、对象存储、真实 OCR、Android HTTP 客户端和真实大模型集成。这些能力在后续功能计划中逐步补齐。
- 测试覆盖了后端分类器、后端 API 契约和 Android 分类器。
