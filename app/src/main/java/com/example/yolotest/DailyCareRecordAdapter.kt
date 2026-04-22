package com.example.yolotest

import DailyCareRecord
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DailyCareRecordAdapter(
    private var records: List<DailyCareRecord>,
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<DailyCareRecordAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_care_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.date))
        holder.tvDate.text = dateStr
        // 内容不再显示，只显示日期，所以隐藏内容TextView或清空
        holder.tvContent.visibility = View.GONE

        // 高亮当天
        val isToday = isSameDay(record.date, System.currentTimeMillis())
        if (isToday) {
            holder.itemView.setBackgroundColor(Color.parseColor("#AA4CAF50"))
            holder.tvDate.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.tvDate.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener { onItemClick(record.date) }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<DailyCareRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}