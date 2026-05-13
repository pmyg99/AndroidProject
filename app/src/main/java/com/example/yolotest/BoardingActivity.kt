package com.example.yolotest

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardingActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rgBoardingType: RadioGroup
    private lateinit var svWalkIn: ScrollView
    private lateinit var llAppointmentList: LinearLayout
    private lateinit var btnRealtimeDetect: Button
    private lateinit var btnSubmit: Button

    // 现场入托控件
    private lateinit var etPetName: EditText
    private lateinit var etPetSpecies: AutoCompleteTextView   // 确保是 AutoCompleteTextView
    private lateinit var etPetAge: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var tvBoardingTime: TextView
    private lateinit var tvCheckoutTime: TextView
    private lateinit var etOwnerName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBackupPhone: EditText

    private var startTime: Long = 0
    private var endTime: Long = 0

    // 预约列表控件
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointments = mutableListOf<Appointment>()

    companion object {
        private const val REQUEST_CAMERA = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupWalkIn()
        setupAppointmentList()
        setupRadioGroup()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        if (rgBoardingType.checkedRadioButtonId == R.id.rb_appointment) {
            loadAppointments(etSearch.text.toString())
        }
    }

    private fun initViews() {
        rgBoardingType = findViewById(R.id.rg_boarding_type)
        svWalkIn = findViewById(R.id.sv_walk_in)
        llAppointmentList = findViewById(R.id.ll_appointment_list)
        btnRealtimeDetect = findViewById(R.id.btn_take_photo)
        btnSubmit = findViewById(R.id.btn_submit)

        etPetName = findViewById(R.id.et_pet_name)
        etPetSpecies = findViewById(R.id.et_pet_species)   // 布局中必须为 AutoCompleteTextView
        etPetAge = findViewById(R.id.et_pet_age)
        rgGender = findViewById(R.id.rg_gender)
        tvBoardingTime = findViewById(R.id.tv_boarding_time)
        tvCheckoutTime = findViewById(R.id.tv_checkout_time)
        etOwnerName = findViewById(R.id.et_owner_name)
        etPhone = findViewById(R.id.et_phone)
        etBackupPhone = findViewById(R.id.et_backup_phone)

        etSearch = findViewById(R.id.et_search)
        btnSearch = findViewById(R.id.btn_search)
        rvAppointments = findViewById(R.id.rv_appointments)
        rvAppointments.layoutManager = LinearLayoutManager(this)
    }

    private fun showDateTimePicker(isStart: Boolean) {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                }
                if (isStart) {
                    startTime = cal.timeInMillis
                    tvBoardingTime.text = "入托时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(cal.time)}"
                } else {
                    endTime = cal.timeInMillis
                    tvCheckoutTime.text = "出托时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(cal.time)}"
                }
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupAppointmentList() {
        appointmentAdapter = AppointmentAdapter(appointments,
            onItemClick = { appointment ->
                val intent = Intent(this, ConfirmAppointmentActivity::class.java)
                intent.putExtra("appointment_id", appointment.id)
                startActivity(intent)
            },
            onDeleteClick = { appointment ->
                AlertDialog.Builder(this)
                    .setTitle("拒绝预约")
                    .setMessage("确定拒绝该预约吗？")
                    .setPositiveButton("确定") { _, _ ->
                        dbHelper.deleteAppointment(appointment.id)
                        loadAppointments(etSearch.text.toString())
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        rvAppointments.adapter = appointmentAdapter

        btnSearch.setOnClickListener {
            loadAppointments(etSearch.text.toString())
        }
        loadAppointments("")
    }

    private fun loadAppointments(keyword: String) {
        val list = if (keyword.isBlank()) {
            dbHelper.getPendingAppointments()
        } else {
            dbHelper.searchPendingAppointments(keyword)
        }
        appointments.clear()
        appointments.addAll(list)
        appointmentAdapter.notifyDataSetChanged()
    }

    private fun setupRadioGroup() {
        rgBoardingType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_walk_in -> {
                    svWalkIn.visibility = View.VISIBLE
                    llAppointmentList.visibility = View.GONE
                    btnRealtimeDetect.visibility = View.VISIBLE
                    btnSubmit.visibility = View.VISIBLE
                }
                R.id.rb_appointment -> {
                    svWalkIn.visibility = View.GONE
                    llAppointmentList.visibility = View.VISIBLE
                    btnRealtimeDetect.visibility = View.GONE
                    btnSubmit.visibility = View.GONE
                    loadAppointments(etSearch.text.toString())
                }
            }
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
        btnRealtimeDetect.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivityForResult(intent, REQUEST_CAMERA)
        }
        btnSubmit.setOnClickListener { submitWalkInBoarding() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val detectedSpecies = data?.getStringExtra("detected_species")
            if (!detectedSpecies.isNullOrEmpty()) {
                etPetSpecies.setText(detectedSpecies)
                Toast.makeText(this, "识别品种：$detectedSpecies", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未识别到品种，请手动选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitWalkInBoarding() {
        val petName = etPetName.text.toString()
        if (petName.isBlank()) {
            Toast.makeText(this, "请填写宠物姓名", Toast.LENGTH_SHORT).show()
            return
        }
        val species = etPetSpecies.text.toString()
        val age = etPetAge.text.toString().toIntOrNull() ?: 0
        val gender = if (rgGender.checkedRadioButtonId == R.id.rb_male) "公" else "母"
        val ownerName = etOwnerName.text.toString()
        if (ownerName.isBlank()) {
            Toast.makeText(this, "请填写主人姓名", Toast.LENGTH_SHORT).show()
            return
        }
        val phone = etPhone.text.toString()
        if (phone.isBlank()) {
            Toast.makeText(this, "请填写联系方式", Toast.LENGTH_SHORT).show()
            return
        }
        val backupPhone = etBackupPhone.text.toString()
        if (startTime == 0L) {
            Toast.makeText(this, "请选择入托时间", Toast.LENGTH_SHORT).show()
            return
        }
        if (endTime == 0L) {
            Toast.makeText(this, "请选择出托时间", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerId = dbHelper.insertOwner(ownerName, phone, backupPhone)
        val petId = dbHelper.insertPet(ownerId, petName, species, age, gender)
        val boardingId = dbHelper.insertBoardingRecord(petId, startTime, endTime, 1, "")
        Toast.makeText(this, "登记成功，寄养记录ID=$boardingId", Toast.LENGTH_SHORT).show()
        clearForm()
    }

    private fun setupWalkIn() {
        // 原始完整品种列表（不可变）
        val allSpecies = resources.getStringArray(R.array.pet_species_array).toList()
        // 自定义适配器，实现包含匹配的过滤器
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
        etPetSpecies.setAdapter(adapter)
        etPetSpecies.threshold = 1
        // 设置下拉背景不透明
        etPetSpecies.setDropDownBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
        )
        etPetSpecies.dropDownHeight = 400

        // 其余代码（时间设置等）保持不变...
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 5)
        startTime = now.timeInMillis
        tvBoardingTime.text = "入托时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(now.time)}"
        endTime = 0
        tvCheckoutTime.text = "点击选择出托时间"
        tvBoardingTime.setOnClickListener { showDateTimePicker(true) }
        tvCheckoutTime.setOnClickListener { showDateTimePicker(false) }
    }

    private fun clearForm() {
        etPetName.text.clear()
        etPetSpecies.text.clear()
        etPetAge.text.clear()
        rgGender.clearCheck()
        tvBoardingTime.text = "点击选择入托时间"
        tvCheckoutTime.text = "点击选择出托时间"
        etOwnerName.text.clear()
        etPhone.text.clear()
        etBackupPhone.text.clear()
        startTime = 0
        endTime = 0
    }
}