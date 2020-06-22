package com.nexmo.mobilecallerdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.connection.OTPhone
import com.nexmo.mobilecallerdemo.persistence.PersistenceService
import com.nexmo.mobilecallerdemo.push.PushService
import kotlinx.android.synthetic.main.activity_home.*

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

    private fun initPhone() {
        otPhone.init(mobileNumber)
    }

    private fun initPush() {
        pushService.init(mobileNumber)
    }

    private fun callOut(to: String) {
        otPhone.callOut(to)
    }
}
