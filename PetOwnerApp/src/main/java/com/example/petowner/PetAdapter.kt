package com.example.petowner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PetAdapter(
    private val pets: List<Pet>,
    private val onItemClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        holder.bind(pet)
        holder.itemView.setOnClickListener { onItemClick(pet) }

        // 高亮：正在寄养（绿色）> 有待确认预约（黄色）> 普通（白色/透明）
        when {
            pet.activeRecords.isNotEmpty() -> {
                holder.itemView.setBackgroundColor(0xCC66BB66.toInt()) // 绿色
            }
            pet.hasPendingAppointment -> {
                holder.itemView.setBackgroundColor(0xCCFFFF66.toInt()) // 黄色
            }
            else -> {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    override fun getItemCount() = pets.size

    class PetViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_pet_name)
        private val tvSpecies: TextView = itemView.findViewById(R.id.tv_pet_species)
        private val tvAge: TextView = itemView.findViewById(R.id.tv_pet_age)
        private val tvGender: TextView = itemView.findViewById(R.id.tv_pet_gender)

        fun bind(pet: Pet) {
            tvName.text = pet.name
            tvSpecies.text = pet.species
            tvAge.text = "${pet.age}岁"
            tvGender.text = pet.gender
        }
    }
}