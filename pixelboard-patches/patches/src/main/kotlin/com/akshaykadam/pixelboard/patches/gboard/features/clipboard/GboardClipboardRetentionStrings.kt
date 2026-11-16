package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

// Google's pin tip says unpinned clips are deleted after 1 hour, which is only true at that
// retention. Give every locale the same tip for PixelBoard's other retentions, so
// GboardClipboardRetentionTextRuntime can show the one that matches:
//   24 hours, 7 days, 30 days: Google's tip with only its duration changed.
//   Never, or a locale below has no duration for: just the tip's first sentence,
//   "Touch and hold a clip to pin it."
private const val PIN_TIP = "string_0x7f1401a6" // "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour." (18.5.3)
private const val PIN_TIP_1863 = "string_0x7f1401a5" // (18.6.3)
internal const val PIN_TIP_SHORT = "pixelboard_clipboard_pin_tip_short"
internal const val PIN_TIP_24_HOURS = "pixelboard_clipboard_pin_tip_24h"
internal const val PIN_TIP_7_DAYS = "pixelboard_clipboard_pin_tip_7d"
internal const val PIN_TIP_30_DAYS = "pixelboard_clipboard_pin_tip_30d"

private val SENTENCE_ENDS = listOf("。", "।", "။", "።", "։", "۔", "។")
private val PERIOD_BEFORE_SPACE = Regex("""\.(?=\s)""")
private val PIN_TIP_REGEX = Regex("""<string name="(?:$PIN_TIP|$PIN_TIP_1863)">(.*?)</string>""")

/** Google's duration in a locale's pin tip, and what it becomes at each longer retention. */
internal class PinTipDurations(
    val oneHour: String,
    val hours24: String,
    val days7: String,
    val days30: String,
)

private fun d(oneHour: String, hours24: String, days7: String, days30: String) =
    PinTipDurations(oneHour, hours24, days7, days30)

