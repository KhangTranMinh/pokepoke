# Poke Poke — Kế hoạch setup multi-module

> Trạng thái: **đã chốt**, chưa implement. Xem yêu cầu gốc tại [requirements.md](requirements.md).
>
> Chi tiết để implement: [design/contracts.md](design/contracts.md), [design/layers.md](design/layers.md), [design/screens.md](design/screens.md), [design/designsystem.md](design/designsystem.md), [design/deeplinks.md](design/deeplinks.md), [design/build.md](design/build.md); danh sách việc: [tasks.md](tasks.md).

## 0. Hiện trạng

- AGP 9.5.2, Gradle 9.6.0, Kotlin 2.2.10, daemon JVM toolchain 25.
- Chỉ có module `:app`; `compileSdk`/`minSdk`/`targetSdk`, `versionCode`/`versionName` đang hardcode trong `app/build.gradle.kts`.
- Java target đang là 11.

## 1. Nguyên tắc

1. **Core chỉ cung cấp function / hạ tầng generic**, không biết gì về logic nghiệp vụ hay feature nào.
2. **Dữ liệu nghiệp vụ thuộc về module sở hữu nó** (flag onboarding, token, favourite...).
3. **Business model riêng, không phụ thuộc API response.** DTO chỉ tồn tại trong feature gọi API; mọi module khác chỉ thấy business model.
4. **Single source of truth, network-first:** luôn cố gắng fetch network trước; chỉ khi xác nhận không có network mới dùng cache. UI luôn đọc dữ liệu từ storage và chỉ cập nhật **một lần** cho mỗi lần tải (mục 5.2).
5. **Feature tự quyết định navigation và deeplink của mình**; core chỉ cung cấp contract + cơ chế dispatch.
6. **Mỗi screen có 1 `UiState` + 1 `UiAction`**; chỉ composable root phụ thuộc ViewModel, phần UI thật sự là stateless và preview được (mục 7).
7. **Test nằm trong từng module**, không có module chứa test của module khác.
8. **Chỉ `:app` biết các module `impl`** — được build kiểm tra tự động (mục 2.3).
9. **Công cụ debug chỉ có trong debug build**, độc lập, không feature nào biết tới (mục 8).

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
- Bất kỳ module nào ngoài `:app` phụ thuộc vào `:debugtool`.
- `:app` khai báo `:debugtool` bằng configuration khác `debugImplementation`, hoặc release runtime classpath chứa `:debugtool`.

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
:core:common-kotlin              ← (JVM thuần) DataResult / AppResult, network-first helper, qualifier dispatcher, AppInfo
:core:common-android             ← AppInfoImpl + Hilt module (dispatchers, ApplicationScope); chỉ :app phụ thuộc
:core:designsystem                ← theme, màu, typography, component dùng chung
:core:ui                          ← composable dùng chung (RemoteImage, loading/error/empty state, adaptive layout)
:core:network:api / :impl         ← HttpClient (base URL do :app cấp), JSON, mapping lỗi,
                                    NetworkMonitor (trạng thái kết nối), hook Set<Interceptor>
:core:storage:api / :impl         ← key-value store generic (DataStore)
:core:securestorage:api / :impl   ← lưu/đọc có mã hóa, generic
:core:navigation:api / :impl      ← NavKey, Navigator, contract entry/interceptor/tab
:core:deeplink:api / :impl        ← DeepLinkHandler, DeepLinkDispatcher, URI matching
:translation                      ← mọi strings.xml (values/ en + values-vi/ vi), chuỗi không dịch ở non-translation.xml; sau này có thể thêm remote translation
:core:testing                     ← tiện ích test (không chứa test case)

# Data — kho dữ liệu nghiệp vụ dùng chung (SSOT), không gọi network
:data:pokemon:api / :impl         ← business model + PokemonRepository (query/save, trả DataResult) + Room DB

