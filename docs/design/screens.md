# Screens

> Mọi screen theo pattern ở [plan.md](../plan.md) mục 7. Contract ở [contracts.md](contracts.md), layer ở [layers.md](layers.md), component ở [designsystem.md](designsystem.md), deeplink ở [deeplinks.md](deeplinks.md).

## 0. Quy ước file cho mỗi screen

```
feature/<x>/impl/src/main/kotlin/com/khangtm/pokepoke/feature/<x>/impl/
  data/            ← data source (Retrofit service, Room DAO), DTO, mapper DTO → model, Repository (interface + Impl)
  domain/          ← UseCase (fun interface + Impl), model nội bộ của domain; ViewModel chỉ gọi use case
  presentation/<screen>/
    <Screen>UiState.kt
    <Screen>UiAction.kt
    <Screen>ViewModel.kt
    <Screen>Route.kt       ← chỉ chứa @Composable <Screen>Route(...) (excluded khỏi coverage)
    <Screen>Screen.kt      ← @Composable <Screen>Screen(state, onAction, modifier) + các composable con
    <Screen>Previews.kt    ← PreviewParameterProvider + @Preview
  navigation/      ← EntryProviderInstaller, TopLevelDestination, DeepLinkHandler, interceptor
  di/              ← Hilt module
```

- ViewModel nhận tham số từ key qua **assisted injection**: `@HiltViewModel(assistedFactory = X.Factory::class)` + `hiltViewModel<X, X.Factory>(creationCallback = { it.create(key.pokemonId) })`.
- Layer, kiểu kết quả, luật lỗi: [layers.md](layers.md). ViewModel chỉ inject UseCase + `Navigator` + tham số `@Assisted`.
- `Failure(e)` hiển thị dạng snackbar dùng `e.toUiMessage()` (core:ui), trừ khi screen nói khác.
- Route nhận `modifier` và ViewModel mặc định `hiltViewModel()`.
- Screen nhận thêm `snackbarHostState` nếu cần hiển thị `UiMessage`.
- Test mỗi screen: `<Screen>ViewModelTest` (unit, fake use case), `<UseCase>Test` cho mỗi use case (fake repository), `<Repository>Test` cho mỗi repository (data source thật trong môi trường test, phủ bảng lỗi layers.md §2) + `<Screen>ScreenTest` (Compose UI test Robolectric: hiển thị đúng theo state, click phát đúng action) + `<Screen>ScreenshotTest` (Roborazzi trên các state mẫu của Previews).

---

## 1. `:app` và `:feature:main` (shell)

### 1.1. `:app`

- `PokePokeApplication` (`@HiltAndroidApp`, implements `SingletonImageLoader.Factory`): Coil `ImageLoader` với `OkHttpNetworkFetcherFactory` dùng **OkHttpClient riêng** (không có interceptor debug), `crossfade(true)`.
- Hilt module của app: `@BaseUrl` = `"https://pokeapi.co/api/v2/"`, `DeepLinkConfig(setOf("pokepoke"), setOf("pokepoke.com"))`.
- `MainActivity : AppCompatActivity` (`@AndroidEntryPoint`; AppCompat cần để đổi ngôn ngữ trên Android 11–12):
  - `installSplashScreen().setKeepOnScreenCondition { navigator.backStack.isEmpty() }`, `enableEdgeToEdge()`.
  - `savedInstanceState == null && navigator.backStack.isEmpty()`: nếu `intent.data != null` → `deepLinkDispatcher.dispatch(uri)`; nếu không có hoặc dispatch trả `false` → `navigator.execute(ResetTo(listOf(HomeNavKey)))`.
  - `onNewIntent`: `intent.data?.let { deepLinkDispatcher.dispatch(it.toString()) }`.
  - `setContent { PokeTheme { MainShell(navigator, destinations) { NavDisplay(...) } } }`.
- `NavDisplay`:
  - `backStack = navigator.backStack`, `onBack = { if (!navigator.goBack()) finish() }`.
  - `entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator())`.
  - `sceneStrategy = rememberListDetailSceneStrategy()`.
  - `entryProvider = entryProvider { installers.forEach { it.install(this) } }`.
- Manifest: `MainActivity` `launchMode="singleTask"`, `configChanges="orientation|screenSize|screenLayout|smallestScreenSize"`, theme `Theme.PokePoke.Starting`, intent-filter theo deeplinks.md; `android:enableOnBackInvokedCallback="true"`; service `androidx.appcompat.app.AppLocalesMetadataHolderService` (`enabled=false`, `exported=false`, meta-data `autoStoreLocales=true`).
- Locale: `res/resources.properties` chứa `unqualifiedResLocale=en`; `androidResources.generateLocaleConfig = true`.
- `app_name` = "PokePoke" (cả en/vi).
- Theme XML: `Theme.PokePoke.Starting` (parent `Theme.SplashScreen`, `windowSplashScreenBackground` = primary light `#B8401F` / dark `#8F2A0B` trong `values-night`, `windowSplashScreenAnimatedIcon` = `@drawable/ic_launcher_foreground`, `postSplashScreenTheme` = `@style/Theme.PokePoke`).

