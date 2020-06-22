package com.nexmo.mobilecallerdemo.connection

class OTAction {
    companion object {
        const val INCOMING_CALL = "com.nexmo.mobilecallerdemo.action.INCOMING_CALL"

        const val SHOW_RINGER = "com.nexmo.mobilecallerdemo.action.RINGER"

        const val LOCAL_ANSWER = "com.nexmo.mobilecallerdemo.action.LOCAL_ANSWER"
        const val REMOTE_ANSWER = "com.nexmo.mobilecallerdemo.action.REMOTE_ANSWER"

        const val LOCAL_REJECT = "com.nexmo.mobilecallerdemo.action.LOCAL_REJECT"
        const val REMOTE_REJECT = "com.nexmo.mobilecallerdemo.action.REMOTE_REJECT"

        const val LOCAL_HANGUP = "com.nexmo.mobilecallerdemo.action.LOCAL_HANGUP"
        const val REMOTE_HANGUP = "com.nexmo.mobilecallerdemo.action.REMOTE_HANGUP"
    }
}