# Feature
:feature:onboarding:api / :impl   ← tự lưu flag "đã xem onboarding" (qua core:storage)
:feature:auth:api / :impl         ← token (qua core:securestorage), AuthState, interceptor RequiresAuth, LoginNavKey
:feature:main:api / :impl         ← khung 3 tab (MainShell): bottom bar / navigation rail; api để trống
:feature:home:api / :impl         ← tab Home (HomeNavKey): list + search; tự gọi API list / search → save vào PokemonRepository
:feature:details:api / :impl      ← màn Details; tự gọi API detail/species/evolution → save vào PokemonRepository
:feature:favourite:api / :impl    ← tab Favourite; Room DB riêng (chỉ lưu id); api public `ObserveIsFavouriteUseCase`, `ToggleFavouriteUseCase`
:feature:profile:api / :impl      ← tab Me: thông tin đăng nhập, phiên bản, đổi ngôn ngữ, logout

# Debug — chỉ gắn vào debug build
:debugtool                        ← network log, xem / query local DB, xem local storage (mục 8)
```

- **`:impl` của feature**: chia package theo Clean Architecture `data/`, `domain/`, `presentation/`.
- **`:api`**: chỉ chứa `NavKey`, **use case interface** (chỉ khi module khác cần), model mà use case cần, extension point; fake để trong `testFixtures`. Repository luôn `internal` trong `:impl` (chi tiết: contracts.md "Quy tắc `:feature:*:api`"). Core public interface kiểu capability (`Navigator`, `KeyValueStore`...); `data:pokemon:api` public `PokemonRepository`.

### 4.1. Phân chia theo team

| Nhóm module | Sở hữu |
|---|---|
| `core:*`, `data:*`, `build-logic`, `debugtool` | **Team Core** |
| `feature:*` (mỗi feature một team, hoặc một team giữ nhiều feature) | **Các team Feature** |
| `:app` | Team Core (chỉ là nơi lắp ráp; feature team review phần đăng ký của mình) |

Phụ thuộc giữa các feature:

| Feature | Phụ thuộc |
|---|---|
| `feature:home` | `data:pokemon:api`, `details:api` (mở Details qua `DetailsNavKey`) |
| `feature:details` | `data:pokemon:api`, `favourite:api` (nút add/remove favourite), `home:api` (deeplink dựng back stack `[HomeNavKey, DetailsNavKey]`) |
| `feature:favourite` | `data:pokemon:api`, `details:api`, `auth:api` |
| `feature:profile` | `auth:api` |

- Mỗi tab / màn hình là một feature độc lập, không bị nghẽn qua một module chung.
- Sửa `impl` không làm các module phụ thuộc `api` build lại; chỉ đổi contract trong `api` mới cần thống nhất giữa các team.
- Đổi contract của `core` / `data` → team Core review.
- Gắn CODEOWNERS theo thư mục.

### 4.2. Core chỉ cung cấp function

| Module | Cung cấp | Không làm (thuộc về module khác) |
|---|---|---|
| `core:storage` | Key-value store generic | Flag onboarding → `onboarding:impl` |
| `core:securestorage` | Lưu/đọc có mã hóa | Token → `auth:impl` |
| `core:network` | HttpClient, JSON, mapping lỗi, `NetworkMonitor`, hook interceptor | Base URL → `:app`; endpoint, DTO → feature gọi API; network log → `debugtool` |
| `core:ui` | `RemoteImage` (bọc Coil) | Cấu hình `ImageLoader` → `:app`; URL ảnh Pokémon → `data:pokemon` |
| *(không có `core:database`)* | — | Room yêu cầu mọi entity trong một class `@Database` → DB chung buộc core biết entity. Mỗi module có DB riêng, dùng chung cấu hình qua plugin `pokepoke.room` |
| *(không có `core:locale`)* | — | Xem mục 9 |

### 4.3. Core tách `api` / `impl` (đã chốt)

- **Tách** cho core có implementation thay được: `network`, `storage`, `securestorage`, `navigation`, `deeplink`. Module khác chỉ phụ thuộc `:api`; fake để trong `testFixtures` của `:api`.
- **Kotlin/JVM thuần** cho `network:api`, `storage:api`, `securestorage:api`, `data:pokemon:api` và `common-kotlin`; `navigation:api` / `deeplink:api` giữ Android library (xem build.md §3).
- **Không tách** `api`/`impl` cho `designsystem`, `ui`, `testing`; `common` tách theo nền tảng (`kotlin` / `android`) — code UI / tiện ích thuần, tách ra chỉ thêm module rỗng.

## 5. Dữ liệu: business model & single source of truth

### 5.1. `data:pokemon` — kho dữ liệu, không gọi network

```
data:pokemon:api
  model:   PokemonSummary(id, name, imageUrl), PokemonDetail(...), EvolutionStage(...)
  PokemonRepository (mọi hàm trả DataResult / Flow<DataResult>):
    observeList(limit), observeTotalCount(), saveList(items, offset, totalCount)
    saveSummary(summary), observeDetail(id), saveDetail(detail)
    observeByNameOrId(query), observeSummaries(ids)
  PokemonImageUrls.kt  ← logic dựng URL ảnh (mục 5.3)
  (chữ ký đầy đủ: design/contracts.md §8)

