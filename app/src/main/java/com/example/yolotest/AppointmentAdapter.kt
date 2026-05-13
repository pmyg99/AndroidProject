package com.example.yolotest

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AppointmentAdapter(
    private val appointments: List<Appointment>,
    private val onItemClick: (Appointment) -> Unit,
    private val onDeleteClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
        holder.itemView.setOnClickListener { onItemClick(appointment) }
        holder.btnDelete.setOnClickListener { onDeleteClick(appointment) }
    }

    override fun getItemCount() = appointments.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvPetName: TextView = itemView.findViewById(R.id.tv_pet_name)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(appointment: Appointment) {
            tvPetName.text = appointment.petName
            val start = dateFormat.format(Date(appointment.startTime))
            val end = appointment.endTime?.let { dateFormat.format(Date(it)) } ?: "未指定"
            tvTime.text = "预约时间: $start 至 $end"
        }
    }
}