### 1.2. `MainShell` (`:feature:main:impl`)

```kotlin
@Composable
fun MainShell(navigator: Navigator, destinations: Set<TopLevelDestination>, content: @Composable () -> Unit)
```

- `NavigationSuiteScaffold`: item từ `destinations.sortedBy { it.order }`; label `stringResource(label)`; icon `selectedIcon` khi chọn.
- Tab được chọn = destination có `key == navigator.backStack.firstOrNull()`.
- Click tab: `navigator.execute(ResetTo(listOf(dest.key)))` (click lại tab đang chọn cũng reset về gốc tab).
- **Ẩn** thanh điều hướng khi `backStack.lastOrNull() is FullScreen` hoặc `backStack.firstOrNull()` không phải key của destination nào (vd. đang ở Onboarding).
- Kiểu thanh (bottom bar / rail) do `NavigationSuiteScaffold` tự chọn theo window size.

### 1.3. List-detail (điện thoại gập / tablet)

- Entry `HomeNavKey`, `FavouriteNavKey`: `metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { DetailPlaceholder() })`.
- Entry `DetailsNavKey`: `metadata = ListDetailSceneStrategy.detailPane()`.
- `DetailPlaceholder` (trong home và favourite impl, dùng `EmptyState` + icon Pokeball): `home_detail_placeholder` "Select a Pokémon to see details" / "Chọn một Pokémon để xem chi tiết".
- Mở Details từ list: nếu `backStack.last() is DetailsNavKey` → `ReplaceTop(DetailsNavKey(id))`, ngược lại `Push(DetailsNavKey(id))`.

---

## 2. Onboarding (`:feature:onboarding:impl`)

Key: `OnboardingNavKey(pending)` (FullScreen). Không có tab.

```kotlin
data class OnboardingUiState(val page: Int = 0, val pageCount: Int = 3, val isFinishing: Boolean = false) {
    val isLastPage get() = page == pageCount - 1
}
sealed interface OnboardingUiAction {
    data class PageChanged(val page: Int) : OnboardingUiAction
    data object Next : OnboardingUiAction      // trang cuối → Finish
    data object Skip : OnboardingUiAction      // = Finish
    data object Finish : OnboardingUiAction
}
```

- Layout: `HorizontalPager` 3 trang. Mỗi trang: icon minh họa 120.dp màu primary trong vòng tròn `primaryContainer` 200.dp, tiêu đề `headlineMedium`, mô tả `bodyLarge` căn giữa. Dưới: page indicator (3 chấm, chấm hiện tại dài 24.dp màu primary), nút chính "Next"/"Get started". Góc trên phải: "Skip" (ẩn ở trang cuối).
- Use case (internal): `CompleteOnboardingUseCase`, `IsOnboardingCompletedUseCase` (contracts.md §9).
- `Finish`: `isFinishing = true` → `completeOnboarding()` → (Success hoặc Failure, đều đi tiếp) `navigator.execute(key.pending)`.
- Back hệ thống ở onboarding: thoát app (stack chỉ có 1 key).

| Trang | Icon | Title (en / vi) | Body (en / vi) |
|---|---|---|---|
| 1 | Pokeball | Discover Pokémon / Khám phá Pokémon | Browse more than a thousand Pokémon with artwork and details. / Duyệt hơn một nghìn Pokémon kèm hình ảnh và thông tin chi tiết. |
| 2 | Search | Find them fast / Tìm kiếm nhanh | Search any Pokémon by its exact name or number. / Tìm Pokémon theo đúng tên hoặc số thứ tự. |
| 3 | FavouriteSelected | Keep your favourites / Lưu Pokémon yêu thích | Sign in to save Pokémon and come back to them anytime. / Đăng nhập để lưu Pokémon và xem lại bất cứ lúc nào. |

Nút: `onboarding_next` Next/Tiếp, `onboarding_skip` Skip/Bỏ qua, `onboarding_get_started` Get started/Bắt đầu.

---

## 3. Login (`:feature:auth:impl`)

Key: `LoginNavKey(pending)` (FullScreen).

```kotlin
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val showInvalidCredentials: Boolean = false,
    val message: UiMessage? = null,
) { val canSubmit get() = username.isNotBlank() && password.isNotEmpty() && !isSubmitting }

sealed interface LoginUiAction {
    data class UsernameChanged(val value: String) : LoginUiAction   // xóa showInvalidCredentials
    data class PasswordChanged(val value: String) : LoginUiAction   // xóa showInvalidCredentials
    data object TogglePasswordVisibility : LoginUiAction
    data object Submit : LoginUiAction
    data object Close : LoginUiAction                               // navigator.goBack()
    data object MessageShown : LoginUiAction
}
```

