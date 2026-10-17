# YSMSync Web API

本文档说明 YSMSync 内置 Web 模型门户的 HTTP 接口，供自定义网页前端、管理后台或服务端程序调用。

## 1. 服务地址

Web 门户由插件启动时监听，默认配置如下：

```yaml
web-portal:
  enabled: true
  host: "0.0.0.0"
  port: 25584
  max-upload-bytes: 16777216
  admin-users:
    - memzgbl
```

直接访问：

```text
http://服务器地址:25584/
```

API 根路径为：

```text
http://服务器地址:25584/api
```

如果使用反向代理把门户发布在 `/ysm/`，前端应使用当前页面路径拼接 `/api`，例如：

```text
https://example.com/ysm/api
```

## 2. 认证方式

接口使用 HTTP Cookie 会话认证，不使用 API Key 或 Bearer Token。

登录成功后，服务端返回：

```http
Set-Cookie: ysmportal=<session>; HttpOnly; SameSite=Lax; Path=/
```

后续请求必须携带 `ysmportal` Cookie。

浏览器前端建议显式设置 `credentials`：

```javascript
const response = await fetch('/api/me', {
  credentials: 'include'
});
const data = await response.json();
```

注册和登录使用 `application/x-www-form-urlencoded`。用户名会被转换为小写，只允许 3 至 24 位英文、数字和下划线；密码至少 8 位。

## 3. 跨域和反向代理

当前 Web API 不返回 `Access-Control-Allow-Origin` 等 CORS 响应头。因此，浏览器前端不能直接从另一个域名调用该接口。

推荐架构：

```text
浏览器
  |
  | HTTPS，同源访问
  v
Nginx / 宝塔反向代理
  |
  | HTTP 127.0.0.1:25584
  v
Minecraft 服务端上的 YSMSync
```

将插件监听地址改为本机：

```yaml
web-portal:
  host: "127.0.0.1"
```

然后由 Nginx 或宝塔提供 HTTPS。不要直接把没有 TLS 的 `25584` 端口暴露到公网。

## 4. 通用响应格式

成功响应使用 JSON。例如：

```json
{
  "ok": true
}
```

请求处理失败时，当前实现通常返回 HTTP `400`：

```json
{
  "error": "请先登录"
}
```

未知路径返回 HTTP `404`：

```json
{
  "error": "not found"
}
```

调用方应同时检查 HTTP 状态码和 JSON 中的 `error` 字段。

## 5. 接口列表

| 方法 | 路径 | 是否登录 | 用途 |
|---|---|---:|---|
| GET | `/` | 否 | 返回内置网页 |
| POST | `/api/register` | 否 | 注册网页账户 |
| POST | `/api/login` | 否 | 登录并创建 Cookie 会话 |
| POST | `/api/logout` | 建议 | 退出登录 |
| GET | `/api/me` | 是 | 获取当前账户状态 |
| POST | `/api/bind` | 是 | 生成游戏账户绑定码 |
| GET | `/api/models` | 是 | 获取当前账户可见的模型 |
| POST | `/api/upload` | 是且已绑定 | 上传 `.ysm` 模型 |
| POST | `/api/model/{id}/use` | 是且已绑定 | 应用模型到绑定的在线玩家 |
| POST | `/api/model/{id}/grant` | 是 | 授权玩家使用私人模型 |
| GET | `/api/admin/models` | 管理员 | 获取全部网页模型 |
| POST | `/api/model/{id}/delete` | 管理员 | 删除网页模型 |
| POST | `/api/admin/sync` | 管理员 | 让在线 YSM 客户端重新同步 |

`{id}` 必须进行 URL 编码。模型 ID 通常包含 `/`，例如：

```text
portal/public/example.ysm
```

在 JavaScript 中使用 `encodeURIComponent(model.id)`。

## 6. 注册

### 请求

```http
POST /api/register
Content-Type: application/x-www-form-urlencoded
```

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `username` | string | 是 | 3 至 24 位英文、数字或下划线 |
| `password` | string | 是 | 至少 8 位 |

示例：

```bash
curl -i -X POST "${API_ROOT}/register" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'username=developer' \
  --data-urlencode 'password=change-this-password'
```

成功响应 HTTP `201`：

