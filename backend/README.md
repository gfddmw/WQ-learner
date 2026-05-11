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