- Layout: `PokeTopAppBar(title = "", onBack = Close, icon Close)`. Nội dung căn giữa, rộng tối đa `maxContentWidth`: icon Pokeball 72.dp, `auth_title` "Sign in"/"Đăng nhập" (headlineMedium), `auth_subtitle` "Sign in to save your favourite Pokémon." / "Đăng nhập để lưu Pokémon yêu thích." , `PokeTextField` username (`auth_username` Username/Tên đăng nhập, ImeAction.Next), password (`auth_password` Password/Mật khẩu, isPassword, ImeAction.Done → Submit), lỗi `auth_invalid_credentials` "Incorrect username or password." / "Sai tên đăng nhập hoặc mật khẩu." (supportingText của password, isError cả 2 field), `PokePrimaryButton(auth_sign_in "Sign in"/"Đăng nhập", enabled = canSubmit, loading = isSubmitting)`.
- Submit: `isSubmitting = true` → `login(username, password)` (`LoginUseCase`, contracts.md §10):
  - `Success(LoginOutcome.LoggedIn)` → `navigator.goBack()` rồi `pending?.let(navigator::execute)`.
  - `Success(LoginOutcome.InvalidCredentials)` → `showInvalidCredentials = true`, `isSubmitting = false`.
  - `Failure(e)` → `message = e.toUiMessage()`, `isSubmitting = false`.
- Màn có `SnackbarHost`.
- Back hệ thống = Close.

---

## 4. Home (`:feature:home:impl`)

Key: `HomeNavKey` (tab order 0, label `home_tab` Home/Trang chủ, icon Home).

### 4.1. API (`data/`)

```kotlin
interface HomeApi {
    @GET("pokemon") suspend fun list(@Query("offset") offset: Int, @Query("limit") limit: Int): PokemonListDto
    @GET("pokemon/{idOrName}") suspend fun pokemon(@Path("idOrName") idOrName: String): PokemonBasicDto
}
@Serializable data class PokemonListDto(val count: Int, val results: List<NamedResourceDto>)
@Serializable data class NamedResourceDto(val name: String, val url: String)
@Serializable data class PokemonBasicDto(val id: Int, val name: String)
```

Mapper: `NamedResourceDto` → `PokemonSummary(id = PokemonImageUrls.idFromResourceUrl(url), name, imageUrl = PokemonImageUrls.artworkUrl(id))` (bỏ item không parse được id). `PokemonBasicDto` → summary tương tự.

```kotlin
internal data class RemotePage(val items: List<PokemonSummary>, val totalCount: Int)
internal interface PokemonRemoteRepository {
    suspend fun fetchPage(offset: Int, limit: Int): DataResult<RemotePage>
    /** query đã chuẩn hóa; 404 → Failure(NotFound). */
    suspend fun fetchByNameOrId(query: String): DataResult<PokemonSummary>
}
```

### 4.1b. Domain (`domain/`, internal)

```kotlin
const val PAGE_SIZE = 20
internal data class PageResult(val visibleCount: Int, val isOffline: Boolean, val endReached: Boolean)
internal sealed interface SearchOutcome {
    data class Found(val pokemon: PokemonSummary, val isOffline: Boolean) : SearchOutcome
    data object NotFound : SearchOutcome
}

internal fun interface ObservePokemonListUseCase { operator fun invoke(limit: Int): Flow<AppResult<List<PokemonSummary>>> }
internal fun interface LoadPokemonPageUseCase { suspend operator fun invoke(offset: Int): AppResult<PageResult> }
internal fun interface SearchPokemonUseCase { suspend operator fun invoke(query: String): AppResult<SearchOutcome> }
```

- `ObservePokemonList(limit)` = `pokemonRepository.observeList(limit).toAppResult()`.
- `LoadPokemonPage(offset)`: `runNetworkFirst(networkMonitor::isOnlineNow) { remote.fetchPage(offset, PAGE_SIZE).flatMap { pokemonRepository.saveList(it.items, offset, it.totalCount) } }`; `target = offset + PAGE_SIZE`; `total` = `pokemonRepository.observeTotalCount().first()` (Failure → trả Failure); `endReached = total != null && target >= total`:
  - `Fetched` → `Success(PageResult(target, isOffline = false, endReached))`.
  - `Offline` → `pokemonRepository.observeList(target).first()`: `Success(list)` và `list.size > offset` → `Success(PageResult(target, isOffline = true, endReached))`; `Success` nhưng không có thêm item → `Failure(AppError.NoConnection)`; `Failure(e)` → `Failure(e.toAppError())`.
  - `Failed(e)` → `Failure(e.toAppError())`.
- `SearchPokemon(query)`: `q = query.trim().lowercase()` (rỗng → `Success(NotFound)`); `runNetworkFirst(...) { remote.fetchByNameOrId(q).flatMap(pokemonRepository::saveSummary) }`:
  - `Fetched` → `observeByNameOrId(q).first()` → `Success(Found(it, isOffline = false))` / Failure theo `toAppError()`.
  - `Offline` → `observeByNameOrId(q).first()` → `Success(Found(it, isOffline = true))`; `Failure(NotFound)` → `Failure(NoConnection)` (không biết có tồn tại hay không).
  - `Failed(DataError.NotFound)` (API 404) → `Success(SearchOutcome.NotFound)`; `Failed(e)` khác → `Failure(e.toAppError())`.

