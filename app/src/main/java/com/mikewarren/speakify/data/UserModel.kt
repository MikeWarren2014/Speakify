package com.mikewarren.speakify.data

data class UserModel(val email: String,
                     val password: String,
                     val firstName: String,
                     val lastName: String,
    ) {
    constructor() : this("", "", "", "")
}
