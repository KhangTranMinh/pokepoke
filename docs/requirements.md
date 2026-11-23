# Yêu cầu

1. Tôi muốn làm ứng dụng hiển thị Pokemon (thông tin, hình ảnh, kỹ năng, các cấp độ tiến hóa) theo
Restful API https://fallendeity.github.io/pokeapi.co/
2. Ứng dụng gồm có các thành phần sau:
  - Onboarding: giới thiệu ứng dụng ngắn gọn
  - Authentication: đăng nhập bằng username/password (hiện tại cho đơn giản thì sẽ dùng 
username/password mặc định là admin/admin và tạo ngẫu nhiên token lưu vào secure storage)
  - Home: gồm 3 tab (Home, Favourite, Me). Mô tả Home ở dòng dưới
3. Tab Home
  - danh sách pokemon theo thứ tự (preload 20, scroll cuối trang sẽ load tiếp 20...)
  - có thanh search để tìm pokemon, search dạng gần đúng, kết quả là danh sách cách pokemon 
gần giống từ khóa. tắt kết quả search sẽ trở về danh sách hiện tại
  - bấm vào 1 pokemon sẽ ra màn hình Details
  - màn hình Details sẽ gồm thông tin, hình ảnh, kỹ năng, các cấp độ tiến hóa, có nút để 
add/remove favourite
4. Tab Favourite
  - danh sách favourite, có nút remove trong từng item
5. Tab Me
  - hiện thông tin đăng nhập, thông tin phiên bản và nút log out, nút đổi ngôn ngữ anh / việt
6. Ứng dụng ko yêu cầu đăng nhập ngay, nếu chưa đăng nhập thì bấm vào item trong tab home sẽ bắt
đăng nhập, vào tab favourite / me sẽ có nút yêu cầu đăng nhập. onboarding chỉ hiện lần đầu cài app.
7. Yêu cầu kĩ thuật
  - Ứng dụng multi-module, mỗi module implement theo clean arch, bao gồm các loại test (quan trọng)
  - dùng splashscreen api của android để tăng tốc độ mở app
  - ứng dụng có 2 ngôn ngữ trong app, ngôn ngữ hiển thị theo ngôn ngữ máy (nếu ko có 
thì dùng default en), đổi ngôn ngữ ko cần restart app, phải luôn dùng strings.xml, ko hardcode
  - từng module phải có 2 module con là api / impl, các module giao tiếp qua api (chỉ có module app 
biết về các module impl)
  - các ui dùng chung phải đc reuse, thống nhất style, thiết kế cho điện thoại gập, ko bắt buộc
single / multi activity (theo best practice). phải dùng compose ui
  - chuẩn bị sẵn navigation từ push notification, deep link / app link (ví dụ có thể mở bất kỳ tab
nào hoặc màn hình details, nếu chưa authen thì phải authen rồi mới đc mở)