### 4.2. State / action

```kotlin
data class HomeUiState(
    val items: List<PokemonSummary> = emptyList(),
    val initialLoad: InitialLoad = InitialLoad.Loading,
    val loadMore: LoadMoreState = LoadMoreState.Idle,
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val message: UiMessage? = null,
)
sealed interface InitialLoad { data object Loading; data object Done; data class Error(val error: AppError) }
data class SearchUiState(val query: String = "", val isActive: Boolean = false, val result: SearchResult = SearchResult.Idle)
sealed interface SearchResult {
    data object Idle; data object Loading
    data class Found(val pokemon: PokemonSummary, val isOffline: Boolean)
    data object NotFound
    data class Error(val error: AppError)
}

sealed interface HomeUiAction {
    data object Retry : HomeUiAction            // retry initial load
    data object Refresh : HomeUiAction          // pull-to-refresh
    data object ReachEnd : HomeUiAction
    data object RetryLoadMore : HomeUiAction
    data class QueryChanged(val query: String) : HomeUiAction
    data object SubmitSearch : HomeUiAction
    data object ClearSearch : HomeUiAction
    data class OpenDetails(val pokemonId: Int) : HomeUiAction
    data object MessageShown : HomeUiAction
}
```

### 4.3. Logic (ViewModel)

ViewModel inject: `ObservePokemonListUseCase`, `LoadPokemonPageUseCase`, `SearchPokemonUseCase`, `Navigator`.

- `visibleCount: MutableStateFlow(0)`. `items` = `visibleCount.flatMapLatest { observePokemonList(it) }`: `Success(list)` → `items = list`; `Failure(e)` → `message = e.toUiMessage()`.
- Áp `PageResult p`: `visibleCount = p.visibleCount`, `isOffline = p.isOffline`, `loadMore = if (p.endReached) EndReached else Idle`.
- **Init / Retry**: `initialLoad = Loading` → `loadPokemonPage(0)`: `Success(p)` → áp `p`, `initialLoad = Done`; `Failure(e)` → `initialLoad = Error(e)`.
- **Refresh**: `isRefreshing = true` → `loadPokemonPage(0)`: `Success(p)` → áp `p` (nếu `p.isOffline` thêm `message = UiMessage(R.string.ui_offline_banner)`); `Failure(e)` → `message = e.toUiMessage()`. Cuối cùng `isRefreshing = false`.
- **ReachEnd** (bỏ qua nếu `loadMore` là Loading/EndReached hoặc `initialLoad != Done`) / **RetryLoadMore**: `loadMore = Loading` → `loadPokemonPage(visibleCount)`: `Success(p)` → áp `p`; `Failure(e)` → `loadMore = Error(e)`.
- **QueryChanged**: cập nhật query; query rỗng (sau trim) → như ClearSearch.
- **SubmitSearch** (query sau trim không rỗng): `isActive = true`, `result = Loading` → `searchPokemon(query)`: `Success(SearchOutcome.Found(p, offline))` → `Found(p, offline)`; `Success(SearchOutcome.NotFound)` → `NotFound`; `Failure(e)` → `Error(e)`.
- **ClearSearch**: `search = SearchUiState()` (về list hiện tại, giữ nguyên vị trí scroll).
- **OpenDetails**: theo screens §1.3.

### 4.4. Layout

- `Scaffold` + `SnackbarHost`. Phía trên: `PokeSearchField` (placeholder `home_search_hint` "Search by name or number" / "Tìm theo tên hoặc số"), padding `lg`. Có `isOffline` → `OfflineBanner` dưới search.
- Search active: thay list bằng kết quả — `Loading` → `LoadingState`; `Found` → một `MediaCard` (rộng tối đa 240.dp); `NotFound` → `EmptyState(home_search_not_found "No Pokémon named \"%1$s\"" / "Không có Pokémon nào tên \"%1$s\"")`; `Error` → `ErrorState(onRetry = SubmitSearch)`. Back hệ thống khi search active → ClearSearch (`BackHandler`).
- List: `PullToRefreshBox` + `LazyVerticalGrid(GridCells.Adaptive(gridMinCellWidth))`, mỗi item `MediaCard(title = displayName, overline = displayId, imageUrl)`, footer full span `LoadMoreFooter`. Gửi `ReachEnd` khi item cuối cùng hiển thị có index ≥ `items.size - 6`.
- `initialLoad`: Loading → `LoadingState`; Error → `ErrorState(onRetry = Retry)`.

### 4.5. Deeplink

`HomeDeepLinkHandler`: `[]` hoặc `["home"]` → `ResetTo(listOf(HomeNavKey))`.

