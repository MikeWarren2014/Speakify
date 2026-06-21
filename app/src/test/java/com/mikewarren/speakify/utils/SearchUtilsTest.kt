package com.mikewarren.speakify.utils

import org.junit.Test
import kotlin.test.assertNotEquals

class SearchUtilsTest {
    @Test
    fun `GetEmojiPosition should return non-negative for strings with emoji characters`() {
        val stringList = listOf(
            "✨",
            "\uD83D\uDD17 Sent a link to you",
            "\uD83D\uDCF7 Sent a photo to you",
        )

        stringList.forEach { stringToSearch ->
            assertNotEquals(-1,
                SearchUtils.GetEmojiPosition(stringToSearch),
                "SearchUtils.GetEmojiPosition should return non-negative for string: $stringToSearch")
        }
    }
}