package com.nexmo.mobilecallerdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.connection.OTPhone
import com.nexmo.mobilecallerdemo.persistence.PersistenceService
import com.nexmo.mobilecallerdemo.push.PushService
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {
    companion object {
        const val TAG = "HomeActivity"
    }

    private lateinit var apiService: ApiService
    private lateinit var pushService: PushService
    private lateinit var persistenceService: PersistenceService

    private lateinit var mobileNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        apiService = ApiService()
        pushService = PushService(this, apiService)
        persistenceService = PersistenceService(this)

        mobileNumber = persistenceService.getMobileNumber() ?: "Unknown"


        tv_mobile_number.text = mobileNumber

        btn_call_out.setOnClickListener { callOut(mobileNumber, et_mobile_number.text.toString()) }

        initPush()
    }

    private fun initPush() {
        pushService.init(mobileNumber)
    }

    private fun callOut(from: String, to: String) {
        val runnable = Runnable {
            val response = apiService.call(from, to)
            Log.d(TAG, response)
            val responseObject = JSONObject(response)

            val apiKey = responseObject.getString("apiKey")
            val sessionId = responseObject.getString("sessionId")
            val token = responseObject.getString("token")

            val uiRunnable = Runnable {
                val newIntent = Intent()
                newIntent.setClass(this, CallActivity::class.java)

                newIntent.putExtra(RingActivity.FROM, to)
                newIntent.putExtra(RingActivity.API_KEY, apiKey)
                newIntent.putExtra(RingActivity.SESSION_ID, sessionId)
                newIntent.putExtra(RingActivity.TOKEN, token)

                startActivity(newIntent)
            }
            runOnUiThread(uiRunnable)
        }
        val thread = Thread(runnable)
        thread.start()
    }
}
