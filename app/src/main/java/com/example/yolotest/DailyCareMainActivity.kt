package com.example.yolotest

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DailyCareMainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PetSummaryAdapter
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_care_main)

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        recyclerView = findViewById(R.id.rv_pets)
        etSearch = findViewById(R.id.et_search)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PetSummaryAdapter(emptyList()) { pet ->
            val intent = Intent(this, PetBoardingRecordsActivity::class.java)
            intent.putExtra("pet_id", pet.petId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loadPets(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadPets("")
    }

    private fun loadPets(search: String) {
        val sql = """
            SELECT 
                p.${DatabaseHelper.COL_PET_ID} as pet_id,
                p.${DatabaseHelper.COL_PET_NAME} as pet_name,
                p.${DatabaseHelper.COL_PET_SPECIES} as species,
                p.${DatabaseHelper.COL_PET_AGE} as age,
                p.${DatabaseHelper.COL_PET_GENDER} as gender,
                o.${DatabaseHelper.COL_OWNER_NAME} as owner_name
            FROM ${DatabaseHelper.TABLE_PETS} p
            JOIN ${DatabaseHelper.TABLE_OWNERS} o ON p.${DatabaseHelper.COL_PET_OWNER_ID} = o.${DatabaseHelper.COL_OWNER_ID}
            WHERE (p.${DatabaseHelper.COL_PET_NAME} LIKE ? OR o.${DatabaseHelper.COL_OWNER_NAME} LIKE ?)
            AND EXISTS (
                SELECT 1 FROM ${DatabaseHelper.TABLE_BOARDING_RECORDS} b 
                WHERE b.${DatabaseHelper.COL_BOARDING_PET_ID} = p.${DatabaseHelper.COL_PET_ID} 
                AND b.${DatabaseHelper.COL_BOARDING_IS_ACTIVE} = 1
            )
            ORDER BY p.${DatabaseHelper.COL_PET_NAME}
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf("%$search%", "%$search%"))
        val pets = mutableListOf<PetSummary>()
        while (cursor.moveToNext()) {
            val pet = PetSummary(
                petId = cursor.getLong(cursor.getColumnIndexOrThrow("pet_id")),
                petName = cursor.getString(cursor.getColumnIndexOrThrow("pet_name")),
                species = cursor.getString(cursor.getColumnIndexOrThrow("species")),
                age = cursor.getInt(cursor.getColumnIndexOrThrow("age")),
                gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")),
                ownerName = cursor.getString(cursor.getColumnIndexOrThrow("owner_name")),
                isBoarding = true
            )
            pets.add(pet)
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