/** Keyed by resource folder. Each `oneHour` appears exactly once in that folder's stock tip. */
internal val PIN_TIP_DURATIONS: Map<String, PinTipDurations> = mapOf(
    "values" to d("1 hour", "24 hours", "7 days", "30 days"),
    "values-af" to d("1 uur", "24 uur", "7 dae", "30 dae"),
    "values-am" to d("1 ሰዓት", "24 ሰዓት", "7 ቀናት", "30 ቀናት"),
    "values-ar" to d("ساعة واحدة", "24 ساعة", "7 أيام", "30 يومًا"),
    "values-as" to d("1 ঘণ্টা", "24 ঘণ্টা", "7 দিন", "30 দিন"),
    "values-az" to d("1 saat", "24 saat", "7 gün", "30 gün"),
    "values-b+es+419" to d("Después de una hora", "Después de 24 horas", "Después de 7 días", "Después de 30 días"),
    "values-b+my+Qaag" to d("၁ နာရီ", "၂၄ နာရီ", "၇ ရက္", "၃၀ ရက္"),
    "values-b+sr+Latn" to d("posle sat vremena", "posle 24 sata", "posle 7 dana", "posle 30 dana"),
    "values-be" to d("праз гадзіну", "праз 24 гадзіны", "праз 7 дзён", "праз 30 дзён"),
    "values-bg" to d("1 час", "24 часа", "7 дни", "30 дни"),
    "values-bn" to d("১ ঘণ্টা", "২৪ ঘণ্টা", "৭ দিন", "৩০ দিন"),
    "values-bs" to d("nakon jednog sata", "nakon 24 sata", "nakon 7 dana", "nakon 30 dana"),
    "values-ca" to d("al cap d'una hora", "al cap de 24 hores", "al cap de 7 dies", "al cap de 30 dies"),
    "values-cs" to d("po hodině", "po 24 hodinách", "po 7 dnech", "po 30 dnech"),
    "values-da" to d("1 time", "24 timer", "7 dage", "30 dage"),
    "values-de" to d("nach einer Stunde", "nach 24 Stunden", "nach 7 Tagen", "nach 30 Tagen"),
    "values-el" to d("1 ώρα", "24 ώρες", "7 ημέρες", "30 ημέρες"),
    "values-en-rGB" to d("1 hour", "24 hours", "7 days", "30 days"),
    "values-es" to d("al cabo de una hora", "al cabo de 24 horas", "al cabo de 7 días", "al cabo de 30 días"),
    "values-et" to d("ühe tunni pärast", "24 tunni pärast", "7 päeva pärast", "30 päeva pärast"),
    "values-eu" to d("ordubeteren buruan", "24 orduren buruan", "7 egunen buruan", "30 egunen buruan"),
    "values-fa" to d("۱ ساعت", "۲۴ ساعت", "۷ روز", "۳۰ روز"),
    "values-fi" to d("tunnin kuluttua", "24 tunnin kuluttua", "7 päivän kuluttua", "30 päivän kuluttua"),
    "values-fr" to d("au bout d'une heure", "au bout de 24 heures", "au bout de 7 jours", "au bout de 30 jours"),
    "values-fr-rCA" to d("au bout d'une heure", "au bout de 24 heures", "au bout de 7 jours", "au bout de 30 jours"),
    "values-gl" to d("1 hora", "24 horas", "7 días", "30 días"),
    "values-gu" to d("1 કલાક", "24 કલાક", "7 દિવસ", "30 દિવસ"),
    "values-hi" to d("एक घंटे बाद", "24 घंटे बाद", "7 दिन बाद", "30 दिन बाद"),
    "values-hr" to d("nakon sat vremena", "nakon 24 sata", "nakon 7 dana", "nakon 30 dana"),
    "values-hu" to d("egy óra elteltével", "24 óra elteltével", "7 nap elteltével", "30 nap elteltével"),
    "values-hy" to d("1 ժամից", "24 ժամից", "7 օրից", "30 օրից"),
    "values-in" to d("1 jam", "24 jam", "7 hari", "30 hari"),
    "values-is" to d("1 klukkustund", "24 klukkustundir", "7 daga", "30 daga"),
    "values-it" to d("dopo un'ora", "dopo 24 ore", "dopo 7 giorni", "dopo 30 giorni"),
    "values-iw" to d("כעבור שעה", "כעבור 24 שעות", "כעבור 7 ימים", "כעבור 30 יום"),
    "values-ja" to d("1 時間後", "24 時間後", "7 日後", "30 日後"),
    "values-ka" to d("1 საათის", "24 საათის", "7 დღის", "30 დღის"),
    "values-kk" to d("1 сағаттан", "24 сағаттан", "7 күннен", "30 күннен"),
    "values-km" to d("1 ម៉ោង", "24 ម៉ោង", "7 ថ្ងៃ", "30 ថ្ងៃ"),
    "values-kn" to d("1 ಗಂಟೆಯ ನಂತರ", "24 ಗಂಟೆಗಳ ನಂತರ", "7 ದಿನಗಳ ನಂತರ", "30 ದಿನಗಳ ನಂತರ"),
    "values-ko" to d("1시간 후", "24시간 후", "7일 후", "30일 후"),
    "values-ky" to d("1 сааттан", "24 сааттан", "7 күндөн", "30 күндөн"),
    // Google's Lao tip says 1 day (ມື້) where every other locale says 1 hour.
    "values-lo" to d("1 ມື້", "24 ຊົ່ວໂມງ", "7 ມື້", "30 ມື້"),
    "values-lt" to d("po 1 valandos", "po 24 valandų", "po 7 dienų", "po 30 dienų"),
    "values-lv" to d("pēc stundas", "pēc 24 stundām", "pēc 7 dienām", "pēc 30 dienām"),
    "values-mk" to d("по 1 час", "по 24 часа", "по 7 дена", "по 30 дена"),
    "values-ml" to d("ഒരു മണിക്കൂറിന് ശേഷം", "24 മണിക്കൂറിന് ശേഷം", "7 ദിവസത്തിന് ശേഷം", "30 ദിവസത്തിന് ശേഷം"),
    "values-mn" to d("1 цагийн дараа", "24 цагийн дараа", "7 хоногийн дараа", "30 хоногийн дараа"),
    "values-mr" to d("एका तासानंतर", "24 तासांनंतर", "7 दिवसांनंतर", "30 दिवसांनंतर"),
    "values-ms" to d("selepas sejam", "selepas 24 jam", "selepas 7 hari", "selepas 30 hari"),
    "values-my" to d("၁ နာရီ", "၂၄ နာရီ", "၇ ရက်", "၃၀ ရက်"),
    "values-my-rZG" to d("၁ နာရီ", "၂၄ နာရီ", "၇ ရက္", "၃၀ ရက္"),
    "values-nb" to d("etter én time", "etter 24 timer", "etter 7 dager", "etter 30 dager"),
    "values-ne" to d("१ घन्टापछि", "२४ घन्टापछि", "७ दिनपछि", "३० दिनपछि"),
    "values-nl" to d("na één uur", "na 24 uur", "na 7 dagen", "na 30 dagen"),
    "values-or" to d("1 ଘଣ୍ଟା", "24 ଘଣ୍ଟା", "7 ଦିନ", "30 ଦିନ"),
    "values-pa" to d("1 ਘੰਟੇ ਬਾਅਦ", "24 ਘੰਟੇ ਬਾਅਦ", "7 ਦਿਨਾਂ ਬਾਅਦ", "30 ਦਿਨਾਂ ਬਾਅਦ"),
    "values-pl" to d("po godzinie", "po 24 godzinach", "po 7 dniach", "po 30 dniach"),
    "values-pt-rBR" to d("após uma hora", "após 24 horas", "após 7 dias", "após 30 dias"),
    "values-pt-rPT" to d("após 1 hora", "após 24 horas", "após 7 dias", "após 30 dias"),
    "values-ro" to d("după o oră", "după 24 de ore", "după 7 zile", "după 30 de zile"),
    "values-ru" to d("через час", "через 24 часа", "через 7 дней", "через 30 дней"),
    "values-si" to d("පැයකට පසු", "පැය 24කට පසු", "දින 7කට පසු", "දින 30කට පසු"),
    "values-sk" to d("po hodine", "po 24 hodinách", "po 7 dňoch", "po 30 dňoch"),
    "values-sl" to d("po eni uri", "po 24 urah", "po 7 dneh", "po 30 dneh"),
    "values-sq" to d("pas 1 ore", "pas 24 orësh", "pas 7 ditësh", "pas 30 ditësh"),
    "values-sr" to d("после сат времена", "после 24 сата", "после 7 дана", "после 30 дана"),
    "values-sv" to d("efter en timme", "efter 24 timmar", "efter 7 dagar", "efter 30 dagar"),
    "values-sw" to d("baada ya saa 1", "baada ya saa 24", "baada ya siku 7", "baada ya siku 30"),
    "values-ta" to d("1 மணிநேரத்திற்குப்", "24 மணிநேரத்திற்குப்", "7 நாட்களுக்குப்", "30 நாட்களுக்குப்"),
    "values-te" to d("ఒక గంట తర్వాత", "24 గంటల తర్వాత", "7 రోజుల తర్వాత", "30 రోజుల తర్వాత"),
    "values-th" to d("1 ชั่วโมง", "24 ชั่วโมง", "7 วัน", "30 วัน"),
    "values-tl" to d("1 oras", "24 na oras", "7 araw", "30 araw"),
    "values-tr" to d("1 saat sonra", "24 saat sonra", "7 gün sonra", "30 gün sonra"),
    "values-uk" to d("через 1 годину", "через 24 години", "через 7 днів", "через 30 днів"),
    "values-ur" to d("1 گھنٹے کے بعد", "24 گھنٹے کے بعد", "7 دن کے بعد", "30 دن کے بعد"),
    "values-uz" to d("1 soatdan keyin", "24 soatdan keyin", "7 kundan keyin", "30 kundan keyin"),
    "values-vi" to d("sau 1 giờ", "sau 24 giờ", "sau 7 ngày", "sau 30 ngày"),
    "values-zh" to d("1 小时", "24 小时", "7 天", "30 天"),
    "values-zh-rHK" to d("1 小時", "24 小時", "7 日", "30 日"),
    "values-zh-rTW" to d("1 小時", "24 小時", "7 天", "30 天"),
    "values-zu" to d("kwehora elingu-1", "kwamahora angu-24", "kwezinsuku ezingu-7", "kwezinsuku ezingu-30"),
)

