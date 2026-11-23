# Design system & UI dùng chung

> Phong cách: **Material 3**, tông chủ đạo đỏ san hô ấm + navy, sáng sủa, bo góc mềm. Không dùng dynamic color (giữ nhận diện thống nhất). Hỗ trợ light/dark theo hệ thống. Không dùng logo / font / asset thương hiệu Pokémon.

## 1. `:core:designsystem`

Package `com.khangtm.pokepoke.core.designsystem`.

### 1.1. Màu (`Color.kt`, `PokeColorScheme.kt`)

| Role | Light | Dark |
|---|---|---|
| primary | `#B8401F` | `#FFB5A0` |
| onPrimary | `#FFFFFF` | `#5F1500` |
| primaryContainer | `#FFDBD1` | `#8F2A0B` |
| onPrimaryContainer | `#3B0900` | `#FFDBD1` |
| secondary | `#3D5A80` | `#A9C7F0` |
| onSecondary | `#FFFFFF` | `#0F3152` |
| secondaryContainer | `#D3E3FF` | `#2A4869` |
| onSecondaryContainer | `#0B1C33` | `#D3E3FF` |
| tertiary | `#7A5900` | `#F2BF48` |
| onTertiary | `#FFFFFF` | `#402D00` |
| tertiaryContainer | `#FFDEA6` | `#5C4200` |
| onTertiaryContainer | `#261900` | `#FFDEA6` |
| error | `#BA1A1A` | `#FFB4AB` |
| onError | `#FFFFFF` | `#690005` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |
| background / surface | `#FFF8F6` | `#1A110F` |
| onBackground / onSurface | `#231917` | `#F1DFDA` |
| surfaceVariant | `#F5DED8` | `#53433F` |
| onSurfaceVariant | `#53433F` | `#D8C2BC` |
| outline | `#85736E` | `#A08C87` |
| outlineVariant | `#D8C2BC` | `#53433F` |
| surfaceContainerLowest | `#FFFFFF` | `#140C0A` |
| surfaceContainerLow | `#FFF1ED` | `#231917` |
| surfaceContainer | `#FCEAE5` | `#271D1B` |
| surfaceContainerHigh | `#F6E4DF` | `#322825` |
| surfaceContainerHighest | `#F1DFDA` | `#3D3230` |

### 1.2. Typography (`Type.kt`)

Type scale mặc định của Material 3, font hệ thống. Ghi đè: `headlineMedium` và `titleLarge` dùng `FontWeight.Bold`; `labelLarge` `FontWeight.SemiBold`.

### 1.3. Spacing, shape, kích thước (`Dimens.kt`, `Shape.kt`)

```kotlin
object PokeSpacing { val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp }
object PokeSizes { val minTouchTarget = 48.dp; val cardImage = 96.dp; val detailImage = 220.dp; val gridMinCellWidth = 160.dp; val maxContentWidth = 600.dp }
// Shapes: extraSmall 4, small 8, medium 12, large 16, extraLarge 28 (RoundedCornerShape)
```

### 1.4. Theme

