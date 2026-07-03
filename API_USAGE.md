# Hướng Dẫn Sử Dụng Smart Calendar API

Tài liệu này mô tả cách Android app và các API client kết nối backend Smart Calendar, lấy Cloudflare Tunnel URL và sử dụng API v1.

## 1. Mô Hình Kết Nối

```text
Android app / API client
        |
        | GET raw GitHub file
        v
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
        |
        | response body là một dòng URL
        v
https://xxxx.trycloudflare.com
        |
        | gọi REST API
        v
Spring Boot backend :7923
        |
        | JPA/Hibernate
        v
SQL Server / SmartCalendarDB
```

Raw GitHub URL không kết nối trực tiếp database. URL này chỉ cung cấp `baseUrl` public của backend.

## 2. Base URL

Backend local:

```text
http://localhost:7923
```

Raw URL cung cấp Cloudflare Tunnel:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

Ví dụ nội dung raw file:

```text
https://example-abc.trycloudflare.com
```

Khi đó endpoint API có dạng:

```text
https://example-abc.trycloudflare.com/api/v1/auth/login
https://example-abc.trycloudflare.com/api/v1/events
```

## 3. Lấy Tunnel URL

### PowerShell

```powershell
$rawUrl = "https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt"
$baseUrl = (Invoke-WebRequest -Uri $rawUrl).Content.Trim()
```

### Postman

Tạo collection variable:

```text
rawTunnelUrl = https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
baseUrl
sessionToken
userId
accountId
tagId
eventId
```

Request đầu tiên trong collection:

```http
GET {{rawTunnelUrl}}
```

Tests script của request:

```javascript
const baseUrl = pm.response.text().trim().replace(/\/+$/, "");
pm.collectionVariables.set("baseUrl", baseUrl);
```

Không tạo environment variable `baseUrl` rỗng vì nó sẽ ghi đè collection variable.

### Android

1. Dùng OkHttp gọi raw GitHub URL.
2. Đọc và `trim()` response text.
3. Đảm bảo Retrofit base URL kết thúc bằng `/`.
4. Tạo API client với URL mới.

## 4. Định Dạng Response

```json
{
  "code": 200,
  "message": "success",
  "body": {}
}
```

Ví dụ lỗi:

```json
{
  "code": 404,
  "message": "user not found",
  "body": null
}
```

Client phải kiểm tra `json.code`, không chỉ kiểm tra HTTP status.

## 5. Auth Và Session

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
```

Register:

```json
{
  "username": "mobileuser",
  "email": "mobileuser@example.com",
  "fullName": "Mobile User",
  "loginName": "mobileuser",
  "password": "123456"
}
```

Login:

```json
{
  "loginName": "arufa",
  "password": "123456"
}
```

Sau login hoặc register, lưu:

```text
body.sessionToken
body.accountId
body.userId
```

Kiểm tra session:

```http
GET /api/v1/auth/me
X-Session-Token: <sessionToken>
```

Logout:

```json
{
  "sessionToken": "<sessionToken>"
}
```

Logout chỉ đặt Session thành `active=false`, không xóa Session record.

## 6. Users API

```text
GET    /api/v1/users?keyword=abc
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

Create hoặc update:

```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "fullName": "New User"
}
```

## 7. Accounts API

```text
GET    /api/v1/accounts?keyword=abc
GET    /api/v1/accounts/{id}
POST   /api/v1/accounts
PUT    /api/v1/accounts/{id}
DELETE /api/v1/accounts/{id}
```

Create:

```json
{
  "username": "testuser",
  "loginName": "testuser",
  "password": "123456",
  "userId": 1
}
```

Update không đổi password:

```json
{
  "username": "testuser_updated",
  "loginName": "testuser_updated",
  "password": null,
  "userId": 1
}
```

Một User chỉ có thể sở hữu một Account. Account username và loginName phải duy nhất.

## 8. Tags API

```text
GET    /api/v1/tags?keyword=study&userId=1
GET    /api/v1/tags/{id}
POST   /api/v1/tags
PUT    /api/v1/tags/{id}
DELETE /api/v1/tags/{id}
```

Tag của User:

```json
{
  "name": "Study",
  "color": "#2196F3",
  "userId": 1
}
```

Tag hệ thống:

```json
{
  "name": "System Default",
  "color": "#607D8B",
  "userId": null
}
```

Không thể xóa Tag đang được Event sử dụng.

## 9. Events API

```text
GET    /api/v1/events?keyword=Spring&tagId=1&userId=1&from=...&to=...
GET    /api/v1/events/{id}
POST   /api/v1/events
PUT    /api/v1/events/{id}
DELETE /api/v1/events/{id}
```

Create hoặc update:

```json
{
  "title": "Học Spring Boot",
  "description": "Kiểm thử API",
  "startTime": "2026-06-20T08:00:00",
  "endTime": "2026-06-20T10:00:00",
  "tagId": 1,
  "userId": 1
}
```

Quy tắc:

- `title`, `startTime`, `endTime` là bắt buộc.
- `endTime` phải sau `startTime`.
- `tagId` và `userId` có thể là `null`.
- Nếu Tag thuộc một User cụ thể, Event phải thuộc cùng User đó.

## 10. Chạy Postman Collection

Collection JSON không được lưu trong repository này. Import bản collection đang dùng vào Postman, sau đó:

1. Chọn collection và mở tab **Variables**.
2. Đặt `rawTunnelUrl`; để request đầu tiên cập nhật `baseUrl`.
3. Chạy request lấy tunnel URL trước khi gọi API.
4. Chạy login và lưu `body.sessionToken`, `body.accountId`, `body.userId` vào collection variables.
5. Gửi `X-Session-Token: {{sessionToken}}` khi gọi `/api/v1/auth/me`.
6. Kiểm tra `json.code` trong response envelope, không chỉ kiểm tra HTTP status.

Khi chạy CRUD tự động, tách ID đăng nhập khỏi ID dữ liệu test:

```text
authUserId
authAccountId
testUserId
testAccountId
testTagId
testEventId
```

Không xóa tài khoản seed dùng để đăng nhập. Cleanup dữ liệu test theo thứ tự:

```text
event -> tag -> account -> user
```

Logout chỉ đặt Session thành `active=false`. Account còn Session history có thể không xóa được do foreign key; đây là hành vi bảo vệ dữ liệu của backend.

## 11. Lỗi Thường Gặp

### URL trở thành `http://api/v1/...`

Environment đang có `baseUrl` rỗng và ghi đè collection variable. Xóa `baseUrl` khỏi environment.

### `invalid login name or password`

Sai loginName hoặc password. Password seed mặc định:

```text
123456
```

### `invalid or expired session`

Token sai, hết hạn hoặc đã logout.

### `tag does not belong to user`

Tag không thuộc User được gán cho Event.

### `endTime must be after startTime`

`endTime` phải lớn hơn `startTime`.

### Tunnel URL rỗng hoặc cũ

Backend và tunnel publisher phải đang chạy:

```bat
gradlew.bat :backend:bootRun --console=plain
```

## 12. Seed Data

| Login | Password |
|---|---|
| `arufa` | `123456` |
| `thang` | `123456` |
| `hien` | `123456` |

Kiểm tra SQL:

```sql
SELECT id, username, email, full_name FROM users;
SELECT id, username, login_name, user_id FROM accounts;
SELECT id, name, user_id FROM tags;
SELECT id, title, tag_id, user_id FROM events;
SELECT id, account_id, active, created_at, expires_at FROM sessions;
```
