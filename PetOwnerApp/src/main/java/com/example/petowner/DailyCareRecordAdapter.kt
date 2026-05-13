// DailyCareRecordAdapter.kt
package com.example.petowner

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DailyCareRecordAdapter(
    private val records: List<DailyCareRecord>,
    private val onItemClick: (DailyCareRecord) -> Unit   // 新增点击回调
) : RecyclerView.Adapter<DailyCareRecordAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_care_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
        // 设置点击事件
        holder.itemView.setOnClickListener { onItemClick(record) }
    }

    override fun getItemCount() = records.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate = itemView.findViewById<TextView>(R.id.tv_care_date)
        private val tvRemarks = itemView.findViewById<TextView>(R.id.tv_care_remarks)

        fun bind(record: DailyCareRecord) {
            tvDate.text = dateFormat.format(Date(record.date))
            tvRemarks.text = record.remarks.ifEmpty { "无备注" }
        }
    }
}