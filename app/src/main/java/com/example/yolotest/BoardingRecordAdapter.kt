package com.example.yolotest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardingRecordAdapter(
    private var records: List<BoardingRecord>,
    private val onItemClick: (BoardingRecord) -> Unit
) : RecyclerView.Adapter<BoardingRecordAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIndex: TextView = itemView.findViewById(R.id.tv_index)
        val tvPetName: TextView = itemView.findViewById(R.id.tv_pet_name)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvProcess: TextView = itemView.findViewById(R.id.tv_process)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_boarding_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        // 显示序号（如“第1次寄养”）
        val indexText = when (record.index) {
            1 -> "第一次寄养"
            2 -> "第二次寄养"
            3 -> "第三次寄养"
            else -> "第${record.index}次寄养"
        }
        holder.tvIndex.text = indexText
        holder.tvPetName.text = record.petName
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val startStr = dateFormat.format(Date(record.startTime))
        val endStr = dateFormat.format(Date(record.endTime))
        holder.tvTime.text = "$startStr 至 $endStr"
        holder.tvProcess.text = record.process

        // 高亮当前正在进行的寄养记录
        if (record.isActive) {
            holder.itemView.setBackgroundColor(Color.parseColor("#AA4CAF50"))  // 半透明绿色
            holder.tvIndex.setTextColor(Color.WHITE)
            holder.tvPetName.setTextColor(Color.WHITE)
            holder.tvTime.setTextColor(Color.WHITE)
            holder.tvProcess.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.tvIndex.setTextColor(Color.BLACK)
            holder.tvPetName.setTextColor(Color.BLACK)
            holder.tvTime.setTextColor(Color.DKGRAY)
            holder.tvProcess.setTextColor(Color.DKGRAY)
        }

        holder.itemView.setOnClickListener { onItemClick(record) }
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<BoardingRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}