package com.mikewarren.speakify.viewsAndViewModels.widgets

interface IStringConverter<T> {
    fun toSourceString(value: T): String
    fun toViewString(value: T): String
}