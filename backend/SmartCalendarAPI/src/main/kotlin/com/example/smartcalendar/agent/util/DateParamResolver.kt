package com.example.smartcalendar.agent.util

import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Component
class DateParamResolver {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    fun today(): LocalDate = LocalDate.now(zone)

    fun resolveDate(message: String, currentDate: LocalDate = today()): String? {
        val text = normalize(message)
        explicitIso(text)?.let { return it }
        explicitVietnameseDate(text, currentDate)?.let { return it }
        return when {
            "hom nay" in text || "hôm nay" in message.lowercase() -> currentDate
            "ngay mai" in text || text.containsWord("mai") -> currentDate.plusDays(1)
            "hom qua" in text -> currentDate.minusDays(1)
            "tuan sau" in text -> currentDate.plusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "tuan nay" in text -> currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            else -> null
        }?.format(ISO)
    }

    fun resolveDateRange(message: String, currentDate: LocalDate = today()): Pair<LocalDate, LocalDate>? {
        val hits = explicitDateHits(message, currentDate)
        if (hits.size >= 2) {
            val first = hits[0].date
            val second = hits[1].date
            return if (first <= second) first to second else second to first
        }
        val text = normalize(message)
        return when {
            "tuan nay" in text -> {
                val start = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusDays(6)
            }
            "tuan sau" in text -> {
                val start = currentDate.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusDays(6)
            }
            else -> null
        }
    }

    fun resolvePage(message: String): String? =
        Regex("""(?:trang|page)\s+(\d+)""", RegexOption.IGNORE_CASE)
            .find(normalize(message))
            ?.groupValues
            ?.get(1)

    fun resolveSize(message: String): String? =
        Regex("""size\s+(\d+)""", RegexOption.IGNORE_CASE)
            .find(normalize(message))
            ?.groupValues
            ?.get(1)

    fun resolveIdDot(message: String): String? =
        Regex("""(?:dot|iddot|dot dang ky|đợt|idDot)\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(normalize(message))
            ?.groupValues
            ?.get(1)

    fun resolveCategoryId(message: String): String? =
        Regex("""(?:loai|category|categoryid|loại)\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(normalize(message))
            ?.groupValues
            ?.get(1)

    private fun explicitIso(text: String): String? =
        Regex("""\b(\d{4}-\d{2}-\d{2})\b""").find(text)?.groupValues?.get(1)

    private fun explicitVietnameseDate(text: String, currentDate: LocalDate): String? {
        val match = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{4}))?\b""").find(text) ?: return null
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val year = match.groupValues.getOrNull(3)?.takeIf(String::isNotBlank)?.toInt() ?: currentDate.year
        return runCatching { LocalDate.of(year, month, day).format(ISO) }.getOrNull()
    }

    private fun explicitDateHits(message: String, currentDate: LocalDate): List<DateHit> {
        val hits = mutableListOf<DateHit>()
        Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""").findAll(message).forEach { match ->
            val date = runCatching {
                LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt()
                )
            }.getOrNull()
            if (date != null) hits += DateHit(match.range.first, date)
        }
        Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{4}))?\b""").findAll(message).forEach { match ->
            val day = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val year = match.groupValues.getOrNull(3)?.takeIf(String::isNotBlank)?.toInt() ?: currentDate.year
            val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (date != null) hits += DateHit(match.range.first, date)
        }
        return hits.sortedBy { it.position }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('à', 'a').replace('á', 'a').replace('ả', 'a').replace('ã', 'a').replace('ạ', 'a')
            .replace('ă', 'a').replace('ằ', 'a').replace('ắ', 'a').replace('ẳ', 'a').replace('ẵ', 'a').replace('ặ', 'a')
            .replace('â', 'a').replace('ầ', 'a').replace('ấ', 'a').replace('ẩ', 'a').replace('ẫ', 'a').replace('ậ', 'a')
            .replace('è', 'e').replace('é', 'e').replace('ẻ', 'e').replace('ẽ', 'e').replace('ẹ', 'e')
            .replace('ê', 'e').replace('ề', 'e').replace('ế', 'e').replace('ể', 'e').replace('ễ', 'e').replace('ệ', 'e')
            .replace('ì', 'i').replace('í', 'i').replace('ỉ', 'i').replace('ĩ', 'i').replace('ị', 'i')
            .replace('ò', 'o').replace('ó', 'o').replace('ỏ', 'o').replace('õ', 'o').replace('ọ', 'o')
            .replace('ô', 'o').replace('ồ', 'o').replace('ố', 'o').replace('ổ', 'o').replace('ỗ', 'o').replace('ộ', 'o')
            .replace('ơ', 'o').replace('ờ', 'o').replace('ớ', 'o').replace('ở', 'o').replace('ỡ', 'o').replace('ợ', 'o')
            .replace('ù', 'u').replace('ú', 'u').replace('ủ', 'u').replace('ũ', 'u').replace('ụ', 'u')
            .replace('ư', 'u').replace('ừ', 'u').replace('ứ', 'u').replace('ử', 'u').replace('ữ', 'u').replace('ự', 'u')
            .replace('ỳ', 'y').replace('ý', 'y').replace('ỷ', 'y').replace('ỹ', 'y').replace('ỵ', 'y')
            .replace('đ', 'd')

    private fun String.containsWord(word: String): Boolean =
        Regex("""(^|\W)${Regex.escape(word)}($|\W)""").containsMatchIn(this)

    companion object {
        private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private data class DateHit(val position: Int, val date: LocalDate)
}
