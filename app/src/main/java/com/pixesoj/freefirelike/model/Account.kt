package com.pixesoj.freefirelike.model

data class Account(
    var uid: String,
    var username: String,
    var region: String,
    var avatarResId: Int = 0,
    var avatarId: String = "",
    var likes: Int = 0,
    var status: String = "SUCCESS"
)