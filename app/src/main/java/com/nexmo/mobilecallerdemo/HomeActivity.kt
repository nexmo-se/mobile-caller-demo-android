package com.nexmo.mobilecallerdemo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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
    private lateinit var otPhone: OTPhone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        otPhone = OTPhone(this)
        apiService = ApiService()
        pushService = PushService(this, apiService)
        persistenceService = PersistenceService(this)

        mobileNumber = persistenceService.getMobileNumber() ?: "Unknown"


        tv_mobile_number.text = mobileNumber
        btn_call_out.setOnClickListener { callOut(et_mobile_number.text.toString()) }

        initPush()
        initPhone()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initPhone() {
        otPhone.init(mobileNumber)
    }

    private fun initPush() {
        pushService.init(mobileNumber)
    }

    private fun logout() {
        val runnable = Runnable {
            persistenceService.setMobileNumber(null)
            apiService.unregisterToken(mobileNumber)

            runOnUiThread {
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent()
            intent.setClass(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        val thread = Thread(runnable)
        thread.start()
    }

    private fun callOut(to: String) {
        if (to.isEmpty()) {
            Toast.makeText(this, "Invalid Mobile Number", Toast.LENGTH_SHORT).show()
            return
        }

        if (to == mobileNumber) {
            Toast.makeText(this, "Calling self is not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val runnable = Runnable {
            val responseBody = apiService.createSession()
            val jsonBody = JSONObject(responseBody)

            val apiKey = jsonBody.getString("apiKey")
            val sessionId = jsonBody.getString("sessionId")
            val token = jsonBody.getString("token")

            if (apiKey.isNullOrEmpty() || sessionId.isNullOrEmpty() || token.isNullOrEmpty()){
                runOnUiThread {
                    Toast.makeText(this, "Failed to make call on server", Toast.LENGTH_SHORT)
                        .show()
                }
                return@Runnable
            }

            otPhone.callOut(to, apiKey, sessionId, token)
        }
        val thread = Thread(runnable)
        thread.start()
    }
}
