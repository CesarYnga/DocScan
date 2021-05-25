package com.cesarynga.docscan.sample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.image.ImageScannerView
import com.cesarynga.docscan.rotateWithExif
import com.cesarynga.docscan.sample.databinding.ActivityScannerImageBinding
import java.lang.Exception

class ScannerImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerImageBinding

    private var sampleImage = R.drawable.sample_check

    private var scanResult: ImageScannerView.ScanResult? = null

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
            Base64.NO_WRAP
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, sampleImage)
            scanImage(bitmap)
        }
    }

    private fun scanImage(bitmap: Bitmap) {
        with(binding) {
            scannerImageView.scan(
                bitmap,
                callback = object : ImageScannerView.ScannerImageCallback {
                    override fun onScanSuccess(scanResult: ImageScannerView.ScanResult) {
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
        menuInflater.inflate(R.menu.menu_scanner_image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_crop) {
            crop()
        } else {
            sampleImage = when (item.itemId) {
                R.id.action_sample_check -> R.drawable.sample_check
                R.id.action_sample_document -> R.drawable.sample_document
                R.id.action_sample_multiple_shapes -> R.drawable.sample_mutlitple_shapes
                else -> R.drawable.sample_check
            }
            item.isChecked = true
            val bitmap = BitmapFactory.decodeResource(resources, sampleImage)
            scanImage(bitmap)
        }

        return true
    }
}