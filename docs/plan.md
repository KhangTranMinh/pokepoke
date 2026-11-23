# Poke Poke — Kế hoạch setup multi-module

> Trạng thái: **đã chốt**, chưa implement. Xem yêu cầu gốc tại [requirements.md](requirements.md).

## 0. Hiện trạng

- AGP 9.5.2, Gradle 9.6.0, Kotlin 2.2.10, daemon JVM toolchain 25.
- Chỉ có module `:app`; `compileSdk`/`minSdk`/`targetSdk`, `versionCode`/`versionName` đang hardcode trong `app/build.gradle.kts`.
- Java target đang là 11.

## 1. Nguyên tắc

1. **Core chỉ cung cấp function / hạ tầng generic**, không biết gì về logic nghiệp vụ hay feature nào.
2. **Dữ liệu nghiệp vụ thuộc về module sở hữu nó** (flag onboarding, token, favourite...).
3. **Business model riêng, không phụ thuộc API response.** DTO chỉ tồn tại trong feature gọi API; mọi module khác chỉ thấy business model.
4. **Single source of truth:** luôn fetch API, nhưng UI luôn đọc dữ liệu từ storage.
5. **Feature tự quyết định navigation và deeplink của mình**; core chỉ cung cấp contract + cơ chế dispatch.
6. **Test nằm trong từng module**, không có module chứa test của module khác.
7. **Chỉ `:app` biết các module `impl`** — được build kiểm tra tự động (mục 2.3).

## 2. Share config chung: convention plugins (`build-logic`)

Dùng **included build `build-logic`** chứa các convention plugin (cách của Now in Android). Không dùng `subprojects {}` / `allprojects {}` hay helper function trong `buildSrc`.

Mỗi module chỉ cần khai báo plugin + namespace + dependency riêng:

```kotlin
// feature/home/impl/build.gradle.kts
plugins {
    alias(libs.plugins.pokepoke.android.feature.impl)
}
android { namespace = "com.khangtm.pokepoke.feature.home.impl" }
dependencies {
    implementation(projects.data.pokemon.api)
    implementation(projects.feature.details.api)
}
```

### 2.1. Danh sách convention plugin

| Plugin | Cấu hình |
|---|---|
| `pokepoke.android.application` | compileSdk/minSdk/targetSdk, JVM toolchain, buildTypes, version (mục 3) |
| `pokepoke.android.library` | SDK, JVM, lint, thư viện unit test chung |
| `pokepoke.android.compose` | Bật Compose, Compose BOM, tooling preview, Compose UI test (Robolectric), Roborazzi |
| `pokepoke.android.feature.api` | library + `testFixtures` (chứa fake của các interface public) |
| `pokepoke.android.feature.impl` | library + compose + hilt + `core:designsystem`, `core:ui`, `core:navigation:api` + thư viện test |
| `pokepoke.hilt` | Hilt + KSP |
| `pokepoke.room` | Room + KSP + schema export + thư viện test DAO |
| `pokepoke.jvm.library` | Module Kotlin thuần (không phụ thuộc Android) |
| `pokepoke.kover` | Coverage từng module, có thể đặt ngưỡng tối thiểu từng module |

### 2.2. SDK / JVM version để trong version catalog

```toml
# gradle/libs.versions.toml
[versions]
compileSdk = "37"
minSdk = "30"
targetSdk = "37"
jvm = "21"
```

Convention plugin đọc qua `VersionCatalogsExtension`. Chỉ sửa một chỗ; Renovate/Dependabot đọc được.

**Java 21** cho cả toolchain và target (`jvmToolchain(21)`):
- Android Studio đi kèm JBR 21 → IDE, terminal, CI dùng cùng một JDK.
- Project viết bằng Kotlin, D8 desugar bytecode xuống Android; API `java.*` dùng được phụ thuộc API level của máy, không phụ thuộc JDK. Với minSdk 30 / compileSdk 37 không có vấn đề.

### 2.3. Chặn vi phạm kiến trúc

Check trong convention plugin, **build fail** nếu:
- Một module `:impl` phụ thuộc vào `:impl` khác (chỉ `:app` được phép).
- `:core:*` phụ thuộc vào `:data:*` hoặc `:feature:*`.
- `:data:*` phụ thuộc vào `:feature:*`.

## 3. Versioning: `version.properties`

```properties
# version.properties (ở root, commit vào git)
versionMajor=1
versionMinor=2
versionPatch=0
versionCode=12
```

