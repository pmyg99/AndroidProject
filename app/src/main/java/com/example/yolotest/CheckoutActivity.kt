package com.example.yolotest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CheckoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }
}