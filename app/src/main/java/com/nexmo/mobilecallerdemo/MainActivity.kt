package com.nexmo.mobilecallerdemo

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val RC_PERMISSIONS = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            Manifest.permission.CALL_PHONE
        )

        if (EasyPermissions.hasPermissions(this, *perms)) {
            Log.d(TAG, "Permission already given")
            val intent = Intent()
            intent.setClass(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
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
