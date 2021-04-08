package com.cesarynga.docscan.sample

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.init
import com.cesarynga.docscan.sample.databinding.ActivityScannerImageBinding

class ScannerImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerImageBinding

    private val sampleImage = R.drawable.sample_document

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        scanImage()
    }

    private fun scanImage() {
        with(binding) {
            val bitmap = BitmapFactory.decodeResource(resources, sampleImage)
            scannerImageView.setBitmap(bitmap)
        }
    }
}