data:pokemon:impl
  Room DB, entity, mapper entity ↔ model (không có DTO, không có network)
```

- Use case của feature gọi remote repository của mình (API → business model) rồi gọi `save*`; đọc lại qua `observe*`. ViewModel không biết repository (mục 7.1).
- **Luật merge do `data:pokemon:impl` nắm:** `saveList` chỉ upsert `name` / `position`, không ghi đè dữ liệu detail; `saveDetail` không làm mất `position` của list. Có unit test riêng.
- Lưu cột `position` (thứ tự trong API) để sắp xếp list — không sắp theo `id` vì PokeAPI có các form id 10001+ ở cuối danh sách.
- Upsert theo `id`, không xóa rồi ghi lại. Không xóa cache khi logout (dữ liệu public).

### 5.2. Chiến lược tải: network-first, cache khi không có network

Mục tiêu: UI chỉ cập nhật **một lần** cho mỗi lần tải (không hiện cache rồi nhảy sang dữ liệu mới).

```
load():
  emit Loading
  if NetworkMonitor.isOnline == false      → dùng cache ngay (không chờ timeout)
  else fetch → save vào storage (DataResult)
       Failure(Network.NoConnection | Timeout) → dùng cache
       Failure khác (HTTP 4xx/5xx, parse)      → Error (riêng search: 404 → kết quả "không tìm thấy", không phải lỗi)
  bắt đầu đọc storage → emit Content(data, isOffline)
  không có dữ liệu trong cache                 → Error(NoConnection) + retry
```

- **Chỉ bắt đầu collect Flow từ storage sau khi fetch xong / xác nhận offline**, nên lần emit đầu tiên đã là dữ liệu cuối cùng. Sau đó vẫn tiếp tục observe storage để nhận thay đổi thật (vd. favourite đổi).
- Dùng cache do offline → `UiState` có cờ `isOffline` để hiện banner nhỏ "đang xem dữ liệu đã lưu".
- `NetworkMonitor` (trong `core:network:api`): `isOnline: Flow<Boolean>` dựa trên `ConnectivityManager`. Fake trong `testFixtures`.
- Logic này viết một lần dạng helper generic trong `core:common-kotlin` (`runNetworkFirst`, `networkFirst`), **use case** gọi helper và truyền lambda. Unit test riêng cho helper.

### 5.3. URL ảnh: `PokemonImageUrls.kt`

- API list chỉ trả `name` + `url`: **id** tách từ `url` (`.../pokemon/25/` → 25); **ảnh** dựng theo quy ước repo sprites của PokeAPI (vd. `.../sprites/pokemon/other/official-artwork/{id}.png`).
- Toàn bộ quy ước này nằm trong **một file duy nhất** `data/pokemon/api/.../PokemonImageUrls.kt` (Kotlin thuần): `fun artworkUrl(id: Int): String`, và hàm tách id từ `url`. Đổi quy ước chỉ sửa một chỗ.
- Có unit test riêng trong `data:pokemon:api`.

### 5.4. Danh sách (tab Home) — không dùng thư viện Paging

Giới hạn ở tầng view, chỉ dùng Flow, không phụ thuộc Android framework:

```
HomeViewModel:
  visibleCount = 20
  list = observePokemonList(limit = visibleCount)  ← UI chỉ đọc từ đây (use case → PokemonRepository)
  onStart / refresh: loadPokemonPage(offset = 0) theo mục 5.2
  onReachEnd:        loadPokemonPage(offset = visibleCount) theo mục 5.2
                     → Success (fetch thành công hoặc xác nhận offline có cache) mới tăng visibleCount
