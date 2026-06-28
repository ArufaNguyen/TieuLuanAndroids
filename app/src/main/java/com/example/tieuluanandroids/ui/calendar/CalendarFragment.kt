package com.example.tieuluanandroids.ui.calendar

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import com.example.tieuluanandroids.ui.ViewModelFactory

import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository

import com.example.tieuluanandroids.databinding.CalendarLayoutBinding


class CalendarFragment : Fragment(){

    private var _binding: CalendarLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels {
        ViewModelFactory {
            CalendarViewModel(
                (requireActivity().application as SmartCalendarApplication).repository
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}