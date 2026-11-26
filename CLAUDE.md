# CLAUDE.md

## Git / GitHub authentication (BẮT BUỘC)

- Remote: `https://github.com/KhangTranMinh/pokepoke.git` (branch chính: `main`).
- **Chỉ được dùng token trong `configs/credentials/.env` (biến `GITHUB_TOKEN`)** cho mọi thao tác với GitHub của project này. KHÔNG dùng token/credential global (osxkeychain, `gh auth`, `GITHUB_TOKEN`/`GH_TOKEN` có sẵn trong shell, ...).
- `git push/pull/fetch`: repo đã được cấu hình credential helper local (`git config --local credential.helper`) đọc token từ `configs/credentials/.env` và đã tắt helper global. Không sửa / không thêm helper khác; không nhúng token vào remote URL.
- `gh` CLI / GitHub API: luôn truyền token từ file, ví dụ:
  ```sh
  GH_TOKEN=$(grep '^GITHUB_TOKEN=' configs/credentials/.env | cut -d= -f2-) gh pr create ...
  ```
- Không bao giờ in token ra output, commit file `.env`, hay ghi token vào file nào khác. `configs/credentials/` đã nằm trong `.gitignore`.
- Signing: `configs/signing/release.*` (keystore + mật khẩu release) **không bao giờ commit / in ra**; `configs/signing/debug.*` được commit (khóa debug dùng chung).

## Implement project (dành cho agent)

- **Bắt đầu từ [`docs/tasks.md`](docs/tasks.md)**: làm task đầu tiên chưa tick, theo đúng thứ tự, đủ DoD rồi mới sang task sau.
- **Mỗi lần chỉ làm 1 task** (bắt buộc). Xong task, theo đúng thứ tự:
  1. Cập nhật `docs/tasks.md`: tick `[x]` task vừa xong.
  2. Commit (quy tắc bên dưới).
  3. Push (theo mục Git / GitHub authentication ở trên).
  4. **Dừng lại**: báo user task vừa xong (tóm tắt, kết quả DoD, quyết định đã ghi vào `decisions.md`) và **chờ user yêu cầu** mới làm task tiếp theo. Không tự bắt đầu task sau.
- **Mỗi task = đúng 1 commit** (bắt buộc):
  - Chỉ commit khi DoD đã xanh; không commit dở dang giữa chừng, không gộp 2 task vào 1 commit, không tách 1 task thành nhiều commit.
  - Commit chứa trọn task: code, test, ảnh screenshot Roborazzi, dấu `[x]` trong `docs/tasks.md`, dòng mới trong `docs/decisions.md` (nếu có).
  - Message: dòng đầu `Txx: <tóm tắt>` (2 chữ số: `T01`, `T17`); body liệt kê ý chính và các quyết định đã ghi vào `decisions.md`; cuối message có dòng `Co-Authored-By` theo quy định của harness.
  - Phát hiện lỗi của task trước khi đang làm task sau: sửa luôn trong commit của task hiện tại, ghi `Fix Tyy: <mô tả>` trong body. Không amend / rebase commit đã có.
- Tài liệu (thứ tự ưu tiên khi mâu thuẫn): `docs/design/contracts.md` > `docs/design/layers.md` > `docs/design/screens.md` > `docs/design/designsystem.md`, `docs/design/deeplinks.md`, `docs/design/build.md` > `docs/plan.md` > `docs/requirements.md`.
- **Không dừng lại để hỏi.** Chỗ tài liệu chưa nói rõ: chọn phương án đơn giản nhất, nhất quán với tài liệu, rồi ghi vào `docs/decisions.md`. Lệch khỏi tài liệu (version, API thư viện khác thực tế) cũng phải ghi.
- Version thư viện: chỉ dùng version trong `docs/design/build.md` §1; lỗi tương thích → làm theo build.md §1.3.
- API thư viện mới (Navigation 3, Material3 Adaptive, AGP 9 DSL): khi không chắc, đọc sources jar / KDoc của version đã pin trong Gradle cache, không đoán.

