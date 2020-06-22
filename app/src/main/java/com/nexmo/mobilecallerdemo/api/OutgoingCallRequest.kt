package com.nexmo.mobilecallerdemo.api

data class OutgoingCallRequest(
    val to: String,
    val apiKey: String,
    val sessionId: String,
    val token: String
)