---

## 5. Details (`:feature:details:impl`)

Key: `DetailsNavKey(pokemonId)` (`RequiresAuth`).

### 5.1. API

```kotlin
interface DetailsApi {
    @GET("pokemon/{id}") suspend fun pokemon(@Path("id") id: Int): PokemonDto
    @GET("pokemon-species/{id}") suspend fun species(@Path("id") id: Int): SpeciesDto
    @GET("evolution-chain/{id}") suspend fun evolutionChain(@Path("id") id: Int): EvolutionChainDto
}
@Serializable data class PokemonDto(
    val id: Int, val name: String, val height: Int, val weight: Int,
    @SerialName("base_experience") val baseExperience: Int? = null,
    val types: List<TypeSlotDto>, val abilities: List<AbilitySlotDto>, val stats: List<StatDto>,
    val sprites: SpritesDto, val species: NamedResourceDto,
)
@Serializable data class TypeSlotDto(val slot: Int, val type: NamedResourceDto)
@Serializable data class AbilitySlotDto(val slot: Int, @SerialName("is_hidden") val isHidden: Boolean, val ability: NamedResourceDto)
@Serializable data class StatDto(@SerialName("base_stat") val baseStat: Int, val stat: NamedResourceDto)
@Serializable data class SpritesDto(val other: OtherSpritesDto? = null)
@Serializable data class OtherSpritesDto(@SerialName("official-artwork") val officialArtwork: ArtworkDto? = null)
@Serializable data class ArtworkDto(@SerialName("front_default") val frontDefault: String? = null)
@Serializable data class SpeciesDto(@SerialName("evolution_chain") val evolutionChain: UrlDto)
@Serializable data class UrlDto(val url: String)
@Serializable data class EvolutionChainDto(val chain: ChainLinkDto)
@Serializable data class ChainLinkDto(
    val species: NamedResourceDto,
    @SerialName("evolution_details") val evolutionDetails: List<EvolutionDetailDto> = emptyList(),
    @SerialName("evolves_to") val evolvesTo: List<ChainLinkDto> = emptyList(),
)
@Serializable data class EvolutionDetailDto(
    val trigger: NamedResourceDto, @SerialName("min_level") val minLevel: Int? = null, val item: NamedResourceDto? = null,
)
```

`internal interface PokemonDetailRemoteRepository { suspend fun fetchDetail(id: Int): DataResult<PokemonDetail> }` — gọi 3 API + map **trong một** `dataResultOf(Throwable::toNetworkDataError)`. Thứ tự: `pokemon(id)` → species id = `idFromResourceUrl(species.url)` → `species(speciesId)` → chain id = `idFromResourceUrl(evolutionChain.url)` → `evolutionChain(chainId)` → map. Ảnh: `sprites.other.officialArtwork.frontDefault ?: artworkUrl(id)`. Stage tiến hóa: `PokemonSummary(speciesId, species.name, artworkUrl(speciesId))`; condition = `evolutionDetails.firstOrNull()`.

Domain (internal): `fun interface ObservePokemonDetailUseCase { operator fun invoke(id: Int): Flow<AppResult<DataSnapshot<PokemonDetail>>> }` = `networkFirst(networkMonitor::isOnlineNow, fetch = { remote.fetchDetail(id).flatMap(pokemonRepository::saveDetail) }, observe = { pokemonRepository.observeDetail(id) })`.

### 5.2. State / action

```kotlin
data class DetailsUiState(
    val content: LoadState<PokemonDetail> = LoadState.Loading,
    val isFavourite: Boolean = false,
    val message: UiMessage? = null,
)
sealed interface DetailsUiAction {
    data object Retry : DetailsUiAction
    data object Back : DetailsUiAction                         // navigator.goBack()
    data object ToggleFavourite : DetailsUiAction
    data class OpenEvolution(val pokemonId: Int) : DetailsUiAction   // id hiện tại → bỏ qua; khác → Push(DetailsNavKey(id))
    data object MessageShown : DetailsUiAction
}
```

ViewModel inject: `ObservePokemonDetailUseCase`, `ObserveIsFavouriteUseCase`, `ToggleFavouriteUseCase` (từ `favourite:api`), `Navigator`, `@Assisted pokemonId`.

- `content` = `retryTrigger.flatMapLatest { observePokemonDetail(id).map { it.toLoadState() }.onStart { emit(LoadState.Loading) } }` (`toLoadState()` ở core:ui); Retry → phát `retryTrigger`.
- `isFavourite` = `observeIsFavourite(id)`: `Success(b)` → `b`; `Failure` → `false` (không báo lỗi).
- ToggleFavourite → `toggleFavourite(id)` (`ToggleFavouriteUseCase`). `Failure` → `message` = `details_favourite_error` "Couldn't update favourites." / "Không cập nhật được danh sách yêu thích.". `Success(true)` / `Success(false)` → `message` = `details_added_favourite` "Added to favourites" / "Đã thêm vào yêu thích" hoặc `details_removed_favourite` "Removed from favourites" / "Đã xóa khỏi yêu thích".

