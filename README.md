# SnapTask (Locket × Notion) - Android Application

> **Snap-to-Do & Visual Task Manager**  
> Ứng dụng ghi chú việc cần làm bằng hình ảnh trực quan, lấy cảm hứng từ camera & widget tương tác tức thì của **Locket** kết hợp với cấu trúc phân loại khoa học của **Notion**. Tối ưu hóa đặc biệt cho các dòng máy Android phân khúc phổ thông / giá rẻ (Low-End Devices).

---

## 📸 1. Triết lý Sản phẩm: Locket × Notion

* **Từ Locket:**
  * **Camera-First:** Mở app là có thể bấm chụp ngay lập tức (<0.5s).
  * **Interactive Home Widget:** Đưa hình ảnh việc cần làm trực tiếp lên màn hình chính. Bấm "✓ Xong ngay" trực tiếp trên Widget mà không cần mở app.
  * **Visual Feed:** Xem danh sách việc dưới dạng lưới ảnh trực quan, dễ liên tưởng hơn text thuần túy.
* **Từ Notion:**
  * **Cấu trúc dữ liệu Block:** Mỗi ảnh chụp là một Cover Image gắn liền với Tiêu đề, Danh mục (Tags), Mức độ ưu tiên (Priority), Hạn chót (Due Date) và Danh sách công việc con (Subtasks Checklist).
  * **Đa góc nhìn (Multi-view):** Chuyển đổi linh hoạt giữa giao diện Lưới ảnh (Grid Feed) và Bảng Kanban (Board View: Cần làm - Đang làm - Đã xong).

---

## ⚡ 2. Chiến lược Tối ưu hóa cho Máy Low-End (1GB - 3GB RAM)

1. **Chống tràn RAM (OOM) khi chụp ảnh:**
   * CameraX được cấu hình chế độ `CAPTURE_MODE_MINIMIZE_LATENCY`.
   * Sử dụng thuật toán `inSampleSize` giảm độ phân giải cảm biến thô (48MP/64MP) xuống tối đa 1280px ngay trong bộ nhớ trước khi xử lý Bitmap.
   * Định dạng màu `RGB_565` (16-bit) giúp tiết kiệm 50% RAM so với `ARGB_8888`.
2. **Nén ảnh chuẩn WebP:**
   * Tự động nén ảnh chụp sang WebP chất lượng 78%, giảm dung lượng tệp từ ~5MB xuống chỉ còn **120KB - 250KB** (tiết kiệm 90% bộ nhớ trong).
   * Tự động sinh thêm ảnh Thumbnail (360px WebP, ~20KB) riêng cho Widget và Grid View.
3. **Quản lý Cache & Thu dọn bộ nhớ:**
   * Giới hạn RAM Cache của thư viện Coil tối đa 15% RAM khả dụng.
   * Lắng nghe tín hiệu `onTrimMemory()` của hệ điều hành để dọn sạch bộ đệm Bitmap ngay khi máy có dấu hiệu thiếu RAM.
4. **Tối ưu kích thước file APK (< 12MB):**
   * Bật R8 Full Mode & ProGuard loại bỏ code thừa.
   * Bật **ABI Splits** tạo riêng 2 gói APK tối ưu: `armeabi-v7a` (32-bit đời cũ) và `arm64-v8a` (64-bit).

---

## 🛠️ 3. Cấu trúc Dự án (Architecture)

```
snaptask-android/
├── .github/workflows/
│   └── build_apk.yml           # CI/CD tự động build APK trên GitHub Actions
├── app/
│   ├── src/main/
│   │   ├── java/com/snaptask/app/
│   │   │   ├── SnapTaskApplication.kt    # Khởi tạo DB, Coil cache, xử lý Low Memory
│   │   │   ├── data/
│   │   │   │   ├── local/               # Room Entity, DAO, AppDatabase, Converters
│   │   │   │   └── repository/          # TaskRepository, TaskRepositoryImpl (Offline-first)
│   │   │   ├── domain/model/            # Task, TaskPriority, TaskStatus, TaskTag, SubTask
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt      # Điều hướng Compose, cấp quyền Camera
│   │   │   │   ├── theme/               # Design System: Color, Type, Theme (Anti-Slop)
│   │   │   │   └── screens/
│   │   │   │       ├── home/            # Grid View, Tag Filter, Locket Camera FAB
│   │   │   │       ├── camera/          # CameraX Viewfinder độ trễ thấp
│   │   │   │       ├── editor/          # Quick Snap Modal Bottom Sheet
│   │   │   │       └── board/           # Kanban Board View (To-Do / In Progress / Done)
│   │   │   ├── widget/                  # Jetpack Glance Home Screen Widget & Action Callback
│   │   │   └── util/                    # ImageCompressor, FileUtils
│   │   └── res/                         # Strings, Colors, Themes, AppWidgetProvider XML
│   ├── build.gradle.kts                 # Cấu hình ABI splits, R8, Jetpack Compose
│   └── proguard-rules.pro               # Quy tắc tối ưu hóa ProGuard/R8
├── build_apk.ps1                        # Script kiểm tra môi trường và build tự động
└── README.md
```

---

## 🚀 4. Hướng dẫn Build APK Tự Động

### Cách 1: Tự động hoàn toàn qua GitHub Actions (Khuyên dùng - Không cần cài gì trên máy)
1. Đẩy mã nguồn dự án lên một kho lưu trữ GitHub bất kỳ (Private hoặc Public).
2. GitHub Actions sẽ tự động kích hoạt workflow `.github/workflows/build_apk.yml`.
3. Sau khoảng 2-3 phút, truy cập tab **Actions** $\rightarrow$ Click vào bản build mới nhất $\rightarrow$ Tải trực tiếp file `SnapTask-Release-APK` hoặc `SnapTask-Debug-APK` về điện thoại để cài đặt.

### Cách 2: Build bằng Android Studio trên máy
1. Mở thư mục `snaptask-android` bằng **Android Studio**.
2. Đợi Gradle đồng bộ các thư viện (Sync Project with Gradle Files).
3. Chọn menu **Build** $\rightarrow$ **Build Bundle(s) / APK(s)** $\rightarrow$ **Build APK(s)**.

### Cách 3: Chạy script PowerShell
Mở PowerShell tại thư mục dự án và chạy:
```powershell
.\build_apk.ps1
```
