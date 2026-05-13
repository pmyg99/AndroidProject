package com.example.petowner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etName = findViewById(R.id.et_name)
        etPhone = findViewById(R.id.et_phone)
        btnLogin = findViewById(R.id.btn_login)

        btnLogin.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                // 直接跳转，实际应用应验证主人信息
                val intent = Intent(this, PetListActivity::class.java)
                intent.putExtra("owner_name", name)
                intent.putExtra("owner_phone", phone)
                startActivity(intent)
            } else {
                Toast.makeText(this, "请输入姓名和联系方式", Toast.LENGTH_SHORT).show()
            }
        }
    }
}