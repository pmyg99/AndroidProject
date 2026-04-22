package com.example.yolotest

import DailyCareRecord
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DailyCareDetailActivity : AppCompatActivity() {  // 注意：实际应命名为 BoardingRecordDetailActivity，但保持原名避免改动过多

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: DailyCareRecordAdapter
    private var boardingId: Long = -1
    private lateinit var boardingInfo: BoardingInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_record_detail)  // 使用新布局

        boardingId = intent.getLongExtra("boarding_id", -1)
        if (boardingId == -1L) {
            Toast.makeText(this, "寄养记录ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val tvPetDetail = findViewById<TextView>(R.id.tv_pet_detail)

        boardingInfo = getBoardingInfo(boardingId)
        tvTitle.text = "${boardingInfo.petName} - 寄养记录"
        tvPetDetail.text = boardingInfo.getFormattedDetail()

        val recyclerView = findViewById<RecyclerView>(R.id.rv_daily_records)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DailyCareRecordAdapter(emptyList()) { date ->
            val intent = Intent(this, TodayCareActivity::class.java)
            intent.putExtra("boarding_id", boardingId)
            intent.putExtra("care_date", date)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadDailyRecords()
    }

    private fun loadDailyRecords() {
        val startDate = getStartOfDay(boardingInfo.startTime)
        val endDate = getStartOfDay(boardingInfo.endTime)
        val allDates = generateDates(startDate, endDate)

        val cursor = db.query(
            DatabaseHelper.TABLE_DAILY_CARE,
            null,
            "${DatabaseHelper.COL_CARE_BOARDING_ID} = ?",
            arrayOf(boardingId.toString()),
            null, null, null
        )
        val recordMap = mutableMapOf<Long, DailyCareRecord>()
        while (cursor.moveToNext()) {
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CARE_DATE))
            val record = DailyCareRecord(
                date = date,
                foodBrand = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_BRAND)),
                mealAmount = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_AMOUNT)),
                waterAmount = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_AMOUNT)),
                waterChangeTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_CHANGE_TIME)),
                remarks = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REMARKS)),
                medicationUsed = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_USED)),
                medicationDose = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_DOSE)),
                medicationTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_TIME)),
                medicationMethod = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_METHOD)),
                walkTime1 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME1)),
                walkTime2 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME2)),
                peeStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PEE_STATUS)),
                poopStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_POOP_STATUS)),
                spiritStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SPIRIT_STATUS)),
                appearanceStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_APPEARANCE_STATUS))
            )
            recordMap[date] = record
        }
        cursor.close()

        val records = allDates.map { date ->
            recordMap[date] ?: DailyCareRecord(date)
        }
        adapter.updateData(records)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun generateDates(start: Long, end: Long): List<Long> {
        val dates = mutableListOf<Long>()
        var current = start
        while (current <= end) {
            dates.add(current)
            current += 24 * 60 * 60 * 1000L
        }
        return dates
    }

    private fun getBoardingInfo(boardingId: Long): BoardingInfo {
        val sql = """
            SELECT b.*, p.name as pet_name, p.species, p.age, p.gender, o.name as owner_name, o.phone, o.backup_phone
            FROM ${DatabaseHelper.TABLE_BOARDING_RECORDS} b
            JOIN ${DatabaseHelper.TABLE_PETS} p ON b.${DatabaseHelper.COL_BOARDING_PET_ID} = p.${DatabaseHelper.COL_PET_ID}
            JOIN ${DatabaseHelper.TABLE_OWNERS} o ON p.${DatabaseHelper.COL_PET_OWNER_ID} = o.${DatabaseHelper.COL_OWNER_ID}
            WHERE b.${DatabaseHelper.COL_BOARDING_ID} = ?
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(boardingId.toString()))
        cursor.moveToFirst()
        val info = BoardingInfo(
            petName = cursor.getString(cursor.getColumnIndexOrThrow("pet_name")),
            species = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_SPECIES)),
            age = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_AGE)),
            gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_GENDER)),
            ownerName = cursor.getString(cursor.getColumnIndexOrThrow("owner_name")),
            phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_PHONE)),
            backupPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_BACKUP_PHONE)),
            startTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_START_TIME)),
            endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_END_TIME)),
            process = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PROCESS))
        )
        cursor.close()
        return info
    }

    data class BoardingInfo(
        val petName: String,
        val species: String,
        val age: Int,
        val gender: String,
        val ownerName: String,
        val phone: String,
        val backupPhone: String,
        val startTime: Long,
        val endTime: Long,
        val process: String
    ) {
        fun getFormattedDetail(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val startStr = dateFormat.format(Date(startTime))
            val endStr = dateFormat.format(Date(endTime))
            return """
                宠物: $petName
                品种: $species
                年龄: $age 岁
                性别: $gender
                主人: $ownerName
                电话: $phone
                备用电话: $backupPhone
                寄养时间: $startStr 至 $endStr
                备注: $process
            """.trimIndent()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        dbHelper.close()
    }
}