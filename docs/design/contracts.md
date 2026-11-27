# Contracts giữa các module

> **Nguồn chuẩn** cho mọi type public trong module `:api` và module không tách (`core:common-kotlin`, `ui:common`...). Implement đúng tên, package, chữ ký. Được phép thêm member `internal`/`private`; **không** đổi chữ ký public mà không ghi vào [`docs/decisions.md`](../decisions.md).
>
> Quy ước package = namespace module: `com.khangtm.pokepoke.<nhóm>.<tên>[.api|.impl]` (nhóm: `core`, `domain`, `data`, `feature`). Ví dụ `:core:storage:api` → `com.khangtm.pokepoke.core.storage.api`; module không tách như `:core:navigation` → `com.khangtm.pokepoke.core.navigation`.

---

## 1. `:core:common-kotlin` và `:core:common-android`

- `:core:common-kotlin` (Kotlin/JVM thuần, package `com.khangtm.pokepoke.core.common`): mọi type bên dưới trừ phần Hilt. Mọi module khác chỉ phụ thuộc module này.
- `:core:common-android` (Android library, package `com.khangtm.pokepoke.core.common.android`): `AppInfoImpl` (đọc `PackageManager`) và Hilt module cung cấp dispatchers, `@ApplicationScope`, binding `AppInfo`. Chỉ `:app` phụ thuộc để Hilt gom binding.
- `:core:storage:api`, `:core:securestorage:api` cũng là Kotlin/JVM thuần (không có `@Inject` class, không có Hilt module).

Kiểu kết quả của từng layer (luật dùng: [layers.md](layers.md)).

```kotlin
package com.khangtm.pokepoke.core.common

// ======== Layer data: Repository trả DataResult ========
sealed interface DataError {
    sealed interface Network : DataError {
        data object NoConnection : Network                    // IOException (không phải timeout), NetworkMonitor offline
        data object Timeout : Network                         // SocketTimeoutException
        data class Http(val code: Int, val serverMessage: String? = null) : Network  // HTTP khác 2xx, khác 404; serverMessage = message lỗi server trả về (đã đúng ngôn ngữ), hiện luôn null (T15e)
        data class Parse(val cause: Throwable) : Network      // SerializationException, lỗi map DTO → model
    }
    sealed interface Local : DataError {
        data class Read(val cause: Throwable) : Local         // Room / DataStore / Tink lỗi khi đọc
        data class Write(val cause: Throwable) : Local        // ... khi ghi
    }
    data object NotFound : DataError                          // HTTP 404, hoặc không có bản ghi trong storage
    data class Unexpected(val cause: Throwable) : DataError
}

sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

/** Cách DUY NHẤT để Repository gọi data source: bắt mọi Throwable (trừ CancellationException — ném lại) → Failure(mapError(e)). */
suspend inline fun <T> dataResultOf(mapError: (Throwable) -> DataError, block: () -> T): DataResult<T>
/**
 * map mỗi giá trị → Success. Upstream ném lỗi → emit Failure(mapError(e)) rồi TỰ SUBSCRIBE LẠI upstream
 * (retryWhen, chờ backoff 1s, 2s, 4s… tối đa 30s; về lại 1s sau khi có Success). Flow trả về KHÔNG BAO GIỜ kết thúc vì lỗi.
 * CancellationException ném lại. Bản cài đặt: map { Success(it) }.retryWhen { e, _ -> emit(Failure(mapError(e))); delay(backoff); true }.
 */
fun <T> Flow<T>.asDataResult(mapError: (Throwable) -> DataError): Flow<DataResult<T>>
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R>
inline fun <T, R> DataResult<T>.flatMap(transform: (T) -> DataResult<R>): DataResult<R>   // inline → gọi suspend được trong lambda

// ======== Layer domain: UseCase trả AppResult ========
sealed interface AppError {
    data object NoConnection : AppError          // không có mạng / timeout
    data object NotFound : AppError              // HTTP 404 hoặc không có dữ liệu
    data class Server(val code: Int, val serverMessage: String? = null) : AppError  // HTTP khác 2xx/404; serverMessage từ DataError.Network.Http
    data class Unknown(val cause: Throwable? = null) : AppError  // parse lỗi, lỗi storage, lỗi khác
}

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Network.NoConnection, Network.Timeout → NoConnection; NotFound → NotFound; Network.Http(c, m) → Server(c, m);
 *  Network.Parse(e), Local.Read(e), Local.Write(e), Unexpected(e) → Unknown(e). */
fun DataError.toAppError(): AppError
inline fun <T> DataResult<T>.toAppResult(mapError: (DataError) -> AppError = { it.toAppError() }): AppResult<T>
fun <T> Flow<DataResult<T>>.toAppResult(mapError: (DataError) -> AppError = { it.toAppError() }): Flow<AppResult<T>>
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R>
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T>
inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T>

// ======== Network-first (plan.md §5.2) — dùng trong UseCase ========
sealed interface FetchOutcome {
    data object Fetched : FetchOutcome                      // fetch + save thành công
    data object Offline : FetchOutcome                      // xác nhận không có mạng → dùng cache
    data class Failed(val error: DataError) : FetchOutcome  // lỗi khác → báo lỗi
}

/**
 * isOnline() == false → Offline (không gọi fetch).
 * fetch() → Success → Fetched; Failure(Network.NoConnection | Network.Timeout) → Offline; Failure(khác) → Failed(error).
 */
suspend fun runNetworkFirst(isOnline: () -> Boolean, fetch: suspend () -> DataResult<Unit>): FetchOutcome

/** Dữ liệu kèm nguồn: isOffline = true khi lấy từ cache vì không có mạng. */
data class DataSnapshot<out T>(val value: T, val isOffline: Boolean)

/**
 * runNetworkFirst → CHỈ SAU ĐÓ mới collect observe() (UI nhận đúng 1 lần emit đầu tiên cho mỗi lần tải):
 *  - Fetched: Success(v) → Success(DataSnapshot(v, false)); Failure(e) → Failure(e.toAppError())
 *  - Offline: Success(v) → Success(DataSnapshot(v, true)); Failure(NotFound) → Failure(AppError.NoConnection); Failure(e) khác → Failure(e.toAppError())
 *  - Failed(e): emit Failure(e.toAppError()), không đọc cache, không phát gì thêm (fetch là việc một lần, không phải luồng);
 *    tải lại = ViewModel gọi lại use case (Retry)
 * Sau lần emit đầu vẫn tiếp tục phát theo observe() (giữ nguyên cờ isOffline).
 */
fun <T> networkFirst(
    isOnline: () -> Boolean,
    fetch: suspend () -> DataResult<Unit>,
    observe: () -> Flow<DataResult<T>>,
): Flow<AppResult<DataSnapshot<T>>>

// ======== Dispatchers ========
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope   // CoroutineScope sống theo app
// Hilt module trong core:common-android cung cấp: @IoDispatcher Dispatchers.IO, @DefaultDispatcher Dispatchers.Default,
// @ApplicationScope CoroutineScope(SupervisorJob() + Dispatchers.Default)

// ======== Thông tin app ========
data class AppVersion(val name: String, val code: Long)
interface AppInfo { val version: AppVersion }   // impl đọc PackageManager; Hilt binding trong core:common-android
```

