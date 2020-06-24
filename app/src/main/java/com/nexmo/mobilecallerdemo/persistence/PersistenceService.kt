package com.nexmo.mobilecallerdemo.persistence

import android.content.Context

class PersistenceService(private val context: Context) {
    companion object {
        const val TAG = "PersistenceService"

        const val SHARED_PREFERENCES_NAME = "mobilecallerdemo"
        const val MOBILE_NUMBER_KEY = "mobileNumber"
    }
    fun setMobileNumber(mobileNumber: String?) {
        val sharedPreferences = context.getSharedPreferences(
            SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE
        )

        val editor = sharedPreferences.edit()
        editor.putString(MOBILE_NUMBER_KEY, mobileNumber)
        editor.apply()
    }

    fun getMobileNumber(): String? {
        val sharedPreferences = context.getSharedPreferences(
            SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE
        )

        return sharedPreferences.getString(MOBILE_NUMBER_KEY, null)
    }
}