```

- Tăng `visibleCount` **sau khi** trang đã được lưu → list cập nhật một lần, không nhảy.
- Trong lúc tải trang tiếp: loading ở cuối list. Offline và cache có đủ trang đó → hiện từ cache; không có → dòng lỗi + retry ở cuối list.
- Hết dữ liệu: dựa vào `count` trong response API (lưu cùng `saveList`); `visibleCount >= totalCount` thì ngừng.
- Đang tải một trang thì bỏ qua các lần `onReachEnd` tiếp theo.
- List item hiển thị: **#id, tên, ảnh** (dựng theo mục 5.3).

### 5.5. Search — tìm chính xác

API không hỗ trợ fuzzy search → chỉ tìm **chính xác**:
- Nhập tên hoặc id → chuẩn hóa (trim, lowercase) → gọi `/pokemon/{name|id}` theo mục 5.2.
- Tìm thấy → `saveSummary` → kết quả **đọc từ storage** (`observeByNameOrId`). Toàn bộ trong `SearchPokemonUseCase`.
- 404 → trạng thái "không tìm thấy". Offline → tìm chính xác trong storage.
- Tắt search → quay về list hiện tại.

### 5.6. Details — có cache

- `details:impl` gọi `/pokemon/{id}`, `/pokemon-species/{id}`, `/evolution-chain/{id}` → map → `saveDetail`, theo mục 5.2 (`ObservePokemonDetailUseCase`).
- UI đọc `observeDetail(id)` (qua use case) sau khi fetch xong / xác nhận offline. Offline mà chưa có cache → màn lỗi + retry.

### 5.7. Favourite

- `favourite:impl` có Room DB riêng, **chỉ lưu id** (+ thời điểm thêm để sắp xếp).
- Dữ liệu hiển thị lấy từ `PokemonRepository` (đã có vì Details lưu trước khi user bấm favourite) → không cần snapshot, SSOT trọn vẹn.
- `FavouriteRepository` **internal** trong `favourite:impl`. Details chỉ thấy `ObserveIsFavouriteUseCase`, `ToggleFavouriteUseCase` trong `favourite:api`.

### 5.8. Hình ảnh

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

- **`:api`** khai báo `NavKey` public (vd. `DetailsNavKey(id)`, `LoginNavKey(pending)`, `HomeNavKey`, `FavouriteNavKey`, `ProfileNavKey`); feature khác navigate bằng key này. Mỗi tab là một key gốc của back stack; đổi tab = `ResetTo([tabKey])`.
- **`:impl`** đăng ký:
  - Màn hình cho từng key.
  - Deeplink handler của chính feature (vd. `pokepoke://pokemon/25` → `DetailsNavKey(25)`).
  - Tab (`TopLevelDestination`), nếu feature là một tab.
- Màn hình cần đăng nhập: key implement `RequiresAuth`.
- **`auth:impl`** đóng góp `NavigationInterceptor`: key có `RequiresAuth` mà chưa đăng nhập → chuyển sang `LoginNavKey(redirect = key)`, đăng nhập xong mở tiếp màn hình đích.
- **`main:impl`** dựng bottom bar / navigation rail từ `Set<TopLevelDestination>`, không biết tab nào thuộc feature nào.
- **`onboarding:impl`** cũng đóng góp `NavigationInterceptor` (chạy trước auth): chưa xem onboarding → chuyển sang `OnboardingNavKey(pending)`, xong thì tiếp tục request ban đầu.

### 6.5. `:app`

- Single Activity với `NavDisplay`, gom entries, interceptors, deeplink handlers từ Hilt.
- Nhận intent (deeplink / app link / notification) → `DeepLinkDispatcher`.
- SplashScreen API: giữ splash tới khi back stack có màn đầu tiên. App luôn `ResetTo([HomeNavKey])` (hoặc dispatch deeplink); interceptor onboarding / auth tự chuyển hướng nếu cần.

## 7. Presentation: 1 UiState + 1 UiAction mỗi screen

Mỗi screen gồm 4 phần, đặt trong package `presentation/<screen>/` của `:impl`:

