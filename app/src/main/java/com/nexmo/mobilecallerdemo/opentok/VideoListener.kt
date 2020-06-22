package com.nexmo.mobilecallerdemo.opentok

interface VideoListener {
    fun onCallStart() {}
    fun onCallEnd() {}

    fun onLocalJoin() {}
    fun onRemoteJoin() {}

    fun onLocalHangup() {}
    fun onRemoteHangup() {}
}