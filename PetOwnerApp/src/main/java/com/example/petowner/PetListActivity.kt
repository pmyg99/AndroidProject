package com.example.petowner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PetListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvOwnerInfo: TextView
    private lateinit var btnAppointment: Button
    private lateinit var btnMyAppointments: Button
    private var pets: List<Pet>? = null
    private var ownerId: Long = 0
    private var ownerName: String = ""
    private var ownerPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_list)

        ownerName = intent.getStringExtra("owner_name") ?: ""
        ownerPhone = intent.getStringExtra("owner_phone") ?: ""
        ownerId = intent.getLongExtra("owner_id", 0)

        tvOwnerInfo = findViewById(R.id.tv_owner_info)
        tvOwnerInfo.text = "欢迎，$ownerName"

        recyclerView = findViewById(R.id.rv_pets)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnAppointment = findViewById(R.id.btn_appointment)
        btnMyAppointments = findViewById(R.id.btn_my_appointments)

        loadPets()
        setupButtons()
    }

    private fun loadPets() {
        ApiClient.getPets(ownerName, ownerPhone) { petList ->
            runOnUiThread {
                if (petList != null) {
                    this.pets = petList
                    ApiClient.getMyAppointments(ownerId) { appointments ->
                        val pendingPetIds = appointments?.filter { it.status == "pending" }?.map { it.petId } ?: emptyList()
                        val updatedPets = petList.map { pet ->
                            pet.copy(hasPendingAppointment = pendingPetIds.contains(pet.id))
                        }
                        val adapter = PetAdapter(updatedPets) { pet ->
                            val intent = Intent(this, PetDetailActivity::class.java)
                            intent.putExtra("pet", pet)
                            startActivity(intent)
                        }
                        recyclerView.adapter = adapter
                    }
                } else {
                    Toast.makeText(this, "获取数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        btnAppointment.setOnClickListener {
            val intent = Intent(this, CreateAppointmentActivity::class.java)
            intent.putParcelableArrayListExtra("pets", ArrayList(pets ?: emptyList()))
            intent.putExtra("owner_id", ownerId)
            startActivity(intent)
        }
        btnMyAppointments.setOnClickListener {
            val intent = Intent(this, MyAppointmentsActivity::class.java)
            intent.putExtra("owner_id", ownerId)
            startActivity(intent)
        }
    }

    // 按返回键时直接退出应用（或回到登录页，可注释掉）
    override fun onBackPressed() {
        // 回到登录页（清空任务栈）
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
        // 如果不想回到登录页而是退出，可以调用 finishAffinity()
    }
}