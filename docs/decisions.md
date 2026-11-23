# Decisions log

Ghi lại mọi quyết định phát sinh khi implement mà tài liệu chưa mô tả, hoặc khi phải lệch khỏi tài liệu (version, API thư viện khác...). Mới nhất ở trên.

| Ngày | Task | Quyết định | Lý do |
|---|---|---|---|
| 2026-11-23 | T01 | `compileSdk` của `:app` khai báo `release(37) { minorApiLevel = 1 }` (API 37.1); `jvm` của app đặt 21 | Compose UI 1.13.0 (BOM 2026.11.01) yêu cầu compile against 37.1 trở lên; build.md §1 đặt `jvm = "21"`. Sẽ chuyển vào convention plugin ở T02 (plugin phải dùng cùng `minorApiLevel = 1`). |
| 2026-11-23 | T01 | Thay `androidx-lifecycle-runtime-ktx` bằng `androidx-lifecycle-runtime-compose`; đổi alias test theo build.md §1.1 (`androidx-test-junit`, `espresso-core`) | Catalog mới theo build.md; `app` chỉ sửa tối thiểu để build. |
