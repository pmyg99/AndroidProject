package com.example.petowner

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL

class PetListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PetAdapter
    private lateinit var tvOwnerInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_list)

        val ownerName = intent.getStringExtra("owner_name") ?: ""
        val ownerPhone = intent.getStringExtra("owner_phone") ?: ""

        tvOwnerInfo = findViewById(R.id.tv_owner_info)
        tvOwnerInfo.text = "欢迎，$ownerName"

        recyclerView = findViewById(R.id.rv_pets)
        recyclerView.layoutManager = LinearLayoutManager(this)
        Log.d("PetListActivity", "Calling getPets with name=$ownerName, phone=$ownerPhone")
        // 请求数据
        ApiClient.getPets(ownerName, ownerPhone) { pets ->
            runOnUiThread {
                if (pets != null) {
                    adapter = PetAdapter(pets)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(this, "获取数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Thread {
            try {
                Log.d("Test", "开始测试请求")
                val url = URL("http://127.0.0.1:8080/pets")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val body = "{\"name\":\"张三\",\"phone\":\"13800001111\"}"
                conn.outputStream.write(body.toByteArray())
                conn.outputStream.flush()
                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()
                Log.d("Test", "Response code: $responseCode, body: $response")
            } catch (e: Exception) {
                Log.e("Test", "Error", e)
            }
        }.start()
    }
}