- Tách riêng, **không** gộp vào `gradle.properties`: sửa version không đụng cấu hình Gradle, diff gọn.
- `versionName = "$versionMajor.$versionMinor.$versionPatch"`.
- Convention plugin `pokepoke.android.application` đọc qua `providers.fileContents(...)`: Gradle theo dõi file, configuration cache tự invalidate khi version đổi.
- Hiện tại tăng version bằng cách **sửa tay** file này. CI chưa có; khi có CI sẽ bàn thêm cách tăng (chỉ cần bổ sung override, không phải đổi cấu trúc).

## 4. Cấu trúc module

```
:app                              ← single Activity, gom DI / entries / interceptors / deeplink handlers,
                                    cấu hình Coil, base URL PokeAPI, locales_config
:build-logic (included)

# Core — chỉ hạ tầng generic
:core:common                      ← dispatchers, Result, coroutine utils
:core:designsystem                ← theme, màu, typography, component dùng chung
:core:ui                          ← composable dùng chung (RemoteImage, loading/error/empty state, adaptive layout)
:core:network:api / :impl         ← HttpClient (base URL do :app cấp), JSON, mapping lỗi
:core:storage:api / :impl         ← key-value store generic (DataStore)
:core:securestorage:api / :impl   ← lưu/đọc có mã hóa, generic
:core:navigation:api / :impl      ← NavKey, Navigator, contract entry/interceptor/tab
:core:deeplink:api / :impl        ← DeepLinkHandler, DeepLinkDispatcher, URI matching
:core:testing                     ← tiện ích test (không chứa test case)

# Data — kho dữ liệu nghiệp vụ dùng chung (SSOT), không gọi network
:data:pokemon:api / :impl         ← business model + PokemonStore (query/save) + Room DB

# Feature
:feature:onboarding:api / :impl   ← tự lưu flag "đã xem onboarding" (qua core:storage)
:feature:auth:api / :impl         ← token (qua core:securestorage), AuthState, interceptor RequiresAuth, LoginKey
:feature:main:api / :impl         ← khung 3 tab: bottom bar / navigation rail, MainKey(tab)
:feature:home:api / :impl         ← tab Home: list + search; tự gọi API list / search → save vào PokemonStore
:feature:details:api / :impl      ← màn Details; tự gọi API detail/species/evolution → save vào PokemonStore
:feature:favourite:api / :impl    ← tab Favourite; FavouriteRepository + Room DB riêng (chỉ lưu id)
:feature:profile:api / :impl      ← tab Me: thông tin đăng nhập, phiên bản, đổi ngôn ngữ, logout
```

- **`:impl` của feature**: chia package theo Clean Architecture `data/`, `domain/`, `presentation/`.
- **`:api`**: chỉ chứa interface, model public, `NavKey`; fake của interface để trong `testFixtures`.

### 4.1. Phân chia theo team

Mỗi tab / màn hình là một feature độc lập, không bị nghẽn qua một module chung:

| Module | Sở hữu | Phụ thuộc |
|---|---|---|
| `feature:home` | Team Home | `data:pokemon:api`, `details:api` (mở Details qua `DetailsKey`) |
| `feature:details` | Team Details | `data:pokemon:api`, `favourite:api` (nút add/remove favourite) |
| `feature:favourite` | Team Favourite | `data:pokemon:api`, `details:api` |
| `feature:profile` | Team Me | `auth:api` |
| `data:pokemon` | Chủ sở hữu được chỉ định (ít thay đổi) | `core:*` |

- Sửa `impl` không làm các module phụ thuộc `api` build lại; chỉ đổi contract trong `api` mới cần thống nhất giữa các team.
- Gắn CODEOWNERS theo thư mục.

### 4.2. Core chỉ cung cấp function

| Module | Cung cấp | Không làm (thuộc về module khác) |
|---|---|---|
| `core:storage` | Key-value store generic | Flag onboarding → `onboarding:impl` |
| `core:securestorage` | Lưu/đọc có mã hóa | Token → `auth:impl` |
| `core:network` | HttpClient, JSON, mapping lỗi | Base URL → `:app`; endpoint, DTO → feature gọi API |
| `core:ui` | `RemoteImage` (bọc Coil) | Cấu hình `ImageLoader` → `:app`; URL ảnh Pokémon → `data:pokemon` |
| *(không có `core:database`)* | — | Room yêu cầu mọi entity trong một class `@Database` → DB chung buộc core biết entity. Mỗi module có DB riêng, dùng chung cấu hình qua plugin `pokepoke.room` |
| *(không có `core:locale`)* | — | Xem mục 7 |

