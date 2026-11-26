# Layer & kiểu kết quả

> Áp dụng cho `:business:domain`, `:business:data` và `:feature:*`. Không áp dụng cho `:core:*` (hạ tầng: là data source hoặc tiện ích) và `:debugtool` (công cụ debug, đọc thẳng data source). Kiểu dữ liệu ở [contracts.md §1](contracts.md).

## 1. Các layer

```
Screen ──UiAction──▶ ViewModel ──▶ UseCase ──▶ Repository ──▶ Data source
       ◀──UiState───           ◀AppResult◀            ◀DataResult◀          ◀ giá trị / exception
```

| Layer | Đặt ở | Được inject | Trả về | Exception |
|---|---|---|---|---|
| **Data source** | Retrofit `XxxApi`, Room `XxxDao`, `KeyValueStore`, `SecureStorage` (core) | — | DTO / entity / giá trị thô | **được ném** |
| **Repository** | interface ở `:business:domain`, cài đặt `internal` ở `:business:data`; hoặc `feature:*:impl/data/` (dữ liệu riêng của onboarding, auth) | data source, `NetworkMonitor`, `@IoDispatcher` | `DataResult<T>` / `Flow<DataResult<T>>` | không ném: mọi lời gọi data source bọc `dataResultOf` / `asDataResult` |
| **UseCase** | `:business:domain` (interface public, `Impl` internal); `feature:*:api` (public) hoặc `feature:*:impl/domain/` (internal, chỉ ghép use case cho riêng màn hình / dữ liệu riêng của feature) | Repository, UseCase khác, `NetworkMonitor`, `AppInfo`, wrapper platform không ném lỗi (vd. `AppLocaleController`), tiện ích thuần (`TokenGenerator`, `Clock`) | `AppResult<T>` / `Flow<AppResult<T>>` | không ném, không `try` |
| **ViewModel** | `feature:*:impl/presentation/` | **chỉ** UseCase, `Navigator`, tham số `@Assisted` | `StateFlow<UiState>` | không ném, không `try` |
| **Navigation glue** (interceptor, `DeepLinkHandler`, `EntryProviderInstaller`) | `feature:*:impl/navigation/` | UseCase, `Navigator` | theo contract core | không `try` |

- `CancellationException` luôn được ném lại — chỉ helper trong `core:common-kotlin` (`dataResultOf`, `asDataResult`) bắt exception, nên luật này tự đúng.
- **Cấm `runCatching`** trong toàn bộ `domain` / `data` / `feature` (nó nuốt `CancellationException`).
- Repository là chỗ **duy nhất** dịch exception → `DataError`. UseCase là chỗ **duy nhất** dịch `DataError` → `AppError` (mặc định `toAppError()`, use case được map riêng khi có ngữ cảnh, vd. offline + `NotFound` → `NoConnection`).
- ViewModel chỉ `when (result) { is Success -> …; is Failure -> … }` (hoặc `onSuccess` / `onFailure`), map sang `UiState`.

### 1.1. Domain, data, feature: ai làm gì

```
feature:*:impl ──▶ :business:domain  ◀── :business:data        (chỉ :app phụ thuộc :business:data để Hilt gom binding)
 UI, ViewModel      model, repository interface,   Retrofit API, DTO, Room, mapper,
                    use case                       cài đặt repository (internal)
```

| Loại logic | Đặt ở | Ví dụ |
|---|---|---|
| Hạ tầng dùng chung | `core:*` | gắn token qua `@AppInterceptors`, timeout, `toNetworkDataError`, `NetworkMonitor` |
| **Làm thế nào** lấy / lưu dữ liệu | `:business:data` | kiểm tra offline trước khi gọi API (`NetworkMonitor` → `Network.NoConnection`), endpoint, DTO, mapper, Room, luật merge, chuỗi gọi API chỉ để lấy được dữ liệu (pokemon → species → evolution-chain) |
| **Khi nào / có được không** — quyết định nghiệp vụ | `:business:domain` (use case) | network-first (thứ tự fetch → đọc cache), offline + `NotFound` → `NoConnection`, 404 khi search là kết quả "không tìm thấy" |
| Ghép riêng cho một màn hình | `feature:*:impl/domain/` | favourite chỉ hiện khi đã đăng nhập (ghép `auth:api` + `:business:domain`) |
| Hiển thị | `feature:*:impl/presentation/` | UiState, ViewModel, UI |