/**
 * Adds the pin tips for PixelBoard's other retentions to one strings.xml, leaving everything
 * else untouched. [folder] is the strings.xml's resource folder, such as "values-fr".
 */
internal fun addPinTips(xml: String, folder: String): String {
    if (xml.contains("name=\"$PIN_TIP_SHORT\"")) return xml
    val tip = PIN_TIP_REGEX.find(xml)?.groupValues?.get(1)?.let(::decodeUnicodeEscapes) ?: return xml
    val end = xml.lastIndexOf("</resources>")
    if (end < 0) return xml
    val tips = buildString {
        appendString(PIN_TIP_SHORT, firstSentence(tip))
        val durations = PIN_TIP_DURATIONS[folder]
        if (durations != null) {
            withDuration(tip, durations.oneHour, durations.hours24)?.let { appendString(PIN_TIP_24_HOURS, it) }
            withDuration(tip, durations.oneHour, durations.days7)?.let { appendString(PIN_TIP_7_DAYS, it) }
            withDuration(tip, durations.oneHour, durations.days30)?.let { appendString(PIN_TIP_30_DAYS, it) }
        }
    }
    return xml.substring(0, end) + tips + xml.substring(end)
}

private val UNICODE_ESCAPE = Regex("""(?<!\\)\\u([0-9a-fA-F]{4})""")

