package com.example.tieuluanandroids.ui.addhar

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.databinding.AddHarFileBinding
import com.example.tieuluanandroids.ui.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddHarFileFragment : Fragment() {

    private var _binding: AddHarFileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddHarFileViewModel by viewModels {
        ViewModelFactory {
            AddHarFileViewModel(
                (requireActivity().application as SmartCalendarApplication).repository
            )
        }
    }

    private val selectFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) upload(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddHarFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSelectFile.setOnClickListener {
            selectFile.launch(arrayOf("*/*"))
        }
        binding.buttonDiscoveryJob.setOnClickListener {
            findNavController().navigate(
                R.id.action_addHarFileFragment_to_discoveryJobsFragment
            )
        }
        observeViewModel()
    }

    private fun upload(uri: Uri) {
        val resolver = requireContext().contentResolver
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedFile = withContext(Dispatchers.IO) {
                runCatching {
                    val fileName = resolver.query(uri, null, null, null, null).use(::displayName)
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Cannot read selected file")
                    fileName to bytes
                }
            }

            selectedFile.onSuccess { (fileName, bytes) ->
                viewModel.upload(fileName, bytes)
            }.onFailure { error ->
                viewModel.reportReadError(error.message ?: "Cannot read selected file")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.messages.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun render(state: AddHarFileUiState) {
        binding.buttonSelectFile.isEnabled = state.status != UploadStatus.UPLOADING
        binding.textFileStatus.setText(
            when (state.status) {
                UploadStatus.IDLE -> R.string.no_file_added
                UploadStatus.UPLOADING -> R.string.uploading_har_file
                UploadStatus.SUCCESS -> R.string.added_file
                UploadStatus.ERROR -> R.string.har_upload_failed
            }
        )
    }

    private fun displayName(cursor: Cursor?): String {
        if (cursor == null || !cursor.moveToFirst()) return "capture.har"
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        return if (index >= 0) cursor.getString(index) else "capture.har"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