`LoadState` (trạng thái hiển thị Loading / Content / Error) là kiểu của UI → nằm ở `ui:common` (designsystem.md §2).

## 2. `:core:network` và `:core:connectivity`

Hai module Android library **không tách `api`/`impl`**: phần public là interface / helper, phần cài đặt là `internal` (feature không với tới), Hilt module `internal`. Chỉ `:app` cần phụ thuộc để Hilt gom binding. Nếu sau này có nhiều implementation hoặc cần contract trung lập thì mới tách `:api` (giữ nguyên package, feature chỉ đổi dòng dependency).

### 2.1. `:core:network`

`api(retrofit)` + `api(okhttp)` để module `:business:data` gọi API viết `interface XxxApi` (`@GET`...) và `:debugtool` viết OkHttp `Interceptor`; converter kotlinx-serialization là `implementation`.

```kotlin
package com.khangtm.pokepoke.core.network

/** Base URL do :app cung cấp: @Provides @BaseUrl fun baseUrl(): String = "https://pokeapi.co/api/v2/" */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class BaseUrl

/** Interceptor bổ sung (vd. debugtool). Module khai báo @Multibinds Set<Interceptor> với qualifier này. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AppInterceptors

interface ApiServiceFactory {
    fun <T : Any> create(service: Class<T>): T
}
inline fun <reified T : Any> ApiServiceFactory.create(): T = create(T::class.java)

/** Dùng trong Repository gọi Retrofit: dataResultOf(Throwable::toNetworkDataError) { api.xxx() }.
 *  SocketTimeoutException → Network.Timeout; IOException khác → Network.NoConnection; HttpException 404 → NotFound;
 *  HttpException khác → Network.Http(code) (serverMessage = null: chưa parse body lỗi — khi có server thật, `:app` cấp parser, thiết kế lúc đó); SerializationException, IllegalArgumentException → Network.Parse(e); còn lại → Unexpected(e). */
fun Throwable.toNetworkDataError(): DataError
```

Cài đặt (`internal`): OkHttpClient (connect 10s, read 15s, các interceptor từ `@AppInterceptors Set<Interceptor>`), `Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }`, Retrofit với `asConverterFactory("application/json".toMediaType())`, `@Multibinds @AppInterceptors abstract fun interceptors(): Set<Interceptor>`. Manifest khai báo `INTERNET`.

testFixtures: `createTestApiServiceFactory(baseUrl: String, interceptors: Set<Interceptor> = emptySet()): ApiServiceFactory` — `ApiServiceFactory` thật, cùng cấu hình Json / OkHttp / Retrofit với production, dùng trong test Repository của `:business:data` với `MockWebServer`: `testImplementation(testFixtures(projects.core.network))`.

### 2.2. `:core:connectivity`

```kotlin
package com.khangtm.pokepoke.core.connectivity

interface NetworkMonitor {
    val isOnline: Flow<Boolean>   // phát trạng thái hiện tại khi collect, rồi mỗi thay đổi
    fun isOnlineNow(): Boolean    // NetworkCapabilities: INTERNET && VALIDATED
}
```

Cài đặt (`internal`): `NetworkMonitorImpl` (`callbackFlow` + `registerDefaultNetworkCallback`), Hilt `@Binds`. Manifest khai báo `ACCESS_NETWORK_STATE`. testFixtures: `FakeNetworkMonitor(var online: Boolean = true)`.

Tách khỏi `:core:network` để module chỉ cần biết trạng thái mạng (vd. sync, banner offline) không kéo theo Retrofit / OkHttp.

## 3. `:core:storage:api`

```kotlin
package com.khangtm.pokepoke.core.storage.api

interface KeyValueStoreFactory {
    /** Mỗi name là một file DataStore riêng ("<name>.preferences_pb"); cùng name trả cùng instance. */
    fun create(name: String): KeyValueStore
    /** Tên các store đang tồn tại trên disk (cho debugtool). */
    fun storeNames(): List<String>
}

interface KeyValueStore {
    val name: String
    fun observeString(key: String): Flow<String?>
    fun observeBoolean(key: String): Flow<Boolean?>
    fun observeLong(key: String): Flow<Long?>
    fun observeInt(key: String): Flow<Int?>
    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putLong(key: String, value: Long)
    suspend fun putInt(key: String, value: Int)
    suspend fun remove(key: String)
    suspend fun clear()
    fun observeAll(): Flow<Map<String, Any>>
}
```

Core storage / securestorage / Retrofit service / Room DAO là **data source**: được phép ném exception. Chỉ Repository gọi chúng, qua `dataResultOf` / `asDataResult` (layers.md).

