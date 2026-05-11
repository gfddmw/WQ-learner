# WQ Learner 后端

这是 WQ Learner Android 应用的 FastAPI 后端 MVP。

## 运行

```powershell
uvicorn app.main:app --app-dir backend --reload
```

## 测试

```powershell
pytest backend/tests -v
```

当前版本使用 SQLite 持久化，并支持通过环境变量切换到表格存储、OSS、通义千问 VL OCR 和通义千问变形题生成。未配置云端模型时，会回退到模拟 OCR 和模拟变形题生成，方便本地开发。

## 数据

默认数据库路径：

```text
backend/data/wq_learner.db
```

如果需要切换数据库文件，可以设置 `WQ_LEARNER_DB`：

```powershell
$env:WQ_LEARNER_DB="E:\A-NJU\WQ-learner\backend\data\dev.db"
uvicorn app.main:app --app-dir backend --reload
```

## OSS 图片存储

本地开发默认使用 `backend/data/uploads` 保存图片。函数计算环境配置 OSS bucket 后，`POST /questions/upload` 会把图片写入阿里云 OSS，并在错题记录中保存私有对象引用。

当前 OSS 配置：

```text
Bucket: wq-learner
Region: cn-hangzhou
Endpoint: oss-cn-hangzhou.aliyuncs.com
权限: 私有读写
```

函数计算环境变量：

```text
WQ_LEARNER_OSS_BUCKET=wq-learner
WQ_LEARNER_OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
WQ_LEARNER_OSS_REGION=cn-hangzhou
```

函数计算应绑定可访问 `wq-learner` bucket 的 RAM 角色。运行时会优先读取函数计算/RAM 角色注入的临时凭证环境变量：

```text
ALIBABA_CLOUD_ACCESS_KEY_ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET
ALIBABA_CLOUD_SECURITY_TOKEN
```

如果不是函数计算环境，也可以使用兼容的开发环境变量：

```text
OSS_ACCESS_KEY_ID
OSS_ACCESS_KEY_SECRET
OSS_SECURITY_TOKEN
```

上传对象 key 规则：

```text
users/{user_id}/questions/{uuid}.{jpg|png|bin}
```

因为 bucket 是私有读写，后端当前返回并保存的是 `oss://wq-learner/{object_key}` 形式的私有引用。后续如果 Android 需要直接预览云端图片，应由后端生成短期签名 URL 或增加图片代理接口。

## 表格存储

本地开发默认使用 SQLite。函数计算环境配置表格存储后，后端会使用 `TableStoreStore` 保存用户、登录 token、错题和练习记录。

当前表格存储配置：

```text
Instance: wq-learner
Region: cn-hangzhou
经典网内网 Endpoint: https://wq-learner.cn-hangzhou.ots-internal.aliyuncs.com
公网 Endpoint: https://wq-learner.cn-hangzhou.ots.aliyuncs.com
```

函数计算和表格存储都在 `cn-hangzhou` 时，推荐使用经典网内网 Endpoint：

```text
WQ_LEARNER_TABLESTORE_INSTANCE=wq-learner
WQ_LEARNER_TABLESTORE_ENDPOINT=https://wq-learner.cn-hangzhou.ots-internal.aliyuncs.com
WQ_LEARNER_TABLESTORE_REGION=cn-hangzhou
```

函数计算应绑定可访问该实例的 RAM 角色。运行时会优先读取函数计算/RAM 角色注入的临时凭证环境变量：

```text
ALIBABA_CLOUD_ACCESS_KEY_ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET
ALIBABA_CLOUD_SECURITY_TOKEN
```

如果不是函数计算环境，也可以使用兼容的开发环境变量：

```text
OTS_ACCESS_KEY_ID
OTS_ACCESS_KEY_SECRET
OTS_SECURITY_TOKEN
```

需要在表格存储实例中创建 4 张表：

```text
wq_users
  主键：email STRING
  属性：id, password

wq_tokens
  主键：token STRING
  属性：user_id, email

wq_questions
  主键：user_id STRING, id STRING
  属性：image_url, content_md_latex, subject, chapter, status, mastery

wq_practices
  主键：user_id STRING, id STRING
  属性：mode, question_ids_json, variant_json, result
```

表格存储没有配置时，后端仍会回退到 SQLite，方便本地测试。

## OCR/公式识别

`POST /questions/upload` 的处理流程是：

1. Android 上传 PNG/JPEG 图片。
2. 后端保存图片到本地文件或 OSS。
3. 后端调用 OCR 服务得到 Markdown + LaTeX 题干、科目和章节。
4. 后端把 OCR 草稿写入 SQLite 或表格存储。

当前默认使用 `SimulatedOcrService`，会返回一段稳定的模拟 Markdown + LaTeX 文本，并复用关键词分类器归类到 11408 科目和章节。这样上传、存储、题库刷新链路可以先完整跑通。

函数计算环境配置通义千问 VL 后，会使用 `DashScopeVisionOcrService` 进行真实识别：

```text
WQ_LEARNER_OCR_PROVIDER=dashscope
WQ_LEARNER_OCR_MODEL=qwen3-vl-plus
DASHSCOPE_API_KEY=你的百炼 API Key
WQ_LEARNER_DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

`WQ_LEARNER_DASHSCOPE_BASE_URL` 可省略，默认使用北京地域 OpenAI 兼容接口。函数计算依赖层需要包含 `openai`。

后续接入真实 OCR/视觉模型时，只需要实现同样的接口：

```text
recognize(image_content, content_type, image_url) -> OcrResult
```

`OcrResult` 字段：

```text
content_md_latex
subject
chapter
confidence
```

真实服务的密钥、endpoint 和模型名称应通过函数计算环境变量配置，不写入代码。

## 大模型变形题

`POST /practice/variant` 的处理流程是：
1. Android 传入原错题 ID 和主题。
2. 后端校验原错题属于当前登录用户。
3. 后端调用变形题生成服务，生成同知识点、不同条件的变式题。
4. 后端把练习记录写入 SQLite 或表格存储。

默认使用 `SimulatedVariantGenerator`，便于无密钥本地测试。函数计算环境配置通义千问后，会使用 `DashScopeVariantGenerator`：

```text
WQ_LEARNER_VARIANT_PROVIDER=dashscope
WQ_LEARNER_VARIANT_MODEL=qwen-plus
DASHSCOPE_API_KEY=你的百炼 API Key
WQ_LEARNER_DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

`WQ_LEARNER_DASHSCOPE_BASE_URL` 可与 OCR 共用并省略。函数计算依赖层需要包含 `openai`。

变形题返回字段：

```text
source_question_id
title
content_md_latex
answer_md_latex
explanation_md_latex
```

## 主要接口

认证：

- `POST /auth/register`
- `POST /auth/login`
- `GET /me`

错题：

- `POST /questions/upload`
- `POST /questions/{id}/confirm`
- `GET /questions`
- `GET /questions/{id}`
- `PATCH /questions/{id}`

练习：

- `POST /practice/original`
- `POST /practice/variant`
- `POST /practice/{id}/review`
