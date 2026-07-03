# Cài đặt Smart Calendar Backend

Tài liệu này mô tả cấu hình hiện tại để chạy backend Spring Boot Kotlin trên Windows/VPS.

Có 2 phương án database:

1. **Dùng Docker + MSSQL container**: phù hợp máy Windows mới, có virtualization.
2. **Cài thẳng SQL Server vào VPS**: phù hợp VPS không bật được virtualization hoặc Docker Desktop báo lỗi.

## Cấu hình hiện tại

| Thành phần | Giá trị |
|---|---|
| Java | JDK 21 |
| Backend framework | Spring Boot 3.3.5 + Kotlin |
| Backend port | `7923` |
| Database | Microsoft SQL Server |
| Database name | `SmartCalendarDB` |
| Docker container | `TLANDROIDserver` |
| Docker MSSQL port | `6969:1433` |
| Package backend | `com.example.smartcalendar` |
| Tunnel | Cloudflare Quick Tunnel qua `tunnel-url-publisher` |

Backend đọc cấu hình từ:

```text
backend/SmartCalendarAPI/.env
```

Ví dụ khi dùng Docker:

```env
HYPERVISOR=docker
DB_HOST=localhost
DB_PORT=6969
DB_NAME=SmartCalendarDB
DB_USERNAME=sa
DB_PASSWORD=your_strong_password

SERVER_PORT=7923

DEV_MODE=true
```

`.env` đã được `.gitignore` chặn, không commit lên GitHub.

Biến `HYPERVISOR` quyết định cách chạy database:

| Giá trị | Ý nghĩa |
|---|---|
| `docker` | Dùng Docker Desktop và MSSQL container |
| `direct` | Không dùng Docker, kết nối SQL Server cài trực tiếp trên máy/VPS |

## Yêu cầu chung

Máy chạy backend cần:

- Windows 10/11 hoặc Windows Server.
- JDK 21 đầy đủ, có `java.exe` và `jlink.exe`.
- SQL Server, chạy bằng Docker hoặc cài trực tiếp.
- Cloudflared nếu cần expose API ra Internet.

Kiểm tra Java:

```powershell
java -version
where.exe java
where.exe jlink
```

## Direct mode tu cai SQL Server

Khi chon:

```env
HYPERVISOR=direct
```

file:

```bat
install-backend-environment.bat
```

se tu lam cac buoc sau:

1. Kiem tra SQL Server service `MSSQLSERVER`.
2. Neu chua co SQL Server, tu tai va cai SQL Server 2022 Express.
3. Cai theo default instance `MSSQLSERVER`.
4. Bat SQL authentication bang tai khoan `sa`.
5. Dung `DB_PASSWORD` trong `backend\SmartCalendarAPI\.env` lam mat khau `sa`.
6. Bat TCP/IP va cau hinh port theo `DB_PORT`, mac dinh la `1433`.
7. Chay `setup-smart-calendar-database.bat` de tao/cap nhat database, table, foreign key va seed data.

Voi VPS khong chay duoc Docker, chay:

```bat
install-backend-environment.bat
```

Sau do nhap:

```text
direct
```

Neu SQL Server da cai san, script khong cai lai, chi cau hinh port va setup database.

Không cần cài Gradle riêng vì project dùng Gradle Wrapper:

```text
gradlew.bat
gradle/wrapper/
```

## Phương án A: Dùng Docker + MSSQL container

Giữ phương án này nếu máy hỗ trợ virtualization.

### Yêu cầu Docker

Docker Desktop cần:

- Windows 10 22H2 build `19045` trở lên, hoặc Windows 11 23H2 build `22631` trở lên.
- CPU bật virtualization trong BIOS/UEFI.
- WSL 2 / Virtual Machine Platform hoạt động.

Kiểm tra Windows:

```powershell
systeminfo | findstr /B /C:"OS Name" /C:"OS Version"
```

Kiểm tra virtualization:

```powershell
Get-CimInstance Win32_Processor | Select-Object Name,VirtualizationFirmwareEnabled
```

Nếu Docker báo:

```text
Docker Desktop failed to start because virtualisation support wasn't detected
```

thì cần bật virtualization trong BIOS/UEFI. Nếu là VPS, nhà cung cấp VPS có thể không hỗ trợ nested virtualization; khi đó dùng **Phương án B**.

### Cài tự động

Chạy:

```text
install-backend-environment.bat
```

Script sẽ cài/check JDK 21, Docker Desktop, Cloudflared, tạo `.env`, tạo MSSQL container và build backend.

### Tạo MSSQL container thủ công

Thay `your_strong_password` bằng password giống trong `.env`:

```powershell
docker run `
  -e "ACCEPT_EULA=Y" `
  -e "MSSQL_SA_PASSWORD=your_strong_password" `
  -p 6969:1433 `
  --name TLANDROIDserver `
  -v mssql_data:/var/opt/mssql `
  -d mcr.microsoft.com/mssql/server:2022-latest
```

Tạo database:

```powershell
docker exec TLANDROIDserver /opt/mssql-tools18/bin/sqlcmd `
  -C -S localhost -U sa -P "your_strong_password" `
  -Q "IF DB_ID(N'SmartCalendarDB') IS NULL CREATE DATABASE SmartCalendarDB"
```

Khởi động lại container:

```powershell
docker start TLANDROIDserver
```

### Chạy backend với Docker

Với:

```env
HYPERVISOR=docker
```

lệnh sau sẽ tự khởi động Docker container và tunnel publisher:

```powershell
.\gradlew.bat :backend:bootRun
```

Nếu chỉ muốn chạy Spring Boot, không gọi task Docker/tunnel:

