package com.example.yolotest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ConfirmAppointmentActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var appointmentId: Long = 0
    private var appointment: Appointment? = null
    private var isNewPet = false
    private val REQUEST_CAMERA = 100

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    private lateinit var autoComplete: AutoCompleteTextView  // 提取为成员变量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_appointment)

        dbHelper = DatabaseHelper(this)
        appointmentId = intent.getLongExtra("appointment_id", 0)

        if (appointmentId == 0L) {
            Toast.makeText(this, "无效的预约", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadAppointmentData()
        setupSpeciesAutoComplete()
        setupButtons()
    }

    private fun loadAppointmentData() {
        appointment = dbHelper.getAppointmentById(appointmentId)
        if (appointment == null) {
            Toast.makeText(this, "预约不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startCalendar.timeInMillis = appointment!!.startTime
        endCalendar.timeInMillis = appointment!!.endTime ?: (appointment!!.startTime + 7 * 24 * 60 * 60 * 1000L)

        val pet = if (appointment!!.petId != null) dbHelper.getPetById(appointment!!.petId!!) else null
        val owner = if (pet != null) dbHelper.getOwnerById(pet.ownerId) else null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.tv_pet_name).text = "宠物：${appointment!!.petName}"
        findViewById<TextView>(R.id.tv_owner).text = "主人：${owner?.name ?: "未知"}"
        findViewById<TextView>(R.id.tv_backup_phone).text = if (!owner?.backupPhone.isNullOrBlank()) "备用电话：${owner?.backupPhone}" else ""
        updateTimeDisplay()

        if (pet == null && appointment!!.tempPetName != null) {
            isNewPet = true
            findViewById<EditText>(R.id.et_pet_name).setText(appointment!!.tempPetName)
            findViewById<EditText>(R.id.et_pet_species).setText(appointment!!.tempPetSpecies ?: "")
            findViewById<EditText>(R.id.et_pet_age).setText(appointment!!.tempPetAge?.toString() ?: "")
            if (appointment!!.tempPetGender == "公") {
                findViewById<RadioButton>(R.id.rb_male).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rb_female).isChecked = true
            }
        } else {
            findViewById<EditText>(R.id.et_pet_name).setText(pet?.name)
            findViewById<EditText>(R.id.et_pet_name).isEnabled = false
            findViewById<EditText>(R.id.et_pet_species).setText(pet?.species)
            findViewById<EditText>(R.id.et_pet_species).isEnabled = false
            findViewById<EditText>(R.id.et_pet_age).setText(pet?.age.toString())
            findViewById<EditText>(R.id.et_pet_age).isEnabled = false
            findViewById<RadioGroup>(R.id.rg_gender).isEnabled = false
            if (pet?.gender == "公") findViewById<RadioButton>(R.id.rb_male).isChecked = true
            else findViewById<RadioButton>(R.id.rb_female).isChecked = true
        }
    }

    private fun updateTimeDisplay() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.tv_start_time).text = "开始时间：${dateFormat.format(startCalendar.time)}"
        findViewById<TextView>(R.id.tv_end_time).text = "结束时间：${dateFormat.format(endCalendar.time)}"
    }

    private fun setupSpeciesAutoComplete() {
        val allSpecies = resources.getStringArray(R.array.pet_species_array).toList()
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, allSpecies.toMutableList()) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        if (constraint.isNullOrEmpty()) {
                            results.values = allSpecies
                            results.count = allSpecies.size
                        } else {
                            val filterPattern = constraint.toString().lowercase(Locale.getDefault())
                            val filtered = allSpecies.filter {
                                it.lowercase(Locale.getDefault()).contains(filterPattern)
                            }
                            results.values = filtered
                            results.count = filtered.size
                        }
                        return results
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()
                        if (results != null && results.values != null) {
                            @Suppress("UNCHECKED_CAST")
                            addAll(results.values as Collection<String>)
                        }
                        notifyDataSetChanged()
                    }
                }
            }
        }
        autoComplete = findViewById(R.id.et_pet_species)
        autoComplete.setAdapter(adapter)
        autoComplete.threshold = 1
        autoComplete.setDropDownBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
        )
        autoComplete.dropDownHeight = 400
    }
    private fun setupButtons() {
        findViewById<Button>(R.id.btn_start_time).setOnClickListener {
            showDateTimePicker(true)
        }
        findViewById<Button>(R.id.btn_end_time).setOnClickListener {
            showDateTimePicker(false)
        }
        findViewById<Button>(R.id.btn_take_photo).setOnClickListener {
            startActivityForResult(Intent(this, CameraActivity::class.java), REQUEST_CAMERA)
        }
        findViewById<Button>(R.id.btn_confirm).setOnClickListener { confirm() }
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val cal = if (isStart) startCalendar else endCalendar
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            updateTimeDisplay()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val detectedSpecies = data?.getStringExtra("detected_species")
            if (!detectedSpecies.isNullOrEmpty()) {
                autoComplete.setText(detectedSpecies)
                Toast.makeText(this, "识别品种：$detectedSpecies", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未识别到品种，请手动选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirm() {
        val petName = findViewById<EditText>(R.id.et_pet_name).text.toString()
        if (petName.isBlank()) {
            Toast.makeText(this, "请填写宠物姓名", Toast.LENGTH_SHORT).show()
            return
        }
        val petSpecies = autoComplete.text.toString().ifBlank { "未知" }
        val petAge = findViewById<EditText>(R.id.et_pet_age).text.toString().toIntOrNull() ?: 0
        val petGender = if (findViewById<RadioGroup>(R.id.rg_gender).checkedRadioButtonId == R.id.rb_male) "公" else "母"
        val careRemarks = findViewById<EditText>(R.id.et_care_remarks).text.toString()

        val finalStart = startCalendar.timeInMillis
        val finalEnd = endCalendar.timeInMillis

        if (finalStart != appointment!!.startTime || finalEnd != (appointment!!.endTime ?: 0)) {
            dbHelper.updateAppointmentTime(appointmentId, finalStart, finalEnd)
        }

        var petId = appointment!!.petId
        if (isNewPet) {
            val ownerId = appointment!!.ownerId
            if (ownerId == null) {
                Toast.makeText(this, "无法确定主人信息", Toast.LENGTH_SHORT).show()
                return
            }
            petId = dbHelper.insertPet(ownerId, petName, petSpecies, petAge, petGender)
            dbHelper.updateAppointmentPetId(appointmentId, petId)
        }

        if (petId == null) {
            Toast.makeText(this, "无法确定宠物信息", Toast.LENGTH_SHORT).show()
            return
        }

        val boardingId = dbHelper.insertBoardingRecord(petId, finalStart, finalEnd, 1, appointment!!.notes)
        dbHelper.updateAppointmentStatus(appointmentId, "completed")

        if (careRemarks.isNotBlank()) {
            val today = getStartOfDay(System.currentTimeMillis())
            dbHelper.addDailyCare(boardingId, today, careRemarks)
        }

        Toast.makeText(this, "确认入托成功，寄养记录ID=$boardingId", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}