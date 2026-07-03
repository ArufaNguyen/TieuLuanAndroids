# Tổng Quan Dự Án Smart Calendar

Tài liệu này tóm tắt trạng thái hiện tại của backend, cơ sở dữ liệu, tunnel publisher, Android API client và cách kiểm thử bằng Postman collection.

## 1. Kiến Trúc Tổng Thể

```text
Android / Postman / API client
    |
    | đọc public base URL
    v
GitHub raw: smart-calendar.txt
    |
    v
https://xxxx.trycloudflare.com
    |
    v
Cloudflare Quick Tunnel
    |
    v
Spring Boot backend :7923
    |
    v
SQL Server / SmartCalendarDB
```

Dự án gồm:

- `app`: Android client.
- `backend/SmartCalendarAPI`: REST API Kotlin Spring Boot.
- `tunnel-url-publisher`: chạy Cloudflare Quick Tunnel và publish URL lên GitHub.
- `install-backend-environment.bat`: cài đặt và cấu hình môi trường backend.
- `setup-smart-calendar-database.bat`: tạo, cập nhật schema và seed dữ liệu.

## 2. Backend

Stack chính:

- Kotlin JVM `1.9.25`
- Spring Boot `3.3.5`
- Java `21`
- Spring Web, Data JPA, Validation
- Microsoft SQL Server

Backend đọc cấu hình từ:

```text
backend/SmartCalendarAPI/.env
```

Các biến chính:

```env
HYPERVISOR=docker|direct
DB_HOST=localhost
DB_PORT=6969|1433
DB_NAME=SmartCalendarDB
DB_USERNAME=sa
DB_PASSWORD=...
SERVER_PORT=7923
DEV_MODE=true
AUTO_START_TUNNEL=true
```

Hai chế độ cơ sở dữ liệu:

- `docker`: container `TLANDROIDserver`, host port `6969`.
- `direct`: SQL Server hoặc SQLEXPRESS cài trực tiếp, port `1433`.

## 3. Kiến Trúc Phân Tầng

```text
common/       ApiResponse
config/       CORS, dotenv
controller/   Xử lý HTTP request, query và header
dto/          Request/response DTO theo domain
exception/    Global exception handling
model/        JPA entity
repository/   Truy cập cơ sở dữ liệu
service/      Validation, business logic và entity-to-DTO mapping
```

Các JPA entity:

- `User`
- `Account`
- `Session`
- `Tag`
- `Event`

Backend không trả trực tiếp JPA entity ra API. Service chuyển entity thành response DTO.

## 4. Định Dạng Response

Tất cả API trả về envelope:

```json
{
  "code": 200,
  "message": "success",
  "body": {}
}
```

Các giá trị `code` thường dùng:

- `200`: thành công
- `201`: tạo mới thành công
- `400`: request không hợp lệ
- `401`: chưa xác thực hoặc session không hợp lệ
- `404`: không tìm thấy dữ liệu
- `409`: xung đột dữ liệu hoặc ràng buộc
- `500`: lỗi nội bộ

Controller hiện trả `ApiResponse` trực tiếp, không dùng `ResponseEntity`. Vì vậy client phải kiểm tra `json.code`, không chỉ dựa vào HTTP status.

Shape của `body`:

- List, search, filter: array
- Get, create, update: object
- Delete, logout: string
- Lỗi: `null`

## 5. API V1

Tất cả endpoint hiện tại dùng prefix:

```text
/api/v1
```

### Auth

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

Login và register tạo session UUID hết hạn sau 30 ngày. `/auth/me` đọc header:

```text
X-Session-Token: <sessionToken>
```

### Users

```text
GET    /api/v1/users?keyword=abc
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

### Accounts

```text
GET    /api/v1/accounts?keyword=abc
GET    /api/v1/accounts/{id}
POST   /api/v1/accounts
PUT    /api/v1/accounts/{id}
DELETE /api/v1/accounts/{id}
```

### Tags

```text
GET    /api/v1/tags?keyword=study&userId=1
GET    /api/v1/tags/{id}
POST   /api/v1/tags
PUT    /api/v1/tags/{id}
DELETE /api/v1/tags/{id}
```

### Events

```text
GET    /api/v1/events?keyword=Java&tagId=1&userId=1&from=...&to=...
GET    /api/v1/events/{id}
POST   /api/v1/events
PUT    /api/v1/events/{id}
DELETE /api/v1/events/{id}
```

`from` và `to` dùng ISO local date-time:

```text
2026-06-20T08:00:00
```

Các route cũ `/api/...`, `/api/session/...` và `POST */search` không còn được sử dụng.

## 6. Business Rules

Auth:

- Login bằng `loginName` hoặc account `username`.
- Register kiểm tra trùng username, email và loginName.
- Logout đặt session thành `active=false`.

Account:

- Một user chỉ có một account.
- Account username và loginName là duy nhất.
- Update với password `null` hoặc rỗng sẽ giữ password cũ.

Tag:

- `name` là bắt buộc.
- `userId` có thể là `null` đối với tag hệ thống hoặc dùng chung.
- Không thể xóa tag đang được event sử dụng.

Event:

- `title`, `startTime`, `endTime` là bắt buộc.
- `endTime` phải sau `startTime`.
- Nếu tag thuộc một user cụ thể, tag và event phải cùng user.
- Search sắp xếp theo `startTime ASC`.

## 7. Thiết Lập Cơ Sở Dữ Liệu

`setup-smart-calendar-database.bat`:

- Hỗ trợ Docker và SQL Server trực tiếp.
- Tạo hoặc cập nhật năm bảng: `users`, `accounts`, `sessions`, `tags`, `events`.
- Tạo foreign key.
- Seed user, account, tag và event mẫu.
- Nếu direct mode không có `sqlcmd`, fallback sang PowerShell SqlClient.
- Nếu `sa` chưa đăng nhập được, thử Windows Authentication để bật Mixed Mode và đặt lại mật khẩu `sa`.

Tài khoản seed:

```text
arufa / 123456
thang / 123456
hien  / 123456
```

## 8. Tunnel Publisher

Publisher đọc:

```env
GITHUB_TOKEN=...
TUNNEL_BACKEND_URL=http://localhost:7923
```

Luồng hiện tại:

```text
bootRun lên lịch chạy publisher
-> publisher chờ GET http://localhost:7923/api/v1/events thành công
-> khởi động cloudflared
-> tìm URL https://*.trycloudflare.com
-> cập nhật ArufaNguyen/tunnel-exposure/main/smart-calendar.txt
```

Tunnel chỉ được bật sau khi backend và database đã sẵn sàng.

Public URL source:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

## 9. Lệnh Chạy

Thiết lập database:

```bat
setup-smart-calendar-database.bat
```

Build:

```bat
gradlew.bat :backend:build :tunnel-url-publisher:build
```

Chạy backend và tự động bật tunnel:

```bat
gradlew.bat :backend:bootRun --console=plain
```

Tắt tự động bật tunnel:

```env
AUTO_START_TUNNEL=false
```

Chạy hoặc kiểm tra publisher riêng:

```bat
gradlew.bat :tunnel-url-publisher:run
gradlew.bat :tunnel-url-publisher:run --args="--check-token"
```

## 10. Tài Liệu Liên Quan

- `backend/SmartCalendarAPI/API_DOCUMENT.md`: API contract cho client.
- `INSTALL_BACKEND.md`: cài đặt backend và database.
- `API_USAGE.md`: hướng dẫn kết nối, sử dụng API và chạy Postman collection.