```powershell
.\gradlew.bat :backend:bootJar
java -jar backend\SmartCalendarAPI\build\libs\backend-0.0.1-SNAPSHOT.jar
```

## Phương án B: Cài thẳng SQL Server vào VPS

Dùng phương án này khi VPS không chạy được Docker Desktop, không có virtualization hoặc không hỗ trợ nested virtualization.

### Cài SQL Server

Cài một trong các bản sau:

- SQL Server Developer Edition.
- SQL Server Express.

Khi cài, bật:

- Database Engine Services.
- Mixed Mode Authentication.
- Tài khoản `sa`.
- Đặt password mạnh cho `sa`.

Cài thêm công cụ quản lý:

- SQL Server Management Studio neu muon quan ly database bang giao dien.
- `sqlcmd` la tuy chon. Neu khong co `sqlcmd`, `setup-smart-calendar-database.bat` se tu fallback sang PowerShell SqlClient.

### Bật TCP/IP cho SQL Server

Mở:

```text
SQL Server Configuration Manager
```

Vào:

```text
SQL Server Network Configuration
> Protocols for MSSQLSERVER
> TCP/IP
```

Đặt:

```text
Enabled = Yes
TCP Port = 1433
```

Restart service:

```text
SQL Server (MSSQLSERVER)
```

Nếu dùng named instance, port có thể khác `1433`; hãy dùng đúng port đó trong `.env`.

### Tạo database

Dung SSMS hoac `sqlcmd` neu muon tao thu cong:

```powershell
sqlcmd -S localhost -U sa -P "your_strong_password" -Q "IF DB_ID(N'SmartCalendarDB') IS NULL CREATE DATABASE SmartCalendarDB"
```

Neu `sqlcmd` khong co trong PATH, co the tao database bang SSMS:

```sql
CREATE DATABASE SmartCalendarDB;
```

Hoac chay script tu dong:

```powershell
.\setup-smart-calendar-database.bat
```

Script nay se tu dung `sqlcmd` neu co. Neu khong co `sqlcmd`, script dung PowerShell SqlClient de tao/cap nhat database.

### Cấu hình `.env` khi cài SQL Server trực tiếp

Sửa:

```text
backend/SmartCalendarAPI/.env
```

Nội dung:

```env
HYPERVISOR=direct
DB_HOST=localhost
DB_PORT=1433
DB_NAME=SmartCalendarDB
DB_USERNAME=sa
DB_PASSWORD=your_strong_password

SERVER_PORT=7923

DEV_MODE=true
```

Điểm khác với Docker:

```text
Docker: DB_PORT=6969
SQL Server cài trực tiếp: DB_PORT=1433
```

### Chạy backend trên VPS không dùng Docker

Với:

```env
HYPERVISOR=direct
```

`bootRun` sẽ bỏ qua Docker và tunnel auto-start:

```powershell
.\gradlew.bat :backend:bootRun
```

Nếu muốn tách hẳn khỏi root task, dùng cách chạy JAR:

```powershell
.\gradlew.bat :backend:bootJar
java -jar backend\SmartCalendarAPI\build\libs\backend-0.0.1-SNAPSHOT.jar
```

Backend chạy tại:

```text
http://localhost:7923
```

Nếu muốn máy khác gọi vào VPS, mở firewall cho port `7923`:

```powershell
New-NetFirewallRule `
  -DisplayName "Smart Calendar Backend 7923" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 7923 `
  -Action Allow
```

Không nên mở public port `1433` nếu không cần. Nếu bắt buộc mở SQL Server ra ngoài, cần giới hạn IP nguồn bằng firewall.

## Tunnel publisher

Nếu muốn expose backend qua Cloudflare Quick Tunnel, tạo:

```text
tunnel-url-publisher/.env
```

Nội dung:

```env
GITHUB_TOKEN=your_github_token_here
```

Token cần quyền `Contents: Read and write` với repository `tunnel-exposure`.

Chạy tunnel riêng:

```powershell
.\gradlew.bat :tunnel-url-publisher:run
```

Điều kiện:

- Backend đang chạy ở `http://localhost:7923`.
- Cloudflared đã cài và có trong PATH.

## Build backend

```powershell
.\gradlew.bat :backend:build
```

## API kiểm tra

Local:

```text
http://localhost:7923/api/v1/events
http://localhost:7923/api/v1/tags
http://localhost:7923/api/v1/users
```

Nếu bật tunnel, app sẽ lấy URL public từ:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

## Lỗi thường gặp

### Docker Desktop failed to start because virtualisation support wasn't detected

Nguyên nhân:

- Virtualization chưa bật trong BIOS/UEFI.
- VPS không hỗ trợ nested virtualization.
- Windows quá cũ hoặc thiếu WSL 2.

Cách xử lý:

1. Nếu là máy thật, bật Intel VT-x / AMD-V trong BIOS.
2. Nếu là VPS không hỗ trợ virtualization, dùng Phương án B: cài SQL Server trực tiếp.

### Windows quá cũ để cài Docker Desktop

Kiểm tra:

```powershell
systeminfo | findstr /B /C:"OS Name" /C:"OS Version"
```

Docker Desktop hiện cần Windows 10 22H2 build `19045` trở lên hoặc Windows 11 23H2 build `22631` trở lên.

### MSSQL không kết nối được

Kiểm tra `.env`:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Kiểm tra port:

```powershell
netstat -ano | findstr :1433
netstat -ano | findstr :6969
```

### `jlink.exe does not exist`

IDE hoặc Gradle đang dùng JRE rút gọn thay vì JDK 21 đầy đủ.

Kiểm tra:

```powershell
where.exe java
where.exe jlink
```