```kotlin
@Composable fun PokeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

XML theme (đặt trong `:app`, xem screens.md §1): `Theme.PokePoke` (parent `Theme.AppCompat.DayNight.NoActionBar`, `android:windowBackground` = background) và `Theme.PokePoke.Starting` (parent `Theme.SplashScreen`).

### 1.5. Icon (`PokeIcons.kt`)

Dùng `material-icons-core` (`Icons.Filled` / `Icons.Outlined`): Home, Favorite, FavoriteBorder, Person, Search, Clear, Close, ArrowBack, Refresh, Delete, Lock, Info, Settings, Check, Warning, KeyboardArrowRight.
Icon không có trong core → **vector drawable** copy từ Material Symbols (Apache 2.0) trong `res/drawable` của designsystem: `ic_language`, `ic_cloud_off`, `ic_visibility`, `ic_visibility_off`, `ic_catching_pokemon` (icon minh họa onboarding / placeholder). `ic_bug_report` đặt trong `:debugtool`.

```kotlin
object PokeIcons {
    val Home = Icons.Outlined.Home; val HomeSelected = Icons.Filled.Home
    val Favourite = Icons.Outlined.FavoriteBorder; val FavouriteSelected = Icons.Filled.Favorite
    val Profile = Icons.Outlined.Person; val ProfileSelected = Icons.Filled.Person
    val Search = Icons.Filled.Search; val Clear = Icons.Filled.Clear; val Close = Icons.Filled.Close
    val Back = Icons.AutoMirrored.Filled.ArrowBack; val Delete = Icons.Outlined.Delete
    val Language: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_language)
    val CloudOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_cloud_off)
    val Visibility / VisibilityOff / Pokeball  // tương tự
}
```

### 1.6. Component (prefix `Poke`)

| Component | Chữ ký | Ghi chú |
|---|---|---|
| `PokeTopAppBar` | `(title: String, modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})` | `TopAppBar`; có `onBack` → icon back với contentDescription `R.string.designsystem_back` |
| `PokePrimaryButton` | `(text: String, onClick, modifier, enabled = true, loading = false)` | `Button` full width, cao 52.dp; `loading` → `CircularProgressIndicator` 20.dp thay text, disable click |
| `PokeSecondaryButton` | như trên | `OutlinedButton` |
| `PokeTextButton` | `(text, onClick, modifier)` | `TextButton` |
| `PokeTextField` | `(value, onValueChange, label: String, modifier, isPassword = false, isError = false, supportingText: String? = null, imeAction = ImeAction.Next, onImeAction: () -> Unit = {})` | `OutlinedTextField`, single line; `isPassword` → nút show/hide (icon visibility) và `PasswordVisualTransformation` |
| `PokeSearchField` | `(query, onQueryChange, onSearch: () -> Unit, onClear: () -> Unit, placeholder: String, modifier)` | `OutlinedTextField` bo `extraLarge`, leading Search, trailing Clear khi query không rỗng, `ImeAction.Search` |
| `PokeCard` | `(onClick: (() -> Unit)?, modifier, content: @Composable ColumnScope.() -> Unit)` | `ElevatedCard`, shape `large` |
| `PokeChip` | `(text: String, containerColor: Color, contentColor: Color, modifier)` | nhãn tĩnh bo `small`, padding ngang `md` |
| `PokeDialog` | `(title: String, text: String, confirmText: String, onConfirm, dismissText: String, onDismiss)` | `AlertDialog` |
| `PokeRadioDialog<T>` | `(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, onDismiss)` | chọn ngôn ngữ |
| `PokeListItem` | `(headline: String, modifier, supporting: String? = null, leadingIcon: ImageVector? = null, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null)` | `ListItem` |

Mọi component có `@Preview` light + dark và screenshot test Roborazzi.

## 2. `:core:ui`

Package `com.khangtm.pokepoke.core.ui`. Composable dùng chung **không biết nghiệp vụ**.

| Composable / type | Chữ ký | Hành vi |
|---|---|---|
| `RemoteImage` | `(url: String?, contentDescription: String?, modifier, contentScale = ContentScale.Fit)` | Coil `AsyncImage`; khi loading: nền `surfaceContainerHigh`; lỗi / url null: icon `PokeIcons.Pokeball` màu `outline` ở giữa |
| `LoadingState` | `(modifier)` | `CircularProgressIndicator` ở giữa, chiếm hết vùng |
| `ErrorState` | `(error: AppError, onRetry: () -> Unit, modifier)` | icon + tiêu đề + mô tả theo `error` (bảng dưới) + `PokeSecondaryButton(R.string.ui_retry)` |
| `EmptyState` | `(title: String, modifier, message: String? = null, icon: ImageVector? = null, actionText: String? = null, onAction: (() -> Unit)? = null)` | dùng cho rỗng, cần đăng nhập, placeholder pane detail |
| `OfflineBanner` | `(modifier)` | hàng full width nền `tertiaryContainer`, icon CloudOff + `R.string.ui_offline_banner` |
| `LoadMoreFooter` | `(state: LoadMoreState, onRetry: () -> Unit, modifier)` | `Loading` → progress nhỏ; `Error` → text lỗi + nút retry; `Idle`/`EndReached` → không hiển thị gì |
| `LoadMoreState` | `sealed interface { Idle; Loading; data class Error(val error: AppError); EndReached }` | |
| `MediaCard` | `(title: String, overline: String, imageUrl: String?, onClick: () -> Unit, modifier, action: (@Composable () -> Unit)? = null)` | `PokeCard`: ảnh vuông `cardImage` ở giữa trên, `overline` (labelMedium, onSurfaceVariant), `title` (titleMedium); `action` ở góc trên phải |
| `UiMessage` | `data class UiMessage(@StringRes val textRes: Int, val args: List<Any> = emptyList(), val id: Long = System.nanoTime())` | sự kiện một lần trong UiState |
| `MessageEffect` | `(message: UiMessage?, snackbarHostState: SnackbarHostState, onShown: () -> Unit)` | `LaunchedEffect(message?.id)`: show snackbar rồi gọi `onShown` |
| `AppError.titleRes()` / `messageRes()` | extension `@StringRes` | |
| `AppError.toUiMessage()` | `fun AppError.toUiMessage(): UiMessage` | `UiMessage(messageRes(), args)`; `Server` truyền `code` làm arg |
| `LoadState<T>` | `sealed interface { data object Loading; data class Content<T>(val data: T, val isOffline: Boolean); data class Error(val error: AppError) }` | trạng thái hiển thị của dữ liệu tải theo network-first |
| `toLoadState()` | `fun <T> AppResult<DataSnapshot<T>>.toLoadState(): LoadState<T>` | `Success(s)` → `Content(s.value, s.isOffline)`; `Failure(e)` → `Error(e)` |

| AppError | title | message |
|---|---|---|
| NoConnection | `ui_error_no_connection_title` "No connection" / "Không có kết nối" | `ui_error_no_connection_message` "Check your internet connection and try again." / "Kiểm tra kết nối mạng và thử lại." |
| NotFound | `ui_error_not_found_title` "Not found" / "Không tìm thấy" | `ui_error_not_found_message` "The data you're looking for doesn't exist." / "Dữ liệu bạn tìm không tồn tại." |
| Server | `ui_error_server_title` "Server error" / "Lỗi máy chủ" | `ui_error_server_message` "Something went wrong on the server (%1$d)." / "Máy chủ gặp sự cố (%1$d)." |
| Unknown | `ui_error_unknown_title` "Something went wrong" / "Đã xảy ra lỗi" | `ui_error_unknown_message` "Please try again." / "Vui lòng thử lại." |

Strings chung khác: `ui_retry` Retry/Thử lại, `ui_offline_banner` "You're offline. Showing saved data." / "Bạn đang offline. Đang hiển thị dữ liệu đã lưu.", `ui_load_more_error` "Couldn't load more." / "Không tải thêm được."

## 3. Quy tắc UI

- Mọi text hiển thị và `contentDescription` lấy từ `strings.xml` trong `:translation` (`values/` English và `values-vi/` Tiếng Việt, key có tiền tố module); chuỗi không cần dịch ở `non-translation.xml`. Ngoại lệ duy nhất: dữ liệu (tên Pokémon, tên ability, số id) — format qua `PokemonFormat`.
- Key string có tiền tố module: `designsystem_`, `ui_`, `home_`, `details_`, `favourite_`, `profile_`, `auth_`, `onboarding_`, `main_`, `debugtool_`.
- Mỗi screen có màu nền `surface`; nội dung dạng form (login, profile) giới hạn `maxContentWidth` và căn giữa trên màn rộng.
- Edge-to-edge: `Scaffold` xử lý insets; không hardcode padding status bar.
- Không dùng `Color(...)` trực tiếp trong feature, trừ bảng màu type ở `details:impl` (screens.md).
