package com.mikewarren.speakify.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactModel(val id: Long, val name:String, val phoneNumber: String) : Parcelable