```kotlin
// HomeUiState.kt — immutable, mô tả toàn bộ những gì screen hiển thị
data class HomeUiState(
    val items: List<PokemonSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val message: UiMessage? = null,   // snackbar / lỗi một lần, xóa qua action MessageShown
)

// HomeUiAction.kt — mọi thao tác của user trên screen
sealed interface HomeUiAction {
    data object Refresh : HomeUiAction
    data object ReachEnd : HomeUiAction
    data class OpenDetails(val id: Int) : HomeUiAction
    data object MessageShown : HomeUiAction
}

// HomeViewModel.kt — một StateFlow, một entry point
class HomeViewModel : ViewModel() {
    val uiState: StateFlow<HomeUiState>
    fun onAction(action: HomeUiAction)
}

// HomeScreen.kt
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {     // root: phụ thuộc ViewModel, không preview
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeUiAction) -> Unit) { ... }  // stateless, preview được
```

- **Root (`XxxRoute`)**: thứ duy nhất biết ViewModel; chỉ collect state và chuyển action. Được đăng ký vào `EntryProviderInstaller`.
- **Screen (`XxxScreen`) và các composable con**: chỉ nhận `state` + `onAction` (hoặc tham số thuần), không biết ViewModel / Hilt → **preview được** với `PreviewParameterProvider` cung cấp các state mẫu (loading, content, offline, error, empty).
- **Navigation**: cũng là action (vd. `OpenDetails`); ViewModel xử lý bằng `Navigator` inject qua Hilt → navigation có unit test như mọi action khác, interceptor (auth) được áp dụng tự động.
- **Sự kiện một lần** (snackbar, lỗi): nằm trong `UiState` (`message`), UI hiển thị xong gửi action `MessageShown` để xóa — không dùng `Channel`/`SharedFlow` riêng.
- **Test**:
  - ViewModel: unit test — gửi `UiAction`, assert `UiState` (Turbine) và lệnh gọi `Navigator` (fake).
  - Screen: Compose UI test + screenshot test (Roborazzi) trên chính các state mẫu của preview.

### 7.1. Layer & kiểu kết quả (chi tiết: [design/layers.md](design/layers.md))

```
Screen ──UiAction──▶ ViewModel ──▶ UseCase ──▶ Repository ──▶ Data source (Retrofit, Room, DataStore)
       ◀──UiState───           ◀AppResult◀            ◀DataResult◀          ◀ giá trị / exception
```

- **Data source** được ném exception. **Repository** là chỗ duy nhất bắt exception và dịch thành `DataError` cụ thể (`Network.NoConnection`, `Network.Timeout`, `Network.Http(code)`, `NotFound`, `Local.Read`...), trả `DataResult`.
- **UseCase** chứa logic nghiệp vụ (network-first, kết hợp nhiều repository), dịch `DataError` → `AppError`, luôn trả `AppResult` / `Flow<AppResult>`.
- **ViewModel** chỉ inject use case + `Navigator`, chỉ xử lý `AppResult` → `UiState`. Không `try`/`catch`.
- Kết quả nghiệp vụ (sai mật khẩu, chưa đăng nhập, dữ liệu offline) là giá trị trong `Success`; `Failure` chỉ cho lỗi kỹ thuật.
- Luật là **quy ước bắt buộc** ghi trong layers.md (§5 là checklist review); chưa dùng thư viện kiểm tra tự động.

## 8. Module `:debugtool`

Công cụ cho dev / QA, **chỉ có trong debug build**, độc lập với feature.

### 8.1. Tính năng

| Tính năng | Cách lấy dữ liệu (generic, không biết feature) |
|---|---|
| **Network log** — request/response, header, body, thời gian, status | Đóng góp một OkHttp `Interceptor` vào hook `Set<Interceptor>` của `core:network:impl`; lưu ring buffer trong bộ nhớ (vd. 500 request gần nhất) |
| **Local DB** — liệt kê DB, bảng, xem dữ liệu, tìm kiếm / chạy câu `SELECT` | Duyệt `context.databaseList()`, mở **read-only** bằng `SQLiteDatabase`; đọc schema từ `sqlite_master` → không cần biết entity của feature |
| **Local storage** — xem key/value | Hàm generic `observeAll()` trong `core:storage:api`; `core:securestorage` chỉ hiện key, value bị che (bấm để hiện) |

