package com.example.yolotest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class BoardingActivity : AppCompatActivity() {

    private lateinit var etPetName: EditText
    private lateinit var etPetSpecies: AutoCompleteTextView
    private lateinit var etPetAge: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var etOwnerName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBackupPhone: EditText
    private lateinit var tvBoardingTime: TextView
    private lateinit var tvCheckoutTime: TextView
    private lateinit var rgBoardingType: RadioGroup
    private lateinit var llAppointmentTime: LinearLayout
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker

    private lateinit var dbHelper: DatabaseHelper

    private val boardingCalendar = Calendar.getInstance()
    private val checkoutCalendar = Calendar.getInstance()

    private val speciesList = listOf(
        "非洲野犬", "阿彭策尔山犬", "伯恩山犬", "边境牧羊犬", "佛兰德牧牛犬", "布拉班特格里芬犬", "布列塔尼猎犬",
        "卡迪根威尔士柯基", "杜宾犬", "英国塞特犬", "英国激飞猎犬", "恩特布赫山地犬", "爱斯基摩犬", "法国斗牛犬",
        "德国牧羊犬", "戈登塞特犬", "大丹犬", "大白熊犬", "大瑞士山地犬", "爱尔兰雪达犬", "爱尔兰水猎犬",
        "莱昂伯格犬", "墨西哥无毛犬", "纽芬兰犬", "古英国牧羊犬", "彭布罗克威尔士柯基", "博美犬", "罗威纳犬",
        "圣伯纳犬", "萨摩耶犬", "喜乐蒂牧羊犬", "西伯利亚哈士奇", "萨塞克斯猎犬", "藏獒", "威尔士激飞猎犬",
        "猴面梗", "巴仙吉犬", "拳师犬", "布里牧犬", "斗牛獒", "松狮犬", "克伦伯猎犬", "可卡犬", "柯利牧羊犬",
        "亚洲豺犬", "澳洲野犬", "比利时格罗安达牧羊犬", "荷兰毛狮犬", "澳大利亚卡尔比犬", "可蒙犬", "库瓦兹犬",
        "阿拉斯加雪橇犬", "比利时玛利诺犬", "迷你宾莎犬", "迷你贵宾犬", "巴哥犬", "比利时史基伯犬", "标准贵宾犬",
        "玩具贵宾犬", "匈牙利维兹拉犬"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding)

        dbHelper = DatabaseHelper(this)

        // 绑定控件
        etPetName = findViewById(R.id.et_pet_name)
        etPetSpecies = findViewById(R.id.et_pet_species)
        etPetAge = findViewById(R.id.et_pet_age)
        rgGender = findViewById(R.id.rg_gender)
        etOwnerName = findViewById(R.id.et_owner_name)
        etPhone = findViewById(R.id.et_phone)
        etBackupPhone = findViewById(R.id.et_backup_phone)
        tvBoardingTime = findViewById(R.id.tv_boarding_time)
        tvCheckoutTime = findViewById(R.id.tv_checkout_time)
        rgBoardingType = findViewById(R.id.rg_boarding_type)
        llAppointmentTime = findViewById(R.id.ll_appointment_time)
        datePicker = findViewById(R.id.date_picker)
        timePicker = findViewById(R.id.time_picker)

        // 品种下拉适配器（包含匹配）
        val speciesListMutable = speciesList.toMutableList()
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, speciesListMutable) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        val filtered = mutableListOf<String>()
                        if (constraint.isNullOrEmpty()) {
                            filtered.addAll(speciesList)
                        } else {
                            val lower = constraint.toString().lowercase(Locale.getDefault())
                            for (item in speciesList) {
                                if (item.lowercase(Locale.getDefault()).contains(lower)) {
                                    filtered.add(item)
                                }
                            }
                        }
                        results.values = filtered
                        results.count = filtered.size
                        return results
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()
                        if (results != null && results.count > 0) {
                            addAll(results.values as List<String>)
                        }
                        notifyDataSetChanged()
                    }
                }
            }
        }
        etPetSpecies.setAdapter(adapter)
        etPetSpecies.threshold = 1

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // 现场/预约切换
        rgBoardingType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_walk_in) {
                llAppointmentTime.visibility = LinearLayout.GONE
                tvBoardingTime.isEnabled = false
                tvBoardingTime.alpha = 0.7f
                tvCheckoutTime.isEnabled = true
                updateBoardingTime()
            } else {
                llAppointmentTime.visibility = LinearLayout.VISIBLE
                tvBoardingTime.isEnabled = false
                tvCheckoutTime.isEnabled = false
            }
        }

        updateBoardingTime()

        tvCheckoutTime.setOnClickListener {
            if (rgBoardingType.checkedRadioButtonId == R.id.rb_walk_in) {
                showDateTimePicker()
            } else {
                Toast.makeText(this, "预约入托请在上方选择时间", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_realtime_detect).setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivityForResult(intent, REQUEST_CAMERA)
        }

        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            submitBoarding()
        }
    }

    private fun updateBoardingTime() {
        boardingCalendar.time = Date()
        boardingCalendar.add(Calendar.MINUTE, 5)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        tvBoardingTime.text = format.format(boardingCalendar.time)
    }

    private fun showDateTimePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                checkoutCalendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        checkoutCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        checkoutCalendar.set(Calendar.MINUTE, minute)
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        tvCheckoutTime.text = format.format(checkoutCalendar.time)
                    },
                    checkoutCalendar.get(Calendar.HOUR_OF_DAY),
                    checkoutCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            checkoutCalendar.get(Calendar.YEAR),
            checkoutCalendar.get(Calendar.MONTH),
            checkoutCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitBoarding() {
        val petName = etPetName.text.toString().trim()
        val species = etPetSpecies.text.toString().trim()
        val ageStr = etPetAge.text.toString().trim()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rb_male -> "公"
            R.id.rb_female -> "母"
            else -> ""
        }
        val ownerName = etOwnerName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val backupPhone = etBackupPhone.text.toString().trim()

        // 必填校验
        if (petName.isEmpty()) {
            Toast.makeText(this, "请填写宠物姓名", Toast.LENGTH_SHORT).show()
            return
        }
        if (species.isEmpty()) {
            Toast.makeText(this, "请填写或选择宠物品种", Toast.LENGTH_SHORT).show()
            return
        }
        if (ageStr.isEmpty()) {
            Toast.makeText(this, "请填写宠物年龄", Toast.LENGTH_SHORT).show()
            return
        }
        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(this, "年龄必须是大于0的数字", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender.isEmpty()) {
            Toast.makeText(this, "请选择宠物性别", Toast.LENGTH_SHORT).show()
            return
        }
        if (ownerName.isEmpty()) {
            Toast.makeText(this, "请填写主人姓名", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "请填写联系方式", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取时间
        val startTime: Long
        val endTime: Long
        val isActive: Int
        if (rgBoardingType.checkedRadioButtonId == R.id.rb_walk_in) {
            if (tvCheckoutTime.text.toString() == "点击选择出托时间") {
                Toast.makeText(this, "请选择出托时间", Toast.LENGTH_SHORT).show()
                return
            }
            startTime = boardingCalendar.timeInMillis
            endTime = checkoutCalendar.timeInMillis
            isActive = 1
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth,
                timePicker.hour, timePicker.minute, 0)
            startTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            endTime = calendar.timeInMillis
            isActive = 0
        }

        val db = dbHelper.writableDatabase

        // 插入或获取主人ID
        val ownerId = getOrInsertOwner(ownerName, phone, backupPhone)

        // 插入宠物（静态信息）
        val petValues = android.content.ContentValues().apply {
            put(DatabaseHelper.COL_PET_NAME, petName)
            put(DatabaseHelper.COL_PET_SPECIES, species)
            put(DatabaseHelper.COL_PET_AGE, age)
            put(DatabaseHelper.COL_PET_GENDER, gender)
            put(DatabaseHelper.COL_PET_OWNER_ID, ownerId)
        }
        val petId = db.insert(DatabaseHelper.TABLE_PETS, null, petValues)

        // 插入寄养记录
        val boardingValues = android.content.ContentValues().apply {
            put(DatabaseHelper.COL_BOARDING_PET_ID, petId)
            put(DatabaseHelper.COL_BOARDING_START_TIME, startTime)
            put(DatabaseHelper.COL_BOARDING_END_TIME, endTime)
            put(DatabaseHelper.COL_BOARDING_IS_ACTIVE, isActive)
            put(DatabaseHelper.COL_BOARDING_PROCESS, "寄养过程")
        }
        db.insert(DatabaseHelper.TABLE_BOARDING_RECORDS, null, boardingValues)

        db.close()
        Toast.makeText(this, "登记成功", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getOrInsertOwner(name: String, phone: String, backupPhone: String): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_OWNERS,
            arrayOf(DatabaseHelper.COL_OWNER_ID),
            "${DatabaseHelper.COL_OWNER_NAME} = ? AND ${DatabaseHelper.COL_OWNER_PHONE} = ?",
            arrayOf(name, phone),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val id = cursor.getLong(0)
            cursor.close()
            id
        } else {
            cursor.close()
            val values = android.content.ContentValues().apply {
                put(DatabaseHelper.COL_OWNER_NAME, name)
                put(DatabaseHelper.COL_OWNER_PHONE, phone)
                put(DatabaseHelper.COL_OWNER_BACKUP_PHONE, backupPhone)
            }
            db.insert(DatabaseHelper.TABLE_OWNERS, null, values)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val species = data?.getStringExtra("detected_species")
            if (!species.isNullOrEmpty()) {
                runOnUiThread {
                    etPetSpecies.setText(species)
                    Toast.makeText(this, "已填入品种: $species", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "未能识别到品种", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 100
    }
}