```json
{
  "ok": true
}
```

注册不会自动登录，注册成功后仍需调用登录接口。

## 7. 登录

### 请求

```http
POST /api/login
Content-Type: application/x-www-form-urlencoded
```

参数与注册相同。

使用 curl 时保存 Cookie：

```bash
curl -i -c ysmsync-cookies.txt -X POST "${API_ROOT}/login" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'username=developer' \
  --data-urlencode 'password=change-this-password'
```

成功响应 HTTP `200`：

```json
{
  "ok": true
}
```

## 8. 查询当前账户

### 请求

```http
GET /api/me
Cookie: ysmportal=<session>
```

示例响应：

```json
{
  "username": "developer",
  "bound": false,
  "uuid": null,
  "admin": false
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `username` | 当前网页账户名 |
| `bound` | 是否已绑定 Minecraft 玩家 |
| `uuid` | 已绑定玩家的 UUID，未绑定时为 `null` |
| `admin` | 当前账户名是否在 `web-portal.admin-users` 中 |

## 9. 绑定 Minecraft 玩家

只有完成绑定后，账户才能上传模型或使用私人模型。

### 生成绑定码

```http
POST /api/bind
Cookie: ysmportal=<session>
```

响应：

```json
{
  "instruction": "/ysmsyncweb bind 12345678"
}
```

玩家进入服务器后，在游戏聊天栏执行响应中的命令：

```text
/ysmsyncweb bind 12345678
```

绑定码只能使用一次；插件重启后尚未使用的绑定码会失效。绑定成功后重新请求 `/api/me` 确认 `bound` 变为 `true`。

## 10. 获取可见模型

### 请求

```http
GET /api/models
Cookie: ysmportal=<session>
```

已登录账户可以看到：

- 所有公共模型
- 自己上传的私人模型
- 已授权给绑定玩家的私人模型

响应是模型数组：

```json
[
  {
    "id": "portal/public/robot.ysm",
    "name": "robot",
    "owner": "developer",
    "private": false,
    "grants": [
      "00000000-0000-0000-0000-000000000000"
    ]
  }
]
```

字段说明：

| 字段 | 说明 |
|---|---|
| `id` | 服务端内部模型 ID，后续接口使用该值 |
| `name` | 模型显示名称 |
| `owner` | 上传账户名 |
| `private` | 是否为私人模型 |
| `grants` | 已授权玩家 UUID 集合 |

## 11. 上传模型

上传使用 `multipart/form-data`，文件字段名必须为 `model`。

### 请求

```http
POST /api/upload
Cookie: ysmportal=<session>
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `model` | file | 是 | `.ysm` 文件 |
| `visibility` | string | 否 | `public` 或 `private`，默认按非 `private` 处理为公共模型 |

默认单个文件最大为 16 MiB，实际限制由 `web-portal.max-upload-bytes` 决定。模型上传后会经过 YSM 解析校验，校验失败时不会保留文件。

示例：

```bash
curl -i -b ysmsync-cookies.txt -X POST "${API_ROOT}/upload" \
  -F 'model=@./robot.ysm' \
  -F 'visibility=public'
```

成功响应 HTTP `201`：

```json
{
  "ok": true,
  "message": "模型已校验并加入原生YSM同步"
}
```

模型 ID 的生成规则：

```text
公共模型：portal/public/{文件名}.ysm
私人模型：portal/private/{绑定玩家UUID}/{文件名}.ysm
```

文件名会被清理，只保留安全字符，最终长度最多 80 个字符。

当前 Web API 没有独立的图片上传接口，也没有独立的模型缩略图接口。外部前端只能通过 `.ysm` 文件上传模型；文档或 UI 中提到的图片不应当理解为可单独调用的 HTTP 路由。

## 12. 应用模型

账户必须已经绑定 Minecraft 玩家，且该玩家必须在线。

### 请求

```http
POST /api/model/{url-encoded-id}/use
Cookie: ysmportal=<session>
```

示例：

```javascript
const modelId = 'portal/public/robot.ysm';
const response = await fetch(
  '/api/model/' + encodeURIComponent(modelId) + '/use',
  {
    method: 'POST',
    credentials: 'include'
  }
);
const data = await response.json();
```

成功响应：

