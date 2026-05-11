# WQ Learner 后续功能实施计划

> **给自动化开发者的要求：** 实施本计划时，应按任务逐项执行。每个步骤使用复选框跟踪，功能完成后先汇报给用户，得到确认后再提交并推送到 GitHub。

**目标：** 将剩余生产路径功能逐个补齐，并在每个功能完成后单独汇报。

**架构：** 每个功能都应该可以独立验证和提交。先补后端持久化，再补 Android 与后端联通，然后补真实图片选择/拍照，最后接入 OCR 和大模型服务。

**技术栈：** Python sqlite3、FastAPI、pytest、Android Kotlin、Jetpack Compose、JUnit、Gradle。

---

## 功能顺序

1. 后端 SQLite 持久化。
2. Android HTTP 客户端和 token 状态管理。
3. Android 题库页接入真实 `/questions` 数据。
4. Android 相册选图。
5. Android 拍照。
6. 后端上传图片文件存储。
7. OCR/公式识别服务适配器。
8. 真实大模型变形题服务适配器。

## 功能 1：后端 SQLite 持久化

**文件：**

- 修改：`backend/app/store.py`
- 修改：`backend/app/main.py`
- 新增：`backend/tests/test_persistence.py`
- 修改：`backend/README.md`

步骤：

- [x] 编写失败测试：在一个 SQLite Store 中注册用户、上传并确认错题，再创建第二个指向同一数据库文件的 Store，重新登录并确认错题仍可查询。
- [x] 运行测试，确认因为只有内存 Store 而失败。
- [x] 将内存字典替换为 SQLite Store，同时保留 `main.py` 使用的公共方法。
- [x] 更新 `main.py`，避免直接访问内部字典，改用 Store 辅助方法。
- [x] 运行后端测试。
- [x] 向用户汇报功能 1 完成。

## 功能 2：Android HTTP 客户端和 token 状态管理

**文件：**

- 新增：`app/src/main/java/com/example/wq_learner1/network/WqLearnerApiClient.kt`
- 新增：`app/src/test/java/com/example/wq_learner1/network/WqLearnerApiClientTest.kt`
- 修改：`app/src/main/AndroidManifest.xml`
- 修改：`app/src/main/java/com/example/wq_learner1/MainActivity.kt`

步骤：

- [x] 编写失败测试：模拟后端响应，验证登录会保存 token，题库请求会携带 Bearer token，退出会清空会话。
- [x] 实现 `HttpTransport`、`UrlConnectionTransport`、`WqLearnerApiClient`、`SessionState` 和相关数据模型。
- [x] 为 Android 添加网络权限和开发阶段 HTTP 明文访问配置。
- [x] 在“我的”页面加入邮箱、密码、登录、注册并登录、退出和 token 状态展示。
- [x] 运行后端测试、Android 单元测试和 debug 构建。
- [ ] 用户确认后提交并推送到 GitHub。

## 后续功能 3：Android 题库页接入真实后端数据

目标：

- 登录后可从后端 `/questions` 拉取错题。
- 题库页不再只展示本地样例数据。
- 保留科目筛选，并将筛选条件传给后端。

建议测试：

- 使用 fake API client 验证有 token 时会调用 `listQuestions`。
- 验证无 token 时题库页显示登录提示。
- 验证后端返回错题会转换成 UI 题卡。
