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

class BoardingRecordDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: DailyCareRecordAdapter
    private var boardingId: Long = -1
    private var boardingIndex: Int = 0
    private lateinit var boardingRecord: BoardingRecord
    private lateinit var pet: Pet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_record_detail)

        boardingId = intent.getLongExtra("boarding_id", -1)
        boardingIndex = intent.getIntExtra("boarding_index", 0)
        if (boardingId == -1L) {
            Toast.makeText(this, "寄养记录ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        loadBoardingRecord()
        loadPetInfo()

        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val indexText = when (boardingIndex) {
            1 -> "第一次寄养"
            2 -> "第二次寄养"
            3 -> "第三次寄养"
            else -> if (boardingIndex > 0) "第${boardingIndex}次寄养" else "寄养记录"
        }
        tvTitle.text = "${pet.name} - $indexText"

        val tvPetDetail = findViewById<TextView>(R.id.tv_pet_detail)
        tvPetDetail.text = getPetDetailString()

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

    private fun loadBoardingRecord() {
        val cursor = db.query(
            DatabaseHelper.TABLE_BOARDING_RECORDS,
            null,
            "${DatabaseHelper.COL_BOARDING_ID} = ?",
            arrayOf(boardingId.toString()),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            val petId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PET_ID))
            val startTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_START_TIME))
            val endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_END_TIME))
            val isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_IS_ACTIVE)) == 1
            val process = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PROCESS))
            boardingRecord = BoardingRecord(boardingId, petId, startTime, endTime, isActive, process)
        } else {
            Toast.makeText(this, "未找到寄养记录", Toast.LENGTH_SHORT).show()
            finish()
        }
        cursor.close()
    }

    private fun loadPetInfo() {
        val cursor = db.query(
            DatabaseHelper.TABLE_PETS,
            null,
            "${DatabaseHelper.COL_PET_ID} = ?",
            arrayOf(boardingRecord.petId.toString()),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_NAME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_SPECIES))
            val age = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_AGE))
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_GENDER))
            val ownerId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_OWNER_ID))
            pet = Pet(id, name, species, age, gender, ownerId)
        }
        cursor.close()
    }

    private fun getPetDetailString(): String {
        var ownerName = ""
        var phone = ""
        var backupPhone = ""
        val ownerCursor = db.query(
            DatabaseHelper.TABLE_OWNERS,
            null,
            "${DatabaseHelper.COL_OWNER_ID} = ?",
            arrayOf(pet.ownerId.toString()),
            null, null, null
        )
        if (ownerCursor.moveToFirst()) {
            ownerName = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_NAME))
            phone = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_PHONE))
            backupPhone = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_BACKUP_PHONE))
        }
        ownerCursor.close()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val startStr = dateFormat.format(Date(boardingRecord.startTime))
        val endStr = dateFormat.format(Date(boardingRecord.endTime))
        return """
            宠物ID: ${pet.id}
            姓名: ${pet.name}
            品种: ${pet.species}
            年龄: ${pet.age} 岁
            性别: ${pet.gender}
            主人: $ownerName
            电话: $phone
            备用电话: $backupPhone
            寄养时间: $startStr 至 $endStr
        """.trimIndent()
    }

    private fun loadDailyRecords() {
        val startDate = getStartOfDay(boardingRecord.startTime)
        val endDate = getStartOfDay(boardingRecord.endTime)
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
                foodBrand = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_BRAND)) ?: "",
                mealAmount = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_AMOUNT)) ?: "",
                waterAmount = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_AMOUNT)) ?: "",
                waterChangeTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_CHANGE_TIME)) ?: "",
                remarks = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REMARKS)) ?: "",
                medicationUsed = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_USED)) ?: "",
                medicationDose = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_DOSE)) ?: "",
                medicationTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_TIME)) ?: "",
                medicationMethod = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_METHOD)) ?: "",
                walkTime1 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME1)) ?: "08:00",
                walkTime2 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME2)) ?: "18:00",
                peeStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PEE_STATUS)) ?: "",
                poopStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_POOP_STATUS)) ?: "",
                spiritStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SPIRIT_STATUS)) ?: "",
                appearanceStatus = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_APPEARANCE_STATUS)) ?: ""
            )
            recordMap[date] = record
        }
        cursor.close()

        val records = allDates.map { date ->
            recordMap[date] ?: DailyCareRecord(date = date)
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

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        dbHelper.close()
    }
}