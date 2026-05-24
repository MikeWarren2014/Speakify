package com.mikewarren.speakify.data.constants

object PackageNames {
    const val GoogleVoice = "com.google.android.apps.googlevoice"
    const val GoogleCalendar = "com.google.android.calendar"

    const val GEOH = "com.gogeoh.geoh"
    val PhoneAppList = listOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.android.phone",
    )

    val MessagingAppList = listOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.samsung.android.messaging",
    )

    val FacebookMessengerAppList = listOf(
        "com.facebook.orca",
        "com.facebook.mlite",
    )

    val CommunicationAppList = PhoneAppList + MessagingAppList + FacebookMessengerAppList + listOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "org.thoughtcrime.securesms", // Signal
        "com.discord",
        "com.viber.voip",
        "com.skype.raider",
        "com.google.android.talk", // Hangouts/Chat
    )

    val BusinessProductivityAppList = listOf(
        GoogleCalendar,
        "com.google.android.gm", // Gmail
        "com.microsoft.office.outlook",
        "com.slack",
        "com.microsoft.teams",
        "com.google.android.apps.tasks",
        "com.todoist",
        "com.anydo",
        "com.asana.app",
        "com.trello",
    )

    val ShoppingAppList = listOf(
        "com.amazon.mShop.android.shopping",
        "com.ubercab.eats",
        "com.dd.doordash",
        "com.instacart.client",
        "com.ebay.mobile",
        "com.walmart.android",
    )
}
