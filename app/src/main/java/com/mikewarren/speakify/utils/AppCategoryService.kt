package com.mikewarren.speakify.utils

// Source - https://stackoverflow.com/a/52484674
// Posted by SebastianJeg, modified by community. See post 'Timeline' for change history
// Retrieved 2026-05-22, License - CC BY-SA 4.0

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.util.Locale

class AppCategoryService {
    companion object {
        private const val APP_URL = "https://play.google.com/store/apps/details?id="
        private const val CATEGORY_STRING = "category/"
    }

    suspend fun fetchCategory(packageName: String): RawAppCategory {
        val url = "$APP_URL$packageName&hl=en" //https://play.google.com/store/apps/details?id=com.example.app&hl=en
        val categoryRaw = parseAndExtractCategory(url) ?: return RawAppCategory.OTHER
        return RawAppCategory.fromCategoryName(categoryRaw)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun parseAndExtractCategory(url: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val genreLink = Jsoup.connect(url).get()
                .selectXpath("//div[@itemprop='genre']/a")

            if (genreLink.isNullOrEmpty())
                return@withContext null

            val href = genreLink.attr("abs:href")

            if (href != null && href.length > 4 && href.contains(CATEGORY_STRING)) {
                getCategoryTypeByHref(href)
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun getCategoryTypeByHref(href: String) = href.substring(href.indexOf(CATEGORY_STRING) + CATEGORY_STRING.length, href.length)
}

// Source - https://stackoverflow.com/a/52484674
// Posted by SebastianJeg, modified by community. See post 'Timeline' for change history
// Retrieved 2026-05-22, License - CC BY-SA 4.0

// Note: Enum name matches API value and should not be changed
enum class RawAppCategory {
    OTHER,
    ART_AND_DESIGN,
    AUTO_AND_VEHICLES,
    BEAUTY,
    BOOKS_AND_REFERENCE,
    BUSINESS,
    COMICS,
    COMMUNICATION,
    DATING,
    EDUCATION,
    ENTERTAINMENT,
    EVENTS,
    FINANCE,
    FOOD_AND_DRINK,
    HEALTH_AND_FITNESS,
    HOUSE_AND_HOME,
    LIBRARIES_AND_DEMO,
    LIFESTYLE,
    MAPS_AND_NAVIGATION,
    MEDICAL,
    MUSIC_AND_AUDIO,
    NEWS_AND_MAGAZINES,
    PARENTING,
    PERSONALIZATION,
    PHOTOGRAPHY,
    PRODUCTIVITY,
    SHOPPING,
    SOCIAL,
    SPORTS,
    TOOLS,
    TRAVEL_AND_LOCAL,
    VIDEO_PLAYERS,
    WEATHER,
    GAMES;

    companion object {
        private val map = entries.associateBy(RawAppCategory::name)
        private const val CATEGORY_GAME_STRING = "GAME_" // All games start with this prefix

        fun fromCategoryName(name: String): RawAppCategory {
            if (name.contains(CATEGORY_GAME_STRING)) return GAMES
            return map[name.uppercase(Locale.ROOT)] ?: OTHER
        }
    }
}