/**
 * The patcher hands strings.xml over with non-ASCII text written as Java-style escapes (a
 * backslash, "u" and four hex digits), which Android also accepts. Turn them back into
 * characters so sentences and durations can be found.
 */
internal fun decodeUnicodeEscapes(value: String): String =
    UNICODE_ESCAPE.replace(value) { it.groupValues[1].toInt(16).toChar().toString() }

private fun StringBuilder.appendString(name: String, value: String) {
    append("    <string name=\"").append(name).append("\">").append(value).append("</string>\n")
}

/**
 * Swaps the tip's one-hour duration for [duration], or returns null when the tip does not
 * contain it exactly once (a Gboard update reworded it), so the short tip is shown instead.
 */
internal fun withDuration(tip: String, oneHour: String, duration: String): String? {
    for (stock in listOf(oneHour, oneHour.replace("'", "\\'")).distinct()) {
        val index = tip.indexOf(stock)
        if (index >= 0 && tip.indexOf(stock, index + 1) < 0) {
            return tip.substring(0, index) + duration + tip.substring(index + stock.length)
        }
    }
    return null
}

/** Keeps the first sentence ("Touch and hold a clip to pin it.") of a two-sentence string. */
internal fun firstSentence(value: String): String {
    val ends = SENTENCE_ENDS.mapNotNull { end ->
        value.indexOf(end).takeIf { it >= 0 }?.plus(end.length)
    } + listOfNotNull(PERIOD_BEFORE_SPACE.find(value)?.range?.last?.plus(1))
    val end = ends.minOrNull()
    if (end != null) return value.substring(0, end)
    if (value.endsWith(".") || SENTENCE_ENDS.any(value::endsWith)) return value
    // Thai separates sentences with a space instead of punctuation.
    val space = value.indexOf(' ')
    return if (space > 0) value.substring(0, space) else value
}