Luật cài đặt (impl): mỗi key chỉ giữ một giá trị một kiểu (put kiểu khác thay giá trị cũ); `observeXxx` đọc theo tên key rồi ép kiểu (khác kiểu → `null`); tên store khớp `[A-Za-z0-9_.-]+` và không bắt đầu bằng `secure_`; file ở `filesDir/datastore/<name>.preferences_pb`.

testFixtures: `FakeKeyValueStoreFactory`, `FakeKeyValueStore` (in-memory `MutableStateFlow<Map<String, Any>>`, `hasBeenWritten` — `storeNames()` chỉ trả store đã ghi, `var failWith: Throwable?` → mọi thao tác ném lỗi để test nhánh `Local.*`), `KeyValueStoreContract` (bộ test chung chạy cho fake và impl).

## 4. `:core:securestorage:api`

```kotlin
package com.khangtm.pokepoke.core.securestorage.api

interface SecureStorageFactory {
    fun create(name: String): SecureStorage
    fun storeNames(): List<String>
}

interface SecureStorage {
    val name: String
    suspend fun get(key: String): String?
    fun observe(key: String): Flow<String?>
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
    fun observeKeys(): Flow<Set<String>>
}
```

**impl**: DataStore Preferences file `secure_<name>.preferences_pb`; value lưu = Base64(`aead.encrypt(value.utf8, associatedData = key.utf8)`). `interface AeadProvider { fun aead(): Aead }` (internal trong impl) — bản thật: Tink `AeadConfig.register()`, `AndroidKeysetManager.Builder().withSharedPref(context, "pokepoke_tink_keyset", "pokepoke_tink_prefs").withKeyTemplate(KeyTemplates.get("AES256_GCM")).withMasterKeyUri("android-keystore://pokepoke_master_key").build().keysetHandle`. Unit test (Robolectric không có Android Keystore): `AeadProvider` test tạo keyset cleartext bằng `KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"))`. `KeyValueStoreFactory.storeNames()` phải **loại** file có tiền tố `secure_`.

testFixtures: `FakeSecureStorageFactory`, `FakeSecureStorage` (`hasBeenWritten`, `var failWith: Throwable?` → mọi thao tác ném lỗi), `SecureStorageContract` (bộ test chung chạy cho fake và impl).

## 5. `:core:navigation`

Một module Android library **không tách `api`/`impl`** (cùng lý do `:core:network`, plan.md §4.3): cài đặt không kéo thêm thư viện nào ngoài Hilt và chỉ có một cài đặt. Contract bên dưới là public; `NavigatorImpl` và Hilt module là `internal`. Feature và `:app` đều `implementation(projects.core.navigation)`.

```kotlin
package com.khangtm.pokepoke.core.navigation

import androidx.navigation3.runtime.NavKey     // api(navigation3-runtime)

/** Key cần đăng nhập. Auth interceptor (feature:auth) xử lý. */
interface RequiresAuth
/** Key hiển thị toàn màn hình: shell ẩn bottom bar / navigation rail. */
interface FullScreen

sealed interface NavRequest {
    data class Push(val key: NavKey) : NavRequest
    data class ReplaceTop(val key: NavKey) : NavRequest   // thay key trên cùng; stack rỗng → như Push
    data class ResetTo(val keys: List<NavKey>) : NavRequest
}

interface Navigator {
    /** Back stack hiện tại (Compose state – đọc trong composition sẽ tự recompose). */
    val backStack: List<NavKey>
    /** Chạy interceptor rồi áp dụng. Không suspend; xử lý tuần tự trong scope nội bộ. */
    fun execute(request: NavRequest)
    /** Pop key trên cùng, không qua interceptor. Trả false nếu chỉ còn ≤ 1 key (activity tự finish). */
    fun goBack(): Boolean
}

sealed interface InterceptResult {
    data object Proceed : InterceptResult
    data class Redirect(val request: NavRequest) : InterceptResult
}

interface NavigationInterceptor {
    /** Nhỏ chạy trước. Onboarding = 0, Auth = 100. */
    val priority: Int
    /** currentStack: stack trước khi áp dụng request. */
    suspend fun intercept(request: NavRequest, currentStack: List<NavKey>): InterceptResult
}

/** Feature đăng ký màn hình của mình (Hilt @IntoSet). */
fun interface EntryProviderInstaller {
    fun install(scope: EntryProviderScope<NavKey>)    // androidx.navigation3.runtime.EntryProviderScope
}

/** Tab cấp cao nhất (Hilt @IntoSet). */
interface TopLevelDestination {
    val key: NavKey
    val order: Int
    @get:StringRes val label: Int
    val icon: ImageVector
    val selectedIcon: ImageVector
}
```

Luật của `Navigator` (`internal class NavigatorImpl`, `@ActivityRetainedScoped`, package `com.khangtm.pokepoke.core.navigation`, Hilt module ở `...core.navigation.di`):
- Interceptor sắp theo `priority`; chạy lần lượt; **Redirect đầu tiên thắng** và request redirect **không** bị intercept lại (tránh vòng lặp). Request được tiếp tục sau này (vd. sau login) đi qua `execute` lại nên vẫn bị intercept bình thường.
- `execute` xử lý tuần tự bằng `Mutex` trong `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` của retained component; `backStack` là `SnapshotStateList`.
- Stack chỉ giữ trong bộ nhớ (sống qua config change / đổi ngôn ngữ). Process death → mở lại từ đầu (chấp nhận).

testFixtures: `FakeNavigator` (ghi lại `requests: List<NavRequest>`, `backCount`, áp dụng request trực tiếp không interceptor), `NavigatorContract` (bộ test chung, chạy cho `FakeNavigator` và `NavigatorImpl`), `TestNavKey(id)`. Module khác dùng `testImplementation(testFixtures(projects.core.navigation))`.

## 6. `:core:deeplink`

