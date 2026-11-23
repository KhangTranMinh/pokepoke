# Tasks

> Làm **tuần tự** từ T01. Mỗi task: đọc các tài liệu được chỉ định → implement → chạy **DoD** → đánh dấu `[x]` ở đây → **đúng 1 commit cho task đó** (message `Txx: <tóm tắt>`) → push → **dừng, chờ user yêu cầu làm task tiếp**. Quy tắc đầy đủ ở `CLAUDE.md`. Không chuyển task khi DoD chưa xanh.
>
> Thứ tự ưu tiên tài liệu khi mâu thuẫn: `contracts.md` > `screens.md` > `designsystem.md` / `deeplinks.md` / `build.md` > `plan.md` > `requirements.md`. Điểm chưa được mô tả: chọn phương án đơn giản nhất nhất quán với tài liệu, ghi vào [`decisions.md`](decisions.md), **không dừng lại để hỏi**.
>
> DoD chung cho mọi task (ngoài DoD riêng): `./gradlew assembleDebug` xanh; không có string hardcode trong UI; module mới có test trong chính module đó.

## Giai đoạn 1 — Build

- [x] **T01 — Version catalog & toolchain.** Đọc: build.md §1, §8. Cập nhật `gradle/libs.versions.toml` (toàn bộ version + library + plugin), `gradle.properties`. Áp dụng các bước ở build.md §1.3 nếu lỗi.
  DoD: `./gradlew assembleDebug` xanh với app hiện tại (cập nhật `app/build.gradle.kts` tối thiểu để build).
- [x] **T02 — `build-logic` + convention plugins + `version.properties`.** Đọc: build.md §2–§7, plan.md §2–3. Tạo `build-logic`, mọi plugin trong bảng build.md §3, task `verifyModuleDependencies`, `verifyReleaseHasNoDebugTool`, `version.properties` (1.0.0 / 1). Chuyển `:app` sang `pokepoke.android.application` + compose + hilt (tạo `PokePokeApplication` rỗng `@HiltAndroidApp`).
  DoD: `./gradlew assembleDebug assembleRelease check` xanh; APK debug có `versionName` `1.0.0-debug`, `versionCode` 1.
- [x] **T03 — Skeleton module.** Đọc: plan.md §4. Tạo **mọi** module (core, data, feature, debugtool) với `build.gradle.kts` dùng convention plugin đúng loại, namespace, thư mục `src/main/kotlin/<package>`, `src/test/kotlin/<package>`. Khai báo dependency giữa module đúng như contracts.md / plan.md §4.1. Thêm vào `settings.gradle.kts`.
  DoD: `./gradlew assembleDebug assembleRelease check` xanh. Kiểm tra check kiến trúc hoạt động: tạm thêm `implementation(projects.feature.home.impl)` vào `feature:details:impl` → build phải fail với message rõ ràng → gỡ ra.

## Giai đoạn 2 — Core

- [x] **T04 — `:core:testing`.** `MainDispatcherRule` (JUnit4, `StandardTestDispatcher`), helper `runComposeScreenshot`/rule Roborazzi dùng chung, `TestDispatchers`.
  DoD: `./gradlew :core:testing:check`.
- [ ] **T05 — `:core:common`.** Đọc contracts.md §1. Gồm `DataError`, `DataResult` + `dataResultOf`/`asDataResult`/`map`/`flatMap`, `AppError`, `AppResult` + `toAppError`/`toAppResult`/`map`/`onSuccess`/`onFailure`, dispatchers + Hilt module, `runNetworkFirst`, `DataSnapshot`, `networkFirst`, `AppInfo`.
  Test: `dataResultOf` / `asDataResult` bắt lỗi và **ném lại CancellationException**; `asDataResult` sau lỗi phát Failure rồi subscribe lại, phát tiếp Success khi upstream hết lỗi, backoff đúng (dùng virtual time); `toAppError` đủ mọi `DataError`; mọi nhánh của `runNetworkFirst` / `networkFirst` (online-success, offline do monitor, NoConnection / Timeout → offline, offline-có-cache, offline-không-cache (NotFound → NoConnection), Http / Parse → lỗi không đọc cache, emission đầu tiên đã là dữ liệu cuối cùng (không phát cache trước), cache cập nhật sau đó vẫn phát).
  DoD: `./gradlew :core:common:check koverVerify`.
