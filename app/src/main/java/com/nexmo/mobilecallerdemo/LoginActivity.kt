package com.nexmo.mobilecallerdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.persistence.PersistenceService
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    companion object {
        const val TAG = "LoginActivity"
    }

    private lateinit var apiService: ApiService
    private lateinit var persistenceService: PersistenceService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiService = ApiService()
        persistenceService = PersistenceService(this)


        btn_login.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val mobileNumber = et_mobile_number.text.toString()


        if (mobileNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid Mobile Number", Toast.LENGTH_SHORT).show()
            return
        }

        persistenceService.setMobileNumber(mobileNumber)

        Log.d(TAG, "Logging in")
        val intent = Intent()
        intent.setClass(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