### 4.3. Core tách `api` / `impl` (đã chốt)

- **Tách** cho core có implementation thay được: `network`, `storage`, `securestorage`, `navigation`, `deeplink`. Module khác chỉ phụ thuộc `:api`; fake để trong `testFixtures` của `:api`.
- **Không tách** cho `common`, `designsystem`, `ui`, `testing` — code UI / tiện ích thuần, tách ra chỉ thêm module rỗng.

## 5. Dữ liệu: business model & single source of truth

### 5.1. `data:pokemon` — kho dữ liệu, không gọi network

```
data:pokemon:api
  model:   PokemonSummary(id, name, imageUrl), PokemonDetail(...), EvolutionChain(...)
  PokemonStore:
    observeList(limit): Flow<List<PokemonSummary>>
    saveList(items, offset, totalCount)
    observeTotalCount(): Flow<Int?>
    observeDetail(id): Flow<PokemonDetail?>
    saveDetail(detail)
    findByNameOrId(query): Flow<PokemonSummary?>
  PokemonImageUrls.kt  ← logic dựng URL ảnh (mục 5.2)

data:pokemon:impl
  Room DB, entity, mapper entity ↔ model (không có DTO, không có network)
```

- Feature tự gọi API của mình → map DTO → business model → gọi `save*`. UI chỉ đọc qua `observe*`.
- **Luật merge do `data:pokemon:impl` nắm:** `saveList` chỉ upsert `name` / `position`, không ghi đè dữ liệu detail; `saveDetail` không làm mất `position` của list. Có unit test riêng.
- Lưu cột `position` (thứ tự trong API) để sắp xếp list — không sắp theo `id` vì PokeAPI có các form id 10001+ ở cuối danh sách.
- Upsert theo `id`, không xóa rồi ghi lại → list không nháy khi refresh. Không xóa cache khi logout (dữ liệu public).

### 5.2. URL ảnh: `PokemonImageUrls.kt`

- API list chỉ trả `name` + `url`: **id** tách từ `url` (`.../pokemon/25/` → 25); **ảnh** dựng theo quy ước repo sprites của PokeAPI (vd. `.../sprites/pokemon/other/official-artwork/{id}.png`).
- Toàn bộ quy ước này nằm trong **một file duy nhất** `data/pokemon/api/.../PokemonImageUrls.kt` (Kotlin thuần): `fun artworkUrl(id: Int): String`, và hàm tách id từ `url` nếu cần. Đổi quy ước chỉ sửa một chỗ.
- Có unit test riêng trong `data:pokemon:api`.

### 5.3. Danh sách (tab Home) — không dùng thư viện Paging

Giới hạn ở tầng view, chỉ dùng Flow, không phụ thuộc Android framework:

```
HomeViewModel:
  visibleCount = 20
  list = store.observeList(limit = visibleCount)   ← UI chỉ đọc từ đây
  onStart / refresh: fetch(offset = 0, limit = 20) → store.saveList
  onReachEnd:        visibleCount += 20
                     fetch(offset = visibleCount - 20, limit = 20) → store.saveList
```

- Trang đã có trong cache: hiển thị ngay, vẫn fetch lại để làm mới. Trang chưa có: loading ở cuối list.
- Hết dữ liệu: dựa vào `count` trong response API (lưu cùng `saveList`); `visibleCount >= totalCount` thì ngừng.
- Lỗi: có cache → snackbar báo lỗi; trang chưa có cache → dòng lỗi + retry ở cuối list.
- Đang fetch một trang thì bỏ qua các lần `onReachEnd` tiếp theo.
- List item hiển thị: **#id, tên, ảnh** (dựng theo mục 5.2).

### 5.4. Search — tìm chính xác

API không hỗ trợ fuzzy search → chỉ tìm **chính xác**:
- Nhập tên hoặc id → chuẩn hóa (trim, lowercase) → gọi `/pokemon/{name|id}`.
- Tìm thấy → `store.saveDetail` → kết quả **đọc từ storage** (`findByNameOrId`).
- 404 → trạng thái "không tìm thấy". Mất mạng → vẫn tìm chính xác trong storage.
- Tắt search → quay về list hiện tại.

### 5.5. Details — có cache

- `details:impl` gọi `/pokemon/{id}`, `/pokemon-species/{id}`, `/evolution-chain/{id}` → map → `store.saveDetail`.
- UI đọc `store.observeDetail(id)`: có cache thì hiện ngay, đồng thời fetch làm mới; mất mạng mà chưa có cache → màn lỗi + retry.