- **Data chỉ trả sự thật** (lấy được hay không, lỗi gì) qua `DataResult`; không biết đăng nhập, ngữ cảnh màn hình, không inject use case. Mọi quyết định dựa trên kết quả đó thuộc về use case.
- **Repository interface nói ngôn ngữ nghiệp vụ**: `observe*` (đọc local), `refresh*` (network → lưu), lệnh ghi nghiệp vụ (`toggle`, `remove`, `clear`). Không lộ hàm kỹ thuật như `save*` / `upsert*`.
- **Phải gọi A trước mới lấy được B** — hỏi: use case có cần biết A không?
  - A chỉ phục vụ việc gọi B, nơi khác không dùng (config, id trung gian, session key...) → repository trong `data` tự gọi tuần tự, có thể cache A.
  - A áp dụng cho mọi request (token, handshake, refresh token) → interceptor / authenticator đóng góp vào `core:network`.
  - A có nghĩa nghiệp vụ, UI hiển thị, hoặc domain khác cũng dùng (vùng, gói subscription, điều khoản) → use case gọi repository A rồi truyền kết quả làm tham số cho repository B (`refreshPage(offset, limit, region)`).
- **Chỉ core business vào `:business:*`** (Pokémon, Favourite). Không thêm module business khác; dữ liệu riêng của một feature (onboarding, auth, profile) nằm trong `feature:*:impl` với `data/` `domain/` riêng theo cùng luật layer.
- Trong `:business:data`, repository của phần này không gọi repository của phần khác; ghép nhiều nguồn chỉ diễn ra ở use case (vd. `ObserveFavouritePokemonUseCase` ghép favourite + pokemon).

### 1.2. Hai dạng hàm: observe (Flow) và one-shot (suspend)

Áp dụng giống nhau ở Repository (`DataResult`) và UseCase (`AppResult`):

| | **Observe** | **One-shot** |
|---|---|---|
| Chữ ký | `fun observeX(...): Flow<DataResult<T>>` (không `suspend`), tên bắt đầu `observe` | `suspend fun x(...): DataResult<T>`, tên là động từ (`save`, `toggle`, `fetch`, `clear`...) |
| Dùng cho | **Chỉ đọc local** (Room, DataStore, SecureStorage) — dữ liệu có thể đổi theo thời gian | **Mọi lệnh ghi**, **mọi lời gọi network**, đọc một lần |
| Helper ở Repository | `asDataResult` | `dataResultOf` |
| Khi lỗi | Phát `Failure` rồi **tự subscribe lại** nguồn (backoff), luồng **không bao giờ kết thúc vì lỗi**; phần tử sau có thể lại là `Success` | Trả **đúng một** `Failure`, **không tự retry** |
| Ai quyết định thử lại | Repository (tự động) | Người gọi: user bấm Retry, hoặc use case quyết định |

- **Không có Flow cho lệnh ghi hoặc network**: network không phải luồng dữ liệu, ghi là hành động.
- **One-shot không tự retry** vì lệnh ghi có thể không idempotent (`toggle` chạy 2 lần = về trạng thái cũ) và việc gọi lại network nên do user / nghiệp vụ quyết định.
- **Đọc một lần từ luồng**: `observeX().first()` lấy phần tử đầu tiên, có thể là `Failure`, và không chờ lần subscribe lại. Dùng trong interceptor và use case one-shot (vd. `IsOnboardingCompletedUseCase`, `SearchPokemonUseCase` đọc lại kết quả).
- **Kết hợp**: `networkFirst` = fetch one-shot rồi observe. Nhánh `Failed` là lỗi của phần one-shot → phát `Failure` và không phát thêm; tải lại bằng cách gọi lại use case (Retry). Khi đã vào phần observe thì theo luật observe.
- ViewModel xử lý từng phần tử của Flow như nhau (`Success` / `Failure`), không cần subscribe lại.

