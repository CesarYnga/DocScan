package com.cesarynga.docscan.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesarynga.docscan.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            btnStaticScanner.setOnClickListener {
                startActivity(Intent(this@MainActivity, ScannerImageActivity::class.java))
            }
            btnRealTimeScanner.setOnClickListener {
                startActivity(Intent(this@MainActivity, ScannerCameraActivity::class.java))
            }
        }
    }
}