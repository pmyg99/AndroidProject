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

class PetDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var adapter: BoardingRecordListAdapter
    private var petId: Long = -1
    private lateinit var petInfo: Pet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        petId = intent.getLongExtra("pet_id", -1)
        if (petId == -1L) {
            Toast.makeText(this, "宠物ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val tvPetDetail = findViewById<TextView>(R.id.tv_pet_detail)
        petInfo = getPetInfo(petId)
        tvPetDetail.text = getFormattedPetDetail(petInfo)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_boarding_records)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BoardingRecordListAdapter(emptyList()) { boardingRecord ->
            val intent = Intent(this, BoardingRecordDetailActivity::class.java)
            intent.putExtra("boarding_id", boardingRecord.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadBoardingRecords()
    }

    private fun loadBoardingRecords() {
        val cursor = db.query(
            DatabaseHelper.TABLE_BOARDING_RECORDS,
            null,
            "${DatabaseHelper.COL_BOARDING_PET_ID} = ?",
            arrayOf(petId.toString()),
            null, null,
            "${DatabaseHelper.COL_BOARDING_START_TIME} DESC"
        )
        val records = mutableListOf<BoardingRecord>()
        while (cursor.moveToNext()) {
            val record = BoardingRecord(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_ID)),
                petId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PET_ID)),
                startTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_START_TIME)),
                endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_END_TIME)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_IS_ACTIVE)) == 1,
                process = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BOARDING_PROCESS)),
                petName = petInfo.name
            )
            records.add(record)
        }
        cursor.close()
        adapter.updateData(records)
    }

    private fun getPetInfo(petId: Long): Pet {
        val cursor = db.query(
            DatabaseHelper.TABLE_PETS,
            null,
            "${DatabaseHelper.COL_PET_ID} = ?",
            arrayOf(petId.toString()),
            null, null, null
        )
        cursor.moveToFirst()
        val pet = Pet(
            id = petId,
            name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_NAME)),
            species = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_SPECIES)),
            age = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_AGE)),
            gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_GENDER)),
            ownerId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PET_OWNER_ID))
        )
        cursor.close()
        // 查询主人信息
        val ownerCursor = db.query(
            DatabaseHelper.TABLE_OWNERS,
            null,
            "${DatabaseHelper.COL_OWNER_ID} = ?",
            arrayOf(pet.ownerId.toString()),
            null, null, null
        )
        if (ownerCursor.moveToFirst()) {
            petInfo = pet.copy(
                ownerName = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_NAME)),
                phone = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_PHONE)),
                backupPhone = ownerCursor.getString(ownerCursor.getColumnIndexOrThrow(DatabaseHelper.COL_OWNER_BACKUP_PHONE))
            )
        }
        ownerCursor.close()
        return petInfo
    }

    private fun getFormattedPetDetail(pet: Pet): String {
        return """
            宠物ID: ${pet.id}
            姓名: ${pet.name}
            品种: ${pet.species}
            年龄: ${pet.age} 岁
            性别: ${pet.gender}
            主人: ${pet.ownerName}
            电话: ${pet.phone}
            备用电话: ${pet.backupPhone}
        """.trimIndent()
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        dbHelper.close()
    }
}