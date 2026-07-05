# Tunnel URL Publisher

Cong cu nay dung de expose backend local qua Cloudflare Quick Tunnel va ghi URL public vao GitHub:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

Backend mac dinh:

```text
http://localhost:7923
```

Publisher se cho backend tra loi thanh cong tai `/api/v1/events` truoc khi khoi dong
`cloudflared`. Vi vay tunnel chi duoc bat sau khi backend va database da san sang.

## Cau hinh

Tao file:

```text
tunnel-url-publisher/.env
```

Noi dung:

```env
GITHUB_TOKEN=github_pat_or_fine_grained_token
TUNNEL_BACKEND_URL=http://localhost:7923
```

`TUNNEL_BACKEND_URL` co the bo qua neu backend chay port `7923`.

Token GitHub can quyen doc/ghi file `smart-calendar.txt` trong repo:

```text
ArufaNguyen/tunnel-exposure
```

## Chay

Tu root project:

```bat
gradlew.bat :tunnel-url-publisher:run
```

Log thanh cong se co:

```text
[tunnel-url-publisher] cloudflared started for http://localhost:7923
[tunnel-url-publisher] tunnel URL found: https://xxxx.trycloudflare.com
[tunnel-url-publisher] GitHub file SHA found: ...
[tunnel-url-publisher] update success
```

Sau do app, Postman hoac API client doc raw URL:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

File nay chi chua mot dong:

```text
https://xxxx.trycloudflare.com
```