### 5.3. Layout

- `PokeTopAppBar(title = displayName (khi có data), onBack = Back, actions = IconButton favourite (FavouriteSelected màu primary / Favourite), contentDescription details_add_favourite / details_remove_favourite)`.
- Loading → `LoadingState`; Error → `ErrorState(onRetry = Retry)`; Content → `LazyColumn` (có `OfflineBanner` đầu nếu isOffline):
  1. **Header**: nền màu type đầu tiên alpha 0.2, bo góc dưới `extraLarge`; `RemoteImage` `detailImage`; `displayId` (titleMedium onSurfaceVariant); tên (headlineMedium); hàng `PokeChip` cho từng type (màu bảng dưới, chữ trắng nếu luminance < 0.5, ngược lại đen; text từ string `details_type_<key>`).
  2. **About** (`details_section_about` About/Thông tin): 3 cột — Height `%.1f m` (dm / 10), Weight `%.1f kg` (hg / 10), Base exp (hoặc "—").
  3. **Abilities** (`details_section_abilities` Abilities/Kỹ năng): mỗi dòng `PokemonFormat.displayName(name)`, ability ẩn có `PokeChip(details_hidden_ability "Hidden"/"Ẩn")`.
  4. **Base stats** (`details_section_stats` Base stats/Chỉ số cơ bản): mỗi stat: nhãn (`details_stat_<key>`: HP, Attack/Tấn công, Defense/Phòng thủ, Sp. Atk/Tấn công đặc biệt, Sp. Def/Phòng thủ đặc biệt, Speed/Tốc độ), giá trị, `LinearProgressIndicator(value / 255f)` màu primary.
  5. **Evolution** (`details_section_evolution` Evolution/Tiến hóa): cây đệ quy — mỗi stage là thẻ nhỏ (ảnh 64.dp + tên), stage hiện tại viền primary 2.dp; con nối bằng hàng gồm icon mũi tên + text điều kiện. Nhiều nhánh → các nhánh xếp dọc. Click stage → `OpenEvolution`. Chuỗi chỉ có 1 stage → text `details_no_evolution` "This Pokémon does not evolve." / "Pokémon này không tiến hóa.".
- Điều kiện tiến hóa: LEVEL_UP + minLevel → `details_evo_level` "Lv. %1$d"; USE_ITEM + item → `details_evo_item` "Use %1$s" / "Dùng %1$s" (item qua `displayName`); TRADE → `details_evo_trade` Trade/Trao đổi; còn lại → `details_evo_other` Special/Đặc biệt.
- Màn rộng (≥ 600.dp): About / Abilities / Stats chia 2 cột dưới header.

Bảng màu type (`TypeColors.kt` trong details:impl): normal `#A8A77A`, fire `#EE8130`, water `#6390F0`, electric `#F7D02C`, grass `#7AC74C`, ice `#96D9D6`, fighting `#C22E28`, poison `#A33EA1`, ground `#E2BF65`, flying `#A98FF3`, psychic `#F95587`, bug `#A6B91A`, rock `#B6A136`, ghost `#735797`, dragon `#6F35FC`, dark `#705746`, steel `#B7B7CE`, fairy `#D685AD`, unknown `#68A090`.

### 5.4. Deeplink

`DetailsDeepLinkHandler`: `["pokemon", "{id}"]` với id là số nguyên dương → `ResetTo(listOf(HomeNavKey, DetailsNavKey(id)))` (details:impl phụ thuộc `home:api`).

---

## 6. Favourite (`:feature:favourite:impl`)

Key: `FavouriteNavKey` (tab order 1, label `favourite_tab` Favourites/Yêu thích, icon Favourite). Chỉ đọc local, không gọi API.

```kotlin
sealed interface FavouriteUiState {
    data object Loading : FavouriteUiState
    data object LoggedOut : FavouriteUiState
    data object Empty : FavouriteUiState
    data class Content(val items: List<PokemonSummary>, val message: UiMessage? = null) : FavouriteUiState
    data class Error(val error: AppError) : FavouriteUiState
}
sealed interface FavouriteUiAction {
    data object Login : FavouriteUiAction                       // Push(LoginNavKey(pending = null))
    data object Retry : FavouriteUiAction                       // subscribe lại observeFavouritePokemon()
    data class OpenDetails(val pokemonId: Int) : FavouriteUiAction
    data class Remove(val pokemonId: Int) : FavouriteUiAction   // Success → message favourite_removed "Removed %1$s" / "Đã xóa %1$s" (tên lấy từ items); Failure → favourite_remove_error "Couldn't remove from favourites." / "Không xóa được khỏi danh sách yêu thích."
    data object MessageShown : FavouriteUiAction
}
```

