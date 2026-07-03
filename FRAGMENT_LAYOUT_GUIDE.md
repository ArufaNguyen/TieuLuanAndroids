# Hướng dẫn tạo Fragment và layout tương ứng

Tài liệu này dùng cho project Android hiện tại, với Fragment viết bằng Kotlin, layout XML trong `res/layout`, và điều hướng bằng Navigation Component trong `res/navigation/nav_graph.xml`.

## 1. Quy ước đặt tên

Nên tạo theo cặp tên thống nhất:

- Fragment Kotlin: `FeatureFragment.kt`
- Package UI: `app/src/main/java/com/example/tieuluanandroids/ui/feature/`
- Layout XML: `fragment_feature.xml`
- ID trong navigation: `@+id/FeatureFragment`
- String label: `@string/feature_fragment_label`

Ví dụ với màn hình "Profile":

- `app/src/main/java/com/example/tieuluanandroids/ui/profile/ProfileFragment.kt`
- `app/src/main/res/layout/fragment_profile.xml`
- Fragment ID: `ProfileFragment`

## 2. Tạo file layout XML

Tạo file trong:

```text
app/src/main/res/layout/fragment_profile.xml
```

Mẫu cơ bản:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    tools:context=".ui.profile.ProfileFragment">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="24dp">

        <TextView
            android:id="@+id/text_profile_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="@string/profile_title"
            android:textAppearance="@style/TextAppearance.MaterialComponents.Headline5"
            android:textStyle="bold"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <Button
            android:id="@+id/button_profile_action"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/profile_action"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/text_profile_title" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.core.widget.NestedScrollView>
```

Lưu ý:

- ID view nên viết theo dạng `text_...`, `button_...`, `layout_...`, `list_...`.
- Text hiển thị nên đưa vào `app/src/main/res/values/strings.xml`.
- Nếu màn hình có nội dung dài, dùng `NestedScrollView` như các layout hiện có.
- Nếu màn hình đơn giản, có thể dùng `ConstraintLayout` làm root.

## 3. Thêm string resource

Mở:

```text
app/src/main/res/values/strings.xml
```

Thêm các chuỗi cần dùng:

```xml
<string name="profile_fragment_label">Profile</string>
<string name="profile_title">Profile</string>
<string name="profile_action">Refresh</string>
```

Dùng string resource giúp tránh hard-code text trong layout và dễ sửa về sau.

## 4. Tạo class Fragment

Tạo file:

```text
app/src/main/java/com/example/tieuluanandroids/ui/profile/ProfileFragment.kt
```

Mẫu cơ bản theo style project:

```kotlin
package com.example.tieuluanandroids.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tieuluanandroids.R
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private lateinit var textProfileTitle: TextView
    private lateinit var buttonProfileAction: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textProfileTitle = view.findViewById(R.id.text_profile_title)
        buttonProfileAction = view.findViewById(R.id.button_profile_action)

        buttonProfileAction.setOnClickListener {
            Snackbar.make(requireView(), "Profile action", Snackbar.LENGTH_SHORT).show()
        }
    }
}
```

Nếu cần truy cập data chung của app, dùng pattern đang có:

```kotlin
private val data: SmartCalendarData
    get() = (requireActivity().application as SmartCalendarApplication).data
```

Và import:

```kotlin
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.service.SmartCalendarData
```

## 5. Khai báo Fragment trong nav_graph.xml

Mở:

```text
app/src/main/res/navigation/nav_graph.xml
```

Thêm node fragment vào trong tag `<navigation>`:

```xml
<fragment
    android:id="@+id/ProfileFragment"
    android:name="com.example.tieuluanandroids.ui.profile.ProfileFragment"
    android:label="@string/profile_fragment_label"
    tools:layout="@layout/fragment_profile" />
```

Nếu Fragment này được mở từ `MenuFragment`, thêm action bên trong node `MenuFragment`:

```xml
<action
    android:id="@+id/action_MenuFragment_to_ProfileFragment"
    app:destination="@id/ProfileFragment" />
```

Sau đó trong code menu, gọi:

```kotlin
findNavController().navigate(R.id.action_MenuFragment_to_ProfileFragment)
```

Cần import:

```kotlin
import androidx.navigation.fragment.findNavController
```

## 6. Điều hướng từ Fragment này sang Fragment khác

Nếu `ProfileFragment` cần đi sang `EventsFragment`, thêm action bên trong node `ProfileFragment`:

```xml
<fragment
    android:id="@+id/ProfileFragment"
    android:name="com.example.tieuluanandroids.ui.profile.ProfileFragment"
    android:label="@string/profile_fragment_label"
    tools:layout="@layout/fragment_profile">

    <action
        android:id="@+id/action_ProfileFragment_to_EventsFragment"
        app:destination="@id/EventsFragment" />
