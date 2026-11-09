package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GboardClipboardRetentionStringsTest {
    @Test
    fun `pin tip keeps only the first sentence in every script`() {
        val cases = mapOf(
            "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour." to
                "Touch and hold a clip to pin it.",
            "Appuyez de manière prolongée sur un élément pour l'épingler. Les éléments retirés seront supprimés au bout d'une heure." to
                "Appuyez de manière prolongée sur un élément pour l'épingler.",
            "クリップを長押しするとクリップが固定されます。固定を解除したクリップは 1 時間後に削除されます。" to
                "クリップを長押しするとクリップが固定されます。",
            "किसी टेक्स्ट को पिन करने के लिए, उसे दबाकर रखें. अनपिन किए गए टेक्स्ट एक घंटे बाद मिटा दिए जाएंगे." to
                "किसी टेक्स्ट को पिन करने के लिए, उसे दबाकर रखें.",
            "क्लिपलाई पिन गर्न त्यसलाई टच एण्ड होल्ड गर्नुहोस्। पिन नगरिएका क्लिपहरू १ घन्टापछि मेटाइने छन्।" to
                "क्लिपलाई पिन गर्न त्यसलाई टच एण्ड होल्ड गर्नुहोस्।",
            "ለመሰካት አንድ ቅንጥብ ነክተው ይያዙ። ያልተሰኩ ቅንጥቦች ከ1 ሰዓት በኋላ ይሰረዛሉ።" to
                "ለመሰካት አንድ ቅንጥብ ነክተው ይያዙ።",
            "Հպեք հոլովակին և պահեք՝ այն ամրացնելու համար։ Ապամրացված հոլովակները կջնջվեն 1 ժամից:" to
                "Հպեք հոլովակին և պահեք՝ այն ամրացնելու համար։",
            "اسے پن کرنے کے لیے کلپ کو ٹچ کر کے دبائے رکھیں۔ 1 گھنٹے کے بعد پن ہٹائی گئی کلپس کو حذف کر دیا جائے گا۔" to
                "اسے پن کرنے کے لیے کلپ کو ٹچ کر کے دبائے رکھیں۔",
            "ចុច​លើ​ផ្នែកដក​ចេញ​ឱ្យជាប់ ដើម្បីខ្ទាស់​វា។ ផ្នែក​ដកចេញ​ដែលបាន​ដកខ្ទាស់​នឹង​ត្រូវ​លុប បន្ទាប់ពីរយៈពេល​ 1 ម៉ោង។" to
                "ចុច​លើ​ផ្នែកដក​ចេញ​ឱ្យជាប់ ដើម្បីខ្ទាស់​វា។",
            "ဗီဒီယိုအပိုင်းကို ပင်ထိုးရန် ဖိထားပါ။ ပင်ထိုးမထားသည့် ဗီဒီယိုပိုင်းများကို ၁ နာရီကြာလျှင် ဖျက်လိုက်ပါမည်။" to
                "ဗီဒီယိုအပိုင်းကို ပင်ထိုးရန် ဖိထားပါ။",
            "แตะคลิปค้างไว้เพื่อปักหมุด ระบบจะลบคลิปที่เลิกปักหมุดหลังจากผ่านไป 1 ชั่วโมง" to
                "แตะคลิปค้างไว้เพื่อปักหมุด",
        )
        for ((stock, expected) in cases) {
            assertEquals(expected, firstSentence(stock))
            assertEquals(expected, firstSentence(expected))
        }
    }

    @Test
    fun `pin tips are added next to Google's tip without touching other strings`() {
        val stock = """
            <resources>
              <string name="string_0x7f140192">Gboard's clipboard lets you copy text and images, keeping them for 1 hour, to quickly paste many things at once.</string>
              <string name="string_0x7f1401a6">Appuyez de manière prolongée sur un élément pour l\'épingler. Les éléments retirés seront supprimés au bout d\'une heure.</string>
            </resources>
        """.trimIndent()

        val patched = addPinTips(stock, "values-fr")

        val tip = "Appuyez de manière prolongée sur un élément pour l\\'épingler."
        val removed = "Les éléments retirés seront supprimés au bout de"
        assertEquals(
            stock.removeSuffix("</resources>") +
                "    <string name=\"pixelboard_clipboard_pin_tip_short\">$tip</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_24h\">$tip $removed 24 heures.</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_7d\">$tip $removed 7 jours.</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_30d\">$tip $removed 30 jours.</string>\n" +
                "</resources>",
            patched,
        )
        assertEquals(patched, addPinTips(patched, "values-fr"))
        assertEquals("<resources>\n</resources>", addPinTips("<resources>\n</resources>", "values-fr"))
    }

    @Test
    fun `pin tips are found in the escaped text the patcher hands over`() {
        // The patcher writes non-ASCII as Java-style escapes, such as "保" for 保.
        fun escaped(value: String) = value.map { c ->
            if (c.code < 0x80) c.toString() else "\\u%04X".format(c.code)
        }.joinToString("")
        val tip = "クリップを長押しするとクリップが固定されます。固定を解除したクリップは 1 時間後に削除されます。"
        val stock = "<resources>\n  <string name=\"string_0x7f1401a6\">${escaped(tip)}</string>\n</resources>"

        val patched = addPinTips(stock, "values-ja")

        assertEquals(
            stock.removeSuffix("</resources>") +
                "    <string name=\"pixelboard_clipboard_pin_tip_short\">クリップを長押しするとクリップが固定されます。</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_24h\">${tip.replace("1 時間後", "24 時間後")}</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_7d\">${tip.replace("1 時間後", "7 日後")}</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_30d\">${tip.replace("1 時間後", "30 日後")}</string>\n" +
                "</resources>",
            patched,
        )
        assertEquals("a\\\\u0041", decodeUnicodeEscapes("a\\\\u0041"))
    }

    @Test
    fun `a locale without durations, or a reworded tip, gets only the short tip`() {
        val stock = "<resources>\n" +
            "  <string name=\"string_0x7f1401a6\">Touch and hold a clip to pin it. Unpinned clips are cleared hourly.</string>\n" +
            "</resources>"
        val shortOnly = "<resources>\n" +
            "  <string name=\"string_0x7f1401a6\">Touch and hold a clip to pin it. Unpinned clips are cleared hourly.</string>\n" +
            "    <string name=\"pixelboard_clipboard_pin_tip_short\">Touch and hold a clip to pin it.</string>\n" +
            "</resources>"

        assertEquals(shortOnly, addPinTips(stock, "values"))
        assertEquals(shortOnly, addPinTips(stock, "values-xx"))
    }

    @Test
    fun `durations swap only Google's duration, in every script`() {
        val cases = listOf(
            Triple(
                "values",
                "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour.",
                "Touch and hold a clip to pin it. Unpinned clips will be deleted after 24 hours.",
            ),
            Triple(
                "values-ru",
                "Чтобы закрепить фрагмент, нажмите на него и удерживайте. Незакрепленные объекты будут удалены через час.",
                "Чтобы закрепить фрагмент, нажмите на него и удерживайте. Незакрепленные объекты будут удалены через 24 часа.",
            ),
            Triple(
                "values-ja",
                "クリップを長押しするとクリップが固定されます。固定を解除したクリップは 1 時間後に削除されます。",
                "クリップを長押しするとクリップが固定されます。固定を解除したクリップは 24 時間後に削除されます。",
            ),
            Triple(
                "values-it",
                "Tocca una clip e tieni premuto per appuntarla. Le clip non appuntate verranno eliminate dopo un\\'ora.",
                "Tocca una clip e tieni premuto per appuntarla. Le clip non appuntate verranno eliminate dopo 24 ore.",
            ),
        )
        for ((folder, stock, expected) in cases) {
            val durations = PIN_TIP_DURATIONS.getValue(folder)
            assertEquals(expected, withDuration(stock, durations.oneHour, durations.hours24))
        }
        val polish = PIN_TIP_DURATIONS.getValue("values-pl")
        assertEquals(
            "Naciśnij i przytrzymaj element, by go przypiąć. Nieprzypięte elementy są usuwane po 30 dniach.",
            withDuration(
                "Naciśnij i przytrzymaj element, by go przypiąć. Nieprzypięte elementy są usuwane po godzinie.",
                polish.oneHour,
                polish.days30,
            ),
        )
        assertEquals(null, withDuration("1 hour, then 1 hour", "1 hour", "24 hours"))
    }

    @Test
    fun `pin tips match both 18_5_3 and 18_6_3 pin tip resource names`() {
        val stock1863 = """
            <resources>
              <string name="string_0x7f140191">Gboard's clipboard lets you copy text and images, keeping them for 1 hour, to quickly paste many things at once.</string>
              <string name="string_0x7f1401a5">Appuyez de manière prolongée sur un élément pour l\'épingler. Les éléments retirés seront supprimés au bout d\'une heure.</string>
            </resources>
        """.trimIndent()

        val patched = addPinTips(stock1863, "values-fr")
        val tip = "Appuyez de manière prolongée sur un élément pour l\\'épingler."
        val removed = "Les éléments retirés seront supprimés au bout de"
        assertEquals(
            stock1863.removeSuffix("</resources>") +
                "    <string name=\"pixelboard_clipboard_pin_tip_short\">$tip</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_24h\">$tip $removed 24 heures.</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_7d\">$tip $removed 7 jours.</string>\n" +
                "    <string name=\"pixelboard_clipboard_pin_tip_30d\">$tip $removed 30 jours.</string>\n" +
                "</resources>",
            patched,
        )
    }
}