- [ ] **T06 — `:core:designsystem`.** Đọc designsystem.md §1. Theme, màu, type, dimens, shape, icons, mọi component + Previews.
  Test: screenshot light/dark cho từng component; UI test cho `PokeTextField` (show/hide password), `PokePrimaryButton` (loading không click được).
  DoD: `./gradlew :core:designsystem:check recordRoborazziDebug` rồi `verifyRoborazziDebug` xanh; commit ảnh.
- [ ] **T07 — `:core:ui`.** Đọc designsystem.md §2. Test: screenshot từng composable với mọi state; `MessageEffect` gọi `onShown`; `ErrorState` đúng text theo từng `AppError`; `toUiMessage`, `toLoadState`.
  DoD: như T06 cho `:core:ui`.
- [ ] **T08 — `:core:network`.** Đọc contracts.md §2. Test (MockWebServer): service tạo từ `ApiServiceFactory` parse JSON (bỏ field lạ), interceptor trong set được gọi, `toNetworkDataError` cho IOException / timeout / 404 / 500 / JSON lỗi; `NetworkMonitorImpl` (Robolectric, shadow ConnectivityManager).
  DoD: `./gradlew :core:network:api:check :core:network:impl:check`.
- [ ] **T09 — `:core:storage`.** Đọc contracts.md §3. Test (Robolectric, file tạm): put/observe từng kiểu, remove, clear, `observeAll`, cùng name trả cùng instance, `storeNames` bỏ file `secure_`. Fake trong testFixtures + test cho fake.
  DoD: `./gradlew :core:storage:api:check :core:storage:impl:check`.
- [ ] **T10 — `:core:securestorage`.** Đọc contracts.md §4. Test với `AeadProvider` cleartext: round-trip, dữ liệu trên disk không chứa plaintext, đổi key (associated data) không giải mã được, `observeKeys`.
  DoD: `./gradlew :core:securestorage:api:check :core:securestorage:impl:check`.
- [ ] **T11 — `:core:navigation`.** Đọc contracts.md §5. `NavigatorImpl` (`@ActivityRetainedScoped`), `@Multibinds` cho các set. Test: Push / ReplaceTop / ResetTo / goBack; thứ tự priority; Redirect đầu tiên thắng và không bị intercept lại; request tuần tự; `FakeNavigator`.
  DoD: `./gradlew :core:navigation:api:check :core:navigation:impl:check`.
- [ ] **T12 — `:core:deeplink`.** Đọc contracts.md §6, deeplinks.md. Test: chuẩn hóa custom scheme / https / host lạ / scheme lạ / `/` cuối / hoa thường / query; `DeepLinkPattern`; dispatcher chọn handler đầu tiên khớp, trả false khi không khớp; `DeepLinkIntentFactory` (Robolectric).
  DoD: `./gradlew :core:deeplink:api:check :core:deeplink:impl:check`.

## Giai đoạn 3 — Data

- [ ] **T13 — `:data:pokemon`.** Đọc contracts.md §8. Model, enum, `PokemonImageUrls`, `PokemonFormat`, `PokemonRepository` + `PokemonRepositoryImpl` (Room), testFixtures (`FakePokemonRepository`, `PokemonFixtures`).
  Test: `PokemonImageUrls` (mọi dạng URL, có/không `/` cuối, URL lỗi), `PokemonFormat`, `fromKey` của enum; DAO / repository (Robolectric in-memory): mọi hàm trả `DataResult`, observe bản ghi không có → `Failure(NotFound)`, lỗi DB (đóng DB rồi gọi) → `Local.Read` / `Local.Write`, `saveList` không xóa detail, `saveDetail` không mất vị trí, trùng vị trí bị thay, `observeList(limit)` đúng thứ tự, `observeByNameOrId` theo số / tên, `observeSummaries` giữ thứ tự; fake tuân cùng luật (chạy chung bộ test với impl nếu được).
  DoD: `./gradlew :data:pokemon:api:check :data:pokemon:impl:check koverVerify`.

