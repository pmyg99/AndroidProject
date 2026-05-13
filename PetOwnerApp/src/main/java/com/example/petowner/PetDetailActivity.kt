package com.example.petowner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petowner.BoardingRecordDetailActivity

class PetDetailActivity : AppCompatActivity() {

    private lateinit var pet: Pet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PetDetailActivity", "onCreate started")
        setContentView(R.layout.activity_pet_detail)

        val pet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pet", Pet::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("pet")
        }
        Log.d("PetDetailActivity", "pet = $pet")
        if (pet == null) {
            Log.e("PetDetailActivity", "Pet is null, finishing")
            finish()
            return
        }

        // 显示宠物档案
        findViewById<TextView>(R.id.tv_detail_name).text = pet.name
        findViewById<TextView>(R.id.tv_detail_species).text = pet.species
        findViewById<TextView>(R.id.tv_detail_age).text = "${pet.age}岁"
        findViewById<TextView>(R.id.tv_detail_gender).text = pet.gender

        // 当前寄养记录
        val rvActive = findViewById<RecyclerView>(R.id.rv_active_records)
        rvActive.layoutManager = LinearLayoutManager(this)
        rvActive.adapter = BoardingRecordAdapter(pet.activeRecords) { record ->
            openRecordDetail(record)
        }

        // 历史寄养记录
        val rvCompleted = findViewById<RecyclerView>(R.id.rv_completed_records)
        rvCompleted.layoutManager = LinearLayoutManager(this)
        rvCompleted.adapter = BoardingRecordAdapter(pet.completedRecords) { record ->
            openRecordDetail(record)
        }
    }

    private fun openRecordDetail(record: BoardingRecord) {
        Log.d("PetDetailActivity", "Starting BoardingRecordDetailActivity, record id: ${record.id}")
        val intent = Intent(this, com.example.petowner.BoardingRecordDetailActivity::class.java)
        intent.putExtra("record", record)
        startActivity(intent)
    }
}