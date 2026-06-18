package com.example.tieuluanandroids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PopupFragment : BottomSheetDialogFragment() {
    /*b1 khai bao bien toan cuc*/
    private lateinit var textTitle: TextView
    private lateinit var spinnerTopic: Spinner
    private lateinit var editText: EditText
    private lateinit var timeBtn: Button
    private lateinit var saveBtn: Button

    /*b2 nap xml*/
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_popup, container, false)
    }

    /*b3 anh xa cac su kien*/
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitle = view.findViewById<TextView>(R.id.text_popup_title)
        spinnerTopic = view.findViewById<Spinner>(R.id.spinner_topic)
        editText = view.findViewById<EditText>(R.id.edt_event_content)
        timeBtn = view.findViewById<Button>(R.id.btn_edt_time)
        saveBtn = view.findViewById<Button>(R.id.btn_save_event)

        /* b4 viet code thuc thi */
        val istTopic = arrayOf("Học tập", "Công việc", "Cá nhân", "Giải trí")
    }
}