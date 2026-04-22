package com.example.yolotest

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class TodayCareActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var boardingId: Long = -1
    private var careDate: Long = -1

    private lateinit var etFoodBrand: EditText
    private lateinit var etMealAmount: EditText
    private lateinit var etWaterAmount: EditText
    private lateinit var etWaterChangeTime: EditText
    private lateinit var etRemarks: EditText
    private lateinit var etMedicationUsed: EditText
    private lateinit var etMedicationDose: EditText
    private lateinit var etMedicationTime: EditText
    private lateinit var etMedicationMethod: EditText
    private lateinit var etWalkTime1: EditText
    private lateinit var etWalkTime2: EditText
    private lateinit var spPeeStatus: Spinner
    private lateinit var spPoopStatus: Spinner
    private lateinit var spSpiritStatus: Spinner
    private lateinit var spAppearanceStatus: Spinner
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_care)

        dbHelper = DatabaseHelper(this)
        boardingId = intent.getLongExtra("boarding_id", -1)
        careDate = intent.getLongExtra("care_date", -1)

        if (boardingId == -1L || careDate == -1L) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化控件
        etFoodBrand = findViewById(R.id.et_food_brand)
        etMealAmount = findViewById(R.id.et_meal_amount)
        etWaterAmount = findViewById(R.id.et_water_amount)
        etWaterChangeTime = findViewById(R.id.et_water_change_time)
        etRemarks = findViewById(R.id.et_remarks)
        etMedicationUsed = findViewById(R.id.et_medication_used)
        etMedicationDose = findViewById(R.id.et_medication_dose)
        etMedicationTime = findViewById(R.id.et_medication_time)
        etMedicationMethod = findViewById(R.id.et_medication_method)
        etWalkTime1 = findViewById(R.id.et_walk_time1)
        etWalkTime2 = findViewById(R.id.et_walk_time2)
        spPeeStatus = findViewById(R.id.sp_pee_status)
        spPoopStatus = findViewById(R.id.sp_poop_status)
        spSpiritStatus = findViewById(R.id.sp_spirit_status)
        spAppearanceStatus = findViewById(R.id.sp_appearance_status)
        btnSave = findViewById(R.id.btn_save)

        setupSpinners()
        loadExistingData()

        btnSave.setOnClickListener { save() }
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(careDate))
        findViewById<TextView>(R.id.tv_title).text = "日常看护 - $dateStr"
    }

    private fun setupSpinners() {
        val statusOptions = arrayOf("正常", "异常", "未记录")
        val peeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        peeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPeeStatus.adapter = peeAdapter

        val poopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        poopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPoopStatus.adapter = poopAdapter

        val spiritOptions = arrayOf("活泼", "正常", "萎靡", "未记录")
        val spiritAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spiritOptions)
        spiritAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSpiritStatus.adapter = spiritAdapter

        val appearanceOptions = arrayOf("良好", "一般", "不佳", "未记录")
        val appearanceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appearanceOptions)
        appearanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAppearanceStatus.adapter = appearanceAdapter
    }

    private fun loadExistingData() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_DAILY_CARE,
            null,
            "${DatabaseHelper.COL_CARE_BOARDING_ID} = ? AND ${DatabaseHelper.COL_CARE_DATE} = ?",
            arrayOf(boardingId.toString(), careDate.toString()),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            etFoodBrand.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_BRAND)))
            etMealAmount.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEAL_AMOUNT)))
            etWaterAmount.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_AMOUNT)))
            etWaterChangeTime.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WATER_CHANGE_TIME)))
            etRemarks.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REMARKS)))
            etMedicationUsed.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_USED)))
            etMedicationDose.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_DOSE)))
            etMedicationTime.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_TIME)))
            etMedicationMethod.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MEDICATION_METHOD)))
            etWalkTime1.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME1)))
            etWalkTime2.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_WALK_TIME2)))
            setSpinnerSelection(spPeeStatus, cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PEE_STATUS)))
            setSpinnerSelection(spPoopStatus, cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_POOP_STATUS)))
            setSpinnerSelection(spSpiritStatus, cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SPIRIT_STATUS)))
            setSpinnerSelection(spAppearanceStatus, cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_APPEARANCE_STATUS)))
        }
        cursor.close()
        db.close()
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }

    private fun save() {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CARE_BOARDING_ID, boardingId)
            put(DatabaseHelper.COL_CARE_DATE, careDate)
            put(DatabaseHelper.COL_FOOD_BRAND, etFoodBrand.text.toString())
            put(DatabaseHelper.COL_MEAL_AMOUNT, etMealAmount.text.toString())
            put(DatabaseHelper.COL_WATER_AMOUNT, etWaterAmount.text.toString())
            put(DatabaseHelper.COL_WATER_CHANGE_TIME, etWaterChangeTime.text.toString())
            put(DatabaseHelper.COL_REMARKS, etRemarks.text.toString())
            put(DatabaseHelper.COL_MEDICATION_USED, etMedicationUsed.text.toString())
            put(DatabaseHelper.COL_MEDICATION_DOSE, etMedicationDose.text.toString())
            put(DatabaseHelper.COL_MEDICATION_TIME, etMedicationTime.text.toString())
            put(DatabaseHelper.COL_MEDICATION_METHOD, etMedicationMethod.text.toString())
            put(DatabaseHelper.COL_WALK_TIME1, etWalkTime1.text.toString())
            put(DatabaseHelper.COL_WALK_TIME2, etWalkTime2.text.toString())
            put(DatabaseHelper.COL_PEE_STATUS, spPeeStatus.selectedItem.toString())
            put(DatabaseHelper.COL_POOP_STATUS, spPoopStatus.selectedItem.toString())
            put(DatabaseHelper.COL_SPIRIT_STATUS, spSpiritStatus.selectedItem.toString())
            put(DatabaseHelper.COL_APPEARANCE_STATUS, spAppearanceStatus.selectedItem.toString())
        }
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict(DatabaseHelper.TABLE_DAILY_CARE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}