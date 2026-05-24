package com.mikewarren.speakify.services

import com.mikewarren.speakify.utils.RawAppCategory
import com.mikewarren.speakify.utils.AppCategoryService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AppCategoryServiceTest {

    @Test
    fun `should work`() = runTest {
        assertEquals(RawAppCategory.PRODUCTIVITY,
            AppCategoryService().fetchCategory("com.trello"))
    }


}