## 2. Bảng dịch lỗi

| Nguồn | Exception / tình huống | `DataError` (Repository) | `AppError` mặc định (UseCase) |
|---|---|---|---|
| Network | `SocketTimeoutException` | `Network.Timeout` | `NoConnection` |
| | `IOException` khác | `Network.NoConnection` | `NoConnection` |
| | `HttpException` 404 | `NotFound` | `NotFound` |
| | `HttpException` khác | `Network.Http(code)` | `Server(code)` |
| | `SerializationException`, lỗi map DTO (`IllegalArgumentException`) | `Network.Parse(e)` | `Unknown(e)` |
| Local | lỗi khi đọc Room / DataStore / Tink | `Local.Read(e)` | `Unknown(e)` |
| | lỗi khi ghi | `Local.Write(e)` | `Unknown(e)` |
| | observe một bản ghi không tồn tại | `NotFound` | `NotFound` |
| Khác | mọi thứ còn lại | `Unexpected(e)` | `Unknown(e)` |

Network dùng `Throwable.toNetworkDataError()` (`core:network`). Repository gọi network thì map DTO → model **bên trong** `dataResultOf`, để lỗi map cũng thành `Network.Parse`.

## 3. Kết quả nghiệp vụ ≠ lỗi

Định nghĩa:

- **`Success`** = lời gọi **có câu trả lời** — kể cả khi câu trả lời là "sai mật khẩu" hay "không tìm thấy".
- **`Failure`** = **không có câu trả lời** vì sự cố kỹ thuật (mạng, storage, server, dữ liệu được yêu cầu không tồn tại).

Kết quả nghiệp vụ mà UI hiển thị như một trạng thái bình thường là **giá trị trong `Success`**, có kiểu riêng:

- Tên kiểu `XxxOutcome`, dạng `sealed interface`, mỗi trường hợp là một `data class` / `data object` mang đủ dữ liệu của nó.
- **Cấm** dùng các từ `Success`, `Failure`, `Error`, `Ok` trong tên trường hợp (tránh `Success(LoginOutcome.SUCCESS)`). Đặt theo nghĩa nghiệp vụ: `LoggedIn`, `InvalidCredentials`, `Found`, `NotFound`.

| Tình huống | Trả về |
|---|---|
| Sai mật khẩu | `Success(LoginOutcome.InvalidCredentials)` |
| Đăng nhập đúng | `Success(LoginOutcome.LoggedIn(session))` |
| Search không có Pokémon tên đó (API 404) | `Success(SearchOutcome.NotFound)` |
| Search offline và cache không có | `Failure(NoConnection)` — không biết Pokémon có tồn tại hay không |
| Mở Details (vd. deeplink) với id không tồn tại | `Failure(NotFound)` — user đi theo link sai, không phải đang tìm kiếm → màn lỗi |
| Chưa đăng nhập | `Success(AuthState.LoggedOut)` |
| Đọc cache do offline | `Success(DataSnapshot(value, isOffline = true))` |

ViewModel xử lý hai tầng, đọc tự nhiên:

```kotlin
when (result) {
    is AppResult.Success -> when (val outcome = result.value) {
        is LoginOutcome.LoggedIn -> navigateNext()
        LoginOutcome.InvalidCredentials -> showInvalidCredentials()
    }
    is AppResult.Failure -> showError(result.error)
}
```

## 4. Quy ước viết

