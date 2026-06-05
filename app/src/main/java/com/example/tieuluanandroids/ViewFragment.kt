package com.example.tieuluanandroids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

// Bắt buộc phải kế thừa lớp Fragment()
class ViewFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp file XML man_hinh_chinh.xml (giao diện lịch của bạn) lên
        return inflater.inflate(R.layout.man_hinh_chinh, container, false)
    }
}