Một module Android library **không tách `api`/`impl`** (tiêu chí plan.md §4.3): cài đặt chỉ dùng `java.net.URI` và Android framework (`PackageManager`, `PendingIntent` — vốn đã có trong chữ ký contract) + Hilt, chỉ có một cài đặt. Contract bên dưới là public; `DeepLinkDispatcherImpl`, `DeepLinkIntentFactoryImpl` và Hilt module (`...core.deeplink.di`) là `internal`. Phụ thuộc `api(projects.core.navigation)` (vì `NavRequest` nằm trong chữ ký). Feature, `:app` (và `:debugtool` nếu cần) `implementation(projects.core.deeplink)`.

```kotlin
package com.khangtm.pokepoke.core.deeplink

/** Link đã chuẩn hóa: pokepoke://pokemon/25 và https://pokepoke.com/pokemon/25 → segments ["pokemon","25"]. */
data class DeepLink(val segments: List<String>, val query: Map<String, String>)

/** :app cung cấp theo flavor: DeepLinkConfig(setOf(BuildConfig.DEEPLINK_SCHEME), setOf(BuildConfig.DEEPLINK_HOST)) — build.md §4b, deeplinks.md §1 */
data class DeepLinkConfig(val schemes: Set<String>, val hosts: Set<String>)

/** Feature đăng ký (Hilt @IntoSet). Trả null nếu không khớp. */
fun interface DeepLinkHandler {
    fun handle(link: DeepLink): NavRequest?
}

interface DeepLinkDispatcher {
    /** Parse → handler đầu tiên khớp → navigator.execute(request). Trả false nếu URI không hợp lệ / không handler nào khớp. */
    fun dispatch(uri: String): Boolean
}

/** Pattern dạng "pokemon/{id}". match trả map tham số, hoặc null nếu không khớp (số segment phải bằng nhau). */
class DeepLinkPattern(pattern: String) {
    fun match(link: DeepLink): Map<String, String>?
}

/** PendingIntent mở app với URI (dùng cho push notification / debugtool). */
interface DeepLinkIntentFactory {
    fun pendingIntent(uri: String, requestCode: Int): PendingIntent
}
```

- Parse bằng `java.net.URI` (test được trên JVM). Scheme custom: segments = `[host] + path segments`. Scheme `https`/`http`: host phải thuộc `hosts`, segments = path segments. Segment rỗng bị bỏ; `/` → `[]`.
- `DeepLinkIntentFactory` impl: `packageManager.getLaunchIntentForPackage(packageName)` + `action = VIEW`, `data = uri`, `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP`, `PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`.

## 7. `:ui:common` và `:ui:designsystem`

Xem [designsystem.md](designsystem.md) (chữ ký composable dùng chung).

## 8. Core business: `:business:domain` và `:business:data` — Pokémon

Một chỗ duy nhất cho business cốt lõi của app (Pokémon §8, Favourite §8b). Luật chung: [layers.md](layers.md) §1.1. Feature chỉ phụ thuộc `:business:domain`; chỉ `:app` phụ thuộc `:business:data` (để Hilt gom binding). Không tạo thêm module business khác; dữ liệu chỉ một feature dùng nằm trong feature đó.

### 8.1. `:business:domain` — model, repository interface, use case

**Kotlin/JVM thuần** (build.md §3), package gốc `com.khangtm.pokepoke.business.domain`, phần Pokémon ở sub-package `.pokemon`. Không Android, không Retrofit / Room / DTO, không biết `NetworkMonitor`. Use case `Impl` + Hilt module (`...business.domain.di`, `hilt-core`) là `internal`.

