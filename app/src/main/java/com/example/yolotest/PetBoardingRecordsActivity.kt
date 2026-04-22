package com.example.yolotest

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

class PetBoardingRecordsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: BoardingRecordAdapter
    private var petId: Long = -1
    private lateinit var petName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_boarding_records)

        petId = intent.getLongExtra("pet_id", -1)
        if (petId == -1L) {
            Toast.makeText(this, "宠物ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // 获取宠物名称
        val petCursor = db.query(
            DatabaseHelper.TABLE_PETS,
            arrayOf(DatabaseHelper.COL_PET_NAME),
            "${DatabaseHelper.COL_PET_ID} = ?",
            arrayOf(petId.toString()),
            null, null, null
        )
        if (petCursor.moveToFirst()) {
            petName = petCursor.getString(0)
        }
        petCursor.close()

        val tvTitle = findViewById<TextView>(R.id.tv_title)
        tvTitle.text = "$petName 的寄养记录"

        val recyclerView = findViewById<RecyclerView>(R.id.rv_boarding_records)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BoardingRecordAdapter(emptyList()) { boardingRecord ->
            val intent = Intent(this, BoardingRecordDetailActivity::class.java)
            intent.putExtra("boarding_id", boardingRecord.id)
            intent.putExtra("boarding_index", boardingRecord.index)  // 传递序号
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadBoardingRecords()
    }

    private fun loadBoardingRecords() {
        // 按开始时间升序，最早的在前面
        val cursor = db.query(
            DatabaseHelper.TABLE_BOARDING_RECORDS,
            null,
            "${DatabaseHelper.COL_BOARDING_PET_ID} = ?",
            arrayOf(petId.toString()),
            null, null,
            "${DatabaseHelper.COL_BOARDING_START_TIME} ASC"
        )
        val records = mutableListOf<BoardingRecord>()
        var index = 1
        while (cursor.moveToNext()) {
            val record = BoardingRecord(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_ID)),
                petId = petId,
                startTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_START_TIME)),
                endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_END_TIME)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_IS_ACTIVE)) == 1,
                process = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PROCESS)),
                petName = petName,
                index = index  // 动态添加序号
            )
            records.add(record)
            index++
        }
        cursor.close()
        adapter.updateData(records)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        dbHelper.close()
    }
}