### 5.6. Favourite

- `favourite:impl` có Room DB riêng, **chỉ lưu id** (+ thời điểm thêm để sắp xếp).
- Dữ liệu hiển thị lấy từ `PokemonStore` (đã có vì Details lưu trước khi user bấm favourite) → không cần snapshot, SSOT trọn vẹn.
- `FavouriteRepository` (trong `favourite:api`): `observeIsFavourite(id)`, `add(id)`, `remove(id)`, `observeFavourites()`.

### 5.7. Hình ảnh

- Room **chỉ lưu URL**, không lưu blob.
- Cache bytes do **Coil disk cache** (LRU, tự quản dung lượng) — ảnh được cache **khi hiển thị**, không tải trước.
- Cấu hình `ImageLoader` ở `:app`; composable `RemoteImage` ở `core:ui`; feature không gọi Coil trực tiếp.

## 6. Navigation & deeplink

Dùng **Navigation 3** (đã chốt).

### 6.1. Single Activity — phù hợp điện thoại gập

- Navigation 3 + Material3 Adaptive (`ListDetailSceneStrategy`): mở máy → list và Details hiển thị cạnh nhau; gập → 1 pane. Cùng một back stack, không thêm code điều hướng.
- `NavigationSuiteScaffold` tự chuyển bottom bar ↔ navigation rail theo `WindowSizeClass` (trong `main:impl` / `core:ui`).
- Khai báo `configChanges` (screenSize, smallestScreenSize, screenLayout, orientation) để Compose tự relayout khi gập/mở thay vì recreate activity. State vẫn giữ bằng ViewModel / `rememberSaveable`.
- Một nơi nhận intent, một `Navigator` → deeplink và auth guard đơn giản. Multi activity phải đồng bộ back stack giữa nhiều activity và không hiển thị được 2 pane.

### 6.2. `core:navigation` — chỉ hạ tầng

- `NavKey`, `Navigator` (navigate / back / replace).
- Contract `EntryProviderInstaller`: feature đăng ký màn hình cho key của mình.
- Contract `NavigationInterceptor`: chặn hoặc chuyển hướng một lần navigate.
- Contract `TopLevelDestination` (icon, label, thứ tự, key): feature đăng ký tab.
- Marker generic `RequiresAuth` — chỉ là metadata, core không biết auth là gì.

### 6.3. `core:deeplink` — chỉ hạ tầng

- Contract `DeepLinkHandler`: nhận URI, trả về danh sách `NavKey` (back stack). Feature đăng ký qua Hilt multibinding.
- `DeepLinkDispatcher.dispatch(uri)`: duyệt handler → chạy qua interceptor → gọi `Navigator`.
- Tiện ích match URI pattern.
- Push notification chỉ cần đính kèm URI, đi chung đường với deeplink / app link.

### 6.4. Feature tự quyết định

- **`:api`** khai báo `NavKey` public (vd. `DetailsKey(id)`, `LoginKey(redirect)`, `MainKey(tab)`); feature khác navigate bằng key này.
- **`:impl`** đăng ký:
  - Màn hình cho từng key.
  - Deeplink handler của chính feature (vd. `pokepoke://pokemon/25` → `DetailsKey(25)`).
  - Tab (`TopLevelDestination`), nếu feature là một tab.
- Màn hình cần đăng nhập: key implement `RequiresAuth`.
- **`auth:impl`** đóng góp `NavigationInterceptor`: key có `RequiresAuth` mà chưa đăng nhập → chuyển sang `LoginKey(redirect = key)`, đăng nhập xong mở tiếp màn hình đích.
- **`main:impl`** dựng bottom bar / navigation rail từ `Set<TopLevelDestination>`, không biết tab nào thuộc feature nào.

### 6.5. `:app`

- Single Activity với `NavDisplay`, gom entries, interceptors, deeplink handlers từ Hilt.
- Nhận intent (deeplink / app link / notification) → `DeepLinkDispatcher`.
- SplashScreen API: giữ splash tới khi biết màn hình bắt đầu (hỏi `onboarding:api`: chưa xem → `OnboardingKey`, rồi → `MainKey`).

## 7. Đa ngôn ngữ

