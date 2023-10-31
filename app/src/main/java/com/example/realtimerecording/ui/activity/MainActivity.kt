package com.example.realtimerecording.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.realtimerecording.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.sign).setOnClickListener {
            startActivity(Intent(this@MainActivity, SignatureActivity::class.java))
        }
    }
}