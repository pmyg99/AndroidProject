package com.example.petowner

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class DailyCareDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_care_detail)

        val record = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("care_record", DailyCareRecord::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("care_record")
        }

        if (record == null) {
            Log.e("DailyCareDetail", "record is null, finishing")
            finish()
            return
        }

        Log.d("DailyCareDetail", "record received: ${record.date}")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        findViewById<TextView>(R.id.tv_detail_date).text = "日期：${dateFormat.format(Date(record.date))}"
        findViewById<TextView>(R.id.tv_detail_food).text = "食物：${record.foodBrand} ${record.mealAmount}"
        findViewById<TextView>(R.id.tv_detail_water).text = "饮水：${record.waterAmount} (换水时间 ${record.waterChangeTime})"
        findViewById<TextView>(R.id.tv_detail_medication).text = "用药：${record.medicationUsed} ${record.medicationDose} ${record.medicationMethod} 时间：${record.medicationTime}"
        findViewById<TextView>(R.id.tv_detail_walk).text = "散步：${record.walkTime1} / ${record.walkTime2}"
        findViewById<TextView>(R.id.tv_detail_pee_poop).text = "排泄：尿 ${record.peeStatus} / 便 ${record.poopStatus}"
        findViewById<TextView>(R.id.tv_detail_spirit).text = "精神状态：${record.spiritStatus}  外观：${record.appearanceStatus}"
        findViewById<TextView>(R.id.tv_detail_remarks).text = "备注：${record.remarks}"
    }
}