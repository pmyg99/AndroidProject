package com.example.yolotest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PetForDailyCareAdapter(
    private var pets: List<Pet>,
    private val onItemClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetForDailyCareAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_pet_name)
        val tvInfo: TextView = itemView.findViewById(R.id.tv_pet_info)
        val tvOwner: TextView = itemView.findViewById(R.id.tv_owner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pet = pets[position]
        holder.tvName.text = pet.name
        holder.tvInfo.text = "${pet.species} | ${pet.age}岁 | ${pet.gender}"
        holder.tvOwner.text = "主人：${pet.ownerName}"
        holder.itemView.setOnClickListener { onItemClick(pet) }
    }

    override fun getItemCount(): Int = pets.size

    fun updateData(newPets: List<Pet>) {
        pets = newPets
        notifyDataSetChanged()
    }
}