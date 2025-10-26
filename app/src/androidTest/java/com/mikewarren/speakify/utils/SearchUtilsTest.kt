package com.mikewarren.speakify.utils

import org.junit.Test


class SearchUtilsTest {
    @Test
    fun testIsInPhoneNumberList() {
        assert(SearchUtils.IsInPhoneNumberList(listOf("(317) 555-1234", "(317) 555-4578"),
            "+13175554578"))
    }


}