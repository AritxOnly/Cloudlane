package com.aritxonly.cloudlane.domain

import java.time.LocalDate
import java.time.LocalTime

/** Only structured signals are persisted; OCR text stays in memory. */
data class PhotoEvidence(
    val screenshot: Boolean,
    val protectedDocument: Boolean,
    val temporaryCode: Boolean,
    val deadline: LocalDate?,
)

object PhotoRules {
    private val screenshotPattern = Regex("screenshot|screen_shot|截屏|截图", RegexOption.IGNORE_CASE)
    private val deadlinePattern = Regex("""(?:有效期至|截止日期|有效至)[：:\s]*(20\d{2})[年/.-](\d{1,2})[月/.-](\d{1,2})""")

    fun isNight(time: LocalTime) = time.hour < 7
    fun isScreenshot(name: String, path: String) = screenshotPattern.containsMatchIn("$name $path")

    fun evidence(name: String, path: String, text: String): PhotoEvidence {
        val parts = deadlinePattern.find(text)?.groupValues
        val deadline = parts?.let {
            runCatching { LocalDate.of(it[1].toInt(), it[2].toInt(), it[3].toInt()) }.getOrNull()
        }
        return PhotoEvidence(
            screenshot = isScreenshot(name, path),
            protectedDocument = Regex("发票|合同|身份证|凭证|保修|病历").containsMatchIn(text),
            temporaryCode = Regex("取件码|验证码|动态密码").containsMatchIn(text),
            deadline = deadline,
        )
    }

    fun reason(name: String, path: String, ageDays: Long, text: String, today: LocalDate): String? =
        reason(evidence(name, path, text), ageDays, today)

    /** Reevaluate dates without rehashing or repeating OCR on unchanged files. */
    fun reason(evidence: PhotoEvidence, ageDays: Long, today: LocalDate): String? {
        if (ageDays < 0 || !evidence.screenshot || evidence.protectedDocument) return null
        if (ageDays >= 30 && evidence.temporaryCode) {
            return "疑似临时码截图，保存已超过 30 天，请确认是否仍有用途"
        }
        val deadline = evidence.deadline
        if (deadline != null && deadline.plusDays(7).isBefore(today)) {
            return "识别到截止日期 $deadline；请复查识别结果及保留价值"
        }
        return if (ageDays >= 90) "保存超过 90 天的截图，建议复查" else null
    }

    fun duplicateExtras(hashes: Map<String, String>): Set<String> = hashes.filterValues { it.isNotBlank() }
        .entries.groupBy { it.value }.values.flatMap { group ->
            group.sortedBy { it.key }.drop(1).map { it.key }
        }.toSet()
}