- ViewModel inject: `ObserveFavouritePokemonUseCase`, `RemoveFavouriteUseCase` (internal, contracts.md §11), `Navigator`.
- State = `observeFavouritePokemon()`: `Success(LoggedOut)` → LoggedOut; `Success(Items(empty))` → Empty; `Success(Items(list))` → Content (giữ `message` hiện có); `Failure(e)` → `Error(e)` → `ErrorState(onRetry = Retry)`.
- Layout: `PokeTopAppBar(favourite_title "Favourites"/"Yêu thích")`. LoggedOut → `EmptyState(icon Lock, title favourite_login_title "Sign in to see your favourites" / "Đăng nhập để xem Pokémon yêu thích", actionText auth chung `favourite_login_action` "Sign in"/"Đăng nhập")`. Empty → `EmptyState(icon Favourite, favourite_empty_title "No favourites yet" / "Chưa có Pokémon yêu thích", favourite_empty_message "Tap the heart on a Pokémon to save it here." / "Bấm vào trái tim ở trang chi tiết để lưu Pokémon vào đây.")`. Content → grid giống Home, `MediaCard.action` = `IconButton(Delete, contentDescription favourite_remove "Remove from favourites"/"Xóa khỏi yêu thích")`.
- Mở Details: theo screens §1.3.
- Deeplink: `["favourite"]` → `ResetTo(listOf(FavouriteNavKey))`.

---

## 7. Profile — tab Me (`:feature:profile:impl`)

Key: `ProfileNavKey` (tab order 2, label `profile_tab` Me/Tôi, icon Profile).

```kotlin
enum class AppLanguage { SYSTEM, ENGLISH, VIETNAMESE }   // trong profile:impl
data class ProfileUiState(
    val auth: AuthState? = null,          // null = đang tải
    val language: AppLanguage = AppLanguage.SYSTEM,
    val appVersion: String = "",          // "1.0.0 (1)"
    val showLanguagePicker: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val message: UiMessage? = null,
)
sealed interface ProfileUiAction {
    data object Login : ProfileUiAction                 // Push(LoginNavKey(null))
    data object OpenLanguagePicker : ProfileUiAction
    data class SelectLanguage(val language: AppLanguage) : ProfileUiAction
    data object DismissLanguagePicker : ProfileUiAction
    data object Logout : ProfileUiAction                // showLogoutConfirm = true
    data object ConfirmLogout : ProfileUiAction         // đóng dialog → logout() (LogoutUseCase) → luôn ResetTo(listOf(ProfileNavKey)); Failure → message profile_logout_error "Signed out, but some data couldn't be cleared." / "Đã đăng xuất nhưng chưa xóa hết dữ liệu."
    data object DismissLogout : ProfileUiAction
    data object MessageShown : ProfileUiAction
}
```

- ViewModel inject: `ObserveAuthStateUseCase`, `LogoutUseCase` (từ `auth:api`), `GetAppLanguageUseCase`, `SetAppLanguageUseCase`, `GetAppVersionUseCase` (internal), `Navigator`.
- Domain (internal): `GetAppLanguageUseCase(): AppResult<AppLanguage>`, `SetAppLanguageUseCase(language): AppResult<Unit>`, `GetAppVersionUseCase(): AppResult<AppVersion>` (từ `AppInfo`). Các use case này dùng thẳng `AppLocaleController` / `AppInfo` (không ném lỗi, layers.md §1) và trả `Success`.
- `auth` = `observeAuthState()`: `Success(s)` → `s`; `Failure` → `LoggedOut`. `language` = `getAppLanguage()` (Failure → SYSTEM). `appVersion` = `"${name} (${code})"` (Failure → "").
- SelectLanguage → `setAppLanguage(l)`: `Success` → `language = l`, đóng picker; `Failure(e)` → `message = UiMessage(R.string.profile_language_error)` "Couldn't change language." / "Không đổi được ngôn ngữ.".
- `auth == null` → nội dung phần tài khoản hiện `LoadingState` nhỏ (cao 72.dp).
- `interface AppLocaleController { fun current(): AppLanguage; fun set(language: AppLanguage) }` (internal trong impl) — bản thật: `AppCompatDelegate.getApplicationLocales()` rỗng → SYSTEM, tag `en` → ENGLISH, `vi` → VIETNAMESE; `set` gọi `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("" | "en" | "vi"))` trên main thread. Test dùng fake.
- Layout: `PokeTopAppBar(profile_title "Me"/"Tôi")`, nội dung `LazyColumn` rộng tối đa `maxContentWidth`:
  - LoggedIn: avatar tròn 72.dp nền `primaryContainer` chữ cái đầu username in hoa; username (titleLarge); `profile_logged_in_at` "Signed in %1$s" / "Đăng nhập lúc %1$s" (format `DateFormat.getDateTimeInstance(MEDIUM, SHORT, locale hiện tại)`).
  - LoggedOut: `EmptyState(icon Person, profile_login_title "You're not signed in" / "Bạn chưa đăng nhập", actionText profile_login_action Sign in/Đăng nhập)` (không chiếm hết màn).
  - `PokeListItem(leadingIcon Language, headline profile_language Language/Ngôn ngữ, supporting = tên ngôn ngữ hiện tại, onClick OpenLanguagePicker)`. Tên: `profile_language_system` "System default"/"Theo hệ thống", `profile_language_en` "English" (cả 2 ngôn ngữ), `profile_language_vi` "Tiếng Việt" (cả 2 ngôn ngữ).
  - `PokeListItem(leadingIcon Info, headline profile_version Version/Phiên bản, supporting = appVersion)`.
  - LoggedIn: `PokeSecondaryButton(profile_logout "Sign out"/"Đăng xuất")` màu error.
  - Dialog: `PokeRadioDialog` chọn ngôn ngữ; `PokeDialog(profile_logout_confirm_title "Sign out?"/"Đăng xuất?", profile_logout_confirm_message "Your favourites will be removed from this device." / "Danh sách yêu thích sẽ bị xóa khỏi thiết bị.")`.
