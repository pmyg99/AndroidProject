package com.example.yolotest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PetSummaryAdapter(
    private var pets: List<PetSummary>,
    private val onItemClick: (PetSummary) -> Unit
) : RecyclerView.Adapter<PetSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_pet_name)
        val tvInfo: TextView = itemView.findViewById(R.id.tv_pet_info)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pet = pets[position]
        holder.tvName.text = pet.petName
        holder.tvInfo.text = "${pet.species} | ${pet.age}岁 | ${pet.gender} | 主人：${pet.ownerName}"
        holder.tvStatus.text = if (pet.isBoarding) "● 正在寄养" else "✓ 寄养完成"
        if (pet.isBoarding) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
        }
        holder.itemView.setOnClickListener { onItemClick(pet) }
    }

    override fun getItemCount(): Int = pets.size

    fun updateData(newPets: List<PetSummary>) {
        pets = newPets
        notifyDataSetChanged()
    }
}