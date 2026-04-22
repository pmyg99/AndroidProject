package com.example.yolotest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {

    private lateinit var petServer: SimplePetServer
    private val SERVER_PORT = 8080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 请求网络权限（Android 9+ 需要明文流量许可，已在 manifest 中配置）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
            }
        }

        // 启动 HTTP 服务器
        petServer = SimplePetServer(this, SERVER_PORT)
        try {
            petServer.start()
            Toast.makeText(this, "服务器已启动，端口: $SERVER_PORT", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "服务器启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btn_boarding).setOnClickListener {
            startActivity(android.content.Intent(this, BoardingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_daily_care).setOnClickListener {
            startActivity(android.content.Intent(this, DailyCareMainActivity::class.java))
        }
        findViewById<Button>(R.id.btn_checkout).setOnClickListener {
            // 出托核对功能暂未实现
            Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_record).setOnClickListener {
            startActivity(android.content.Intent(this, RecordActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        petServer.stop()
    }
}