```kotlin
// Use case: fun interface + Impl, mỗi use case một việc. Tên: động từ + danh từ + UseCase.
internal fun interface SearchPokemonUseCase { suspend operator fun invoke(query: String): AppResult<SearchOutcome> }
internal class SearchPokemonUseCaseImpl @Inject constructor(...) : SearchPokemonUseCase { ... }
// Hilt: @Binds trong di/ của module.

// Repository: interface (ở :business:domain) + Impl (internal ở :business:data). Mọi hàm không private trả DataResult; không có property public.
interface PokemonRepository {                                    // :business:domain
    suspend fun refreshPage(offset: Int, limit: Int): DataResult<Unit>
    // ...
}
internal class PokemonRepositoryImpl @Inject constructor(        // :business:data
    private val api: PokemonApi,
    private val dao: PokemonDao,
) : PokemonRepository {
    override suspend fun refreshPage(offset: Int, limit: Int) =
        dataResultOf(Throwable::toNetworkDataError) { api.list(offset, limit).toPage() }
            .flatMap { page -> dataResultOf({ DataError.Local.Write(it) }) { dao.saveList(page.items, offset, page.totalCount) } }
}
```

- Use case chỉ đọc lại dữ liệu (pass-through) **vẫn phải có** — ViewModel không bao giờ gọi Repository.
- Use case đọc một lần từ Flow: `repository.observeX().first().toAppResult()`.
- Fake để test:
  - ViewModel: use case là `fun interface` → fake bằng lambda (`ToggleFavouriteUseCase { AppResult.Success(true) }`) hoặc fake trong `testFixtures` của `:business:domain` / `:api` (`FakeFavourites`).
  - UseCase: fake Repository trong testFixtures của `:business:domain` (`FakePokemonRepository`, `FakeFavouriteRepository`), hoặc trong `src/test` của chính module nếu repository là internal của feature.
  - Repository: test với data source thật trong môi trường test (Room in-memory, `MockWebServer`, `FakeKeyValueStore`) và kiểm tra **từng dòng** bảng §2 mà repository đó gặp.

## 5. Checklist review (quy ước bắt buộc, chưa kiểm tra tự động)

Không áp dụng cho `:core:*` và `:debugtool`. Trước khi commit mỗi task, tự rà:

| # | Luật |
|---|---|
| 1 | Hàm `invoke` của mọi `*UseCase` trả `AppResult<…>` hoặc `Flow<AppResult<…>>`. `SessionCleaner.clear` cũng vậy. |
| 2 | Mọi hàm không `private` của `*Repository` / `*RepositoryImpl` trả `DataResult<…>` hoặc `Flow<DataResult<…>>`; không có property không `private`. |
| 3 | Tham số constructor của `*ViewModel` chỉ là `*UseCase`, `Navigator`, hoặc tham số `@Assisted`. |
| 4 | `*UseCaseImpl` không inject data source (`*Api`, `*Dao`, `*Database`, `KeyValueStore(Factory)`, `SecureStorage(Factory)`). |
| 5 | Package `presentation`, `domain`, `navigation` không có `catch`; không có `runCatching` ở bất kỳ đâu trong `domain` / `data` / `feature`; module `:business:domain` không có `catch`. |
| 6 | Hàm trả `Flow` tên bắt đầu `observe` (use case: `Observe…UseCase`) và không `suspend`; hàm one-shot là `suspend`, không trả `Flow`. Lệnh ghi và network luôn one-shot (§1.2). |
| 7 | Package `presentation` không import `..impl.data..`, `retrofit2..`, `androidx.room..`. |
| 8 | Kết quả nghiệp vụ là `sealed interface XxxOutcome` nằm trong `Success`; tên trường hợp không chứa `Success` / `Failure` / `Error` / `Ok` (§3). |
| 9 | `:business:data` không inject use case, không chứa quyết định nghiệp vụ (§1.1); repository interface ở `:business:domain` không có hàm kỹ thuật `save*` / `upsert*`. |
| 10 | `:business:domain` là Kotlin/JVM thuần (build tự chặn `android.*`); không phụ thuộc thêm thư viện ngoài coroutines, `javax.inject`, `hilt-core`, `core:common-kotlin`. |

Sau này muốn tự động hóa: các luật trên viết được thành test kiến trúc (vd. Konsist) mà không đổi thiết kế.