## Giai đoạn 4 — App shell & feature

- [ ] **T14 — `:app` + `:feature:main`.** Đọc screens.md §1, deeplinks.md §1. `MainActivity`, splash, theme XML, manifest, locale config, Coil, base URL, `DeepLinkConfig`, `MainShell`, `NavDisplay` + list-detail. Tạm thời đăng ký entry placeholder cho `HomeNavKey` nếu home chưa có (xóa ở T17).
  Test: `MainShell` screenshot (bottom bar, rail ở `w841dp`, ẩn khi FullScreen); UI test chọn tab gọi `ResetTo`.
  DoD: `./gradlew :app:assembleDebug :app:assembleRelease :feature:main:impl:check :app:check`; cài lên emulator (nếu có) mở được app.
- [ ] **T15 — `:feature:onboarding`.** Đọc screens.md §2, contracts.md §9. Repository, 2 use case, interceptor, screen, entry (tất cả internal).
  Test: repository (FakeKeyValueStore), use case, ViewModel (Next / Skip / Finish → complete + execute pending, Finish khi Failure vẫn đi tiếp), interceptor (đã / chưa xem / Failure → Proceed, request chứa OnboardingNavKey), UI + screenshot 3 trang.
  DoD: `./gradlew :feature:onboarding:api:check :feature:onboarding:impl:check verifyRoborazziDebug`.
- [ ] **T16 — `:feature:auth`.** Đọc screens.md §3, contracts.md §10. Repository, `LoginUseCase`, `TokenGenerator`, `Clock` (internal); `ObserveAuthStateUseCase`/`LogoutUseCase` (api) + `FakeAuthSession`; interceptor, `SessionCleaner` set, screen.
  Test: repository (FakeSecureStorage, lỗi storage → `Local.*`); login đúng / sai / trim username / lỗi ghi → Failure, token 32 byte ngẫu nhiên; logout xóa session + gọi mọi cleaner (cleaner hoặc clearSession lỗi → vẫn chạy hết, trả Failure đầu tiên); `ObserveAuthState` phát lại khi login / logout; ViewModel (`LoggedIn` / `InvalidCredentials` / Failure → message); interceptor cho Push / ReplaceTop / ResetTo / không RequiresAuth / đã login; ViewModel; UI + screenshot (trống, lỗi, đang gửi).
  DoD: như T15 cho auth.
- [ ] **T17 — `:feature:home`.** Đọc screens.md §4, plan.md §5. API, mapper, `PokemonRemoteRepository`, 3 use case (§4.1b), ViewModel, screen, tab, deeplink.
  Test: mapper; repository với MockWebServer (JSON mẫu thật của PokeAPI lưu trong `src/test/resources`; dịch 404 / 500 / timeout / JSON lỗi); use case mọi nhánh §4.1b (gồm search: 404 → `Success(NotFound)`, offline không có cache → `Failure(NoConnection)`) (fake remote + `FakePokemonRepository` + `FakeNetworkMonitor`); ViewModel mọi nhánh §4.3 với fake use case (init Success online / offline, Failure; load more Success / Failure + EndReached; refresh; search `Success(Found)` / `Success(NotFound)` / Failure; ClearSearch; OpenDetails Push vs ReplaceTop; bỏ qua ReachEnd khi đang tải); deeplink handler; UI + screenshot (loading, content, offline, load-more lỗi, search các trạng thái, grid ở `w841dp`).
  DoD: như T15 cho home.
