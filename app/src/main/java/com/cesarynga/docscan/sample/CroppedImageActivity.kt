package com.cesarynga.docscan.sample

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.sample.databinding.ActivityCroppedImageBinding

class CroppedImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCroppedImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCroppedImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photoUri: Uri? = intent.extras?.getParcelable("croppedUri")

        if (photoUri != null) {
            binding.imageView.setImageURI(photoUri)
        }
    }
}