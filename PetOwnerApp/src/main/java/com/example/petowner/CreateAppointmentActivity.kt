package com.example.petowner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CreateAppointmentActivity : AppCompatActivity() {

    private lateinit var rgPetType: RadioGroup
    private lateinit var llExistingPet: LinearLayout
    private lateinit var llNewPet: LinearLayout
    private lateinit var spPet: Spinner
    private lateinit var tvPetDetail: TextView
    private lateinit var etNewPetName: EditText
    private lateinit var etNewPetSpecies: EditText
    private lateinit var etNewPetAge: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var etBackupPhone: EditText
    private lateinit var btnStartDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnEndTime: Button
    private lateinit var etNotes: EditText
    private lateinit var btnSubmit: Button

    // 使用 Calendar 对象管理时间
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    private var existingPets: List<Pet> = emptyList()
    private var selectedPet: Pet? = null
    private var ownerId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_appointment)

        initViews()
        loadPets()
        setupRadioButtons()
        setupDateTimePickers()
        setupSubmitButton()

        ownerId = intent.getLongExtra("owner_id", 0)
        if (ownerId == 0L) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 初始设置默认开始时间为当前时间+5分钟
        startCalendar.timeInMillis = System.currentTimeMillis()
        startCalendar.add(Calendar.MINUTE, 5)
        updateStartTimeDisplay()

        // 初始结束时间设为开始时间+1小时（仅作示例，用户可改）
        endCalendar.timeInMillis = startCalendar.timeInMillis
        endCalendar.add(Calendar.HOUR_OF_DAY, 1)
        updateEndTimeDisplay()
    }

    private fun initViews() {
        rgPetType = findViewById(R.id.rg_pet_type)
        llExistingPet = findViewById(R.id.ll_existing_pet)
        llNewPet = findViewById(R.id.ll_new_pet)
        spPet = findViewById(R.id.sp_pet)
        tvPetDetail = findViewById(R.id.tv_pet_detail)
        etNewPetName = findViewById(R.id.et_new_pet_name)
        etNewPetSpecies = findViewById(R.id.et_new_pet_species)
        etNewPetAge = findViewById(R.id.et_new_pet_age)
        rgGender = findViewById(R.id.rg_gender)
        etBackupPhone = findViewById(R.id.et_backup_phone)
        btnStartDate = findViewById(R.id.btn_start_date)
        btnStartTime = findViewById(R.id.btn_start_time)
        btnEndDate = findViewById(R.id.btn_end_date)
        btnEndTime = findViewById(R.id.btn_end_time)
        etNotes = findViewById(R.id.et_notes)
        btnSubmit = findViewById(R.id.btn_submit)
    }

    private fun loadPets() {
        existingPets = intent.getParcelableArrayListExtra("pets") ?: emptyList()
        val petNames = existingPets.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPet.adapter = adapter
        spPet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedPet = existingPets[pos]
                tvPetDetail.text = "年龄：${selectedPet!!.age}岁  性别：${selectedPet!!.gender}"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        if (existingPets.isNotEmpty()) selectedPet = existingPets[0]
    }

    private fun setupRadioButtons() {
        rgPetType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_existing) {
                llExistingPet.visibility = View.VISIBLE
                llNewPet.visibility = View.GONE
            } else {
                llExistingPet.visibility = View.GONE
                llNewPet.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDateTimePickers() {
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnStartTime.setOnClickListener { showTimePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        btnEndTime.setOnClickListener { showTimePicker(false) }
    }

    private fun showDatePicker(isStart: Boolean) {
        val cal = if (isStart) startCalendar else endCalendar
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            if (isStart) updateStartTimeDisplay() else updateEndTimeDisplay()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val cal = if (isStart) startCalendar else endCalendar
        TimePickerDialog(this, { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            if (isStart) updateStartTimeDisplay() else updateEndTimeDisplay()
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun updateStartTimeDisplay() {
        btnStartDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(startCalendar.time)
        btnStartTime.text = SimpleDateFormat("HH:mm", Locale.CHINA).format(startCalendar.time)
    }

    private fun updateEndTimeDisplay() {
        btnEndDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(endCalendar.time)
        btnEndTime.text = SimpleDateFormat("HH:mm", Locale.CHINA).format(endCalendar.time)
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener
            val startTimestamp = startCalendar.timeInMillis
            val endTimestamp = endCalendar.timeInMillis

            if (rgPetType.checkedRadioButtonId == R.id.rb_existing) {
                ApiClient.createAppointmentWithPet(
                    ownerId = ownerId,
                    petId = selectedPet!!.id,
                    startTime = startTimestamp,
                    endTime = endTimestamp,
                    notes = etNotes.text.toString()
                ) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "预约成功", Toast.LENGTH_SHORT).show()
                            finish()
                        } else Toast.makeText(this, "预约失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val name = etNewPetName.text.toString()
                val species = etNewPetSpecies.text.toString().ifBlank { "未知" }
                val age = etNewPetAge.text.toString().toIntOrNull() ?: 0
                val gender = if (rgGender.checkedRadioButtonId == R.id.rb_male) "公" else "母"
                ApiClient.createAppointmentWithNewPet(
                    ownerId = ownerId,
                    tempName = name,
                    tempSpecies = species,
                    tempAge = age,
                    tempGender = gender,
                    startTime = startTimestamp,
                    endTime = endTimestamp,
                    notes = etNotes.text.toString()
                ) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "预约成功", Toast.LENGTH_SHORT).show()
                            finish()
                        } else Toast.makeText(this, "预约失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (rgPetType.checkedRadioButtonId == R.id.rb_existing) {
            if (selectedPet == null) {
                Toast.makeText(this, "请选择宠物", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (etNewPetName.text.isBlank()) {
                Toast.makeText(this, "请输入宠物名称", Toast.LENGTH_SHORT).show()
                return false
            }
            val ageText = etNewPetAge.text.toString()
            if (ageText.isBlank()) {
                Toast.makeText(this, "请输入宠物年龄", Toast.LENGTH_SHORT).show()
                return false
            }
            if (rgGender.checkedRadioButtonId == -1) {
                Toast.makeText(this, "请选择宠物性别", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        val startTimestamp = startCalendar.timeInMillis
        val endTimestamp = endCalendar.timeInMillis
        if (startTimestamp <= System.currentTimeMillis()) {
            Toast.makeText(this, "开始时间必须晚于当前时间", Toast.LENGTH_SHORT).show()
            return false
        }
        if (endTimestamp <= startTimestamp) {
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}