```json
{
  "ok": true,
  "message": "已应用模型"
}
```

应用模型后，玩家需要保持在线，插件会通过 YSM 协议完成模型同步。

## 13. 授权私人模型

只有私人模型的上传者可以调用此接口。目标玩家必须至少进入过一次服务器。

### 请求

```http
POST /api/model/{url-encoded-id}/grant
Cookie: ysmportal=<session>
Content-Type: application/x-www-form-urlencoded
```

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `player` | string | 是 | Minecraft 玩家名 |

示例：

```bash
curl -i -b ysmsync-cookies.txt \
  -X POST "${API_ROOT}/model/portal%2Fprivate%2FPLAYER_UUID%2Fsecret.ysm/grant" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'player=PlayerName'
```

成功响应：

```json
{
  "ok": true
}
```

## 14. 管理员接口

管理员由服务器配置中的账户名决定：

```yaml
web-portal:
  admin-users:
    - developer
```

管理员权限不由浏览器提交的字段决定，必须通过配置文件设置。

### 获取全部网页模型

```http
GET /api/admin/models
Cookie: ysmportal=<session>
```

该接口返回公共模型和所有私人模型。

### 删除网页模型

```http
POST /api/model/{url-encoded-id}/delete
Cookie: ysmportal=<session>
```

删除后，插件会重新加载模型，并通知在线 YSM 客户端重新同步。仍然引用该模型的玩家会回退到默认模型。删除不可恢复。

### 同步所有在线客户端

```http
POST /api/admin/sync
Cookie: ysmportal=<session>
```

成功响应：

```json
{
  "ok": true,
  "message": "已向所有在线 YSM 客户端发起模型重新同步"
}
```

## 15. 一个完整的前端调用流程

```javascript
const api = '/api';

async function request(path, options = {}) {
  const response = await fetch(api + path, {
    credentials: 'include',
    ...options
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || `HTTP ${response.status}`);
  }
  return data;
}

async function login(username, password) {
  return request('/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({ username, password })
  });
}

async function getCurrentUser() {
  return request('/me');
}

async function getModels() {
  return request('/models');
}

async function uploadModel(file, visibility = 'public') {
  const body = new FormData();
  body.append('model', file);
  body.append('visibility', visibility);
  return request('/upload', {
    method: 'POST',
    body
  });
}

async function useModel(modelId) {
  return request('/model/' + encodeURIComponent(modelId) + '/use', {
    method: 'POST'
  });
}
```

## 16. 服务端程序调用示例

使用 curl 时，先登录并保存 Cookie：

```bash
API_ROOT='https://example.com/ysm/api'

curl -c ysmsync-cookies.txt -X POST "${API_ROOT}/login" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'username=developer' \
  --data-urlencode 'password=change-this-password'

curl -b ysmsync-cookies.txt "${API_ROOT}/me"
curl -b ysmsync-cookies.txt "${API_ROOT}/models"
```

服务端程序不应把用户密码或会话 Cookie 暴露给浏览器日志。需要为其他系统提供管理接口时，建议由自己的后端保存 YSMSync 会话，并由自己的后端向浏览器提供经过授权的接口。

## 17. 数据和生命周期注意事项

- 账户和网页模型索引保存在 `plugins/YSMSync/web_portal.json`。
- 登录会话保存在内存中，插件重启后需要重新登录。
- 模型文件保存在 `plugins/YSMSync/models/custom/portal/`。
- 上传模型会触发 YSM 模型解析和缓存生成，较大的模型可能需要较长时间。
- 模型删除会影响正在使用该模型的玩家。
- `sync-check-interval-seconds` 只控制插件周期性重新检查在线客户端，不是 HTTP 请求超时时间。

## 18. 安全建议

- 使用 HTTPS 反向代理，不要直接公网暴露明文 HTTP 端口。
- 将 `host` 设置为 `127.0.0.1`，除非确实需要局域网直接访问。
- 为网页账户使用唯一且足够复杂的密码。
- 只把必要的账户加入 `admin-users`。
- 管理员接口可以删除模型并触发所有在线客户端同步，应限制访问范围。
- 不要在 URL、前端日志或错误日志中记录会话 Cookie。
- 外部前端优先使用与门户相同的源，以避免当前实现没有 CORS 头的问题。