</fragment>
```

Trong `ProfileFragment.kt`:

```kotlin
findNavController().navigate(R.id.action_ProfileFragment_to_EventsFragment)
```

## 7. Mẫu function thường dùng trong Fragment

Các function dưới đây là mẫu hay dùng khi tạo Fragment mới. Có thể copy vào class Fragment rồi đổi tên view, string, action theo màn hình cần làm.

### Bind view

Tách phần `findViewById` ra function riêng để `onViewCreated()` gọn hơn:

```kotlin
private fun bindViews(view: View) {
    textProfileTitle = view.findViewById(R.id.text_profile_title)
    buttonProfileAction = view.findViewById(R.id.button_profile_action)
}
```

Dùng trong `onViewCreated()`:

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bindViews(view)
    setupListeners()
}
```

### Setup listener

Gom các sự kiện click vào một function:

```kotlin
private fun setupListeners() {
    buttonProfileAction.setOnClickListener {
        refreshProfile()
    }
}
```

### Hiển thị thông báo

Dùng `Snackbar` giống các Fragment hiện có:

```kotlin
private fun showMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(requireView(), message, duration).show()
}
```

Ví dụ gọi:

```kotlin
showMessage("Cập nhật thành công")
showMessage("Không thể tải dữ liệu", Snackbar.LENGTH_LONG)
```

### Set loading

Dùng khi bấm nút gọi API, sync dữ liệu, hoặc xử lý tác vụ lâu:

```kotlin
private var isLoading = false

private fun setLoading(loading: Boolean) {
    isLoading = loading
    buttonProfileAction.isEnabled = !loading
    buttonProfileAction.setText(
        if (loading) R.string.loading else R.string.profile_action
    )
}
```

Nếu chưa có string `loading`, thêm vào `strings.xml`:

```xml
<string name="loading">Loading...</string>
```

### Gọi tác vụ bất đồng bộ

Dùng `viewLifecycleOwner.lifecycleScope.launch` để coroutine tự gắn với vòng đời của Fragment:

```kotlin
private fun refreshProfile() {
    if (isLoading) return

    viewLifecycleOwner.lifecycleScope.launch {
        setLoading(true)

        val result = data.syncNow()

        setLoading(false)
        showMessage(result.toString(), Snackbar.LENGTH_LONG)
    }
}
```

Cần import:

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
```

### Observe dữ liệu

Nếu data trả về `Flow`, dùng `repeatOnLifecycle` để chỉ collect khi Fragment đang STARTED:

```kotlin
private fun observeProfile() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            data.observeEvents().collect { events ->
                render(events)
            }
        }
    }
}
```

Cần import:

```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
```

### Render UI

Tách render ra function riêng để dễ cập nhật giao diện theo state:

```kotlin
private fun render(events: List<Event>) {
    textProfileTitle.text = resources.getQuantityString(
        R.plurals.events_loaded_count,
        events.size,
        events.size
    )
}
```

### Điều hướng

Function riêng giúp code click dễ đọc hơn:

```kotlin
private fun openEvents() {
    findNavController().navigate(R.id.action_ProfileFragment_to_EventsFragment)
}
```

Cần import:

```kotlin
import androidx.navigation.fragment.findNavController
```

### Mẫu Fragment đầy đủ hơn

```kotlin
package com.example.tieuluanandroids.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tieuluanandroids.R
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var textProfileTitle: TextView
    private lateinit var buttonProfileAction: Button
    private val data: SmartCalendarData
        get() = (requireActivity().application as SmartCalendarApplication).data
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupListeners()
    }

    private fun bindViews(view: View) {
        textProfileTitle = view.findViewById(R.id.text_profile_title)
        buttonProfileAction = view.findViewById(R.id.button_profile_action)
    }

    private fun setupListeners() {
        buttonProfileAction.setOnClickListener {
            refreshProfile()
        }
    }

    private fun refreshProfile() {
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = data.syncNow()
            setLoading(false)
            showMessage(result.toString(), Snackbar.LENGTH_LONG)
        }
    }

    private fun openEvents() {
        findNavController().navigate(R.id.action_ProfileFragment_to_EventsFragment)
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        buttonProfileAction.isEnabled = !loading
    }

    private fun showMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(requireView(), message, duration).show()
    }
}
```

## 8. Checklist sau khi tạo

- Đã tạo file layout trong `app/src/main/res/layout/`.
- Đã tạo class Fragment trong package `ui/...`.
- `onCreateView()` inflate đúng layout.
- `onViewCreated()` bind view bằng `view.findViewById(...)`.
- Đã thêm text vào `strings.xml` nếu layout có text hiển thị.
- Đã khai báo Fragment trong `nav_graph.xml`.
- Đã thêm action điều hướng nếu cần mở Fragment từ màn hình khác.
- Đã build project để bắt lỗi sai ID, sai package, hoặc sai resource.

Lệnh build nhanh:

```powershell
.\gradlew.bat :app:assembleDebug
```

## 9. Lỗi thường gặp

- `Unresolved reference: fragment_profile`: tên file layout chưa đúng hoặc chưa sync Gradle.
- `Unresolved reference: text_profile_title`: ID trong XML khác ID trong Kotlin.
- App crash khi mở màn hình: `android:name` trong `nav_graph.xml` sai package class.
- Bấm nút không chuyển màn hình: chưa thêm action trong `nav_graph.xml` hoặc gọi sai action ID.
- Text bị hard-code: nên chuyển sang `strings.xml`.
