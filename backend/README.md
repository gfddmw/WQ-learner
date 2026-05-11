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

当前版本使用 SQLite 持久化，并使用模拟 OCR 和模拟变形题生成。API 形态已经为后续接入对象存储、真实 OCR/公式识别和大模型服务预留。

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
