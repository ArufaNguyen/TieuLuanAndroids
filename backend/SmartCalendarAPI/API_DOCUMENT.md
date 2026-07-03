# Smart Calendar API v1

Day la API contract hien tai cua backend. Tat ca route hop le dung prefix `/api/v1`.

## Base URL

Local:

```text
http://localhost:7923
```

Public base URL duoc doc tu:

```text
https://raw.githubusercontent.com/ArufaNguyen/tunnel-exposure/refs/heads/main/smart-calendar.txt
```

Vi du:

```text
{{baseUrl}} = https://xxxx.trycloudflare.com
{{baseUrl}}/api/v1/events
```

## Response Envelope

```json
{
  "code": 200,
  "message": "success",
  "body": {}
}
```

Client phai kiem tra `response.body.code`. Backend hien tai khong dung `ResponseEntity`, nen HTTP status co the van la `200` khi envelope `code` la `400`, `401`, `404` hoac `409`.

Khong co field `data`, `result`, `payload` hoac `success`. Du lieu luon nam truc
tiep trong `body`.

Shape cua `body`:

| Loai request | Success code | Shape |
|---|---:|---|
| Register | `201` | Auth object |
| Login, Me | `200` | Auth object |
| Logout | `200` | String |
| Get all/search/filter | `200` | Array |
| Get by ID | `200` | Detail object |
| Create | `201` | Detail object |
| Update | `200` | Detail object |
| Delete | `200` | String |
| Error | `400/401/404/409/500` | `null` |

List response va detail response cua account/tag/event khong cung shape:

- Account list co `userId`; account detail co nested `user`.
- Tag list co `userId`; tag detail co nested `user`.
- Event list co `tagId`, `tagName`, `userId`; event detail co `description`,
  nested `tag` va nested `user`.

## Postman Collection

Collection JSON khong duoc luu trong repository nay. Khi import collection vao
Postman, tao cac collection variables:

```text
rawTunnelUrl
baseUrl
sessionToken
authUserId
authAccountId
testUserId
testAccountId
testTagId
testEventId
```

Request dau tien goi `GET {{rawTunnelUrl}}` va cap nhat `baseUrl`:

```javascript
const baseUrl = pm.response.text().trim().replace(/\/+$/, "");
pm.collectionVariables.set("baseUrl", baseUrl);
```

Sau login, luu cac field trong `body` vao collection variables. Client phai
kiem tra envelope `code`. Cleanup du lieu test theo thu tu:

```text
event -> tag -> account -> user
```

Khong xoa seeded login account/user. Logout khong xoa Session history, nen
delete Account co the tra conflict neu van con Session tham chieu.

## Auth

### Register

```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json
```

```json
{
  "username": "mobile_user",
  "email": "mobile_user@example.com",
  "fullName": "Mobile User",
  "loginName": "mobile_user",
  "password": "123456"
}
```

Success envelope code: `201`.

