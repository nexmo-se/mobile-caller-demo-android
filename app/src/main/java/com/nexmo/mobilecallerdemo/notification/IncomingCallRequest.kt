package com.nexmo.mobilecallerdemo.notification

data class IncomingCallRequest(
    val from: String,
    val apiKey: String,
    val sessionId: String,
    val token: String
)