Không cần module riêng:
- `profile:impl` gọi thẳng `AppCompatDelegate.setApplicationLocales(...)`.
- Lựa chọn được lưu tự động: Android 13+ do hệ thống; Android 11–12 do AppCompat (`autoStoreLocales` trong manifest). Không lưu vào `core:storage`.
- Chưa chọn → theo ngôn ngữ máy (không phải en/vi → en). Đã chọn → giữ nguyên dù đổi ngôn ngữ máy.
- `locales_config.xml` + cấu hình manifest ở `:app`.

## 8. Testing

- Mỗi module (core, data, feature) có `src/test` riêng, và `src/androidTest` khi cần. **Không** có module chứa test của module khác.
- `:core:testing` chỉ là thư viện tiện ích: `MainDispatcherRule`, helper Turbine / Compose test.
- Fake của interface public nằm trong `testFixtures` của module `:api` tương ứng (vd. `FakePokemonStore` trong `data:pokemon:api`); module khác dùng qua `testImplementation(testFixtures(projects.data.pokemon.api))`.
- Convention plugin tự thêm thư viện test theo loại module:

| Loại module / layer | Loại test |
|---|---|
| domain / ViewModel / mapper / `PokemonImageUrls` | Unit test: JUnit, MockK, Turbine, coroutines-test |
| Feature gọi API | MockWebServer (DTO parsing, mapping lỗi) |
| Module có Room (`data:pokemon`, `favourite`) | DAO + luật merge với in-memory DB (Robolectric) |
| UI (`impl`, `designsystem`, `ui`) | Compose UI test (Robolectric), screenshot test (Roborazzi) |
| `:app` | `androidTest` end-to-end cho navigation, deeplink, auth guard |

- Kover: coverage từng module, có thể đặt ngưỡng tối thiểu từng module.

## 9. DI: Hilt (đã chốt)

- Binding nằm trong `:impl`, chỉ tổng hợp ở `:app`.
- Multibinding cho `EntryProviderInstaller`, `NavigationInterceptor`, `TopLevelDestination`, `DeepLinkHandler`.

## 10. Các thay đổi nhỏ đi kèm

- **`settings.gradle.kts`**: thêm `includeBuild("build-logic")` và `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` → khai báo dependency bằng `projects.data.pokemon.api` thay vì chuỗi.
- **`gradle.properties`**: bật `org.gradle.parallel=true`, `org.gradle.caching=true`.
- **`libs.versions.toml`**:
  - Nâng các thư viện cũ: `coreKtx 1.10.1`, `lifecycle 2.6.1`, `activityCompose 1.8.0`.
  - Thêm thư viện sắp dùng: hilt, ksp, navigation3, material3-adaptive, retrofit/ktor, kotlinx-serialization, room, datastore, coil, core-splashscreen, appcompat (per-app locale), turbine, mockk, mockwebserver, robolectric, kover, roborazzi. **Không** dùng thư viện Paging.
  - Thêm `[plugins]` cho các convention plugin.
- **AGP 9** đã tích hợp sẵn Kotlin → không cần plugin `kotlin-android`; convention plugin chỉ thêm compose compiler, ksp, hilt.

## 11. Thứ tự implement

1. Tạo `build-logic` với các convention plugin (kèm check kiến trúc), chuyển `:app` sang dùng.
2. Thêm `version.properties` và logic đọc version.
3. Tạo skeleton các module `core`, `data`, `feature` (rỗng, build được, mỗi module có sẵn thư mục test).
4. Chạy `./gradlew build` để kiểm tra.

## 12. Quyết định

- [x] DI: **Hilt**.
- [x] Java: **21** (toolchain + target).
- [x] Version: `version.properties`, sửa tay; cách tăng trên CI để sau.
- [x] Tên module: `core:storage` / `core:securestorage`.
- [x] Không có `core:locale`; ngôn ngữ do AppCompat lưu.
- [x] `data:pokemon` là kho dữ liệu (SSOT), không gọi network; feature tự fetch rồi save.
- [x] Module feature: `main` (khung tab), `home`, `details`, `favourite`, `profile`, `onboarding`, `auth`.
- [x] List không dùng thư viện Paging; giới hạn ở tầng view.
- [x] Search chính xác (tên / id).
- [x] Cache list + details trong Room; ảnh cache bằng Coil khi hiển thị.
- [x] Ảnh list dựng theo quy ước, logic nằm trong `PokemonImageUrls.kt`.
- [x] Single Activity.
- [x] Core tách `api`/`impl` theo mục 4.3: tách `network`, `storage`, `securestorage`, `navigation`, `deeplink`; không tách `common`, `designsystem`, `ui`, `testing`.
- [x] Navigation: **Navigation 3**.
