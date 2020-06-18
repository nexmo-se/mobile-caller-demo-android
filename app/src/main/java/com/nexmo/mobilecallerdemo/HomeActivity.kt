package com.nexmo.mobilecallerdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.nexmo.mobilecallerdemo.connection.OTPhone
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {
    companion object {
        const val TAG = "HomeActivity"
    }
    private lateinit var otPhone: OTPhone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        otPhone = OTPhone(this)

        btn_register.setOnClickListener {
            otPhone.register("6583206274")
        }

        btn_call_out.setOnClickListener {
            otPhone.callOut("6512341234")
        }

        btn_call_in.setOnClickListener {
            otPhone.callIn("6523452345")
        }

        btn_join.setOnClickListener {
            val intent = Intent()
            intent.setClass(this, CallActivity::class.java)
            startActivity(intent)
        }
    }
}
