# 阿里云函数计算部署说明

本文记录 WQ Learner 后端迁移到阿里云函数计算的基础配置。当前后端已经接入函数计算运行入口、OSS 图片存储、表格存储、通义千问 VL OCR 和通义千问变形题生成。

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

本地开发可继续使用 SQLite 和本地上传目录；函数计算正式环境应使用 OSS、表格存储和通义千问模型服务。

当前可用环境变量：

| 名称 | 用途 | 示例 |
| --- | --- | --- |
| `PORT` | 本地或函数环境启动端口，默认 `9000` | `9000` |
| `WQ_LEARNER_DB` | 本地 SQLite 数据库路径，仅用于开发和临时验证 | `/tmp/wq_learner.db` |
| `WQ_LEARNER_DATABASE_URL` | 云端数据库连接地址，当前为适配入口占位 | `postgresql://...` |
| `WQ_LEARNER_UPLOAD_DIR` | 本地图片上传目录，仅用于开发和测试 | `/tmp/wq-learner-uploads` |
| `WQ_LEARNER_OSS_BUCKET` | OSS bucket 名称 | `wq-learner` |
| `WQ_LEARNER_OSS_ENDPOINT` | OSS endpoint | `oss-cn-hangzhou.aliyuncs.com` |
| `WQ_LEARNER_OSS_REGION` | OSS region | `cn-hangzhou` |
| `WQ_LEARNER_TABLESTORE_INSTANCE` | 表格存储实例名 | `wq-learner` |
| `WQ_LEARNER_TABLESTORE_ENDPOINT` | 表格存储 endpoint | `https://wq-learner.cn-hangzhou.ots.aliyuncs.com` |
| `WQ_LEARNER_TABLESTORE_REGION` | 表格存储 region | `cn-hangzhou` |
| `WQ_LEARNER_OCR_PROVIDER` | OCR 服务提供方，真实识别用 `dashscope` | `dashscope` |
| `WQ_LEARNER_OCR_MODEL` | OCR 多模态模型 | `qwen3-vl-plus` |
| `WQ_LEARNER_VARIANT_PROVIDER` | 变形题服务提供方，真实生成用 `dashscope` | `dashscope` |
| `WQ_LEARNER_VARIANT_MODEL` | 变形题文本模型 | `qwen-plus` |
| `WQ_LEARNER_DASHSCOPE_BASE_URL` | DashScope OpenAI 兼容接口地址，可省略 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `DASHSCOPE_API_KEY` | 百炼 API Key，用于 OCR 和变形题生成 | `sk-...` |

## Android 接入方式

Android 端已经支持在“我的”页切换 API 地址：

- 默认使用当前函数计算 HTTP 触发器地址。
- App 只接受 HTTPS 云端 API 地址，不再支持 `10.0.2.2` 本地后端。
- “我的”页显示当前云端 API 地址，方便排查连接问题。
- 切换云端 API 地址后会清除当前 token，需要重新登录。

拿到函数计算 HTTP 触发器地址后，可以填入“我的”页的“API 地址”输入框，例如：

```text
https://example.cn-hangzhou.fcapp.run
```

## 注意事项

- 函数计算实例可能冷启动，健康检查接口应保持轻量。
- 图片不要保存到函数计算本地磁盘，正式环境使用 OSS。
- 正式用户、错题、练习记录不要依赖函数计算本地 SQLite，正式环境使用表格存储。
- 函数计算依赖层需要包含 `openai`、`oss2` 和 `tablestore`。
- 生产环境应优先使用 HTTPS 自定义域名。
