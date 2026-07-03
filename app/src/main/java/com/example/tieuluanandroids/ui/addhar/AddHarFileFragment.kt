package com.example.tieuluanandroids.ui.addhar

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddHarFileFragment : Fragment() {

    private lateinit var buttonSelectFile: Button
    private lateinit var buttonDiscoveryJob: Button
    private lateinit var textFileStatus: TextView
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isUploading = false

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
        return inflater.inflate(R.layout.add_har_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSelectFile = view.findViewById(R.id.button_select_file)
        buttonDiscoveryJob = view.findViewById(R.id.button_discovery_job)
        textFileStatus = view.findViewById(R.id.text_file_status)

        buttonSelectFile.setOnClickListener {
            selectFile.launch(arrayOf("*/*"))
        }
        buttonDiscoveryJob.setOnClickListener {
            findNavController().navigate(
                R.id.action_addHarFileFragment_to_discoveryJobsFragment
            )
        }
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
                upload(fileName, bytes)
            }.onFailure { error ->
                showUploadStatus(UploadStatus.ERROR)
                Snackbar.make(
                    requireView(),
                    error.message ?: "Cannot read selected file",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun upload(fileName: String, bytes: ByteArray) {
        if (isUploading) return

        viewLifecycleOwner.lifecycleScope.launch {
            isUploading = true
            showUploadStatus(UploadStatus.UPLOADING)
            val result = data.uploadHar(fileName, bytes)
            showUploadStatus(if (result.success) UploadStatus.SUCCESS else UploadStatus.ERROR)
            Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG).show()
            isUploading = false
        }
    }

    private fun showUploadStatus(status: UploadStatus) {
        buttonSelectFile.isEnabled = status != UploadStatus.UPLOADING
        textFileStatus.setText(
            when (status) {
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

}

private enum class UploadStatus { IDLE, UPLOADING, SUCCESS, ERROR }
