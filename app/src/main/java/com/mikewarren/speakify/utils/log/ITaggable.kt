package com.mikewarren.speakify.utils.log

interface ITaggable {
    val TAG: String
        get() = this.javaClass.name
}