```kotlin
package com.khangtm.pokepoke.business.domain.pokemon

data class PokemonSummary(val id: Int, val name: String, val imageUrl: String)

data class PokemonDetail(
    val summary: PokemonSummary,
    val heightDecimeters: Int,
    val weightHectograms: Int,
    val baseExperience: Int?,
    val types: List<PokemonType>,           // theo slot
    val abilities: List<PokemonAbility>,    // theo slot
    val stats: List<PokemonStat>,
    val evolutionChain: EvolutionStage,     // gốc của chuỗi
)

data class PokemonAbility(val name: String, val isHidden: Boolean)    // name: key API, vd "overgrow"
data class PokemonStat(val type: StatType, val baseValue: Int)

data class EvolutionStage(
    val pokemon: PokemonSummary,
    val condition: EvolutionCondition?,      // null ở gốc
    val evolvesTo: List<EvolutionStage>,
)
data class EvolutionCondition(val trigger: EvolutionTrigger, val minLevel: Int?, val item: String?)

enum class EvolutionTrigger { LEVEL_UP, USE_ITEM, TRADE, OTHER;
    companion object { fun fromKey(key: String): EvolutionTrigger }   // "level-up","use-item","trade", còn lại OTHER
}

enum class PokemonType(val key: String) {
    NORMAL("normal"), FIRE("fire"), WATER("water"), ELECTRIC("electric"), GRASS("grass"), ICE("ice"),
    FIGHTING("fighting"), POISON("poison"), GROUND("ground"), FLYING("flying"), PSYCHIC("psychic"),
    BUG("bug"), ROCK("rock"), GHOST("ghost"), DRAGON("dragon"), DARK("dark"), STEEL("steel"),
    FAIRY("fairy"), UNKNOWN("unknown");
    companion object { fun fromKey(key: String): PokemonType }        // không khớp → UNKNOWN
}

enum class StatType(val key: String) {
    HP("hp"), ATTACK("attack"), DEFENSE("defense"), SPECIAL_ATTACK("special-attack"),
    SPECIAL_DEFENSE("special-defense"), SPEED("speed"), UNKNOWN("unknown");
    companion object { fun fromKey(key: String): StatType }
}

// PokemonFormat.kt — format hiển thị dữ liệu (không phải text UI)
object PokemonFormat {
    fun displayId(id: Int): String = "#" + id.toString().padStart(4, '0')     // #0025, #1025, #10001
    fun displayName(apiName: String): String   // "mr-mime" → "Mr Mime", "pikachu" → "Pikachu"
}

/**
 * Repository (SSOT): observe* chỉ đọc local, refresh* gọi network rồi lưu (one-shot, layers.md §1.2).
 * Mọi hàm trả DataResult, không ném exception. "Không có dữ liệu" của observe một bản ghi → Failure(DataError.NotFound).
 * Cài đặt ở :business:data. Không có hàm save* public: cách lưu (luật merge) là việc của data.
 */
interface PokemonRepository {
    /** Các Pokémon có vị trí trong list, sắp theo vị trí, tối đa `limit` (rỗng nếu chưa có). */
    fun observeList(limit: Int): Flow<DataResult<List<PokemonSummary>>>
    /** null = chưa từng lưu trang nào. */
    fun observeTotalCount(): Flow<DataResult<Int?>>
    fun observeDetail(id: Int): Flow<DataResult<PokemonDetail>>
    /** query đã chuẩn hóa (trim, lowercase). Toàn số → so id; còn lại → so name. */
    fun observeByNameOrId(query: String): Flow<DataResult<PokemonSummary>>
    /** Giữ thứ tự của `ids`, bỏ id không có trong storage. */
    fun observeSummaries(ids: List<Int>): Flow<DataResult<List<PokemonSummary>>>

    /** Tải trang [offset, offset + limit) từ API rồi lưu (gán vị trí offset + index, cập nhật totalCount; không đụng detail). */
    suspend fun refreshPage(offset: Int, limit: Int): DataResult<Unit>
    /** query đã chuẩn hóa. Tải một Pokémon theo tên / id rồi lưu summary (không đổi vị trí list, không đụng detail). 404 → Failure(NotFound). */
    suspend fun refreshByNameOrId(query: String): DataResult<Unit>
    /** Tải detail (pokemon → species → evolution-chain) rồi lưu summary + detail trong một transaction (không đổi vị trí list). */
    suspend fun refreshDetail(id: Int): DataResult<Unit>
}

const val POKEMON_PAGE_SIZE = 20
data class PageResult(val visibleCount: Int, val isOffline: Boolean, val endReached: Boolean)
sealed interface SearchOutcome {
    data class Found(val pokemon: PokemonSummary, val isOffline: Boolean) : SearchOutcome
    data object NotFound : SearchOutcome
}

fun interface ObservePokemonListUseCase { operator fun invoke(limit: Int): Flow<AppResult<List<PokemonSummary>>> }
fun interface LoadPokemonPageUseCase { suspend operator fun invoke(offset: Int): AppResult<PageResult> }
fun interface SearchPokemonUseCase { suspend operator fun invoke(query: String): AppResult<SearchOutcome> }
fun interface ObservePokemonDetailUseCase { operator fun invoke(id: Int): Flow<AppResult<DataSnapshot<PokemonDetail>>> }
```

Use case (`Impl` chỉ inject `PokemonRepository`). Kiểm tra offline nằm trong `refresh*` của data (trả `Network.NoConnection` ngay) nên use case gọi helper với `isOnline = { true }`:

- `ObservePokemonList(limit)` = `repository.observeList(limit).toAppResult()`.
- `LoadPokemonPage(offset)`: `runNetworkFirst({ true }) { repository.refreshPage(offset, POKEMON_PAGE_SIZE) }`; `target = offset + POKEMON_PAGE_SIZE`; `total` = `repository.observeTotalCount().first()` (Failure → trả Failure); `endReached = total != null && target >= total`:
  - `Fetched` → `Success(PageResult(target, isOffline = false, endReached))`.
  - `Offline` → `repository.observeList(target).first()`: `Success(list)` và `list.size > offset` → `Success(PageResult(target, isOffline = true, endReached))`; `Success` nhưng không có thêm item → `Failure(AppError.NoConnection)`; `Failure(e)` → `Failure(e.toAppError())`.
  - `Failed(e)` → `Failure(e.toAppError())`.
- `SearchPokemon(query)`: `q = query.trim().lowercase()` (rỗng → `Success(NotFound)`); `runNetworkFirst({ true }) { repository.refreshByNameOrId(q) }`:
  - `Fetched` → `observeByNameOrId(q).first()` → `Success(Found(it, isOffline = false))` / Failure theo `toAppError()`.
  - `Offline` → `observeByNameOrId(q).first()` → `Success(Found(it, isOffline = true))`; `Failure(NotFound)` → `Failure(NoConnection)` (không biết có tồn tại hay không).
  - `Failed(DataError.NotFound)` (API 404) → `Success(SearchOutcome.NotFound)`; `Failed(e)` khác → `Failure(e.toAppError())`.
- `ObservePokemonDetail(id)` = `networkFirst({ true }, fetch = { repository.refreshDetail(id) }, observe = { repository.observeDetail(id) })`.

testFixtures:
- `FakePokemonRepository` — in-memory, cùng luật merge với data. Có "server" giả do test cấu hình: `serverList: List<PokemonSummary>`, `serverDetails: Map<Int, PokemonDetail>`; `refreshPage` / `refreshByNameOrId` / `refreshDetail` chép từ server giả vào storage giả (không có trên server → `Failure(NotFound)`); `var refreshError: DataError?` (khác null → mọi `refresh*` trả Failure đó, storage không đổi); `var failWith: DataError?` (khác null → mọi hàm trả Failure); seed storage trực tiếp: `seedList(items, offset, totalCount)`, `seedSummary(summary)`, `seedDetail(detail)`; đếm lời gọi `refreshCalls`.
- `PokemonFixtures` (bulbasaur/ivysaur/venusaur, eevee với 8 nhánh, pikachu).
- Use case là `fun interface` → ViewModel test fake bằng lambda.

### 8.2. `:business:data` — API, DTO, Room, cài đặt repository

