package com.example.petowner

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardingRecordAdapter(
    private val records: List<BoardingRecord>,
    private val onItemClick: (BoardingRecord) -> Unit
) : RecyclerView.Adapter<BoardingRecordAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_boarding_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
        // 关键：设置点击监听
        holder.itemView.setOnClickListener { onItemClick(record) }

        // 绿色高亮：当前寄养设置浅绿色背景
        val cardView = holder.itemView as? CardView
        if (record.isActive) {
            cardView?.setCardBackgroundColor(0xFFCCFFCC.toInt()) // 浅绿色
        } else {
            cardView?.setCardBackgroundColor(android.graphics.Color.WHITE)
        }
    }

    override fun getItemCount() = records.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime = itemView.findViewById<TextView>(R.id.tv_record_time)
        private val tvProcess = itemView.findViewById<TextView>(R.id.tv_record_process)

        fun bind(record: BoardingRecord) {
            val start = dateFormat.format(Date(record.startTime))
            val end = if (record.isActive) "至今" else dateFormat.format(Date(record.endTime))
            tvTime.text = "$start 至 $end"
            tvProcess.text = record.process.ifEmpty { "无备注" }
        }
    }
}