- Deeplink: `["me"]` → `ResetTo(listOf(ProfileNavKey))`.

---

## 8. Debugtool (`:debugtool`)

Package `com.khangtm.pokepoke.debugtool`. Điều hướng nội bộ bằng back stack riêng (`mutableStateListOf<DebugScreen>()`), **không** dùng Navigator của app. Text vẫn qua `strings.xml` (chỉ cần `values/` English).

| Screen | Nội dung |
|---|---|
| `DebugHome` | `PokeListItem` tới: Network log, Database, Storage, Secure storage, Deeplink tester; dòng thông tin app version |
| `NetworkLogList` | Mới nhất trước: method, status (màu theo 2xx/4xx/5xx/lỗi), path, thời gian (ms), giờ. Ô lọc theo URL; nút Clear |
| `NetworkLogDetail` | Tab Request / Response: URL, header, body (JSON pretty print, chọn được text, nút copy) |
| `DatabaseList` | `context.databaseList()` bỏ `-journal`, `-wal`, `-shm` |
| `TableList` | Bảng từ `sqlite_master WHERE type='table'`, bỏ `android_metadata`, `room_master_table`, `sqlite_sequence`; kèm số dòng |
| `TableRows` | Bảng cuộn 2 chiều, 100 dòng/trang (trước/sau), ô tìm kiếm (`WHERE` mọi cột `LIKE %q%`), ô nhập câu lệnh tùy ý — chỉ chấp nhận câu bắt đầu `SELECT` hoặc `PRAGMA` (không phân biệt hoa thường), còn lại báo lỗi |
| `StorageList` / `StorageEntries` | Store từ `KeyValueStoreFactory.storeNames()` → `observeAll()` (key, kiểu, giá trị) |
| `SecureStorageList` / `SecureEntries` | Store từ `SecureStorageFactory.storeNames()` → `observeKeys()`; giá trị hiện `••••••`, bấm từng dòng để hiện (`get`) |
| `DeeplinkTester` | Ô nhập URI + chip mẫu (deeplinks.md §3); nút "Open" (`Intent(ACTION_VIEW, uri).setPackage(packageName)`), nút "Send notification" (xin `POST_NOTIFICATIONS` nếu cần, channel `debugtool` , `DeepLinkIntentFactory.pendingIntent`) |

- Network log: `DebugNetworkInterceptor` (`@IntoSet @AppInterceptors`) ghi vào `NetworkLogStore` (singleton, ring buffer 500, `StateFlow<List<NetworkLogEntry>>`); body tối đa 250 KB (`peekBody`), chỉ body dạng text; header `Authorization`, `Cookie` hiện `••••`.
- DB mở bằng `SQLiteDatabase.openDatabase(path, null, OPEN_READONLY)`, đóng sau mỗi truy vấn, chạy trên `Dispatchers.IO`.
- Nút con bọ: `DebugToolInitializer : Initializer<Unit>` (khai báo trong manifest của debugtool qua `InitializationProvider` `tools:node="merge"`) đăng ký `ActivityLifecycleCallbacks.onActivityPostCreated`: activity là `ComponentActivity` và **không** phải `DebugToolActivity` → thêm `ComposeView` vào `android.R.id.content` (đặt `setViewTreeLifecycleOwner/ViewModelStoreOwner/SavedStateRegistryOwner` từ activity nếu thiếu). Nút: `SmallFloatingActionButton` 48.dp, icon `ic_bug_report`, kéo thả, khi thả bám mép trái/phải gần nhất, tránh system bars; vị trí lưu `KeyValueStoreFactory.create("debugtool")`; long press → ẩn đến khi activity tạo lại. Bấm → mở `DebugToolActivity`. Lấy dependency bằng `EntryPointAccessors.fromApplication`.
- `DebugToolActivity` (`@AndroidEntryPoint`, `ComponentActivity`, theme `Theme.Material3.DayNight.NoActionBar` hoặc theme Compose thuần), `taskAffinity` mặc định.
