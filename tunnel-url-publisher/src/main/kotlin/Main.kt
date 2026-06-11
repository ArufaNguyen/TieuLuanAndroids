import io.github.cdimascio.dotenv.dotenv
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val OWNER = "ArufaNguyen"
private const val REPO = "tunnel-exposure"
private const val BRANCH = "main"
private const val FILE_PATH = "smart-calendar.txt"
private const val DEFAULT_BACKEND_URL = "http://localhost:7923"
private const val COMMIT_MESSAGE = "Update Smart Calendar tunnel URL"
private val tunnelUrlRegex = Regex("""https://[a-zA-Z0-9-]+\.trycloudflare\.com""")
private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

fun main(args: Array<String>) {
    val token = loadGithubToken()
    val backendUrl = loadEnvValue("TUNNEL_BACKEND_URL")
        ?: loadEnvValue("BACKEND_URL")
        ?: DEFAULT_BACKEND_URL

    if ("--check-token" in args) {
        println("[tunnel-url-publisher] GitHub token check success")
        return
    }

    waitForBackend(backendUrl)
    val process = startCloudflared(backendUrl)
    val latch = CountDownLatch(1)
    var published = false

    Runtime.getRuntime().addShutdownHook(Thread {
        if (process.isAlive) {
            process.destroy()
        }
    })

    val readerThreads = listOf(process.inputStream, process.errorStream).map { stream ->
        Thread {
            stream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    println(line)

                    val tunnelUrl = tunnelUrlRegex.find(line)?.value
                    if (tunnelUrl != null && !published) {
                        published = true
                        println("[tunnel-url-publisher] tunnel URL found: $tunnelUrl")
                        publishTunnelUrl(token, tunnelUrl)
                        latch.countDown()
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    if (!latch.await(90, TimeUnit.SECONDS)) {
        println("[tunnel-url-publisher] URL not found within 90 seconds")
    }

    readerThreads.forEach { it.join() }
    process.waitFor()
}

private fun waitForBackend(backendUrl: String) {
    val healthUrl = backendUrl.trimEnd('/') + "/api/v1/events"
    val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()
    val request = Request.Builder().url(healthUrl).get().build()

    println("[tunnel-url-publisher] waiting for backend: $healthUrl")
    repeat(180) {
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    println("[tunnel-url-publisher] backend is ready")
                    return
                }
            }
        } catch (_: IOException) {
            // Backend is still starting.
        }
        Thread.sleep(1_000)
    }

    error("Backend did not become ready within 180 seconds: $backendUrl")
}

private fun loadGithubToken(): String {
    val token = loadEnvValue("GITHUB_TOKEN")

    if (token.isNullOrBlank()) {
        error("Missing GITHUB_TOKEN in tunnel-url-publisher/.env")
    }

    return token
}

private fun loadEnvValue(key: String): String? {
    System.getenv(key)
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    val dotenvValue = dotenv {
        ignoreIfMissing = true
    }[key]

    if (!dotenvValue.isNullOrBlank()) {
        return dotenvValue
    }

    return listOf(
        Path.of("tunnel-url-publisher", ".env"),
        Path.of(".env")
    ).firstNotNullOfOrNull { path ->
        readEnvFileValue(path, key)
    }
}

private fun readEnvFileValue(path: Path, key: String): String? {
    if (!Files.exists(path)) {
        return null
    }

    return Files.readAllLines(path)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
        .map { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
        .firstOrNull { it.first == key }
        ?.second
        ?.takeIf { it.isNotBlank() }
}

private fun startCloudflared(backendUrl: String): Process {
    return try {
        ProcessBuilder("cloudflared", "tunnel", "--url", backendUrl)
            .redirectErrorStream(false)
            .start()
            .also {
                println("[tunnel-url-publisher] cloudflared started for $backendUrl")
            }
    } catch (exception: IOException) {
        error("cloudflared not installed or not available in PATH: ${exception.message}")
    }
}

private fun publishTunnelUrl(token: String, tunnelUrl: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    try {
        val sha = fetchFileSha(client, token)
        updateGithubFile(client, token, sha, tunnelUrl)
    } catch (exception: IOException) {
        println("[tunnel-url-publisher] network failure: ${exception.message}")
    } catch (exception: RuntimeException) {
        println("[tunnel-url-publisher] GitHub API failure: ${exception.message}")
    }
}

private fun fetchFileSha(client: OkHttpClient, token: String): String {
    val url = "https://api.github.com/repos/$OWNER/$REPO/contents/$FILE_PATH?ref=$BRANCH"
    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .get()
        .build()

    client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string().orEmpty()
        println("[tunnel-url-publisher] HTTP status code: ${response.code}")

        if (!response.isSuccessful) {
            error(responseBody.ifBlank { "failed to fetch GitHub file SHA" })
        }

        val sha = JSONObject(responseBody).getString("sha")
        println("[tunnel-url-publisher] GitHub file SHA found: $sha")
        return sha
    }
}

private fun updateGithubFile(client: OkHttpClient, token: String, sha: String, tunnelUrl: String) {
    val url = "https://api.github.com/repos/$OWNER/$REPO/contents/$FILE_PATH"
    val encodedContent = Base64.getEncoder().encodeToString(tunnelUrl.toByteArray(Charsets.UTF_8))

    val payload = JSONObject()
        .put("message", COMMIT_MESSAGE)
        .put("content", encodedContent)
        .put("sha", sha)
        .put("branch", BRANCH)

    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .put(payload.toString().toRequestBody(jsonMediaType))
        .build()

    client.newCall(request).execute().use { response ->
        val responseBody = response.body?.string().orEmpty()
        println("[tunnel-url-publisher] HTTP status code: ${response.code}")

        if (response.isSuccessful) {
            println("[tunnel-url-publisher] update success")
        } else {
            println("[tunnel-url-publisher] update failure: $responseBody")
        }
    }
}
