package com.nexmo.mobilecallerdemo

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.nexmo.mobilecallerdemo.persistence.PersistenceService
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val RC_PERMISSIONS = 1234
    }

    private lateinit var persistenceService: PersistenceService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        persistenceService = PersistenceService(this)

        requestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(RC_PERMISSIONS)
    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
        )

        if (EasyPermissions.hasPermissions(this, *perms)) {
            Log.d(TAG, "Permission already given")

            val mobileNumber = persistenceService.getMobileNumber()
            if (mobileNumber.isNullOrEmpty()) {
                Log.d(TAG, "Not logged in")
                val intent = Intent()
                intent.setClass(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.d(TAG, "Logged in")
                val intent = Intent()
                intent.setClass(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            Log.d(TAG, "Requesting Permission")
            EasyPermissions.requestPermissions(
                this,
                "This app needs the following permissions to function",
                RC_PERMISSIONS,
                *perms
            )
        }
    }

}
