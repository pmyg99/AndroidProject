package com.example.petowner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PetAdapter(private val pets: List<Pet>) :
    RecyclerView.Adapter<PetAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_pet_name)
        val tvInfo: TextView = itemView.findViewById(R.id.tv_pet_info)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_boarding_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pet = pets[position]
        holder.tvName.text = pet.name
        holder.tvInfo.text = "${pet.species} | ${pet.age}岁 | ${pet.gender}"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val activeRecords = pet.activeRecords
        val completedRecords = pet.completedRecords

        val sb = StringBuilder()
        if (activeRecords.isNotEmpty()) {
            sb.append("正在寄养: ")
            val record = activeRecords.first()
            val startStr = dateFormat.format(Date(record.startTime))
            val endStr = dateFormat.format(Date(record.endTime))
            sb.append("$startStr 至 $endStr")
        } else if (completedRecords.isNotEmpty()) {
            sb.append("寄养完成: ")
            val record = completedRecords.first()
            val startStr = dateFormat.format(Date(record.startTime))
            val endStr = dateFormat.format(Date(record.endTime))
            sb.append("$startStr 至 $endStr")
        }
        holder.tvStatus.text = sb.toString()
    }

    override fun getItemCount(): Int = pets.size
}