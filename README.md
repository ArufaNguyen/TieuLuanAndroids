# Smart Calendar

Smart Calendar là project môn **Lập trình thiết bị di động**, trọng tâm là ứng dụng Android quản lý lịch cá nhân. App cho phép người dùng đăng ký, đăng nhập, quản lý sự kiện, phân loại lịch bằng tag màu, xem lịch theo ngày/tháng và đồng bộ dữ liệu với backend khi có mạng.

Repo hiện là một monorepo gồm Android app, backend REST API, module phân tích HAR/API portal và công cụ publish URL tunnel để app có thể gọi backend đang chạy local.

## Chức Năng Chính

- Đăng ký, đăng nhập và lưu phiên đăng nhập.
- Profile người dùng và đổi mật khẩu, bắt buộc nhập mật khẩu cũ trước khi cập nhật.
- Quản lý sự kiện: thêm, sửa, xóa, tìm kiếm, xem chi tiết và lọc theo dữ liệu người dùng hiện tại.
- Quản lý tag: thêm, sửa, xóa, chọn màu bằng RGB color picker.
- Lịch tháng ở màn hình menu hiển thị các chấm màu theo tag của từng ngày.
- Khi một ngày có nhiều sự kiện, lịch tháng hiển thị số lượng lịch trong ngày.
- Popup chi tiết ngày dùng item sự kiện chung, có thể đi tới màn quản lý tag hoặc màn chi tiết/sửa sự kiện.
- Lịch tuần có chế độ hiển thị cấu hình được trong Settings.
- Dữ liệu sự kiện và tag được lưu local bằng Room Database.
- Đồng bộ nền bằng WorkManager, có hàng đợi thay đổi local khi thiết bị chưa đồng bộ được.
- Upload file HAR để backend phân tích API lịch từ portal.
- WebView hỗ trợ bắt token đăng nhập portal và lưu credential cho agent.
- Agent Chat V2 hỗ trợ hỏi lịch, gọi tool đã học và trả lời dựa trên dữ liệu backend.

## Công Nghệ Sử Dụng

Android app:

- Kotlin, XML Layout
- Single Activity + Fragment
- Navigation Component + DrawerLayout
- Material Components
- Room Database
- WorkManager
- OkHttp
- org.json

Backend:

- Kotlin JVM
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Microsoft SQL Server
- dotenv-kotlin
- Coroutine

Module phụ trợ:

- `backend/ReverseAPIEndpoint`: phân tích HAR, phát hiện endpoint đọc lịch từ portal và tạo knowledge/tool.
- `tunnel-url-publisher`: mở Cloudflare Quick Tunnel cho backend local và publish URL public lên GitHub raw file.

## Cấu Trúc Project

```text
.
+-- app/                         # Android application
+-- backend/
|   +-- SmartCalendarAPI/         # Spring Boot REST API
|   +-- ReverseAPIEndpoint/       # HAR/API discovery module
+-- tunnel-url-publisher/         # Cloudflare Tunnel URL publisher
+-- build.gradle.kts              # Root Gradle config
+-- settings.gradle.kts           # Danh sách module Gradle
+-- gradlew
+-- gradlew.bat
```

Các module Gradle:

```text
:app
:backend
:reverse-api-endpoint
:tunnel-url-publisher
```

## Kiến Trúc Android

App dùng `MainActivity` làm activity chính, bên trong là `NavHostFragment` và `NavigationView` cho drawer menu. Các màn hình chính nằm trong package `app/src/main/java/com/example/tieuluanandroids/ui`.

Các màn hình đáng chú ý:

- `MenuFragment`: màn hình menu/trang chính, lịch tháng, popup sự kiện trong ngày.
- `CalendarFragment`: lịch tuần, đọc cấu hình từ `CalendarSettings`.
- `EventsFragment`: danh sách sự kiện, form thêm/sửa/xóa sự kiện trong cùng màn.
- `TagManagerFragment`: quản lý tag và chọn màu tag.
- `ProfileFragment`: thông tin tài khoản và đổi mật khẩu.
- `LoginFragment`: đăng nhập và đăng ký.
- `AddHarFileFragment`: chọn và upload file HAR.
- `DiscoveryJobFragment`: xem trạng thái job phân tích HAR.
- `ApiWebViewFragment`: mở portal trong WebView và lưu credential.
- `AgentChatV2Fragment`: chat với agent backend.
- `SessionFragment`: xem thông tin phiên đăng nhập.
- `SettingsFragment`: cấu hình chế độ lịch tuần.

## Luồng Dữ Liệu Và Đồng Bộ

App có composition root ở `SmartCalendarApplication`, khởi tạo:

- `SmartCalendarDatabase`
- `RoomLocalDataSource`
- `RoomSessionManager`
- `SmartCalendarRemoteDataSource`
- `SyncManager`
- `SmartCalendarData`

Room Database hiện lưu:

- `EventEntity`
- `TagEntity`
- `SyncOutboxEntity`
- `SessionEntity`

`Event` và `Tag` đều có `syncStatus` và thuộc tính `isSynced`. Khi người dùng thêm, sửa hoặc xóa sự kiện/tag, dữ liệu được ghi vào local trước, sau đó `SyncManager` đẩy thay đổi lên backend và kéo dữ liệu mới về khi có mạng.

## Backend REST API

Backend chính nằm ở `backend/SmartCalendarAPI`, chạy mặc định trên port `7923` theo file `.env.example`.

Các nhóm API chính:

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/auth/me

GET    /api/v1/events
GET    /api/v1/events/{id}
POST   /api/v1/events
PUT    /api/v1/events/{id}
DELETE /api/v1/events/{id}

