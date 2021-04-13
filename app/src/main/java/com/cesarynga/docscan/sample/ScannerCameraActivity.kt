package com.cesarynga.docscan.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cesarynga.docscan.sample.databinding.ActivityScannerCameraBinding

class ScannerCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestCameraPermission()
    }

    private fun scan() {
        with(binding) {
            btnTakePicture.setOnClickListener {
                scannerCameraView.takePicture { uri ->
                    uri?.let {
                        Toast.makeText(this@ScannerCameraActivity, "Uri: ${uri.path}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ScannerCameraActivity, ScannerImageActivity::class.java)
                        intent.putExtra("imageUri", it)
                        startActivity(intent)
                    } ?: Toast.makeText(this@ScannerCameraActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestCameraPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    scan()
                } else {
                    finish()
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                scan()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}