- [ ] **T18 — `:feature:favourite`.** Đọc screens.md §6, contracts.md §11. Làm trước details vì details cần use case của `favourite:api` (Hilt phải có binding thì `:app` mới build được). Tạo `DetailsNavKey` trong `details:api` (contracts.md §11) để Favourite mở được Details. Repository, use case internal (`ObserveFavouritePokemonUseCase`, `RemoveFavouriteUseCase`) và api (`ObserveIsFavouriteUseCase`/`ToggleFavouriteUseCase`) + `FakeFavourites`. Test: DAO / repository (thứ tự mới nhất, toggle trả trạng thái mới, lỗi DB → `Local.*`), use case (LoggedOut / LoggedIn + ids → summaries, Failure lan truyền), `SessionCleaner`, ViewModel (LoggedOut / Empty / Content / Error + Retry, Remove + message Success / Failure, OpenDetails), deeplink, UI + screenshot.
  DoD: như T15 cho favourite.
- [ ] **T19 — `:feature:details`.** Đọc screens.md §5. Dùng `ObserveIsFavouriteUseCase`/`ToggleFavouriteUseCase`, test bằng `FakeFavourites`. Test: mapper (gồm cây Eevee nhiều nhánh, chuỗi 1 stage, thiếu artwork), repository chuỗi gọi 3 API (MockWebServer, lỗi ở từng bước), `ObservePokemonDetailUseCase` (online / offline có cache / offline không cache / lỗi), ViewModel với fake use case (Loading → Content / Error, Retry, toggle favourite + message (thêm / xóa / Failure), OpenEvolution cùng id / khác id), deeplink handler (id hợp lệ / không hợp lệ), UI + screenshot (loading, lỗi, content light/dark, offline, màn rộng).
  DoD: như T15 cho details.
- [ ] **T20 — `:feature:profile`.** Đọc screens.md §7. Test: use case (fake `AppLocaleController` / `AppInfo`), ViewModel với fake use case (auth null / LoggedIn / LoggedOut / Failure → LoggedOut, ngôn ngữ Success / Failure, logout confirm → logout + ResetTo, logout Failure → message, login), `AppLocaleController` mapping (Robolectric), deeplink, UI + screenshot (logged in/out, dialog).
  DoD: như T15 cho profile.

## Giai đoạn 5 — Debug & hoàn thiện

- [ ] **T21 — `:debugtool`.** Đọc screens.md §8, plan.md §8. Test: interceptor ghi log (MockWebServer), ring buffer 500, che header nhạy cảm, đọc schema / rows / chặn câu lệnh không phải SELECT/PRAGMA (Robolectric với DB tạm), UI + screenshot các màn chính.
  DoD: `./gradlew :debugtool:check`, `./gradlew :app:assembleRelease` xanh và `verifyReleaseHasNoDebugTool` xanh; APK release không chứa class `com.khangtm.pokepoke.debugtool` (kiểm tra bằng `apkanalyzer dex packages` hoặc unzip + tìm).
- [ ] **T22 — Test end-to-end trong `:app`.** Robolectric + Hilt (`HiltTestApplication`, `@TestInstallIn` thay `@BaseUrl` bằng MockWebServer với JSON mẫu, fake storage nếu cần). Kịch bản: lần đầu mở → onboarding → Home có list; bấm item khi chưa login → Login → Details; deeplink `pokepoke://pokemon/25` cold start chưa xem onboarding (đúng chuỗi ở deeplinks.md §3); deeplink `pokepoke://favourite` khi chưa login → tab Favourite với nút đăng nhập; logout xóa favourite; đổi ngôn ngữ giữ back stack.
  DoD: `./gradlew :app:testDebugUnitTest`.
- [ ] **T23 — Tổng kiểm tra.**
  DoD: `./gradlew clean assembleDebug assembleRelease check verifyRoborazziDebug koverVerify` xanh; `koverHtmlReport` tạo được; README ngắn ở root (mô tả, cách build, lệnh test, cấu trúc module, link docs).
