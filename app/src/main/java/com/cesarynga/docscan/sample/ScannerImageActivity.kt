package com.cesarynga.docscan.sample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.image.ScannerImageView
import com.cesarynga.docscan.rotateWithExif
import com.cesarynga.docscan.sample.databinding.ActivityScannerImageBinding
import java.lang.Exception

class ScannerImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerImageBinding

    private val sampleImage = R.drawable.sample_mutlitple_shapes

    private var scanResult: ScannerImageView.ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photoUri: Uri? = intent.extras?.getParcelable("imageUri")
        if (photoUri != null) {
            contentResolver.openInputStream(photoUri)?.let {
                var bitmap = BitmapFactory.decodeStream(it)
                if (photoUri.path != null)
                    bitmap = bitmap.rotateWithExif(photoUri.path!!)
                scanImage(bitmap)
            }
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, sampleImage)
            scanImage(bitmap)
        }
    }

    private fun scanImage(bitmap: Bitmap) {
        with(binding) {
            scannerImageView.scan(
                bitmap,
                callback = object : ScannerImageView.ScannerImageCallback {
                    override fun onScanSuccess(scanResult: ScannerImageView.ScanResult) {
                        this@ScannerImageActivity.scanResult = scanResult
                    }

                    override fun onScanError(e: Exception) {
                        Toast.makeText(this@ScannerImageActivity, e.message, Toast.LENGTH_SHORT)
                            .show()
                        this@ScannerImageActivity.scanResult = null
                    }
                })
        }
    }

    private fun crop() {
        val croppedUri = scanResult?.crop()
        if (croppedUri != null) {
            val intent = Intent(this, CroppedImageActivity::class.java)
            intent.putExtra("croppedUri", croppedUri)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.crop, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        crop()
        return true
    }
}