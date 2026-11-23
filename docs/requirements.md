# Poke Poke — Yêu cầu

## 1. Tổng quan

Ứng dụng Android hiển thị thông tin Pokémon (thông tin, hình ảnh, kỹ năng, các cấp độ tiến hóa), dữ liệu lấy từ RESTful API: <https://fallendeity.github.io/pokeapi.co/>

Ứng dụng gồm các thành phần:

| Thành phần | Mô tả |
|---|---|
| Onboarding | Giới thiệu ngắn gọn về ứng dụng |
| Authentication | Đăng nhập bằng username/password |
| Home | Gồm 3 tab: **Home**, **Favourite**, **Me** |

## 2. Yêu cầu chức năng

### 2.1. Onboarding

- Chỉ hiển thị **lần đầu** sau khi cài app.

### 2.2. Authentication

- Đăng nhập bằng username/password.
- Hiện tại để đơn giản: dùng tài khoản mặc định `admin` / `admin`.
- Khi đăng nhập thành công: tạo ngẫu nhiên một token và lưu vào **secure storage**.

### 2.3. Tab Home

- **Danh sách Pokémon** theo thứ tự:
  - Tải trước 20 item.
  - Cuộn tới cuối danh sách thì tải tiếp 20 item (phân trang).
- **Tìm kiếm**:
  - Có thanh search để tìm Pokémon.
  - Tìm kiếm gần đúng (fuzzy): kết quả là danh sách các Pokémon gần giống từ khóa.
  - Tắt kết quả search thì quay lại danh sách hiện tại.
- Bấm vào một Pokémon sẽ mở màn hình **Details**.

### 2.4. Màn hình Details

- Hiển thị thông tin, hình ảnh, kỹ năng và các cấp độ tiến hóa của Pokémon.
- Có nút **add/remove favourite**.

### 2.5. Tab Favourite

- Danh sách Pokémon đã favourite.
- Mỗi item có nút **remove**.

### 2.6. Tab Me

- Hiển thị thông tin đăng nhập.
- Hiển thị thông tin phiên bản ứng dụng.
- Nút **Log out**.
- Nút **đổi ngôn ngữ** Anh / Việt.

### 2.7. Quy tắc đăng nhập

- Ứng dụng **không** yêu cầu đăng nhập ngay khi mở.
- Khi chưa đăng nhập:
  - Bấm vào item trong tab Home sẽ yêu cầu đăng nhập.
  - Tab Favourite / Me hiển thị nút yêu cầu đăng nhập.

## 3. Yêu cầu kỹ thuật

### 3.1. Kiến trúc

- Ứng dụng **multi-module**, mỗi module implement theo **Clean Architecture**.
- Mỗi module có 2 module con là `api` và `impl`:
  - Các module giao tiếp với nhau qua `api`.
  - Chỉ module `app` được biết về các module `impl`.
- Single hay multi activity: không bắt buộc, chọn theo best practice.

### 3.2. Testing

- Mỗi module phải có đầy đủ các loại test (**quan trọng**).

### 3.3. UI

- Bắt buộc dùng **Jetpack Compose**.
- Các UI dùng chung phải được tái sử dụng, thống nhất style.
- Thiết kế hỗ trợ **điện thoại gập** (foldable).
- Dùng **SplashScreen API** của Android để tăng tốc độ mở app.

### 3.4. Đa ngôn ngữ

- Hỗ trợ 2 ngôn ngữ: **English** (mặc định) và **Tiếng Việt**.
- Ngôn ngữ hiển thị theo ngôn ngữ máy; nếu máy dùng ngôn ngữ không được hỗ trợ thì dùng English.
- Đổi ngôn ngữ trong app **không cần restart** app.
- Luôn dùng `strings.xml`, **không hardcode** chuỗi.

### 3.5. Navigation

- Chuẩn bị sẵn điều hướng từ **push notification**, **deep link / app link**:
  - Có thể mở bất kỳ tab nào hoặc màn hình Details.
  - Nếu chưa đăng nhập thì phải đăng nhập xong mới mở màn hình đích.
