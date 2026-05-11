# OSS 图片存储适配说明

WQ Learner 后端已经增加图片存储适配层。当前阶段默认使用本地文件存储，方便测试和开发；当配置 OSS 环境变量后，会进入 OSS 适配入口。真实 OSS 上传会在后续功能中接入。

## 当前实现

后端图片存储入口在：

```text
backend/app/image_storage.py
```

当前包含：

- `ImageStorage`：图片存储协议。
- `LocalImageStorage`：本地文件存储实现，用于测试和开发。
- `OssImageStorage`：OSS 占位实现，后续接入真实 OSS SDK 或签名上传。
- `create_image_storage()`：根据环境变量创建具体图片存储实现。

`/questions/upload` 已经改为先通过图片存储层保存图片，再把返回的 `image_url` 写入错题草稿。

## Object Key 规则

当前图片对象 key 使用：

```text
users/{user_id}/questions/{uuid}.{ext}
```

示例：

```text
users/8f13.../questions/6d5c....jpg
```

本地开发时，返回的图片 URL 形如：

```text
/uploads/users/{user_id}/questions/{uuid}.jpg
```

## 环境变量

| 名称 | 当前用途 | 示例 |
| --- | --- | --- |
| `WQ_LEARNER_UPLOAD_DIR` | 本地文件存储目录 | `/tmp/wq-learner-uploads` |
| `WQ_LEARNER_OSS_BUCKET` | OSS bucket 名称，设置后进入 OSS 适配入口 | `wq-learner-questions` |
| `WQ_LEARNER_OSS_ENDPOINT` | OSS endpoint | `oss-cn-hangzhou.aliyuncs.com` |

当前 `WQ_LEARNER_OSS_BUCKET` 只用于验证适配入口。如果现在设置该变量，上传接口会返回“OSS 图片存储适配层已预留”的未实现错误。

## 后续接入策略

推荐先使用后端代理上传：

- Android 把图片上传到函数计算 API。
- 函数计算校验登录态和文件类型。
- 函数计算将图片写入 OSS。
- 数据库只保存 object key 或可访问 URL。

后续也可以升级为客户端直传：

- 后端生成临时上传凭证或预签名 URL。
- Android 直接上传到 OSS。
- 后端保存上传完成后的 object key。

## 需要你配置阿里云的时机

当前功能不需要你配置 OSS。进入真实 OSS 上传功能时，需要你在阿里云侧准备：

- OSS bucket。
- bucket 所在地域和 endpoint。
- 函数计算访问 OSS 的 RAM 角色或最小权限 AccessKey。
- bucket 读写权限策略。

到那一步我会明确告诉你要在阿里云控制台配置哪些内容。
