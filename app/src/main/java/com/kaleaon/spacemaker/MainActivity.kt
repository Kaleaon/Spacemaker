package com.kaleaon.spacemaker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private lateinit var startScanButton: MaterialButton
    private lateinit var uploadVideoButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startScanButton = findViewById(R.id.startScanButton)
        uploadVideoButton = findViewById(R.id.uploadVideoButton)
        
        startScanButton.setOnClickListener {
            if (checkARCoreSupport() && checkCameraPermission()) {
                startARScan()
            }
        }
        
        uploadVideoButton.setOnClickListener {
            startVideoUpload()
        }
    }

    private fun checkARCoreSupport(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to check availability
            return false
        }
        
        return if (availability.isSupported) {
            true
        } else {
            Toast.makeText(this, R.string.ar_not_supported, Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkARCoreSupport()) {
                        startARScan()
                    }
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startARScan() {
        val intent = Intent(this, ARScanActivity::class.java)
        startActivity(intent)
    }
    
    private fun startVideoUpload() {
        val intent = Intent(this, VideoUploadActivity::class.java)
        startActivity(intent)
    }
}
