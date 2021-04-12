package com.cesarynga.docscan.sample

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.DocScan
import com.cesarynga.docscan.sample.databinding.ActivityDewarpBinding

class DewarpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDewarpBinding
    private val sampleImage = R.drawable.sample_mutlitple_shapes
    private val docScan = DocScan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDewarpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dewarp()
    }

    private fun dewarp() {
        with(binding) {
            val bitmap = BitmapFactory.decodeResource(resources, sampleImage)

            val corners = docScan.scan(bitmap)

            if (corners.size == 4) {
                val cropped = docScan.crop(bitmap, corners)

                imageView.setImageBitmap(cropped)
            }
        }
    }
}