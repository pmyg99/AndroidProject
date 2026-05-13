package com.example.petowner

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var adapter: AppointmentAdapter
    private val appointments = mutableListOf<Appointment>()
    private var ownerId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        ownerId = intent.getLongExtra("owner_id", 0)
        rvAppointments = findViewById(R.id.rv_appointments)
        rvAppointments.layoutManager = LinearLayoutManager(this)

        adapter = AppointmentAdapter(appointments) { appointment ->
            if (appointment.status == "pending") {
                AlertDialog.Builder(this)
                    .setTitle("取消预约")
                    .setMessage("确定取消该预约吗？")
                    .setPositiveButton("确定") { _, _ ->
                        ApiClient.cancelAppointment(appointment.id) { success ->
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
                                    loadAppointments()
                                } else {
                                    Toast.makeText(this, "取消失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
        rvAppointments.adapter = adapter

        findViewById<Button>(R.id.btn_refresh).setOnClickListener { loadAppointments() }
        loadAppointments()
    }

    private fun loadAppointments() {
        ApiClient.getMyAppointments(ownerId) { list ->
            runOnUiThread {
                appointments.clear()
                if (list != null) appointments.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }
}