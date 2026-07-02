# Portal Discovery Function

Module hiện được tổ chức như một thư viện function-only, không phải backend độc lập.

## Public API duy nhất

```kotlin
val result = runDesktopDiscovery(
    DesktopDiscoveryRequest(
        harFilePath = "C:/captures/portal.har",
    ),
)
```

Function:

```kotlin
suspend fun runDesktopDiscovery(
    request: DesktopDiscoveryRequest,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): DesktopDiscoveryResult
```

Không cần Spring context, controller, service, repository, JPA hoặc database.

## Workflow được giữ lại

```text
HAR/CapturedExchange
→ lọc static asset
→ normalize và deduplicate endpoint
→ hard safety
→ 9Router: Palamedes/Percival
→ Foza: Merlin/Arthur decision policy
→ Mordred gate
→ ReplayEngine gọi portal
→ 9Router: schema/mapping
→ Foza: Merlin/Arthur mapping approval
→ tạo get_student_schedule definition
→ trả DesktopDiscoveryResult
```

Mọi trạng thái chỉ tồn tại trong bộ nhớ trong thời gian một lần gọi function.

## Provider

- 9Router là provider chính.
- Foza chỉ dùng cho Merlin/Arthur.
- Không gọi OpenAI hoặc Anthropic trực tiếp.
- `.env` được load tự động.

```env
NINEROUTER_URL=http://localhost:20128
NINEROUTER_KEY=...
NINEROUTER_DEFAULT_COMBO=openrouter-combo
NINEROUTER_CODE_COMBO=opencode-combo
NINEROUTER_PALAMEDES_MODEL=openrouter-combo
NINEROUTER_PERCIVAL_MODEL=openrouter-combo
NINEROUTER_KAY_MODEL=openrouter-combo
NINEROUTER_MORGAN_MODEL=opencode-combo

FOZA_BASE_URL=https://api.foza.ai/v1
FOZA_API_KEY=...
FOZA_CLAUDE_MODEL=hoang/claude-sonnet-4.6
FOZA_GPT_MODEL=hoang/gpt-5.5
FOZA_MERLIN_MODEL=hoang/claude-sonnet-4.6
FOZA_MERLIN_FALLBACK_MODEL=hoang/gpt-5.5
```

## Chạy với HAR

```powershell
.\gradlew.bat runDesktopDiscovery -PharFile="C:\captures\portal.har"
```

## Build/Test

```powershell
.\gradlew.bat clean test build
```

## API Knowledge persistence

Sau khi replay va tool approval thanh cong, knowledge duoc upsert vao:

```text
.portal-discovery/api-knowledge.json
```

Co the doi duong dan bang `.env`:

```env
API_KNOWLEDGE_FILE=C:/smart-calendar/api-knowledge.json
```

Doc knowledge da luu:

```kotlin
val allKnowledge = loadApiKnowledge()
val schedule = findApiKnowledge("get_student_schedule")
val portalKnowledge = loadApiKnowledgeForPortal("https://portal.ut.edu.vn")
val portalSchedule = findApiKnowledge("https://portal.ut.edu.vn", "get_student_schedule")
```

File knowledge khong luu cookie, Authorization, token, request headers hoac response body.
Knowledge `id` la so nguyen tang dan do repository cap; upsert dung natural key `toolName + method + urlTemplate`.
Moi knowledge co `portalUrl`, vi du `https://portal.ut.edu.vn`, de tach knowledge cua nhieu portal.

Chay knowledge da luu o runtime:

```powershell
$env:PORTAL_AUTHORIZATION="Bearer <token>"
.\gradlew.bat runKnownTool -PportalUrl="https://portal.ut.edu.vn" -PtoolName="get_student_schedule" -PtoolParams="date=2026-06-11"
Remove-Item Env:PORTAL_AUTHORIZATION
```

Neu portal dung cookie, su dung `PORTAL_COOKIE`. `PORTAL_COOKIE` va `PORTAL_AUTHORIZATION` chi duoc doc trong memory luc runtime, khong duoc luu vao API Knowledge.

## Safety

- LLM chỉ phân tích và đề xuất.
- ReplayEngine là phần duy nhất gọi portal.
- `DELETE`, `PUT`, `PATCH` và write keyword bị chặn.
- POST chỉ được replay nếu là search/query/filter và có manual approval khi được yêu cầu.
- Session hết hạn trả `SESSION_EXPIRED`.
- Không hardcode key; `.env` không được commit.

## Source hiện tại

```text
src/main/kotlin/com/example/smartcalendar/portaldiscovery/
  PortalDiscovery.kt   # public function + orchestrator in-memory
  Models.kt
  Config.kt
  Main.kt              # CLI mỏng để thử HAR

  agents/
    GalahadCollectorAgent.kt
    PalamedesClassifierAgent.kt
    GawainSafetyAgent.kt
    ScheduleSignalAgent.kt
    PercivalScheduleHunterAgent.kt
    MerlinAdversarialVerifierAgent.kt
    ArthurJudgeAgent.kt
    MordredReplayGatekeeper.kt
    KayCalendarMapperAgent.kt
    BedivereToolDesignerAgent.kt

  core/
    CaptureImporter.kt
    EndpointCollector.kt
    Endpoint.kt
    ReplayEngine.kt
    CalendarNormalizer.kt

  llm/
    AgentLlmRouter.kt
    OpenAiCompatibleLlmClient.kt

src/test/kotlin/com/example/smartcalendar/portaldiscovery/
  PortalDiscoveryTest.kt
```

## Giới hạn

- Không lưu lịch sử/artifact vào database.
- Không có REST API.
- Không có Android capture adapter; Android có thể truyền `capturedExchanges`.
- HAR cũ có thể chứa session đã hết hạn, khi đó replay trả `SESSION_EXPIRED`.
- Mapping hiện hỗ trợ dữ liệu có start/end trực tiếp; period-to-time config chưa được triển khai trong bản function-only.
