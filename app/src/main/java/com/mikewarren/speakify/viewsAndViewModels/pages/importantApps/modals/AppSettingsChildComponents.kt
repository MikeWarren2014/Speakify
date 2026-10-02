package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AdditionalSettingsComponent(
    val packageName: String = "",
    val listName: String = "",
    val appTypes: Array<String> = [],
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class NotificationListComponent(
    val packageName: String = "",
    val listName: String = "",
    val appTypes: Array<String> = [],
)