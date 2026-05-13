package com.example.petowner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardingRecordDetailActivity : AppCompatActivity() {

    private lateinit var record: BoardingRecord
    private lateinit var rvDailyCare: RecyclerView
    private lateinit var adapter: DailyCareRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_record_detail)

        record = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("record", BoardingRecord::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("record")
        } ?: run {
            finish()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        findViewById<TextView>(R.id.tv_detail_start_time).text = "开始时间：${dateFormat.format(Date(record.startTime))}"
        findViewById<TextView>(R.id.tv_detail_end_time).text = if (record.isActive) "结束时间：至今" else "结束时间：${dateFormat.format(Date(record.endTime))}"
        findViewById<TextView>(R.id.tv_detail_status).text = if (record.isActive) "状态：进行中" else "状态：已结束"
        findViewById<TextView>(R.id.tv_detail_process).text = record.process.ifEmpty { "无" }

        // 初始化 RecyclerView
        rvDailyCare = findViewById(R.id.rv_daily_care)
        rvDailyCare.layoutManager = LinearLayoutManager(this)

        loadDailyCareRecords()
        // 加载每日看护记录
        loadDailyCareRecords()
    }

    // BoardingRecordDetailActivity.kt 中的 loadDailyCareRecords 方法
    private fun loadDailyCareRecords() {
        ApiClient.getDailyCareRecords(record.id) { records ->
            runOnUiThread {
                val adapter = DailyCareRecordAdapter(
                    records ?: emptyList()
                ) { clickedRecord ->
                    val intent = Intent(this, DailyCareDetailActivity::class.java)
                    intent.putExtra("care_record", clickedRecord)
                    startActivity(intent)
                }
                rvDailyCare.adapter = adapter
            }
        }
    }
}