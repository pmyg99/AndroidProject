package com.example.yolotest

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.*

class SpeciesDropDownHelper(
    private val context: Context,
    private val editText: AppCompatEditText,
    private val allSpecies: List<String>,
    private val onItemSelected: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var adapter: ArrayAdapter<String>? = null
    private var currentFilterJob: Job? = null

    init {
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s?.toString().orEmpty().trim()
                filterAndShow(input)
            }
        })
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !editText.text.isNullOrEmpty()) {
                filterAndShow(editText.text.toString())
            } else {
                dismissPopup()
            }
        }
    }

    private fun filterAndShow(input: String) {
        currentFilterJob?.cancel()
        currentFilterJob = CoroutineScope(Dispatchers.IO).launch {
            val filtered = if (input.isEmpty()) {
                allSpecies
            } else {
                allSpecies.filter { it.contains(input, ignoreCase = true) }
            }
            withContext(Dispatchers.Main) {
                showPopup(filtered)
            }
        }
    }

    private fun showPopup(filtered: List<String>) {
        if (filtered.isEmpty()) {
            dismissPopup()
            return
        }
        if (popupWindow == null) {
            val listView = ListView(context)
            adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, filtered)
            listView.adapter = adapter
            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val selected = filtered[position]
                editText.setText(selected)
                editText.setSelection(selected.length)
                onItemSelected(selected)
                dismissPopup()
            }
            // 兼容所有 API 的 PopupWindow 构造
            popupWindow = PopupWindow(listView, editText.width, 300, true)
        } else {
            adapter?.clear()
            adapter?.addAll(filtered)
            adapter?.notifyDataSetChanged()
        }
        if (!popupWindow!!.isShowing) {
            popupWindow?.showAsDropDown(editText, 0, 0)
        }
    }

    private fun dismissPopup() {
        popupWindow?.dismiss()
        popupWindow = null
        adapter = null
    }
}