Android library (`pokepoke.android.library` + `pokepoke.hilt` + `pokepoke.room` + `pokepoke.kotlin.serialization`), namespace `com.khangtm.pokepoke.business.data`, phần Pokémon ở sub-package `.pokemon`. **Mọi thứ `internal`**; Hilt module (`...business.data.di`) bind `PokemonRepository`, cung cấp `PokemonApi` (`apiServiceFactory.create()`) và `PokemonDatabase` (`@Singleton`). Phụ thuộc: `api(projects.business.domain)`, `implementation(projects.core.network)`, `implementation(projects.core.connectivity)`; test: `testFixtures(projects.core.network)` (`createTestApiServiceFactory`), `testFixtures(projects.core.connectivity)` (`FakeNetworkMonitor`), MockWebServer, JSON mẫu thật của PokeAPI trong `src/test/resources`.


```kotlin
// toàn bộ DTO là internal
internal interface PokemonApi {
    @GET("pokemon") suspend fun list(@Query("offset") offset: Int, @Query("limit") limit: Int): PokemonListDto
    @GET("pokemon/{idOrName}") suspend fun pokemonBasic(@Path("idOrName") idOrName: String): PokemonBasicDto
    @GET("pokemon/{id}") suspend fun pokemonDetail(@Path("id") id: Int): PokemonDto
    @GET("pokemon-species/{id}") suspend fun species(@Path("id") id: Int): SpeciesDto
    @GET("evolution-chain/{id}") suspend fun evolutionChain(@Path("id") id: Int): EvolutionChainDto
}
@Serializable data class PokemonDto(
    val id: Int, val name: String, val height: Int, val weight: Int,
    @SerialName("base_experience") val baseExperience: Int? = null,
    val types: List<TypeSlotDto>, val abilities: List<AbilitySlotDto>, val stats: List<StatDto>,
    val sprites: SpritesDto, val species: NamedResourceDto,
)
@Serializable data class PokemonListDto(val count: Int, val results: List<NamedResourceDto>)
@Serializable data class NamedResourceDto(val name: String, val url: String)
@Serializable data class PokemonBasicDto(val id: Int, val name: String)
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

Mapper:
- `NamedResourceDto` → `PokemonSummary(id = PokemonImageUrls.idFromResourceUrl(url), name, imageUrl = PokemonImageUrls.artworkUrl(id))` (bỏ item không parse được id). `PokemonBasicDto` → summary tương tự.
- Detail: gọi 3 API + map **trong một** `dataResultOf(Throwable::toNetworkDataError)`. Thứ tự: `pokemonDetail(id)` → species id = `idFromResourceUrl(species.url)` → `species(speciesId)` → chain id = `idFromResourceUrl(evolutionChain.url)` → `evolutionChain(chainId)` → map. Ảnh: `sprites.other.officialArtwork.frontDefault ?: artworkUrl(id)`. Stage tiến hóa: `PokemonSummary(speciesId, species.name, artworkUrl(speciesId))`; condition = `evolutionDetails.firstOrNull()`.

```kotlin
// PokemonImageUrls.kt (internal) — nơi duy nhất chứa quy ước URL ảnh
internal object PokemonImageUrls {
    const val ARTWORK_BASE_URL =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
    fun artworkUrl(id: Int): String = "$ARTWORK_BASE_URL$id.png"
    /** ".../pokemon/25/", ".../pokemon-species/25/", ".../evolution-chain/10/" → id; không parse được → null */
    fun idFromResourceUrl(url: String): Int?
}
```

`internal class PokemonRepositoryImpl(api, dao, networkMonitor, @IoDispatcher)`:
- `refresh*`: `networkMonitor.isOnlineNow() == false` → `Failure(Network.NoConnection)` ngay, không gọi API (không chờ timeout); còn lại = `dataResultOf(Throwable::toNetworkDataError) { api… → map }.flatMap { dataResultOf({ DataError.Local.Write(it) }) { dao.save…(it) } }`: lỗi network / map → `Network.*` / `NotFound`, storage không đổi; lỗi ghi → `Local.Write`.
- `observe*` = Flow của DAO bọc `asDataResult { DataError.Local.Read(it) }` (Flow tạo lười trong `flow { emitAll(...) }`, decisions.md T13); `observeDetail` / `observeByNameOrId` map `null` → `Failure(NotFound)`.
- DAO (`abstract class`) có `saveList(items, offset, totalCount)`, `saveSummary(summary)`, `saveDetail(detail)` (đều `@Transaction`) — luật merge bên dưới.

Room `PokemonDatabase` (`pokemon.db`, version 1, `fallbackToDestructiveMigration(dropAllTables = true)` vì là cache):

| Bảng | Cột |
|---|---|
| `pokemon` | `id` INT PK, `name` TEXT, `image_url` TEXT, `list_position` INT NULL (index), index `name` |
| `pokemon_detail` | `pokemon_id` INT PK, `height` INT, `weight` INT, `base_experience` INT NULL, `types_json`, `abilities_json`, `stats_json`, `evolution_json` TEXT, `updated_at` INT |
| `list_meta` | `id` INT PK (luôn 0), `total_count` INT |

- JSON column dùng `@Serializable` record **internal** (không gắn `@Serializable` lên business model).
- `saveList` trong transaction: với mỗi item → `INSERT OR IGNORE` + `UPDATE name, image_url`; `UPDATE pokemon SET list_position = NULL WHERE list_position = :pos AND id != :id`; `UPDATE pokemon SET list_position = :pos WHERE id = :id`; upsert `list_meta`.
- `saveSummary` không đổi `list_position`, không đụng detail; `saveDetail` = `saveSummary(detail.summary)` + upsert detail, không đổi vị trí.

## 8b. Core business: Favourite (trong `:business:domain` / `:business:data`)

Favourite là business model riêng, dùng `PokemonSummary` của §8. Không biết gì về đăng nhập: chặn khi chưa login và xóa khi logout do `feature:favourite` lo (§11).

### 8b.1. `:business:domain` — sub-package `.favourite`

Use case `Impl` + binding Hilt `internal` (cùng Hilt module của domain).

```kotlin
package com.khangtm.pokepoke.business.domain.favourite

data class FavouriteEntry(val pokemonId: Int, val addedAtEpochMillis: Long)
data class FavouritePokemon(val pokemon: PokemonSummary, val addedAtEpochMillis: Long)