### 8.2. Độc lập & chỉ debug

- `:app` khai báo `debugImplementation(projects.debugtool)` — release build không chứa một dòng code nào của debugtool. Check ở mục 2.3 bảo đảm điều này.
- **Không module nào khác phụ thuộc `:debugtool`**; feature không biết nó tồn tại. Debugtool chỉ phụ thuộc `core:*` (`designsystem`, `ui`, `network:api`, `storage:api`, `securestorage:api`).
- Kết nối hoàn toàn qua Hilt multibinding / contract generic của core: Hilt tự gom module của debugtool khi build debug, release thì hook `Set<Interceptor>` rỗng (khai báo `@Multibinds` trong `core:network:impl`).
- **Một module duy nhất, không tách `api`/`impl`**: không ai gọi vào debugtool nên không có contract để public.
- **Điểm vào: nút con bọ 🐞 overlay trên mọi activity**
  - Debugtool tự đăng ký `ActivityLifecycleCallbacks` qua `androidx.startup` `Initializer` khai báo trong manifest của debugtool (chỉ được merge ở debug) → `:app` không phải gọi gì.
  - Mỗi activity được tạo → gắn một `ComposeView` chứa nút con bọ nổi vào `android.R.id.content` của activity đó. Không cần quyền `SYSTEM_ALERT_WINDOW`.
  - Nút kéo thả được, tự bám mép màn hình, nhớ vị trí; nhấn giữ để ẩn tạm.
  - Bấm nút → mở `DebugToolActivity` riêng (không gắn nút vào chính activity này). Không chen vào navigation của app.
- UI dùng `core:designsystem` để thống nhất style; theo cùng pattern UiState / UiAction (mục 7).
- Có test riêng: interceptor ghi log đúng, ring buffer, đọc schema SQLite (Robolectric).

## 9. Đa ngôn ngữ

Chuỗi giao diện nằm ở `:translation` (mục 9a); việc đổi ngôn ngữ không cần module riêng:
- `profile:impl` gọi thẳng `AppCompatDelegate.setApplicationLocales(...)`.
- Lựa chọn được lưu tự động: Android 13+ do hệ thống; Android 11–12 do AppCompat (`autoStoreLocales` trong manifest). Không lưu vào `core:storage`.
- Chưa chọn → theo ngôn ngữ máy (không phải en/vi → en). Đã chọn → giữ nguyên dù đổi ngôn ngữ máy.
- `locales_config.xml` + cấu hình manifest ở `:app`.

### 9a. `:translation`

- Nằm ở root, **ngang hàng** `core` / `data` / `feature` (không thuộc `core`); không phụ thuộc module nào, mọi module khác được phụ thuộc.
- Module Android library chỉ chứa resource. Mỗi nhóm một file `strings_<module>.xml` ở `values/` (en) và `values-vi/` (vi); key có tiền tố module. Chuỗi không cần dịch (vd. `app_name`) ở `values/non-translation.xml`.
- Module khác dùng `import com.khangtm.pokepoke.translation.R`. Hiện không có code; nếu sau này cần remote translation thì thêm vào module này mà không đụng các feature.
- `:debugtool` giữ strings riêng (chỉ `values/`), vì debugtool không được lọt vào release.
- Test kiểm tra khớp key / placeholder giữa các ngôn ngữ.

## 10. Testing

- Mỗi module (core, data, feature, debugtool) có `src/test` riêng, và `src/androidTest` khi cần. **Không** có module chứa test của module khác.
- `:core:testing` chỉ là thư viện tiện ích: `MainDispatcherRule`, helper Turbine / Compose test.
- Fake của interface public nằm trong `testFixtures` của module `:api` tương ứng (vd. `FakePokemonRepository` trong `data:pokemon:api`, `FakeNetworkMonitor` trong `core:network:api`); module khác dùng qua `testImplementation(testFixtures(projects.data.pokemon.api))`.
- Convention plugin tự thêm thư viện test theo loại module:

