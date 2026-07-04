package com.example.tieuluanandroids

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.CreateTagInput
import com.example.tieuluanandroids.model.Tag
import com.example.tieuluanandroids.model.UpdateTagInput
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Giao diện quản lý chủ đề (topic) cho Spinner.
 * Danh sách chủ đề được lưu bằng Room (ArrayList động lấy từ SmartCalendarData),
 * hỗ trợ Thêm / Sửa / Xóa chủ đề tự chọn (VD: đi chơi, đi ngủ,...).
 *
 * Khi danh sách thay đổi, [onTopicsChanged] sẽ được gọi để nơi gọi (PopupFragment)
 * có thể cập nhật lại Spinner của mình theo thời gian thực.
 */
class TagManagerDialogFragment : BottomSheetDialogFragment() {

    // Mảng động (ArrayList) chứa toàn bộ chủ đề hiện có, thay cho mảng String[] cố định
    private val danhSachChuDe: ArrayList<Tag> = ArrayList()

    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data

    var onTopicsChanged: ((List<Tag>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tag_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val edtNewTopic = view.findViewById<EditText>(R.id.edt_new_topic)
        val btnAddTopic = view.findViewById<Button>(R.id.btn_add_topic)
        val btnClose = view.findViewById<Button>(R.id.btn_close_topic_manager)

        btnAddTopic.setOnClickListener {
            val ten = edtNewTopic.text.toString().trim()
            if (ten.isEmpty()) {
                Toast.makeText(requireContext(), "Vui long nhap ten chu de!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val daTonTai = danhSachChuDe.any { it.name.equals(ten, ignoreCase = true) }
            if (daTonTai) {
                Toast.makeText(requireContext(), "Chu de nay da ton tai!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = data.createTag(CreateTagInput(name = ten))) {
                    is AppResult.Success -> {
                        edtNewTopic.setText("")
                        Toast.makeText(requireContext(), "Da them chu de \"$ten\"", Toast.LENGTH_SHORT).show()
                    }
                    is AppResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnClose.setOnClickListener { dismiss() }

        // Lắng nghe Room theo thời gian thực -> tự cập nhật ArrayList động + vẽ lại danh sách
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    danhSachChuDe.clear()
                    danhSachChuDe.addAll(tags)
                    veLaiDanhSachChuDe(view)
                    onTopicsChanged?.invoke(danhSachChuDe)
                }
            }
        }
    }

    /** Vẽ lại toàn bộ danh sách chủ đề (mỗi hàng có nút Sửa / Xóa) dựa trên ArrayList hiện tại */
    private fun veLaiDanhSachChuDe(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.layout_topic_list)
        val emptyText = rootView.findViewById<TextView>(R.id.text_topic_empty)
        container.removeAllViews()

        emptyText.visibility = if (danhSachChuDe.isEmpty()) View.VISIBLE else View.GONE

        for (chuDe in danhSachChuDe) {
            container.addView(taoHangChuDe(container.context, chuDe))
        }
    }

    /** Tạo 1 hàng gồm tên chủ đề + nút Sửa + nút Xóa */
    private fun taoHangChuDe(context: Context, chuDe: Tag): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 6; bottomMargin = 6 }
        }

        val tvName = TextView(context).apply {
            text = chuDe.name
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnSua = Button(context).apply {
            text = "Sửa"
            textSize = 13f
            isAllCaps = false
            setOnClickListener { hienDialogSuaChuDe(chuDe) }
        }

        val btnXoa = Button(context).apply {
            text = "Xóa"
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#D93025"))
            setOnClickListener { hienDialogXacNhanXoa(chuDe) }
        }

        row.addView(tvName)
        row.addView(btnSua)
        row.addView(btnXoa)
        return row
    }

    /** Hộp thoại Sửa tên chủ đề đã chọn */
    private fun hienDialogSuaChuDe(chuDe: Tag) {
        val edt = EditText(requireContext()).apply {
            setText(chuDe.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Sửa chủ đề")
            .setView(edt)
            .setPositiveButton("Lưu") { _, _ ->
                val tenMoi = edt.text.toString().trim()
                if (tenMoi.isEmpty()) {
                    Toast.makeText(requireContext(), "Ten chu de khong duoc de trong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = data.updateTag(
                        UpdateTagInput(
                            localId = chuDe.localId,
                            name = tenMoi
                        )
                    )) {
                        is AppResult.Success ->
                            Toast.makeText(requireContext(), "Da cap nhat chu de", Toast.LENGTH_SHORT).show()
                        is AppResult.Error ->
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /** Hộp thoại xác nhận Xóa chủ đề đã chọn */
    private fun hienDialogXacNhanXoa(chuDe: Tag) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa chủ đề")
            .setMessage("Bạn có chắc muốn xóa chủ đề \"${chuDe.name}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = data.deleteTag(chuDe.localId)) {
                        is AppResult.Success ->
                            Toast.makeText(requireContext(), "Da xoa chu de", Toast.LENGTH_SHORT).show()
                        is AppResult.Error ->
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}