/** Cài đặt ở :business:data. Mọi hàm trả DataResult. */
interface FavouriteRepository {
    fun observeFavourites(): Flow<DataResult<List<FavouriteEntry>>>     // mới thêm trước
    fun observeIsFavourite(pokemonId: Int): Flow<DataResult<Boolean>>
    suspend fun toggle(pokemonId: Int): DataResult<Boolean>              // transaction; trả trạng thái mới (true = vừa thêm)
    suspend fun remove(pokemonId: Int): DataResult<Unit>
    suspend fun clear(): DataResult<Unit>
}

/** observeFavourites() flatMapLatest PokemonRepository.observeSummaries(ids) → ghép addedAt, giữ thứ tự mới nhất trước, bỏ id không có summary. Failure ở bất kỳ bước nào → Failure. */
fun interface ObserveFavouritePokemonUseCase { operator fun invoke(): Flow<AppResult<List<FavouritePokemon>>> }
fun interface ObserveIsFavouriteUseCase { operator fun invoke(pokemonId: Int): Flow<AppResult<Boolean>> }
/** Success(trạng thái SAU khi toggle: true = vừa thêm). */
fun interface ToggleFavouriteUseCase { suspend operator fun invoke(pokemonId: Int): AppResult<Boolean> }
fun interface RemoveFavouriteUseCase { suspend operator fun invoke(pokemonId: Int): AppResult<Unit> }
fun interface ClearFavouritesUseCase { suspend operator fun invoke(): AppResult<Unit> }
```

testFixtures: `FakeFavouriteRepository` (in-memory, cùng luật, `var failWith: DataError?`); `FakeFavourites` giữ một trạng thái chung và **lộ 5 use case dạng property** `observeFavouritePokemon`, `observeIsFavourite`, `toggle`, `remove`, `clear` (một class không implement được cả 5 `fun interface` vì `invoke` trùng chữ ký trên JVM; test truyền `fake.toggle`...): `val ids = MutableStateFlow<List<Int>>(emptyList())` (mới nhất trước), `val summaries: MutableMap<Int, PokemonSummary>`, `var failWith: AppError? = null` (khác null → mọi hàm trả Failure).

### 8b.2. `:business:data` — sub-package `.favourite`

Mọi thứ `internal`; Hilt bind `FavouriteRepository`, cung cấp `FavouriteDatabase` (`@Singleton`).

- Room `FavouriteDatabase` (`favourite.db`, bảng `favourite(pokemon_id INT PK, added_at INT)`, `exportSchema = true`, **không** destructive migration — dữ liệu của user).
- `added_at` = `System.currentTimeMillis()` lúc thêm. Đọc bọc `asDataResult { Local.Read }`, ghi bọc `dataResultOf({ Local.Write })`.
- Mọi lệnh ghi của DAO (cả `:business:data` Pokémon) là `@Transaction`: trên DB đã đóng, query suspend thường của Room ném `CancellationException` (bị `dataResultOf` ném lại, không thành `Local.Write`), còn transaction ném `IllegalStateException` → `Local.Write` (decisions.md T13b).

## Quy tắc `:feature:*:api`

`:api` của feature chỉ được chứa:

1. `NavKey` (để module khác navigate tới).
2. **Use case interface** — chỉ khi có module khác thực sự cần.
3. Model / enum mà chữ ký use case cần (vd. `AuthState`, `Session`).
4. Extension point để module khác đóng góp vào (vd. `SessionCleaner`).

Repository, DAO, DTO, use case nội bộ, ViewModel luôn `internal` trong `:impl`. Thêm use case vào api = thay đổi contract → ghi `docs/decisions.md`.

Business model, repository interface và use case dùng chung **không** đặt ở `:feature:*:api` mà ở `:business:domain` (§8, §8b). Feature Pokémon / Favourite không có `data/`; onboarding, auth, profile vẫn tự giữ dữ liệu của mình trong `:impl` (ngoài phạm vi `:business:domain`).

Use case luôn trả `AppResult<T>` (suspend) hoặc `Flow<AppResult<T>>` (observe) — luật đầy đủ ở [layers.md](layers.md).

## 9. `:feature:onboarding:api`

```kotlin
package com.khangtm.pokepoke.feature.onboarding.api

data class OnboardingNavKey(val pending: NavRequest) : NavKey, FullScreen
```

Không module nào khác cần đọc trạng thái onboarding → api **chỉ có key**.

impl (`internal`):

```kotlin
internal interface OnboardingRepository {            // KeyValueStoreFactory.create("onboarding"), key "completed"
    fun observeCompleted(): Flow<DataResult<Boolean>>   // chưa có key → false
    suspend fun markCompleted(): DataResult<Unit>
}
internal fun interface IsOnboardingCompletedUseCase { suspend operator fun invoke(): AppResult<Boolean> }  // observeCompleted().first()
internal fun interface CompleteOnboardingUseCase { suspend operator fun invoke(): AppResult<Unit> }
```

Interceptor priority 0: request không chứa `OnboardingNavKey` → `isOnboardingCompleted()`: `Success(false)` → `Redirect(ResetTo(listOf(OnboardingNavKey(pending = request))))`; `Success(true)` hoặc `Failure` → `Proceed` (lỗi storage không được chặn app). Xong onboarding → `completeOnboarding()` → `navigator.execute(pending)` (kể cả khi Failure; lần mở sau sẽ thấy onboarding lại).

## 10. `:feature:auth:api`

```kotlin
package com.khangtm.pokepoke.feature.auth.api

/** pending: request tiếp tục sau khi đăng nhập thành công; null → chỉ đóng màn login. */
data class LoginNavKey(val pending: NavRequest? = null) : NavKey, FullScreen

data class Session(val username: String, val loggedInAtEpochMillis: Long)

sealed interface AuthState {
    data object LoggedOut : AuthState
    data class LoggedIn(val session: Session) : AuthState
}

