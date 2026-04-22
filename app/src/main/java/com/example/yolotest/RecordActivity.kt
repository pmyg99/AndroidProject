package com.example.yolotest

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PetSummaryAdapter
    private lateinit var etSearch: EditText
    private lateinit var rgStatus: RadioGroup

    private var currentStatus = 1 // 1=正在寄养, 0=寄养完成
    private var searchKeyword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        recyclerView = findViewById(R.id.rv_pets)
        etSearch = findViewById(R.id.et_search)
        rgStatus = findViewById(R.id.rg_status)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PetSummaryAdapter(emptyList()) { pet ->
            val intent = Intent(this, PetBoardingRecordsActivity::class.java)
            intent.putExtra("pet_id", pet.petId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        rgStatus.setOnCheckedChangeListener { _, checkedId ->
            currentStatus = if (checkedId == R.id.rb_boarding) 1 else 0
            loadPets()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchKeyword = s.toString()
                loadPets()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<RadioButton>(R.id.rb_boarding).isChecked = true
    }

    private fun loadPets() {
        // 查询宠物及其是否存在正在寄养的记录
        val sql = """
            SELECT 
                p.${DatabaseHelper.COL_PET_ID} as pet_id,
                p.${DatabaseHelper.COL_PET_NAME} as pet_name,
                p.${DatabaseHelper.COL_PET_SPECIES} as species,
                p.${DatabaseHelper.COL_PET_AGE} as age,
                p.${DatabaseHelper.COL_PET_GENDER} as gender,
                o.${DatabaseHelper.COL_OWNER_NAME} as owner_name,
                EXISTS (
                    SELECT 1 FROM ${DatabaseHelper.TABLE_BOARDING_RECORDS} b 
                    WHERE b.${DatabaseHelper.COL_BOARDING_PET_ID} = p.${DatabaseHelper.COL_PET_ID} 
                    AND b.${DatabaseHelper.COL_BOARDING_IS_ACTIVE} = 1
                ) as has_active
            FROM ${DatabaseHelper.TABLE_PETS} p
            JOIN ${DatabaseHelper.TABLE_OWNERS} o ON p.${DatabaseHelper.COL_PET_OWNER_ID} = o.${DatabaseHelper.COL_OWNER_ID}
            WHERE (p.${DatabaseHelper.COL_PET_NAME} LIKE ? OR o.${DatabaseHelper.COL_OWNER_NAME} LIKE ?)
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf("%$searchKeyword%", "%$searchKeyword%"))
        val pets = mutableListOf<PetSummary>()
        while (cursor.moveToNext()) {
            val hasActive = cursor.getInt(cursor.getColumnIndexOrThrow("has_active")) == 1
            if ((currentStatus == 1 && hasActive) || (currentStatus == 0 && !hasActive)) {
                val pet = PetSummary(
                    petId = cursor.getLong(cursor.getColumnIndexOrThrow("pet_id")),
                    petName = cursor.getString(cursor.getColumnIndexOrThrow("pet_name")),
                    species = cursor.getString(cursor.getColumnIndexOrThrow("species")),
                    age = cursor.getInt(cursor.getColumnIndexOrThrow("age")),
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                    ownerName = cursor.getString(cursor.getColumnIndexOrThrow("owner_name")),
                    isBoarding = hasActive
                )
                pets.add(pet)
            }
        }
        cursor.close()
        adapter.updateData(pets)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        dbHelper.close()
    }
}