## Quy ước code

- Package = namespace module: `com.khangtm.pokepoke.<core|domain|data|feature>.<name>[.api|.impl]`; `:app` → `com.khangtm.pokepoke`; `:debugtool` → `com.khangtm.pokepoke.debugtool`; `:translation` → `com.khangtm.pokepoke.translation`.
- Kotlin source ở `src/main/kotlin`, test ở `src/test/kotlin`.
- Chỉ `:app` được phụ thuộc module `:impl`, `:business:data` và `:debugtool` (build tự kiểm tra).
- **Clean Architecture cho Pokémon / Favourite** (`docs/design/layers.md` §1.1): business model + repository interface + use case ở `:business:domain`; API, DTO, Room, cài đặt repository ở `:business:data` (mọi thứ `internal`). Feature chỉ phụ thuộc `:business:domain`, không biết dữ liệu lấy / lưu ở đâu. Data không chứa quyết định nghiệp vụ.
- **Layer (bắt buộc — `docs/design/layers.md`, tự rà theo checklist §5 trước khi commit):** ViewModel chỉ inject UseCase + `Navigator` và chỉ xử lý `AppResult`; UseCase luôn trả `AppResult` / `Flow<AppResult>`; Repository luôn trả `DataResult` / `Flow<DataResult>` và là chỗ duy nhất bắt exception (qua `dataResultOf` / `asDataResult`). Không `try`/`catch` ở ViewModel / UseCase; không `runCatching`.
- `:feature:*:api` chỉ chứa `NavKey`, use case interface (chỉ khi module khác cần), model cho use case, extension point. Repository / DAO / DTO luôn `internal` trong `:impl`.
- Key điều hướng đặt tên `XxxNavKey` (implement `NavKey` của Navigation 3), vd. `HomeNavKey`, `DetailsNavKey(pokemonId)` — tránh nhầm với key của storage / Room / Compose `key()`.
- Mỗi screen: `XxxUiState`, `XxxUiAction`, `XxxViewModel` (`uiState: StateFlow`, `onAction()`), `XxxRoute.kt` (chỉ root, biết ViewModel), `XxxScreen.kt` (stateless, preview được), `XxxPreviews.kt`.
- Không hardcode string: mọi text và `contentDescription` nằm trong module `:translation` (từ T07a), trong `values/strings.xml` (en) và `values-vi/strings.xml` (vi); key có tiền tố module (`home_`, `details_`...). Chuỗi không cần dịch (vd. `app_name`) đặt ở `values/non-translation.xml`. Chỉ có hai file này. Module khác import `com.khangtm.pokepoke.translation.R`. `:debugtool` giữ strings riêng, chỉ cần `values/`. Ngoại lệ duy nhất: `app/src/<flavor>/res/values/non-translation.xml` được override key **đã có** trong `:translation` (hiện chỉ `app_name`) theo flavor, không thêm key mới (`docs/design/build.md` §4a).
- Không dùng `!!`; không `GlobalScope`; coroutine dùng dispatcher inject (`@IoDispatcher`).
- Tên test: backtick mô tả hành vi, vd. ``fun `offline with cache emits content once`()``.

## Lệnh

| Mục đích | Lệnh |
|---|---|
| Build (mọi variant; `prodRelease` cần `configs/signing/release.*`) | `./gradlew assembleDebug assembleRelease` |
| Build một variant | `./gradlew :app:assembleDevDebug` (hoặc `QaDebug`, `QaRelease`, `ProdDebug`, `ProdRelease`) |
| Unit test `:app` | `./gradlew :app:testDevDebugUnitTest` |
| Check một module | `./gradlew :<module>:check` |
| Ghi / kiểm tra screenshot | `./gradlew recordRoborazziDebug` / `./gradlew verifyRoborazziDebug` |
| Toàn bộ | `./gradlew check verifyRoborazziDebug koverVerify` |