### Login

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "loginName": "arufa",
  "password": "123456"
}
```

Success envelope code: `200`.

Response body:

```json
{
  "sessionToken": "uuid",
  "accountId": 1,
  "userId": 1,
  "username": "arufa",
  "loginName": "arufa",
  "email": "arufa@example.com",
  "fullName": "Arufa Nguyen",
  "expiresAt": "2026-07-09T06:00:00"
}
```

### Me

```http
GET {{baseUrl}}/api/v1/auth/me
X-Session-Token: {{sessionToken}}
```

### Logout

```http
POST {{baseUrl}}/api/v1/auth/logout
Content-Type: application/json
```

```json
{
  "sessionToken": "{{sessionToken}}"
}
```

## Users

```text
GET    {{baseUrl}}/api/v1/users
GET    {{baseUrl}}/api/v1/users?keyword=arufa
GET    {{baseUrl}}/api/v1/users/{{testUserId}}
POST   {{baseUrl}}/api/v1/users
PUT    {{baseUrl}}/api/v1/users/{{testUserId}}
DELETE {{baseUrl}}/api/v1/users/{{testUserId}}
```

Create/update body:

```json
{
  "username": "user_name",
  "email": "user_name@example.com",
  "fullName": "User Name"
}
```

Create success envelope code: `201`. Update/get/delete success code: `200`.

## Accounts

```text
GET    {{baseUrl}}/api/v1/accounts
GET    {{baseUrl}}/api/v1/accounts?keyword=arufa
GET    {{baseUrl}}/api/v1/accounts/{{testAccountId}}
POST   {{baseUrl}}/api/v1/accounts
PUT    {{baseUrl}}/api/v1/accounts/{{testAccountId}}
DELETE {{baseUrl}}/api/v1/accounts/{{testAccountId}}
```

Create body:

```json
{
  "username": "account_name",
  "loginName": "account_name",
  "password": "123456",
  "userId": 1
}
```

Update body:

```json
{
  "username": "account_name",
  "loginName": "account_name",
  "password": null,
  "userId": 1
}
```

`password` null/rong khi update se giu password cu.

## Tags

```text
GET    {{baseUrl}}/api/v1/tags
GET    {{baseUrl}}/api/v1/tags?keyword=Study
GET    {{baseUrl}}/api/v1/tags?userId={{testUserId}}
GET    {{baseUrl}}/api/v1/tags?keyword=Study&userId={{testUserId}}
GET    {{baseUrl}}/api/v1/tags/{{testTagId}}
POST   {{baseUrl}}/api/v1/tags
PUT    {{baseUrl}}/api/v1/tags/{{testTagId}}
DELETE {{baseUrl}}/api/v1/tags/{{testTagId}}
```

Create/update body:

```json
{
  "name": "Study",
  "color": "#2196F3",
  "userId": 1
}
```

`userId` co the la `null`.

## Events

```text
GET    {{baseUrl}}/api/v1/events
GET    {{baseUrl}}/api/v1/events?keyword=Spring
GET    {{baseUrl}}/api/v1/events?tagId={{testTagId}}
GET    {{baseUrl}}/api/v1/events?userId={{testUserId}}
GET    {{baseUrl}}/api/v1/events?from=2026-06-01T00:00:00&to=2026-06-30T23:59:59
GET    {{baseUrl}}/api/v1/events/{{testEventId}}
POST   {{baseUrl}}/api/v1/events
PUT    {{baseUrl}}/api/v1/events/{{testEventId}}
DELETE {{baseUrl}}/api/v1/events/{{testEventId}}
```

Create/update body:

```json
{
  "title": "Hoc Spring Boot",
  "description": "Test API",
  "startTime": "2026-06-20T08:00:00",
  "endTime": "2026-06-20T10:00:00",
  "tagId": 1,
  "userId": 1
}
```

Rules:

- `title` khong rong.
- `startTime`, `endTime` bat buoc.
- `endTime` phai sau `startTime`.
- Tag cua user rieng phai cung user voi event.

## Endpoint Migration

| Legacy | Current |
|---|---|
| `POST /api/session/login` | `POST /api/v1/auth/login` |
| `GET /api/session/dev-mode` | `GET /api/v1/auth/me` |
| `/api/auth/...` | `/api/v1/auth/...` |
| `/api/users/...` | `/api/v1/users/...` |
| `/api/accounts/...` | `/api/v1/accounts/...` |
| `/api/tags/...` | `/api/v1/tags/...` |
| `/api/events/...` | `/api/v1/events/...` |
| `POST /api/users/search` | `GET /api/v1/users?keyword=...` |
| `POST /api/accounts/search` | `GET /api/v1/accounts?keyword=...` |
| `POST /api/tags/search` | `GET /api/v1/tags?keyword=...&userId=...` |
| `POST /api/events/search` | `GET /api/v1/events?keyword=...&tagId=...&userId=...&from=...&to=...` |

Legacy routes da bi remove, khong phai alias/deprecated route.

## Seed Credentials

```text
arufa / 123456
thang / 123456
hien  / 123456
```
