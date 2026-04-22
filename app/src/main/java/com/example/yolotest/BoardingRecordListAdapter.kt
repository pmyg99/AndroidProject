package com.example.yolotest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardingRecordListAdapter(
    private var records: List<BoardingRecord>,
    private val onItemClick: (BoardingRecord) -> Unit
) : RecyclerView.Adapter<BoardingRecordListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvProcess: TextView = itemView.findViewById(R.id.tv_process)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_boarding_record_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val startStr = dateFormat.format(Date(record.startTime))
        val endStr = dateFormat.format(Date(record.endTime))
        holder.tvTime.text = "$startStr 至 $endStr"
        holder.tvStatus.text = if (record.isActive) "正在寄养" else "已完成"
        holder.tvProcess.text = record.process
        holder.itemView.setOnClickListener { onItemClick(record) }
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<BoardingRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}