| Loại module / layer | Loại test |
|---|---|
| ViewModel (fake use case) / UseCase (fake repository) / mapper / `PokemonImageUrls` / helper `networkFirst` | Unit test: JUnit, MockK, Turbine, coroutines-test |
| Repository gọi API | MockWebServer (DTO parsing, dịch từng loại lỗi → `DataError`) |
| Module có Room (`data:pokemon`, `favourite`) | DAO + luật merge với in-memory DB (Robolectric) |
| UI (`impl`, `designsystem`, `ui`, `debugtool`) | Compose UI test (Robolectric), screenshot test (Roborazzi) trên các state mẫu |
| `:app` | `androidTest` end-to-end cho navigation, deeplink, auth guard |

- Kover: coverage từng module, có thể đặt ngưỡng tối thiểu từng module.

## 11. DI: Hilt (đã chốt)

- Binding nằm trong `:impl`, chỉ tổng hợp ở `:app`.
- Multibinding cho `EntryProviderInstaller`, `NavigationInterceptor`, `TopLevelDestination`, `DeepLinkHandler`, `Interceptor` (network).

## 12. Các thay đổi nhỏ đi kèm

- **`settings.gradle.kts`**: thêm `includeBuild("build-logic")` và `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` → khai báo dependency bằng `projects.data.pokemon.api` thay vì chuỗi.
- **`gradle.properties`**: bật `org.gradle.parallel=true`, `org.gradle.caching=true`.
- **`libs.versions.toml`**:
  - Nâng các thư viện cũ: `coreKtx 1.10.1`, `lifecycle 2.6.1`, `activityCompose 1.8.0`.
  - Thêm thư viện sắp dùng: hilt, ksp, navigation3, material3-adaptive, retrofit/ktor (trên OkHttp), kotlinx-serialization, room, datastore, coil, core-splashscreen, appcompat (per-app locale), turbine, mockk, mockwebserver, robolectric, kover, roborazzi. **Không** dùng thư viện Paging.
  - Thêm `[plugins]` cho các convention plugin.
- **AGP 9** đã tích hợp sẵn Kotlin → không cần plugin `kotlin-android`; convention plugin chỉ thêm compose compiler, ksp, hilt.

## 13. Thứ tự implement

1. Tạo `build-logic` với các convention plugin (kèm check kiến trúc), chuyển `:app` sang dùng.
2. Thêm `version.properties` và logic đọc version.
3. Tạo skeleton các module `core`, `data`, `feature`, `debugtool` (rỗng, build được, mỗi module có sẵn thư mục test).
4. Chạy `./gradlew build` để kiểm tra.

## 14. Quyết định

- [x] DI: **Hilt**.
- [x] Java: **21** (toolchain + target).
- [x] Version: `version.properties`, sửa tay; cách tăng trên CI để sau.
- [x] Tên module: `core:storage` / `core:securestorage`.
- [x] Không có `core:locale`; ngôn ngữ do AppCompat lưu.
- [x] `data:pokemon` là kho dữ liệu (SSOT), không gọi network; feature tự fetch rồi save.
- [x] Module feature: `main` (khung tab), `home`, `details`, `favourite`, `profile`, `onboarding`, `auth`.
- [x] Team Core sở hữu `core:*`, `data:*`, `build-logic`, `debugtool`; các team Feature sở hữu `feature:*`.
- [x] Network-first: fetch trước, chỉ dùng cache khi xác nhận không có network; UI cập nhật một lần.
- [x] List không dùng thư viện Paging; giới hạn ở tầng view.
- [x] Search chính xác (tên / id).
- [x] Cache list + details trong Room; ảnh cache bằng Coil khi hiển thị.
- [x] Ảnh list dựng theo quy ước, logic nằm trong `PokemonImageUrls.kt`.
- [x] Mỗi screen: 1 `UiState` + 1 `UiAction`; root phụ thuộc ViewModel, UI còn lại stateless & preview được.
- [x] Single Activity.
- [x] Core tách `api`/`impl` theo mục 4.3: tách `network`, `storage`, `securestorage`, `navigation`, `deeplink`; không tách `common`, `designsystem`, `ui`, `testing`.
- [x] Navigation: **Navigation 3**.
- [x] `:debugtool`: một module, chỉ `debugImplementation`, độc lập, chỉ dùng `core`; điểm vào là nút con bọ overlay trên mọi activity.
