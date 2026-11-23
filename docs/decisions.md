# Decisions log

Ghi lại mọi quyết định phát sinh khi implement mà tài liệu chưa mô tả, hoặc khi phải lệch khỏi tài liệu (version, API thư viện khác...). Mới nhất ở trên.

| Ngày | Task | Quyết định | Lý do |
|---|---|---|---|
| 2026-11-23 | T01 | `compileSdk` của `:app` khai báo `release(37) { minorApiLevel = 1 }` (API 37.1); `jvm` của app đặt 21 | Compose UI 1.13.0 (BOM 2026.11.01) yêu cầu compile against 37.1 trở lên; build.md §1 đặt `jvm = "21"`. Sẽ chuyển vào convention plugin ở T02 (plugin phải dùng cùng `minorApiLevel = 1`). |
| 2026-11-23 | T01 | Thay `androidx-lifecycle-runtime-ktx` bằng `androidx-lifecycle-runtime-compose`; đổi alias test theo build.md §1.1 (`androidx-test-junit`, `espresso-core`) | Catalog mới theo build.md; `app` chỉ sửa tối thiểu để build. |
| 2026-11-23 | T02 | Thêm `compileSdkMinor = "1"` vào catalog; convention plugin dùng `release(compileSdk) { minorApiLevel = compileSdkMinor }`. Thêm các alias `*-gradlePlugin` (library) và `pokepoke-*` (plugin, `version = "unspecified"`) vào catalog | Compose UI 1.13.0 cần compileSdk 37.1 (xem dòng T01); build-logic đọc cùng catalog |
| 2026-11-23 | T02 | Chuyển source `:app` từ `src/*/java` sang `src/*/kotlin` | Theo CLAUDE.md (Kotlin ở `src/main/kotlin`) |
| 2026-11-23 | T02 | Thêm `app/src/main/res/resources.properties` (`unqualifiedResLocale=en-US`) | `generateLocaleConfig = true` yêu cầu file này, không có thì `extractDebugSupportedLocales` fail |
| 2026-11-23 | T02 | Luật kiến trúc (`ModuleDependencyRules`) tách thành hàm thuần Kotlin, có unit test trong `build-logic/convention/src/test`; chạy bằng `./gradlew -p build-logic :convention:test` (không nằm trong `check` của root) | Module mới phải có test; included build không chạy test khi `check` ở root |
| 2026-11-23 | T02 | `pokepoke.android.library` thêm `testImplementation(project(":core:testing"))` (trừ chính nó): chỉ dùng được từ T03, chưa module library nào tồn tại ở T02. `pokepoke.kover` chỉ áp cho library/jvm, chưa áp cho `:app`; root aggregate `koverHtmlReport` làm ở T03/T23 khi đã có module | Chưa có module thật để kiểm chứng |
| 2026-11-23 | T02 | Release ký bằng debug keystore tạm (`signingConfigs["debug"]`) | build.md §3 |
