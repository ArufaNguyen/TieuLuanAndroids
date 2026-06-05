package com.example.tieuluanandroids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.databinding.MenuButtonsBinding
import com.google.android.material.snackbar.Snackbar

class MenuFragment : Fragment() {

    private var _binding: MenuButtonsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuButtonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_LoginFragment)
        }

        binding.buttonViewCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_MenuFragment_to_FirstFragment)
        }

        binding.button1.setOnClickListener {
            Snackbar.make(binding.root, "Button1", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
