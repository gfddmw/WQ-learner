# 阿里云函数计算部署说明

本文记录 WQ Learner 后端迁移到阿里云函数计算的基础配置。当前阶段只完成函数计算运行基础：健康检查、依赖清单和启动入口。OSS、云端数据库、OCR 和大模型会在后续功能中逐步接入。

## 部署形态

- 服务类型：阿里云函数计算 Web 函数或自定义运行时 HTTP Server。
- 入口应用：`backend/app/main.py` 中的 FastAPI `app`。
- 启动入口：`backend/fc_bootstrap.py`。
- 默认监听端口：`9000`。
- 推荐调用方式：HTTP 触发器或 HTTPS 自定义域名。

阿里云函数计算 Web 函数会托管用户自己的 HTTP Server，并通过 HTTP 触发器或自定义域名把请求转发给该 HTTP Server。自定义运行时的 HTTP Server 需要监听 `0.0.0.0:CAPort`，默认端口是 `9000`。

参考官方文档：
- [Web 函数](https://help.aliyun.com/zh/functioncompute/fc/user-guide/web-functions)
- [自定义运行时 HTTP Server 配置要求](https://help.aliyun.com/zh/functioncompute/fc/user-guide/principles-1)
- [HTTP 触发器调用函数](https://help.aliyun.com/zh/functioncompute/fc/user-guide/http-trigger-invoking-function)

## 本地验证

在项目根目录运行：

```powershell
pip install -r backend/requirements.txt
python backend/fc_bootstrap.py
```

然后访问：

```text
http://127.0.0.1:9000/health
```

预期返回：

```json
{
  "status": "ok",
  "service": "wq-learner-api",
  "runtime": "fastapi"
}
```

本地开发时仍可继续使用：

```powershell
uvicorn app.main:app --app-dir backend --reload
```

## 函数计算配置建议

创建函数时建议选择 Web 函数或自定义运行时，并保持以下配置：

```text
启动命令：python
启动参数：fc_bootstrap.py
监听端口：9000
请求方式：HTTP 触发器或自定义域名
```

如果打包时以 `backend` 目录作为代码包根目录，代码包内应至少包含：

```text
app/
fc_bootstrap.py
requirements.txt
```

函数计算安装依赖时使用：

```bash
pip install -r requirements.txt -t .
```

## 环境变量

当前阶段保留 SQLite 作为本地开发和测试数据源。函数计算正式环境后续会切换到云端数据库；在此之前，不应把函数计算本地磁盘视为正式持久化数据源。

当前可用环境变量：

| 名称 | 用途 | 示例 |
| --- | --- | --- |
| `PORT` | 本地或函数环境启动端口，默认 `9000` | `9000` |
| `WQ_LEARNER_DB` | 本地 SQLite 数据库路径，仅用于开发和临时验证 | `/tmp/wq_learner.db` |

后续功能会新增：

| 名称 | 用途 |
| --- | --- |
| `WQ_LEARNER_DATABASE_URL` | 云端数据库连接配置 |
| `WQ_LEARNER_OSS_BUCKET` | OSS bucket 名称 |
| `WQ_LEARNER_OSS_ENDPOINT` | OSS endpoint |
| `WQ_LEARNER_MODEL_API_KEY` | OCR/大模型服务密钥 |

## Android 接入方式

Android 端已经支持在“我的”页切换 API 地址：

- 默认使用当前函数计算 HTTP 触发器地址。
- 如需本地调试，可在“我的”页点“本地开发”切换到 `http://10.0.2.2:8000`。
- “我的”页显示当前 API 地址，方便排查连接问题。
- 切换 API 地址后会清除当前 token，需要重新登录。

拿到函数计算 HTTP 触发器地址后，可以填入“我的”页的“API 地址”输入框，例如：

```text
https://example.cn-hangzhou.fcapp.run
```

## 注意事项

- 函数计算实例可能冷启动，健康检查接口应保持轻量。
- 图片不要保存到函数计算本地磁盘，后续改用 OSS。
- 正式用户、错题、练习记录不要依赖函数计算本地 SQLite，后续改用云端数据库。
- 生产环境应优先使用 HTTPS 自定义域名。