/** Dùng bởi favourite, profile (và interceptor của auth). Phát lại mỗi khi login / logout. */
fun interface ObserveAuthStateUseCase { operator fun invoke(): Flow<AppResult<AuthState>> }

/**
 * Dùng bởi profile. Xóa session + gọi mọi SessionCleaner.
 * Xóa session trước; dù bước nào lỗi vẫn chạy hết các bước còn lại; trả Failure(lỗi đầu tiên) nếu có.
 */
fun interface LogoutUseCase { suspend operator fun invoke(): AppResult<Unit> }

/** Extension point: module khác đăng ký (Hilt @IntoSet) để dọn dữ liệu của user khi logout. */
fun interface SessionCleaner { suspend fun clear(): AppResult<Unit> }
```

impl (`internal`):

```kotlin
internal interface AuthRepository {                  // SecureStorageFactory.create("auth"): token, username, logged_in_at
    fun observeSession(): Flow<DataResult<Session?>>  // null = chưa login
    suspend fun saveSession(token: String, session: Session): DataResult<Unit>
    suspend fun clearSession(): DataResult<Unit>
}

/** Kết quả nghiệp vụ (layers.md §3): nằm trong Success. Failure chỉ khi lỗi kỹ thuật (vd. ghi storage). */
internal sealed interface LoginOutcome {
    data class LoggedIn(val session: Session) : LoginOutcome
    data object InvalidCredentials : LoginOutcome
}
internal fun interface LoginUseCase { suspend operator fun invoke(username: String, password: String): AppResult<LoginOutcome> }

internal fun interface TokenGenerator { fun generate(): String }   // 32 byte SecureRandom → Base64 URL-safe không padding
```

- `LoginUseCaseImpl` (giả lập server): `username.trim() == "admin" && password == "admin"` → `saveSession(tokenGenerator.generate(), Session("admin", clock()))` → `Success(LoggedIn(session))` / `Failure`; sai → `Success(InvalidCredentials)` (không ghi gì). Thời gian lấy từ `internal fun interface Clock { fun nowMillis(): Long }` để test được.
- `ObserveAuthStateUseCaseImpl`: `observeSession().toAppResult().map { r -> r.map { it?.let(AuthState::LoggedIn) ?: AuthState.LoggedOut } }`.
- `LogoutUseCaseImpl(repository, cleaners: Set<@JvmSuppressWildcards SessionCleaner>)`.
- Interceptor priority 100: chỉ xét khi request có key `RequiresAuth`; `observeAuthState().first()`: `Success(LoggedIn)` → `Proceed`; `Success(LoggedOut)` hoặc `Failure` → redirect:
  - `Push(k)`/`ReplaceTop(k)` → `Redirect(Push(LoginNavKey(pending = request)))`.
  - `ResetTo(keys)` có key `RequiresAuth` đầu tiên tại vị trí `i` → `Redirect(ResetTo(keys.take(i) + LoginNavKey(pending = request)))`.
- Login thành công: `navigator.goBack()` (bỏ LoginNavKey) rồi `pending?.let(navigator::execute)`.
- testFixtures (`auth:api`): `FakeAuthSession` implement `ObserveAuthStateUseCase` + `LogoutUseCase`: `val state = MutableStateFlow<AppResult<AuthState>>(Success(LoggedOut))`, `var logoutResult: AppResult<Unit> = Success(Unit)`, `var logoutCount = 0`; logout thành công → `state = Success(LoggedOut)`.

## 11. `:feature:home:api`, `:feature:favourite:api`, `:feature:profile:api`, `:feature:details:api`

```kotlin
package com.khangtm.pokepoke.feature.home.api
data object HomeNavKey : NavKey

package com.khangtm.pokepoke.feature.favourite.api
data object FavouriteNavKey : NavKey

package com.khangtm.pokepoke.feature.profile.api
data object ProfileNavKey : NavKey

package com.khangtm.pokepoke.feature.details.api
data class DetailsNavKey(val pokemonId: Int) : NavKey, RequiresAuth
```

favourite impl (`internal`) — chỉ phần ghép với đăng nhập; dữ liệu favourite ở `:business:domain` / `:business:data` (§8b):

```kotlin
internal sealed interface FavouriteState {
    data object LoggedOut : FavouriteState
    data class Items(val items: List<FavouritePokemon>) : FavouriteState
}
/** ObserveAuthStateUseCase() flatMapLatest: LoggedOut → Success(LoggedOut); LoggedIn → ObserveFavouritePokemonUseCase() → Items. Failure ở bất kỳ bước nào → Failure. */
internal fun interface ObserveFavouriteStateUseCase { operator fun invoke(): Flow<AppResult<FavouriteState>> }
```

- ViewModel dùng `ObserveFavouriteStateUseCase` (internal) + `RemoveFavouriteUseCase` (`:business:domain`).
- `FavouriteSessionCleaner : SessionCleaner` (internal) = `ClearFavouritesUseCase()`.
- Details dùng `ObserveIsFavouriteUseCase` / `ToggleFavouriteUseCase` từ `:business:domain` (không phụ thuộc `feature:favourite`); test bằng `FakeFavourites` (testFixtures của `:business:domain`).

## 12. `:feature:main:api`

Không có type public (module giữ cho đúng quy ước api/impl). `main:impl` cung cấp `MainShell` cho `:app` (xem screens.md).

## 13. Hilt multibinding tổng hợp

| Set | Đóng góp bởi |
|---|---|
| `Set<EntryProviderInstaller>` | onboarding, auth, home, details, favourite, profile (impl) |
| `Set<TopLevelDestination>` | home (order 0), favourite (1), profile (2) |
| `Set<NavigationInterceptor>` | onboarding (0), auth (100) |
| `Set<DeepLinkHandler>` | home, favourite, profile, details |
| `Set<SessionCleaner>` | favourite |
| `@AppInterceptors Set<Interceptor>` | debugtool (chỉ debug) |

Mỗi set đều có `@Multibinds` ở module impl của core tương ứng để set rỗng vẫn hợp lệ.