GET    /api/v1/tags
GET    /api/v1/tags/{id}
POST   /api/v1/tags
PUT    /api/v1/tags/{id}
DELETE /api/v1/tags/{id}

GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}

GET    /api/v1/accounts
GET    /api/v1/accounts/{id}
POST   /api/v1/accounts
PUT    /api/v1/accounts/{id}
DELETE /api/v1/accounts/{id}

POST   /api/v1/analyze
GET    /api/v1/analyze
GET    /api/v1/analyze/{id}

POST   /api/v1/tools/by-id/{toolId}/run
POST   /api/v1/tools/{toolName}/run

GET    /api/v1/api-knowledge
GET    /api/v1/api-knowledge/me
GET    /api/v1/api-knowledge/global
POST   /api/v1/api-knowledge/global
POST   /api/v1/api-knowledge/global/preset
POST   /api/v1/api-knowledge/{id}/copy-to-global
GET    /api/v1/api-knowledge/{id}
DELETE /api/v1/api-knowledge/{id}

POST   /api/v1/portal-credentials/capture/start
POST   /api/v1/portal-credentials/capture/{captureId}/complete
GET    /api/v1/portal-credentials/me

POST   /api/v1/agent/chat
POST   /api/v2/agent/chat
```

## Database Backend

Schema SQL nằm ở `backend/SmartCalendarAPI/src/main/sql/schema.sql`. Database chính là `SmartCalendarDB` với các bảng:

- `users`
- `accounts`
- `sessions`
- `tags`
- `events`
- `discovery_jobs`
- `api_knowledge`

Backend đọc cấu hình database từ `backend/SmartCalendarAPI/.env`.

## Cấu Hình Môi Trường

Tạo file backend env:

```powershell
Copy-Item backend\SmartCalendarAPI\.env.example backend\SmartCalendarAPI\.env
```

Các biến quan trọng:

```env
HYPERVISOR=docker
DB_HOST=localhost
DB_PORT=6969
DB_NAME=SmartCalendarDB
DB_USERNAME=sa
DB_PASSWORD=replace-with-sql-password
SERVER_PORT=7923
DEV_MODE=true
AUTO_START_TUNNEL=true
HAR_MAX_FILE_SIZE=30MB
```

Nếu dùng SQL Server local thay vì Docker, đặt:

```env
HYPERVISOR=direct
```

Nếu không muốn tự chạy tunnel khi `bootRun`, đặt:

```env
AUTO_START_TUNNEL=false
```

Nếu dùng tunnel publisher, tạo file:

```powershell
Copy-Item tunnel-url-publisher\.env.example tunnel-url-publisher\.env
```

Sau đó điền `GITHUB_TOKEN` có quyền cập nhật file `smart-calendar.txt` trong repo tunnel exposure.

## Cách Build

Build Android app:

```powershell
.\gradlew.bat :app:assembleDebug
```

Build backend:

```powershell
.\gradlew.bat :backend:bootJar
```

Build module reverse API endpoint:

```powershell
.\gradlew.bat :reverse-api-endpoint:classes
```

Build tunnel publisher:

```powershell
.\gradlew.bat :tunnel-url-publisher:classes
```

Build nhanh các phần chính:

```powershell
.\gradlew.bat :app:assembleDebug :backend:bootJar :tunnel-url-publisher:classes
```

## Cách Chạy

Chạy backend:

```powershell
.\gradlew.bat :backend:bootRun
```

Root Gradle task đã cấu hình để khi chạy backend sẽ:

- Khởi động container MSSQL `TLANDROIDserver` nếu `HYPERVISOR=docker`.
- Bỏ qua Docker nếu `HYPERVISOR=direct`.
- Tự chạy tunnel publisher nếu `AUTO_START_TUNNEL=true`.

Chạy Android app bằng Android Studio:

1. Mở project tại thư mục gốc repo.
2. Chọn module `app`.
3. Chạy trên emulator hoặc thiết bị Android thật.

Hoặc cài debug APK bằng Gradle:

```powershell
.\gradlew.bat :app:installDebug
```

## Kết Nối App Với Backend

Android client dùng `SmartCalendarApiClient` và đọc base URL từ GitHub raw file:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

File này được `tunnel-url-publisher` cập nhật bằng URL Cloudflare Tunnel dạng:

```text
https://xxxx.trycloudflare.com
```

Vì vậy khi demo trên thiết bị thật hoặc emulator, luồng khuyến nghị là:

1. Chạy backend ở máy local.
2. Chạy tunnel publisher để tạo URL public.
3. App đọc URL public và gọi REST API qua OkHttp.

## Kiểm Thử

Chạy unit test Android:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Chạy test backend:

```powershell
.\gradlew.bat :backend:test
```

Chạy test reverse API endpoint:

```powershell
.\gradlew.bat :reverse-api-endpoint:test
```

## Ghi Chú Phát Triển

- App đang dùng XML layout, không dùng Jetpack Compose.
- App gọi API bằng OkHttp trực tiếp, không dùng Retrofit.
- Dữ liệu local ưu tiên Room, backend sync được xử lý qua `SyncManager` và `SyncWorker`.
- Màn `EventsFragment` và `TagManagerFragment` dùng form chỉnh sửa ngay trong màn, không tách dialog riêng.
- Các layout chính nằm trong `app/src/main/res/layout`.
- Các route chính nằm trong `app/src/main/res/navigation/nav_graph.xml`.
- Các item drawer nằm trong `app/src/main/res/menu/drawer_menu.xml`.

## Project link
 - https://github.com/ArufaNguyen/TieuLuanAndroids
## Thành viên nhóm
- Nguyễn Đức Anh
- Nguyễn Quốc Thắng (Nhóm trưởng)
- Phan Thế Hiển
- Lê Duy Khánh
