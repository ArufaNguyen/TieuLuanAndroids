package com.example.tieuluanandroids.ui.tags

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.AppResult
import com.example.tieuluanandroids.model.CreateTagInput
import com.example.tieuluanandroids.model.Tag
import com.example.tieuluanandroids.model.UpdateTagInput
import com.example.tieuluanandroids.model.service.SmartCalendarData
import kotlinx.coroutines.launch

class TagManagerFragment : Fragment() {

    private lateinit var nameInput: EditText
    private lateinit var colorGroup: RadioGroup
    private lateinit var colorHexInput: EditText
    private lateinit var saveButton: Button
    private lateinit var searchInput: EditText
    private lateinit var tagList: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var closeButton: Button

    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var currentTags: List<Tag> = emptyList()
    private var editingTag: Tag? = null
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tag_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameInput = view.findViewById(R.id.edt_new_topic)
        colorGroup = view.findViewById(R.id.radio_tag_color_group)
        colorHexInput = view.findViewById(R.id.edt_tag_color_hex)
        saveButton = view.findViewById(R.id.btn_add_topic)
        searchInput = view.findViewById(R.id.edt_search_topic)
        tagList = view.findViewById(R.id.layout_topic_list)
        emptyText = view.findViewById(R.id.text_topic_empty)
        closeButton = view.findViewById(R.id.btn_close_topic_manager)

        colorGroup.setOnCheckedChangeListener { _, checkedId ->
            colorHexInput.setText(colorForCheckedId(checkedId))
        }
        if (colorHexInput.text.isBlank()) {
            colorHexInput.setText(colorForCheckedId(colorGroup.checkedRadioButtonId))
        }
        saveButton.setOnClickListener { saveTag() }
        closeButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                renderTags()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        renderTags()
        observeTags()
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                data.observeTags().collect { tags ->
                    currentTags = tags
                    renderTags()
                }
            }
        }
    }

    private fun saveTag() {
        val name = nameInput.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Ten tag dang trong", Toast.LENGTH_SHORT).show()
            return
        }
        val color = normalizeColor(colorHexInput.text.toString())

        viewLifecycleOwner.lifecycleScope.launch {
            saveButton.isEnabled = false
            val target = editingTag
            val result = if (target == null) {
                data.createTag(CreateTagInput(name = name, color = color))
            } else {
                data.updateTag(UpdateTagInput(localId = target.localId, name = name, color = color))
            }
            saveButton.isEnabled = true

            when (result) {
                is AppResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        if (target == null) "Da luu tag" else "Da cap nhat tag",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearEditor()
                }
                is AppResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun editTag(tag: Tag) {
        editingTag = tag
        nameInput.setText(tag.name)
        colorHexInput.setText(tag.color?.takeIf { it.isNotBlank() } ?: DEFAULT_COLOR)
        saveButton.text = "Cap nhat tag"
    }

    private fun deleteTag(tag: Tag) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = data.deleteTag(tag.localId)) {
                is AppResult.Success -> {
                    Toast.makeText(requireContext(), "Da xoa tag", Toast.LENGTH_SHORT).show()
                    if (editingTag?.localId == tag.localId) clearEditor()
                }
                is AppResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderTags() {
        if (!::tagList.isInitialized) return
        val filtered = currentTags
            .filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        tagList.removeAllViews()
        emptyText.isVisible = filtered.isEmpty()
        filtered.forEach { tag ->
            tagList.addView(tagRow(tag))
        }
    }

    private fun tagRow(tag: Tag): View {
        val padding = dp(12)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(padding, padding, padding, padding)
            background = rowBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }

            addView(View(context).apply {
                setBackgroundColor(parseTagColor(tag.color))
            }, LinearLayout.LayoutParams(dp(12), dp(42)))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(context).apply {
                    text = tag.name.ifBlank { "Untitled tag" }
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.rgb(17, 24, 39))
                })
                addView(TextView(context).apply {
                    text = buildMetaText(tag)
                    textSize = 12f
                    setTextColor(Color.rgb(107, 114, 128))
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            addView(Button(context).apply {
                text = "Sua"
                textSize = 12f
                isAllCaps = false
                setOnClickListener { editTag(tag) }
            }, LinearLayout.LayoutParams(dp(64), dp(40)))

            addView(Button(context).apply {
                text = "Xoa"
                textSize = 12f
                isAllCaps = false
                setOnClickListener { deleteTag(tag) }
            }, LinearLayout.LayoutParams(dp(64), dp(40)).apply { leftMargin = dp(6) })
        }
    }

    private fun clearEditor() {
        editingTag = null
        nameInput.text?.clear()
        colorHexInput.setText(colorForCheckedId(colorGroup.checkedRadioButtonId))
        saveButton.text = "Them tag"
    }

    private fun buildMetaText(tag: Tag): String {
        val parts = mutableListOf<String>()
        parts += tag.color?.takeIf { it.isNotBlank() } ?: DEFAULT_COLOR
        if (tag.syncStatus.name != "SYNCED") parts += tag.syncStatus.name
        return parts.joinToString("  |  ")
    }

    private fun colorForCheckedId(checkedId: Int): String = when (checkedId) {
        R.id.radio_tag_color_green -> "#059669"
        R.id.radio_tag_color_orange -> "#D97706"
        else -> DEFAULT_COLOR
    }

    private fun normalizeColor(value: String): String? {
        val trimmed = value.trim().ifBlank { return null }
        val normalized = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return normalized.takeIf { runCatching { Color.parseColor(it) }.isSuccess }
    }

    private fun parseTagColor(value: String?): Int {
        return runCatching { Color.parseColor(value ?: DEFAULT_COLOR) }
            .getOrDefault(Color.parseColor(DEFAULT_COLOR))
    }

    private fun rowBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), Color.rgb(228, 234, 242))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_COLOR = "#2563EB"
    }
}
