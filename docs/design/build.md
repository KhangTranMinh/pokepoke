# Build setup chi tiết

> Đọc cùng [plan.md](../plan.md) mục 2–3. File này là **nguồn chuẩn** cho version thư viện, plugin, convention plugin.

## 1. Version (đã kiểm tra trên Maven ngày 2026-11-23, bản stable mới nhất)

```toml
[versions]
# SDK / JVM
compileSdk = "37"
compileSdkMinor = "1"   # compileSdk 37.1: Compose UI 1.13.0 (BOM 2026.11.01) yêu cầu; plugin dùng release(compileSdk) { minorApiLevel = compileSdkMinor }
minSdk = "30"
targetSdk = "37"
jvm = "21"

# Build
agp = "9.5.2"
kotlin = "2.4.21"
ksp = "2.3.14"
hilt = "2.60.1"
room = "2.8.5"
kover = "0.9.11"
roborazzi = "1.79.0"
tomlj = "2.2.0"          # build-logic: đọc configs/env/*.toml (§4b)

# AndroidX
coreKtx = "1.19.1"
coreSplashscreen = "1.2.0"
activityCompose = "1.13.0"
appcompat = "1.8.0"
lifecycle = "2.11.1"
composeBom = "2026.11.01"
materialIconsCore = "1.7.8"
material3Adaptive = "1.3.0"
navigation3 = "1.2.2"
hiltLifecycleViewmodelCompose = "1.4.0"
datastore = "1.3.0"
startup = "1.2.0"

# Kotlin / network / image / security
coroutines = "1.11.0"
javaxInject = "1"        # @Qualifier / @Inject cho module Kotlin/JVM thuần (core:common-kotlin)
serialization = "1.13.0"
retrofit = "3.0.0"
okhttp = "5.5.1"
coil = "3.6.3"
tink = "1.24.0"

# Test
junit = "4.13.2"
androidxTestJunit = "1.3.0"
espresso = "3.7.0"
turbine = "1.2.1"
mockk = "1.14.12"
robolectric = "4.18.1"
konsist = "0.18.1"        # test kiến trúc (module :konsist); 1.0.0 mới là beta
```

### 1.1. Thư viện (artifact)

Cột "Alias gợi ý" là alias trong catalog `pokepoke` hoặc `libs` — phân chia theo §1.4 (module dùng `pokepoke.<alias>` hoặc `libs.<alias>`; tên alias không đổi so với trước T15c).

| Alias gợi ý | Artifact |
|---|---|
| androidx-core-ktx | `androidx.core:core-ktx` |
| androidx-core-splashscreen | `androidx.core:core-splashscreen` |
| androidx-activity-compose | `androidx.activity:activity-compose` |
| androidx-appcompat | `androidx.appcompat:appcompat` |
| androidx-lifecycle-runtime-compose | `androidx.lifecycle:lifecycle-runtime-compose` |
| androidx-lifecycle-viewmodel-compose | `androidx.lifecycle:lifecycle-viewmodel-compose` |
| androidx-lifecycle-viewmodel-navigation3 | `androidx.lifecycle:lifecycle-viewmodel-navigation3` |
| androidx-compose-bom | `androidx.compose:compose-bom` |
| androidx-compose-ui / ui-graphics / ui-tooling / ui-tooling-preview / ui-test-junit4 / ui-test-manifest | `androidx.compose.ui:*` (version từ BOM) |
| androidx-compose-foundation | `androidx.compose.foundation:foundation` (BOM) |
| androidx-compose-material3 | `androidx.compose.material3:material3` (BOM) |
| androidx-compose-material3-navigation-suite | `androidx.compose.material3:material3-adaptive-navigation-suite` (BOM) |
| androidx-compose-material-icons-core | `androidx.compose.material:material-icons-core` (`materialIconsCore`) |
| androidx-compose-material3-adaptive | `androidx.compose.material3.adaptive:adaptive` |
| androidx-compose-material3-adaptive-layout | `androidx.compose.material3.adaptive:adaptive-layout` |
| androidx-compose-material3-adaptive-navigation3 | `androidx.compose.material3.adaptive:adaptive-navigation3` |
| androidx-navigation3-runtime / ui | `androidx.navigation3:navigation3-runtime`, `navigation3-ui` |
| androidx-hilt-lifecycle-viewmodel-compose | `androidx.hilt:hilt-lifecycle-viewmodel-compose` |
| androidx-datastore-preferences | `androidx.datastore:datastore-preferences` |
| androidx-startup | `androidx.startup:startup-runtime` |
| androidx-room-runtime / ktx / compiler / testing | `androidx.room:*` |
| hilt-android / hilt-compiler / hilt-android-testing | `com.google.dagger:*` |
| javax-inject | `javax.inject:javax.inject` |
| hilt-core | `com.google.dagger:hilt-core` (version `hilt`) — Hilt cho module Kotlin/JVM thuần (`:business:domain`) |
| kotlinx-coroutines-core / android / test | `org.jetbrains.kotlinx:kotlinx-coroutines-*` |
| kotlinx-serialization-json | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| retrofit / retrofit-converter-kotlinx-serialization | `com.squareup.retrofit2:retrofit`, `converter-kotlinx-serialization` |
| okhttp / okhttp-mockwebserver | `com.squareup.okhttp3:okhttp`, `mockwebserver3` |
| coil-compose / coil-network-okhttp | `io.coil-kt.coil3:*` |
| tink-android | `com.google.crypto.tink:tink-android` |
| junit, androidx-test-junit, espresso-core, turbine, mockk, robolectric | như tên |
| roborazzi / roborazzi-compose / roborazzi-junit-rule | `io.github.takahirom.roborazzi:*` |
| konsist | `com.lemonappdev:konsist` — catalog `libs`, chỉ `:konsist` (`testImplementation`) |
| tomlj | `org.tomlj:tomlj` — chỉ `build-logic/convention` (`implementation`: chạy lúc build, khác các Gradle plugin `compileOnly`) |

### 1.2. Plugin

Tất cả nằm trong `[plugins]` của catalog `pokepoke` (module dùng `pokepoke.plugins.<alias>`, vd. `pokepoke.plugins.kotlin.compose`, `pokepoke.plugins.pokepoke.hilt`).

| Alias | Id | Version |
|---|---|---|
| android-application | `com.android.application` | agp |
| android-library | `com.android.library` | agp |
| kotlin-jvm | `org.jetbrains.kotlin.jvm` | kotlin |
| kotlin-compose | `org.jetbrains.kotlin.plugin.compose` | kotlin |
| kotlin-serialization | `org.jetbrains.kotlin.plugin.serialization` | kotlin |
| ksp | `com.google.devtools.ksp` | ksp |
| hilt | `com.google.dagger.hilt.android` | hilt |
| room | `androidx.room` | room |
| kover | `org.jetbrains.kotlinx.kover` | kover |
| roborazzi | `io.github.takahirom.roborazzi` | roborazzi |
| pokepoke-* | convention plugin (mục 3) | `unspecified` |

### 1.3. Xử lý khi combo version không build được (không hỏi lại, làm theo thứ tự)

AGP 9 có Kotlin tích hợp sẵn (built-in Kotlin) với KGP đi kèm. Compose compiler plugin và serialization plugin **phải cùng version với KGP thực sự được dùng**.

1. Thử như bảng trên: khai báo `kotlin-compose`, `kotlin-serialization` (`apply false`) ở root `build.gradle.kts` để KGP 2.4.21 nằm trên classpath.
2. Nếu Gradle báo xung đột KGP / built-in Kotlin: thêm `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:<kotlin>") } }` ở root.
3. Nếu vẫn lỗi: hạ `kotlin` về version KGP mà AGP 9.5.2 đi kèm (xem `./gradlew buildEnvironment`), hạ các thư viện báo lỗi "compiled with newer Kotlin" về bản stable gần nhất tương thích.
4. Nếu API của một thư viện khác với mô tả trong docs (vd. Navigation 3, Material3 Adaptive): **theo API thật của version đã pin** (đọc sources jar / KDoc), giữ nguyên ý nghĩa thiết kế.
5. Ghi mọi thay đổi version / lệch API vào [`docs/decisions.md`](../decisions.md).

### 1.4. Ba version catalog: `pokepoke` (build-logic), `coreLibs` (core) và `libs` (app)

Bảng version ở §1 / §1.1 / §1.2 vẫn là nguồn chuẩn cho **giá trị** version; T15c / T15d chia chúng vào ba file đồng cấp:

| File | Catalog | Chứa | Ai dùng |
|---|---|---|---|
| `gradle/pokepoke.versions.toml` | `pokepoke` | Mọi thứ `build-logic` đọc hoặc tự thêm: version SDK / JVM (`compileSdk`, `compileSdkMinor`, `minSdk`, `targetSdk`, `jvm`); artifact Gradle plugin (AGP, KGP, compose compiler, KSP, Hilt, Room, Kover, Roborazzi) + `[plugins]` tương ứng + alias convention plugin `pokepoke-*`; thư viện plugin tự thêm (Compose BOM + ui / foundation / material3 / icons / tooling / test, hilt-android / compiler / testing, room-*, kotlinx-serialization-json, kotlinx-coroutines-core / test, lifecycle-runtime / viewmodel-compose, hilt-lifecycle-viewmodel-compose, navigation3-runtime, material3-adaptive*, junit, mockk, turbine, robolectric, androidx-test-junit, roborazzi*); `tomlj`; cùng họ version với các thư viện trên: `hilt-core`, `kotlinx-coroutines-android`, `androidx-navigation3-ui`, `androidx-lifecycle-viewmodel-navigation3` (để một version chỉ khai báo một lần) | `build-logic` (`Catalog.kt`, `build-logic/settings.gradle.kts`); module `:core:*` và module khác dùng trực tiếp khi cần (`pokepoke.junit`, `pokepoke.plugins.pokepoke.android.library`...) |
| `gradle/core.versions.toml` | `coreLibs` | Thư viện ngoài của module `:core:*` (từ T15d, mục 11): `javax-inject`, `retrofit`, `retrofit-converter-kotlinx-serialization`, `okhttp`, `okhttp-mockwebserver`, `androidx-datastore-preferences`, `tink-android` | `core/**`; module khác khi cần (vd. `coreLibs.retrofit` ở `:business:data`). `build-logic` **không** đọc |
| `gradle/libs.versions.toml` | `libs` | Thư viện riêng của app mà module tự khai báo: coil, startup, activity-compose, core-ktx, splashscreen, appcompat, material3-navigation-suite, espresso, konsist | Module ngoài `:core:*` (`:ui:*`, `:business:*`, `:feature:*`, `:app`, `:debugtool`); `core/**` không dùng |

- **Mỗi artifact (`group:name`) nằm ở đúng một catalog** (kể cả cùng version): trùng → Gradle âm thầm chọn bản cao hơn, khó phát hiện. Test `CatalogConsistencyTest` trong `build-logic` đọc cả ba file (tomlj), fail nếu trùng `group:name` (library) hoặc trùng plugin id giữa hai file bất kỳ, nếu `version.ref` trỏ ngoài `[versions]` của chính file, hoặc nếu alias / version / plugin id mà convention plugin dùng không có trong `pokepoke.versions.toml`. Version tham chiếu (`version.ref`) chỉ trỏ tới `[versions]` **cùng file**.
- Root `settings.gradle.kts` khai báo cả `pokepoke` và `coreLibs`: `versionCatalogs { create("pokepoke") { from(files("gradle/pokepoke.versions.toml")) }; create("coreLibs") { from(files("gradle/core.versions.toml")) } }` (`libs` vẫn được Gradle tự tạo từ `gradle/libs.versions.toml`).
- `build-logic/settings.gradle.kts`: chỉ `create("pokepoke") { from(files("../gradle/pokepoke.versions.toml")) }` — `build-logic` **không** đọc `libs` và `coreLibs`.
- `Catalog.kt`: `named("pokepoke")`; mọi `addDep` / `version(...)` trong plugin chỉ tra catalog này.
- Module: plugin `alias(pokepoke.plugins.pokepoke.android.library)`..., thư viện nền tảng `pokepoke.xxx`, thư viện ngoài của core `coreLibs.xxx`, thư viện riêng của app `libs.xxx`. **`build.gradle.kts` của `:core:*` chỉ dùng `pokepoke.*` và `coreLibs.*`, không dùng `libs.*`** (core publish cùng `coreLibs`; project dùng không có `libs` của repo này).
- Core cần thư viện ngoài mới (không phải thứ plugin tự thêm) → thêm vào `core.versions.toml`.
- **Khi tách / publish:** `build-logic` đi cùng `pokepoke`; core đi cùng `coreLibs`, dùng build-logic đã publish và có version riêng (mục 11).
- **Khi publish `build-logic` lên Maven** (chưa làm): publish catalog `pokepoke` (và `coreLibs` cho core) thành artifact riêng bằng plugin `version-catalog`, cùng version với plugin; project dùng khai báo `create("pokepoke") { from("com.khangtm.pokepoke:catalog:<version>") }` → plugin luôn chạy với bộ version đã được test, `libs` hoàn toàn của project. Các file cấu hình (`configs/build/structure.toml`, `configs/env/`, `configs/signing/`) được đọc theo `rootProject` của project dùng, không nằm trong jar, nên vẫn đúng sau khi publish.

## 2. `settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }   // giữ content filter của google() như hiện tại
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// dependencyResolutionManagement giữ nguyên
rootProject.name = "PokePoke"     // đổi từ "Poke Poke" (dấu cách gây khó chịu cho một số tool)
include(":app", ":debugtool", ":core:common-kotlin", ":core:common-android", ...)  // đủ danh sách ở plan.md mục 4
```

## 3. Convention plugins (`build-logic/convention`)

- `build-logic/settings.gradle.kts` dùng catalog riêng `pokepoke` (§1.4): `versionCatalogs { create("pokepoke") { from(files("../gradle/pokepoke.versions.toml")) } }` (trước T15c: `libs`).
- `build-logic/convention/build.gradle.kts`: `kotlin-dsl`, `compileOnly` AGP, KGP, compose compiler gradle plugin, KSP, Room, Hilt, Kover, Roborazzi gradle plugins.
- Đọc version trong plugin: `extensions.getByType<VersionCatalogsExtension>().named("pokepoke")` (trước T15c: `"libs"`).
- Mỗi plugin là một class Kotlin; đăng ký trong `gradlePlugin { plugins { register(...) } }`; thêm alias vào `[plugins]` của catalog với `version = "unspecified"` hoặc dùng `id("pokepoke.xxx")`.

| Plugin id | Class | Áp dụng / cấu hình |
|---|---|---|
| `pokepoke.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application`; SDK từ catalog; `jvmToolchain(jvm)`; version từ `version.properties` (mục 4); buildTypes `debug` (`applicationIdSuffix = ".debug"`, `versionNameSuffix = "-debug"`) và `release` (giữ `optimization { enable = true }` hiện có); flavor `env` = `dev` / `qa` / `prod`, tắt `devRelease`, signing theo mục 4a; `buildFeatures.buildConfig = true`; `androidResources.generateLocaleConfig = true`; task `verifyReleaseHasNoDebugTool` (mục 5) |
| `pokepoke.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library`; SDK; `jvmToolchain(jvm)`; `testOptions.unitTests.isIncludeAndroidResources = true`; test deps chung (junit, mockk, turbine, coroutines-test, robolectric, androidx-test-junit, `testImplementation` các module trong `[library].testImplementation` của `structure.toml` (mục 3a; hiện `:core:testing`), trừ module trong `excludeAutoDependencies`); áp dụng `pokepoke.kover`; task `verifyModuleDependencies` (mục 5) |
| `pokepoke.android.compose` | `AndroidComposeConventionPlugin` | `org.jetbrains.kotlin.plugin.compose`; `buildFeatures.compose = true`; BOM cho implementation/testImplementation/androidTestImplementation; ui, ui-graphics, foundation, material3, material-icons-core, ui-tooling-preview; `debugImplementation` ui-tooling, ui-test-manifest; `testImplementation` ui-test-junit4; Roborazzi plugin + roborazzi, roborazzi-compose, roborazzi-junit-rule; `roborazzi { outputDir = file("src/test/screenshots") }` |
| `pokepoke.android.feature.api` | `AndroidFeatureApiConventionPlugin` | `pokepoke.android.library`; `android.testFixtures.enable = true`; `api` các module trong `[feature.api]` (mục 3a); `kotlinx-coroutines-core` |
| `pokepoke.android.feature.impl` | `AndroidFeatureImplConventionPlugin` | `pokepoke.android.library` + `pokepoke.android.compose` + `pokepoke.hilt`; `implementation` các module trong `[feature.impl]` (mục 3a) + lifecycle-runtime-compose, lifecycle-viewmodel-compose, hilt-lifecycle-viewmodel-compose, navigation3-runtime, material3-adaptive, material3-adaptive-layout, material3-adaptive-navigation3; `testImplementation(testFixtures(x))` cho mỗi x trong `[feature.impl].testFixtures` |
| `pokepoke.hilt` | `HiltConventionPlugin` | `com.google.devtools.ksp` + `com.google.dagger.hilt.android`; `implementation` hilt-android; `ksp` hilt-compiler; `testImplementation` hilt-android-testing; `kspTest` hilt-compiler |
| `pokepoke.room` | `RoomConventionPlugin` | `androidx.room` + ksp; `room { schemaDirectory("$projectDir/schemas") }`; room-runtime, room-ktx, `ksp` room-compiler; `testImplementation` room-testing |
| `pokepoke.kotlin.serialization` | `KotlinSerializationConventionPlugin` | `org.jetbrains.kotlin.plugin.serialization`; `implementation` kotlinx-serialization-json |
| `pokepoke.jvm.library` | `JvmLibraryConventionPlugin` | `org.jetbrains.kotlin.jvm` + `java-test-fixtures`; `jvmToolchain(jvm)`; `pokepoke.kover`; task `verifyModuleDependencies`; junit, coroutines-test, turbine, mockk (không có `:core:testing` vì đó là Android library) |
| `pokepoke.kover` | `KoverConventionPlugin` | Kover; exclude generated (mục 6); verify rule cho module khớp `[kover].modules` (mục 3a) |

Ghi chú:
- AGP 9 built-in Kotlin: **không** apply `org.jetbrains.kotlin.android`.
- Nếu `testFixtures` Kotlin cần cờ: thêm `android.experimental.enableTestFixturesKotlinSupport=true` vào `gradle.properties`.
- Module `:api` là **Kotlin/JVM thuần** (`pokepoke.jvm.library`) khi không cần Android: `core:storage:api`, `core:securestorage:api`, `core:common-kotlin`. Các module này không được chứa `@Inject` class / Hilt module (KSP Hilt không chạy ở đây).
- **`:business:domain`** — Kotlin/JVM thuần, ngoại lệ có chủ đích của dòng trên: `pokepoke.jvm.library` + plugin `ksp`; `api(core:common-kotlin)`, `api(kotlinx-coroutines-core)`, `implementation(hilt-core)`, `ksp(hilt-compiler)` (Hilt hỗ trợ thư viện Java/Kotlin thuần qua `hilt-core`: `@Inject` + `@Module @InstallIn(SingletonComponent::class)`). Không phụ thuộc `core:connectivity` (Android). Phương án dự phòng nếu Hilt không gom được binding từ module JVM: bỏ Hilt khỏi domain, `Impl` để `public`, `@Binds` use case đặt trong Hilt module của `:business:data` (ghi decisions).
- **`:business:data`** — Android library không tách api/impl: `pokepoke.android.library` + `pokepoke.hilt` + `pokepoke.room` + `pokepoke.kotlin.serialization`; `api(business:domain)`, `implementation(core:network, core:connectivity)`.
- Giữ Android library cho `core:navigation` (`@StringRes`, `ImageVector`, Compose; không tách api/impl), `core:deeplink` (`PendingIntent`, phụ thuộc `core:navigation`; không tách api/impl) và `feature:*:api`.

## 3a. Cấu trúc module: `configs/build/structure.toml`

Convention plugin chỉ chứa **logic**. Mọi **đường dẫn module** mà plugin tự thêm làm dependency, luật kiến trúc (mục 5), danh sách module có ngưỡng Kover (mục 6) và module debugtool đều đọc từ `configs/build/structure.toml` (được commit). Mục đích: dùng lại `build-logic` cho project khác (cùng flavor dev / qa / prod, cùng plugin id `pokepoke.*`) bằng cách **chỉ sửa file này**. Thư viện ngoài (artifact của catalog như junit, mockk, lifecycle...) vẫn nằm trong plugin, vì đó là quy ước kỹ thuật chứ không phải cấu trúc project.

Nội dung hiện tại: xem thẳng [`configs/build/structure.toml`](../../configs/build/structure.toml) — mỗi section / luật có comment giải thích (không chép lại ở đây để khỏi lệch). Các section: `[library]` (`testImplementation`, `excludeAutoDependencies`), `[feature.api]` (`api`), `[feature.impl]` (`implementation`, `testFixtures`), `[app]` (`debugTool`), `[kover]` (`minLineCoverage`, `modules`), `[[rules]]` (`from`, `except`, `to`, `reason`).

**Pattern** (dùng cho `from`, `except`, `to`, `kover.modules`), so khớp theo từng đoạn của project path:
- `*` = đúng **một** đoạn (`:core:*:impl` khớp `:core:storage:impl`, không khớp `:core:a:b:impl`).
- `**` = **một hoặc nhiều** đoạn (`:core:**` khớp `:core:network`, `:core:storage:impl`; `:**:impl` khớp mọi module tên cuối `impl`; `**` đứng một mình = mọi module).
- Không có regex, không có `?`. Đường dẫn không có ký tự đại diện = khớp chính xác.

**Kiểm tra** (vi phạm → build fail, message chỉ rõ file + section + giá trị):
- Đọc qua `providers.fileContents(...)` ở pha 2 (input của configuration cache; sửa file là cache làm mới), parse **một lần** cho cả build (đọc từ root project, dùng chung cho mọi module).
- Section / key lạ, sai kiểu (không phải mảng chuỗi, `minLineCoverage` không phải số nguyên 0–100), `[[rules]]` thiếu `from` / `to` / `reason` → fail.
- Đường dẫn **không có ký tự đại diện** phải là project có trong `settings.gradle.kts` (bắt lỗi gõ sai); pattern có `*` phải khớp ít nhất một project (tránh luật chết).
- Dependency trỏ về chính nó được bỏ qua (như hiện nay); `testFixtures(project(X))` được tính như phụ thuộc vào X.

Code: `build-logic/convention/.../ModuleStructure.kt` (parse + kiểm tra + `matches(pattern, path)`, hàm thuần, có unit test); các plugin nhận `ModuleStructure` thay cho hằng số.

## 3b. `build-logic/README.md` (cho người dùng plugin)

Viết ở T15c. Khác `build.md` (thiết kế của repo này): README dành cho project **khác** dùng `build-logic` (hôm nay qua `includeBuild`, sau này qua Maven), nên phải **tự đọc hiểu được**, không trỏ sang `docs/`. Bằng tiếng Việt, ngắn, nhiều ví dụ copy được. Nội dung bắt buộc:

1. **Plugin có gì**: bảng plugin id `pokepoke.*` → áp dụng gì (một dòng mỗi plugin).
2. **Yêu cầu**: version Gradle / AGP / JDK; Gradle plugin ngoài project phải tự khai báo (`compileOnly` trong build-logic: AGP, KGP, compose compiler, KSP, Hilt, Room, Kover, Roborazzi).
3. **Setup từng bước**: catalog `pokepoke` trong `settings.gradle.kts` (`includeBuild` hiện tại; Maven khi publish); `configs/build/structure.toml` (file mẫu + pattern `*` / `**`); `configs/env/config.toml`, `secrets.example.toml`, `secrets.toml`; `configs/signing/debug.*` (lệnh `keytool` tạo khóa) + `release.*`; các dòng `.gitignore` bắt buộc.
4. **Quy ước cố định**: flavor dev / qa / prod, tắt `devRelease`, chỉ `prodRelease` ký khóa release, BuildConfig + manifest placeholder từ `config.toml`.
5. **Task được thêm**: `verifyModuleDependencies`, `verifyReleaseHasNoDebugTool`, Kover verify — fail khi nào, sửa thế nào.
6. **Lỗi thường gặp**: thiếu `release.properties`, thiếu secret, path sai trong `structure.toml`, artifact trùng giữa hai catalog — message mẫu + cách sửa.
7. **Publish** (chưa làm): ghi "chưa hỗ trợ" + hướng đi (plugin + catalog `pokepoke` + module `:core:*` cùng version, build.md §1.4, §11); cập nhật khi làm thật.

## 4. `version.properties`

```properties
versionMajor=1
versionMinor=0
versionPatch=0
versionCode=1
```

Plugin application đọc qua `providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText` → parse `Properties`. Thiếu key → fail build với message rõ ràng. `versionName = "$major.$minor.$patch"`.

## 4a. Flavor & signing (chỉ `:app`)

Cấu hình trong `pokepoke.android.application` (module library **không** có flavor → lệnh của chúng không đổi).

| | dev | qa | prod |
|---|---|---|---|
| `applicationIdSuffix` | `.dev` | `.qa` | — |
| `versionNameSuffix` | `-dev` | `-qa` | — |
| `BuildConfig` (vd. `BASE_URL`) | `configs/env/` §4b | `configs/env/` §4b | `configs/env/` §4b |
| Tên app | `src/dev/res/values/non-translation.xml`: `app_name` = "PokePoke Dev" | `src/qa/...`: "PokePoke QA" | không override → `app_name` của `:ui:translation` |

- Flavor dimension duy nhất `env`. Kết hợp build type → variant: `devDebug`, `qaDebug`, `qaRelease`, `prodDebug`, `prodRelease`. **Tắt `devRelease`**: `androidComponents { beforeVariants(selector().withFlavor("env" to "dev").withBuildType("release")) { it.enable = false } }`.
- applicationId ví dụ: `com.khangtm.pokepoke.dev.debug`, `com.khangtm.pokepoke.qa`, `com.khangtm.pokepoke` (prodRelease). Các môi trường cài song song được. versionName ví dụ `1.0.0-dev-debug`, `1.0.0-qa`, `1.0.0`.
- `@BaseUrl` trong Hilt module của `:app` = `BuildConfig.BASE_URL`. Giá trị theo môi trường **không** nằm trong `build-logic`: đọc từ `configs/env/config.toml` (§4b). PokeAPI chỉ có một môi trường nên `BASE_URL` chỉ khai báo ở `[common]`.
- **Override `app_name` theo flavor** (ngoại lệ duy nhất của luật "string chỉ ở `:ui:translation`", CLAUDE.md): file `app/src/<flavor>/res/values/non-translation.xml` chỉ được **override key đã có** trong `:ui:translation` (hiện chỉ `app_name`), không thêm key mới, không có `values-vi`. Resource của `:app` thắng resource của library khi merge nên không lỗi trùng; áp dụng cho launcher và mọi chỗ dùng `R.string.app_name`.

**Signing** — file ở `configs/signing/`:

| File | Git | Dùng cho |
|---|---|---|
| `debug.keystore`, `debug.properties` | **commit** (khóa debug dùng chung cả team / CI → cài đè được giữa các máy) | mọi variant trừ `prodRelease` |
| `release.keystore`, `release.properties` | **gitignore** (`configs/signing/release.*`) | chỉ `prodRelease` |

- `.properties` có 4 key: `storeFile` (đường dẫn **tương đối với thư mục `configs/signing/`**), `storePassword`, `keyAlias`, `keyPassword`. Thiếu key → fail với message rõ ràng.
- `sharedDebug`: `signingConfig` DSL đọc `debug.properties` (file luôn có), gán cho cả build type `debug` và `release` → mọi variant mặc định ký bằng debug key.
- `prodRelease` → release key: **không** khai báo DSL `signingConfigs.release` (DSL cần giá trị lúc cấu hình → máy thiếu file sẽ hỏng mọi variant); thay bằng `androidComponents.onVariants(selector().withName("prodRelease")) { it.signingConfig.from(provider) }` với `Provider<SigningConfigInfo>` đọc `release.properties` lười (decisions.md T13c).
- Đọc file qua `providers.fileContents(...)` (configuration cache). **Thiếu `release.properties` / `release.keystore` không được làm hỏng các variant khác**: chỉ task của `prodRelease` (`assembleProdRelease`, `bundleProdRelease`, `packageProdRelease`...) fail với message "Thiếu configs/signing/release.properties — chỉ cần cho prodRelease". CI sau này tạo file này từ secret.
- `.gitignore` (đã có): `/configs/signing/release.*` và ngoại lệ `!/configs/signing/debug.keystore` (luật `*.keystore` chặn mọi keystore khác).

#### 4a.1. Signing chạy thế nào trong vòng đời Gradle

Code: `build-logic/convention/.../AppSigning.kt` (`signingConfigInfo`) và `AndroidApplicationConventionPlugin.kt`.

Gradle build qua 3 pha; signing release được thiết kế để **chỉ đọc file ở pha 3, và chỉ khi cần**:

```
1. Initialization   settings.gradle.kts
2. Configuration    áp plugin → chạy build script → AGP tạo variant → cấu hình task
3. Execution        chạy các task trong task graph (compile, package, sign...)
```

| Code | Chạy khi nào | Làm gì |
|---|---|---|
| `signingConfigInfo("debug", …).get()` | Pha 2, lúc áp plugin | Đọc **ngay** `debug.properties` (luôn có vì được commit) → tạo DSL `signingConfigs.create("sharedDebug")` |
| `buildTypes { debug/release { signingConfig = sharedDebug } }` | Pha 2, lúc áp plugin | Mặc định **mọi** variant ký bằng khóa debug (kể cả `qaRelease`) |
| `signingConfigInfo("release", …)` (không `.get()`) | Pha 2, lúc áp plugin | Chỉ tạo `Provider<SigningConfigInfo>` — **chưa đọc file** |
| `extensions.configure<ApplicationAndroidComponentsExtension> { … }` | Pha 2, ngay lúc áp plugin | Lấy extension `androidComponents` (Variant API của AGP) và **chỉ đăng ký callback** |
| `beforeVariants(selector().withFlavor("env" to "dev").withBuildType("release")) { it.enable = false }` | Pha 2, sau khi `app/build.gradle.kts` chạy xong, lúc AGP **sắp tạo** variant | `selector()` khớp mọi variant → lọc flavor `dev` + build type `release` = `devRelease`; `it` là `VariantBuilder`, `enable = false` → AGP không tạo variant, không có task `assembleDevRelease` |
| `onVariants(selector().withFlavor("env" to "prod").withBuildType("release")) { variant -> … }` | Pha 2, ngay sau khi AGP **đã tạo** variant, trước khi tạo task | `variant` là `ApplicationVariant` đã gộp DSL (lúc này `signingConfig` = `sharedDebug`); mọi thuộc tính là `Property` / `Provider` |
| `variant.signingConfig.from(releaseSigning)` | Pha 2 | **Nối** provider release vào `prodRelease` (thay `sharedDebug`) — vẫn chưa đọc file |
| Thân provider (`providers.fileContents(...)` → parse → kiểm tra key / `storeFile` → `SigningConfigInfo` hoặc ném `GradleException`) | Khi Gradle cần giá trị thật: task của `prodRelease` (`validateSigningProdRelease`, `packageProdRelease`...) nằm trong task graph — lúc lưu configuration cache hoặc lúc task chạy | Đọc `release.properties`; thiếu file → "Thiếu configs/signing/release.properties — chỉ cần cho prodRelease"; thiếu key → "thiếu key '…'" |

Ví dụ:

| Lệnh | Task graph có task `prodRelease`? | Đọc `release.properties`? | Không có file release |
|---|---|---|---|
| `./gradlew :app:assembleDevDebug` / `:app:assembleQaRelease` | không | không | ✅ xanh |
| `./gradlew :app:assembleProdRelease` | có | có | ❌ fail, message rõ ràng |
| `./gradlew assembleRelease` (= `qaRelease` + `prodRelease`) | có | có | ❌ fail |

Vì sao không dùng cách "chuẩn" `signingConfigs.release { … }` trong DSL / gán signing ở flavor `prod`:
- DSL `signingConfigs` cần giá trị (mật khẩu) **lúc cấu hình**; Gradle cấu hình mọi variant mỗi lần build → máy không có `release.properties` sẽ hỏng cả `assembleDevDebug`.
- `signingConfig` của build type thắng của flavor → đặt ở flavor `prod` không có tác dụng với `prodRelease` (build type `release` đã gán `sharedDebug`).
- `providers.fileContents` là input của configuration cache: sửa file `.properties` → cache tự làm mới.

Thay khóa release: chỉ thay `configs/signing/release.keystore` + sửa `release.properties`, không sửa code; kiểm tra `./gradlew :app:assembleProdRelease` rồi `apksigner verify --print-certs` (SHA-256 phải khác bản `qaRelease`). `storeType = "jks"` vẫn đọc được keystore PKCS12 do `keytool` mới tạo.

**`:debugtool`** gắn theo **môi trường**, không theo build type: `:app` khai báo `"devImplementation"(projects.debugtool)` + `"qaImplementation"(projects.debugtool)` (configuration của flavor có sẵn từ khi khai báo flavor, gọi trực tiếp được) → có ở `devDebug`, `qaDebug`, `qaRelease`; **không bao giờ** có ở `prodDebug`, `prodRelease`.

## 4b. Config theo môi trường (`configs/env/`, chỉ `:app`)

Giá trị khác nhau giữa dev / qa / prod (URL, cờ, secret) là **dữ liệu**, không nằm trong code `build-logic`. Plugin `pokepoke.android.application` đọc file TOML và sinh `BuildConfig` cho từng variant; thêm / sửa key **chỉ sửa file**, không sửa plugin.

| File | Git | Nội dung |
|---|---|---|
| `config.toml` | **commit** | giá trị không bí mật |
| `secrets.example.toml` | **commit** | danh sách key secret bắt buộc, giá trị `""` (mẫu cho người mới / CI) |
| `secrets.toml` | **gitignore** (`/configs/env/secrets.toml`), không bao giờ commit / in ra | giá trị secret thật. Máy mới: `cp configs/env/secrets.example.toml configs/env/secrets.toml` rồi điền giá trị |

```toml
# configs/env/config.toml
[common]                 # mọi flavor; flavor ghi đè key trùng tên
BASE_URL = "https://pokeapi.co/api/v2/"

# Deeplink theo môi trường (deeplinks.md §1). Tạm thời: dev / qa gắn tên môi trường vào scheme và host
# để cài song song không tranh link với nhau; prod giữ giá trị gốc.
[dev]
DEEPLINK_SCHEME = "pokepoke-dev"
DEEPLINK_HOST = "dev.pokepoke.com"

[qa]
DEEPLINK_SCHEME = "pokepoke-qa"
DEEPLINK_HOST = "qa.pokepoke.com"

[prod]
DEEPLINK_SCHEME = "pokepoke"
DEEPLINK_HOST = "pokepoke.com"
```

```toml
# configs/env/secrets.example.toml  (secrets.toml cùng cấu trúc, có giá trị thật)
[common]

[dev]

[qa]

[prod]
```

Luật (vi phạm → build fail với message chỉ rõ file, section, key):
- Section chỉ gồm `[common]`, `[dev]`, `[qa]`, `[prod]`; đủ cả 4 (được rỗng). Không bảng con, không mảng.
- Giá trị flavor = `[common]` + phần ghi đè của flavor. Sau khi gộp, **ba flavor phải có cùng bộ key** (tránh dev có key mà prod thiếu → crash lúc chạy).
- Tên key `UPPER_SNAKE_CASE` (định danh Java hợp lệ), vì thành tên field `BuildConfig`.
- **Một key không được nằm ở cả `config.toml` lẫn secret.**
- Kiểu `config.toml` lấy từ TOML: chuỗi → `String`, số nguyên → `long` (`BuildConfig` field `long`), `true` / `false` → `boolean`; kiểu khác (số thực, ngày, mảng, bảng) → fail. **Secret luôn là `String`**.
- Ghi đè trong flavor phải cùng kiểu với `[common]`.
- Bộ key secret bắt buộc = key trong `secrets.example.toml`; `secrets.toml` thiếu key bắt buộc, giá trị rỗng, hoặc có key thừa → fail (chỉ khi variant cần nó, xem dưới).

**Chạy lúc nào** (cùng ý tưởng với release signing, §4a.1):

| Phần | Khi nào đọc | Thiếu / sai |
|---|---|---|
| `config.toml`, `secrets.example.toml` | Pha 2 (cấu hình), qua `providers.fileContents(...)` → input của configuration cache, sửa file là cache làm mới | Fail ngay mọi lệnh (file được commit nên luôn phải đúng) |
| `secrets.toml` | **Lười**: `androidComponents.onVariants { variant.buildConfigFields?.put(key, provider) }` với `Provider<BuildConfigField>` chỉ được lấy giá trị khi task `generate<Variant>BuildConfig` của variant đó nằm trong task graph | Chỉ fail variant cần secret đó: "Thiếu secret 'KEY' cho flavor 'prod' trong configs/env/secrets.toml". `secrets.example.toml` không có key nào → không cần `secrets.toml` |

- **Manifest placeholder**: mọi key của `config.toml` (sau khi gộp) còn được đặt vào `variant.manifestPlaceholders` (giá trị `toString()`), dùng trong manifest dạng `${KEY}` (vd. `android:scheme="${DEEPLINK_SCHEME}"`). **Secret không bao giờ thành placeholder** (manifest đọc được bằng `aapt2 dump`).
- `buildConfig = true` cho `:app` (đã có); field sinh ra có comment `// from configs/env/config.toml` hoặc `// secret` (không có giá trị trong comment).
- Code: `build-logic/convention/.../AppEnvConfig.kt` (parse + gộp + kiểm tra, hàm thuần, có unit test trong `build-logic`) và `AndroidApplicationConventionPlugin.kt` (gắn vào variant).
- **CI**: GitHub Secret `ENV_SECRETS_TOML` (toàn bộ nội dung `secrets.toml`); bước "Write env secrets" ghi ra `configs/env/secrets.toml` **chỉ khi** secret có giá trị (dùng `env:` + `printf '%s'`, không `echo` / không in log). Chưa có secret → bước không làm gì.

⚠️ **"Secret" trong `BuildConfig` vẫn nằm trong APK** và dịch ngược được (R8 không che chuỗi). Tách `secrets.toml` chỉ để **giá trị không nằm trong git**, không giữ được bí mật trên máy người dùng. Key thật sự quan trọng (vd. HMAC ký request có giá trị) phải ký ở server, hoặc ít nhất kết hợp Play Integrity; không đặt secret phía server vào đây.

Thêm key mới: thêm vào `config.toml` (hoặc `secrets.example.toml` + `secrets.toml` + cập nhật GitHub Secret `ENV_SECRETS_TOML`), rồi dùng `BuildConfig.KEY` trong `:app` (chỉ `:app` có `BuildConfig` theo flavor; module khác nhận giá trị qua Hilt qualifier như `@BaseUrl`).

## 5. Check kiến trúc

**Task `verifyModuleDependencies`** (mọi module Android/JVM, hook vào `preBuild` và `check`). Input: danh sách project path mà module khai báo trong mọi configuration (`implementation`, `api`, `compileOnly`, `runtimeOnly`, `debugImplementation`, `releaseImplementation`, `testImplementation`, `testFixturesImplementation`, `testFixturesApi`, `androidTestImplementation`, `ksp`), thu thập lúc cấu hình thành `List<String>` (tương thích configuration cache). Luật đọc từ `[[rules]]` của `configs/build/structure.toml` (mục 3a), vi phạm → fail với message chỉ rõ module + dependency + `reason`. Bảng dưới là nội dung hiện tại của file (đổi luật: sửa file, rồi sửa bảng này cho khớp):

| Module hiện tại | Không được phụ thuộc |
|---|---|
| bất kỳ, trừ `:app` | `:*:impl`, `:debugtool`, `:business:data` |
| `:core:**` | `:ui:**`, `:business:**`, `:feature:**`, `:app`, `:debugtool` — core dùng chung / sẽ publish chỉ phụ thuộc core (mục 11) |
| `:business:**` | `:ui:**`, `:feature:**` (business không biết UI, kể cả string) |
| `:debugtool` | `:business:**`, `:feature:**` |
| `:ui:translation` | mọi module khác (chỉ chứa resource, không phụ thuộc gì) |
| `:konsist` | mọi module khác (test kiến trúc đọc source dạng file, không cần phụ thuộc module) |

Ngoại lệ: `testFixtures(project(X))` được tính như phụ thuộc vào X (áp luật bình thường).

**Task `verifyReleaseHasNoDebugTool`** (chỉ `:app`, hook vào `check` và assemble / bundle của mọi variant `prod`): duyệt `prodDebugRuntimeClasspath` và `prodReleaseRuntimeClasspath` `.incoming.resolutionResult.allComponents` (flavor `prod` không bao giờ có debugtool; dev / qa cố ý có, mục 4a), fail nếu có `ProjectComponentIdentifier` với `projectPath` = `[app].debugTool` của `structure.toml` (hiện `:debugtool`).

## 6. Kover

- Áp dụng cho mọi module; root aggregate (`kover(project(":x"))` cho tất cả) → `./gradlew koverHtmlReport`.
- Exclude: `*_Factory`, `*_MembersInjector`, `*_HiltModules*`, `Hilt_*`, `*_Impl` (Room), `*_Impl$*`, `*ComposableSingletons*`, `*.BuildConfig`, `*Module` / `*Module$*` (Hilt module), `*RouteKt` (composable root — xem screens.md), `*PreviewKt`, `*.di.*`.
- **Verify rule** `minBound([kover].minLineCoverage)` (line, hiện 70) cho module khớp `[kover].modules` của `structure.toml` (mục 3a), hiện là: `:core:common-kotlin`, `:core:common-android`, `:core:network`, `:core:connectivity`, `:core:navigation`, `:core:deeplink`, `:core:*:impl`, `:business:domain`, `:business:data`, `:feature:*:impl`, `:debugtool`. Module khác không đặt ngưỡng.

## 7. Roborazzi / Robolectric

- `src/test/resources/robolectric.properties`: `sdk=36` (nếu Robolectric 4.18.1 báo không hỗ trợ, dùng mức cao nhất được hỗ trợ) và `graphicsMode=NATIVE`.
- Ghi ảnh: `./gradlew recordRoborazziDebug`; kiểm tra: `./gradlew verifyRoborazziDebug`. Ảnh commit vào `src/test/screenshots/` của từng module.
- Thiết bị mặc định: `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`; ảnh tablet/foldable mở: `w841dp-h701dp-xhdpi`.

## 8. `gradle.properties`

Thêm:

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
ksp.incremental=false   # KSP 2.3.14 + AGP 9 lỗi "Number of loaded files in snapshots differs" (decisions.md, T08)
```

Giữ nguyên `org.gradle.configuration-cache=true` và `kotlin.code.style=official` có sẵn.

## 9. Lệnh kiểm tra chuẩn

| Mục đích | Lệnh |
|---|---|
| Build debug + release (mọi variant) | `./gradlew assembleDebug assembleRelease` (`assembleRelease` gồm `prodRelease` → cần `configs/signing/release.*`) |
| Build một variant | `./gradlew :app:assembleDevDebug`, `:app:assembleQaRelease`, `:app:assembleProdRelease` |
| Unit test `:app` | `./gradlew :app:testDevDebugUnitTest` |
| Test + check kiến trúc + lint một module | `./gradlew :<module>:check` |
| Toàn bộ | `./gradlew check verifyRoborazziDebug koverVerify` |
| Coverage report | `./gradlew koverHtmlReport` |
| Test `build-logic` (included build, `./gradlew check` ở root **không** chạy) | `./gradlew -p build-logic check` |
| Giống CI | `./gradlew :app:assembleQaDebug :app:assembleQaRelease check verifyRoborazziDebug koverVerify` + `./gradlew -p build-logic check` |

## 10. CI (GitHub Actions) & bảo vệ `main`

**Workflow `.github/workflows/ci.yml`** — job `build` (tên check bắt buộc trong ruleset):
- Chạy khi: `pull_request` vào `main`, `push` lên `main`; `concurrency` theo ref, hủy run cũ (`cancel-in-progress`).
- Runner **`macos-latest`**: ảnh Roborazzi được ghi trên macOS; Robolectric render font trên Linux khác → `verifyRoborazziDebug` sẽ fail giả trên Ubuntu. Repo public nên runner macOS miễn phí.
- Bước: `actions/checkout` → `actions/setup-java` (Temurin 21) → `gradle/actions/setup-gradle` (cache; chỉ ghi cache ở `main`) → lệnh "Giống CI" ở mục 9 → khi fail: upload artifact `**/build/reports/**`, `**/build/test-results/**`, ảnh diff Roborazzi (`**/build/outputs/roborazzi/**`).
- **Chỉ build `qaDebug` + `qaRelease`** (không cần khóa release; `qaRelease` kiểm tra R8 / minify). `check` vẫn chạy unit test mọi variant của `:app` và mọi module, `verifyReleaseHasNoDebugTool`, `verifyModuleDependencies`, lint.
- Secret: chỉ `ENV_SECRETS_TOML` (tùy chọn, §4b; hiện chưa đặt vì chưa có secret). Khi cần CI build `prodRelease`: tạo `configs/signing/release.*` từ GitHub Secrets trong một job riêng (chưa làm).

- ⏸ **Tạm tắt** (2026-11-26, theo yêu cầu): `env.CI_SKIP: "true"` trong `ci.yml` → job `build` chỉ in một dòng rồi xanh (ruleset vẫn thỏa). Trong lúc tắt, agent **bắt buộc** chạy lệnh "Giống CI" (mục 9) ở local trước khi push và ghi kết quả vào body PR. Bật lại: `CI_SKIP: "false"` (một PR).

**Hai nơi cấu hình, nối với nhau bằng tên check `build`:**

| Nơi | Ở đâu | Quyết định |
|---|---|---|
| `.github/workflows/ci.yml` | trong code | CI **chạy gì**, khi nào; mỗi lần chạy tạo check `build` (xanh / đỏ) trên commit / PR. Không tự chặn gì |
| Ruleset `protect-main` (id `28142956`) | GitHub: Settings → Rules → Rulesets | **Có được merge không**: bắt buộc PR, check `build` xanh... |
| Cài đặt merge | GitHub: Settings → General → Pull Requests | Chỉ Create a merge commit, tự xóa branch sau merge |

⚠️ **Đổi tên job `build` trong `ci.yml` → phải sửa ruleset cùng lúc**; nếu không, ruleset chờ một check không bao giờ xuất hiện → mọi PR bị khóa. Ruleset / cài đặt không nằm trong code: đổi qua web hoặc `gh api repos/KhangTranMinh/pokepoke/rulesets/28142956` (token cần quyền Administration).

**Ruleset `protect-main`** (đã tạo 2026-11-26 bằng GitHub API; từ 2026-11-27 áp dụng cho `main` **và `release/**`** (§12.5), `do_not_enforce_on_create` của check `build` = true để tạo được branch release):
- Bắt buộc PR, **0 approval** (PR do agent tạo bằng token của user → tác giả là user, GitHub không cho tự approve; "review" = user bấm merge).
- Bắt buộc status check `build` xanh, branch phải cập nhật theo `main` (strict).
- Cấm force push, cấm xóa branch; **không có bypass** (kể cả admin — token của agent mang quyền admin).
- Repo chỉ bật **Create a merge commit** (tắt squash, rebase); ruleset cho phép duy nhất cách `merge`, **không** bật *Require linear history*. Mỗi PR trên `main` = commit `Txx: …` (nguyên vẹn, giữ `Co-Authored-By`) + commit `Merge pull request #N from <branch>` → cây git thấy rõ từng task rẽ ra / nhập vào; `git log --first-parent main` chỉ hiện các commit merge (mỗi dòng một PR); gỡ cả task: `git revert -m 1 <merge>`.
- Branch phải cập nhật theo `main` trước khi merge (strict): agent `git rebase main` trên branch chưa merge rồi `--force-with-lease` (giữ branch 1 commit, bong bóng gọn).

## 11. Module dùng chung (`:core:*`) & publish

Mục tiêu: sau này publish lên Maven, để project khác dùng lại, các phần sau:
- `build-logic`, cùng catalog `pokepoke`; core cùng catalog `coreLibs`.
- `:core:common-kotlin`, `:core:common-android`, `:core:connectivity`, `:core:network`, `:core:storage:*`, `:core:securestorage:*`.
- `:core:deeplink`, `:core:navigation`, `:core:testing`.

**Ranh giới = thư mục** (từ T15d):

| Nhóm | Module | Publish |
|---|---|---|
| `:core:*` | hạ tầng generic ở trên | ✅ (sau này) |
| `:ui:*` | `designsystem` (theme, `PokeIcons`, component), `common` (composable dùng chung), `translation` (string) | ❌ riêng của app |
| `:business:*`, `:feature:*`, `:app`, `:debugtool` | | ❌ |

**Luật đã áp dụng ngay**, để code hôm nay luôn publish được:
1. `:core:**` chỉ phụ thuộc `:core:**`. Luật nằm trong `[[rules]]` của `structure.toml` (mục 5) và được build kiểm tra.
2. `build.gradle.kts` của `:core:*` chỉ dùng catalog `pokepoke` + `coreLibs`, không dùng `libs` (mục 1.4).
3. Core không chứa string, resource hay giá trị riêng của app (URL, scheme, tên app). Những giá trị đó do `:app` cấp qua Hilt (`@BaseUrl`, `DeepLinkConfig`...) từ `BuildConfig` (mục 4b).
4. Core có sẵn Hilt module (`@InstallIn`), nên project dùng core phải dùng Hilt. Đây là điều kiện chấp nhận được vì cùng stack, cần ghi rõ trong README khi publish.
5. Phụ thuộc phải đi kèm khi dùng:
   - `deeplink` cần `navigation`.
   - Plugin `pokepoke.android.library` tự thêm `:core:testing` (`structure.toml` `[library]`), nên `testing` phải được publish cùng.

**Hướng phụ thuộc:** core → build-logic, một chiều (qua plugin + version trong `pokepoke`). Code build-logic không biết gì về core: mọi path module nằm ở `structure.toml`.

**Khi publish thật (chưa làm, YAGNI).** Khi đó cần:
- Plugin `pokepoke.publish`: dùng `maven-publish`, groupId `com.khangtm.pokepoke`, artifactId theo path (`core-network`, `core-storage-api`...).
- Version: build-logic + `pokepoke` chung một version; core + `coreLibs` có version riêng, vòng đời độc lập; cả hai tách khỏi `version.properties` của app.
- `consumer-rules.pro` cho R8 (kotlinx-serialization, Retrofit, Tink).
- `explicitApi()` và kiểm tra tương thích nhị phân (binary-compatibility-validator).
- Publish `testFixtures` của `storage:api`, `securestorage:api`, `navigation` (fake cho test). Cần kiểm chứng với AGP.
- `structure.toml` của project dùng: dependency trong `[library]` / `[feature.*]` vẫn là project path, nên cần cơ chế cho phép khai báo artifact Maven. Thiết kế lúc làm.
- Tên trong `core:securestorage` (`pokepoke_tink_keyset`, `android-keystore://pokepoke_master_key`) giữ nguyên. Không gây lỗi, vì dữ liệu và Keystore là riêng của từng app.

## 12. CD — Firebase App Distribution

Mục tiêu: build `qaRelease` / `prodRelease` rồi tự đẩy APK cho tester qua Firebase App Distribution. CD **không** thay CI: CI (mục 10) kiểm tra PR; CD chỉ build + phát hành, chạy sau khi code đã vào `main`.

### 12.1. App trên Firebase

Hai Firebase project, mỗi môi trường một project. Prod tách khỏi qa: tester, quyền và (sau này) Crashlytics / Analytics không lẫn nhau. `dev` không phát hành.

| Firebase project | Android app (package) | Variant phát hành | Khóa ký | SHA-1 | SHA-256 |
|---|---|---|---|---|---|
| `pokepoke2026` | `com.khangtm.pokepoke.qa` | `qaRelease` | debug (`configs/signing/debug.keystore`, commit) | `AE:10:40:82:5F:C5:AA:68:23:95:A7:F4:B6:3F:28:BC:4D:0B:A8:C1` | `B7:A5:BA:D4:10:37:66:F2:7F:FF:9C:4F:BB:07:D1:E3:7F:B3:DC:8F:84:49:28:CF:57:85:F4:87:DD:18:42:9D` |
| `pokepoke2026-prod` | `com.khangtm.pokepoke` | `prodRelease` | release (`configs/signing/release.keystore`, alias `pokepoke`, không commit) | `D5:64:D8:DC:F9:C1:F8:4C:F5:9E:EF:E3:C9:8C:07:2A:7C:95:7D:77` | `EC:B5:0F:C4:75:94:47:9C:BD:8F:B1:1B:F9:AA:76:B4:65:A0:AF:81:F8:65:37:F2:4E:61:3E:7C:D4:B9:71:6C` |

- **SHA** lấy bằng lệnh `keytool -list -v -keystore configs/signing/<debug|release>.keystore -alias <alias>`. Mật khẩu nằm trong `.properties`; truyền qua `-storepass:env`, không in ra.
- **App Distribution không cần SHA.** Khai báo SHA để sau này dùng Google Sign-In, App Check hoặc Dynamic Links thì khỏi phải thêm lại.
- **Đổi khóa release thì phải cập nhật SHA của `pokepoke-prod`** (Firebase `pokepoke2026-prod` → Project settings → app → *Add fingerprint*) và cập nhật bảng trên.
- **`qaDebug` (`com.khangtm.pokepoke.qa.debug`) và `prodDebug` (`com.khangtm.pokepoke.debug`) không phát hành.** Khi nào cần SDK Firebase cho bản debug thì thêm app riêng.
- **Hiện chưa dùng SDK Firebase trong app**, nên không cần `google-services.json` / plugin `google-services`. App Distribution chỉ cần **App ID** (`1:<số>:android:<hash>`, xem ở Project settings). Khi nào thêm SDK thì đặt file ở `app/src/qa/google-services.json` / `app/src/prod/google-services.json` (commit được: API key trong đó không phải bí mật, đã nằm sẵn trong APK; giới hạn key ở Google Cloud Console) và thiết kế lúc đó. Hiện **không** đặt file vào repo: chưa có plugin đọc nó.

### 12.2. Workflow `.github/workflows/cd.yml`

| Job | Khi chạy | GitHub Environment | Build | Phát cho nhóm tester |
|---|---|---|---|---|
| `distribute-qa` | `push` lên `release/**` **có đổi `version.properties`** (`on.push.paths`), tức là merge PR bump version (§12.5); hoặc bấm tay: `workflow_dispatch` với input `env = qa`, chọn branch `release/*` ở *Use workflow from* | `qa` (chỉ branch `release/*`) | `./gradlew :app:assembleQaRelease` | `qa` |
| `distribute-prod` | `push` tag `v*` (vd. `v1.2.0`), hoặc `workflow_dispatch` với input `env = prod` (chọn **tag** ở *Use workflow from*) | `prod` (**bắt buộc user duyệt** trước khi job chạy; chỉ tag `v*`) | `./gradlew :app:assembleProdRelease` | `prod` |

- **Trigger:**
  ```yaml
  on:
    push:
      branches: ['release/**']
      paths: ['version.properties']
      tags: ['v*']
    workflow_dispatch:
      inputs:
        env: { type: choice, options: [qa, prod], required: true }
  ```
  - `paths` chỉ lọc push lên branch, push tag luôn chạy.
  - Job chọn bằng `if:`. `qa` chạy khi `github.ref` bắt đầu `refs/heads/release/`, hoặc khi dispatch với `inputs.env == 'qa'`. `prod` chạy khi `github.ref` bắt đầu `refs/tags/v`, hoặc khi dispatch với `inputs.env == 'prod'`.
- **Vì sao chỉ build khi đổi version:**
  - PR fix bug vào release không đụng `version.properties`, nên không build. Tester chỉ nhận bản khi có PR bump version.
  - Mỗi bản qa có `versionCode` riêng: `1.2.0 (13)`, `1.2.0 (14)`...
- **Runner `ubuntu-latest`:** chỉ build, không chạy test hay screenshot. Bước cài đặt giống CI: Temurin 21, `setup-gradle` (chỉ đọc cache), chấp nhận license SDK, bước "Write env secrets" (§4b). `actions/checkout` dùng `fetch-depth: 0`, cần cho ghi chú phát hành và kiểm tra tag.
- **Không chạy trên `pull_request`**, để secret không lộ cho code chưa review. `permissions: contents: read`.
- **`concurrency`:**
  - `qa` dùng `cd-qa-${{ github.ref }}` với `cancel-in-progress: true`.
  - `prod` dùng `cd-prod-${{ github.ref }}` với `cancel-in-progress: false`.
- **Không bị `CI_SKIP` ảnh hưởng:** `CI_SKIP` chỉ nằm trong `ci.yml`.
- **Kiểm tra trước khi build prod** (sai thì fail, message rõ ràng):
  1. Tag phải đúng `v<versionMajor>.<versionMinor>.<versionPatch>` đọc từ `version.properties` của commit được tag.
  2. Commit được tag phải nằm trên một branch `release/*` (`git branch -r --contains "$GITHUB_SHA"` có `origin/release/`). Không phát prod từ `main`.
- **Ghi chú phát hành (release notes):** dòng đầu là `<versionName> (<versionCode>) · <7 ký tự SHA commit>`, sau đó là danh sách commit (`git log --no-merges --pretty='- %s'`):
  - `qa`: các commit từ lần đổi `version.properties` trước đó tới HEAD, tối đa 30. Lấy bằng `git log -n 2 --format=%H -- version.properties`: dòng thứ hai là mốc.
  - `prod`: các commit từ tag `v*` trước đó tới tag hiện tại. Nếu chưa có tag trước thì lấy 30 commit gần nhất.
- **Bước phát hành:** dùng Firebase CLI chính chủ, pin version, không dùng action của bên thứ ba:
  ```sh
  npx --yes firebase-tools@15.37.0 appdistribution:distribute "$APK" \
    --app "$FIREBASE_APP_ID" --groups "$FIREBASE_GROUPS" --release-notes-file "$RUNNER_TEMP/notes.txt"
  ```
  - Xác thực bằng `GOOGLE_APPLICATION_CREDENTIALS=$RUNNER_TEMP/firebase-sa.json`. File này ghi từ secret bằng `printf '%s'` và xoá ở bước cuối (`if: always()`).
  - Đường dẫn APK: `app/build/outputs/apk/<flavor>/release/app-<flavor>-release.apk`. Job phải kiểm tra file tồn tại.
- **Khóa release cho prod:** ghi từ secret trước khi build:
  - `RELEASE_KEYSTORE_BASE64` → `base64 -d` → `configs/signing/release.keystore`.
  - `RELEASE_PROPERTIES` → `configs/signing/release.properties`.
  - Cả hai được xoá ở bước cuối (`if: always()`).
- **Lưu APK làm artifact** của run (`actions/upload-artifact`, giữ 14 ngày). Có artifact thì khi Firebase lỗi vẫn tải được bản đã build.

### 12.3. Cấu hình & secret

**Không bí mật → commit trong `cd.yml`** (`env:` của từng job): App ID (`1:<số>:android:<hash>`) và alias nhóm tester chỉ là định danh. App ID đã nằm sẵn trong mọi APK có SDK Firebase; không có service account thì không làm được gì.

| Job | `FIREBASE_APP_ID` | `FIREBASE_GROUPS` |
|---|---|---|
| `distribute-qa` | App ID của `com.khangtm.pokepoke.qa` trong `pokepoke2026`: `1:477109968891:android:a2888a4fc232aa4c26355e` | `qa` |
| `distribute-prod` | App ID của `com.khangtm.pokepoke` trong `pokepoke2026-prod`: `1:702504399810:android:b4dada09372e4efd8d1c5b` | `prod` |

`FIREBASE_GROUPS` là **alias** của nhóm (Firebase → App Distribution → Testers & Groups → tên nhóm → alias), không phải tên hiển thị.

**Bí mật → GitHub → Settings → Environments.** Mỗi Environment có bộ giá trị riêng nhưng **cùng tên**, nên workflow không phải phân nhánh theo môi trường.

| Secret | `qa` | `prod` |
|---|---|---|
| `FIREBASE_SERVICE_ACCOUNT` | JSON key của service account `github-cd` thuộc `pokepoke2026` | JSON key của service account `github-cd` thuộc `pokepoke2026-prod` |
| `RELEASE_KEYSTORE_BASE64` | — | `base64 -i configs/signing/release.keystore` |
| `RELEASE_PROPERTIES` | — | nội dung `configs/signing/release.properties` |
| `ENV_SECRETS_TOML` (secret của repo, đã có, §4b) | dùng chung | dùng chung |

- **Environment `prod`:** bật *Required reviewers* (user) và *Deployment branches and tags* chỉ cho tag `v*`.
- **Environment `qa`:** chỉ cho branch `release/*` (*Selected branches and tags* → luật Branch `release/*`). Bản qa luôn build từ branch release, không build từ `main`; chạy tay từ `main` sẽ bị Environment từ chối.
- **Service account riêng `github-cd`, chỉ có role Firebase App Distribution Admin** (Google Cloud Console → IAM & Admin → Service accounts), mỗi project một cái. Tạo key JSON, dán vào secret, rồi **xoá file key ở máy**.
- **Không dùng `firebase-adminsdk-…`:** service account này có quyền quản trị toàn bộ Firebase (Auth, Firestore, Storage, cấu hình...). Lộ key thì mất cả project. CD chỉ cần quyền phát bản.
- **Không bao giờ commit hay in ra** JSON key, keystore, mật khẩu (CLAUDE.md).

### 12.4. Việc user làm tay (một lần, trước khi workflow chạy được)

1. ✅ **Firebase Console:** tạo 2 project `pokepoke2026` (qa) và `pokepoke2026-prod`.
2. ✅ **Thêm Android app vào mỗi project** (package và SHA theo bảng 12.1). Ghi **App ID** vào bảng 12.3.
3. **App Distribution** (mỗi project):
   - Mở *Get started*.
   - ✅ Tab *Testers & Groups*: nhóm `qa` (project qa) / `prod` (project prod), có email tester.
4. **Google Cloud Console** (mỗi project): tạo service account `github-cd`, role *Firebase App Distribution Admin*, tạo key JSON.
5. **GitHub → Settings → Environments:**
   - ✅ Tạo `qa` và `prod`, thêm secret theo bảng 12.3.
   - `prod`: bật Required reviewers và giới hạn tag `v*`.
6. **Xoá file key JSON ở máy** sau khi đã dán vào secret.
7. **Chạy thử:** tạo `release/v1.0` từ `main` (§12.5), rồi Actions → CD → *Run workflow* → branch `release/v1.0`, `env = qa`; hoặc merge một PR bump version vào branch đó. Tester nhận email mời và cài bằng app *Firebase App Tester*.

### 12.5. Quy trình release (branch `release/vX.Y`)

```
main ──●──●──●───────────────────────●(merge về)──●──
            \                        /
   release/v1.2 ●bump──●fix──●fix──●bump──●tag v1.2.0
                 │qa                │qa    │prod (user duyệt)
```

| Việc | Branch / PR | Ai làm | Build |
|---|---|---|---|
| **Cắt release** | tạo `release/vX.Y` từ `main` (GitHub UI hoặc `git push origin main:refs/heads/release/vX.Y`) | user | — |
| **Bump version** | branch `bump/vX.Y.Z-<versionCode>` từ `release/vX.Y`, chỉ sửa `version.properties`; PR vào `release/vX.Y` | agent hoặc user | qa |
| **Fix bug của bản release** | branch `fix/<slug>` từ `release/vX.Y`, PR vào `release/vX.Y` (1 commit, như task) | agent | — |
| **Phát prod** | `git tag vX.Y.Z <commit trên release/vX.Y> && git push origin vX.Y.Z` | user | prod |
| **Đưa fix về `main`** (lúc nào cũng được, nên làm sớm để ít conflict) | branch `merge/release-vX.Y-<ngày>` tạo từ `release/vX.Y` rồi `git merge origin/main` (giải conflict ở đây); PR vào `main` | agent hoặc user | — |

- **Version chỉ đổi trên `release/*`:**
  - Không bump version trên `main`. `main` nhận version mới qua lần merge release về, và release sau cắt từ `main` thì bump tiếp.
  - Hai branch không cùng sửa `version.properties` nên khi merge về hầu như không conflict ở file này.
- **Không mở PR thẳng `release/vX.Y` → `main`, phải đi qua branch `merge/...`.**
  - Ruleset bắt PR phải cập nhật theo base (strict). Sau khi cắt release, `main` thường đã có commit mới, nên PR thẳng sẽ báo *out-of-date*.
  - Muốn hết báo thì phải merge `main` vào `release/vX.Y`: bằng nút *Update branch*, hoặc giải conflict trên web. Làm vậy là đẩy code chưa test vào release, và ruleset cũng cấm push thẳng vào `release/**`, nên bị kẹt.
  - Branch `merge/...` nhận `main` thay cho release: release giữ nguyên, conflict được giải ở đó.
  - Chỉ khi `main` chưa có commit mới kể từ lần đồng bộ trước thì PR thẳng mới merge được; trường hợp này hiếm nên không dùng.
- **PR merge về `main` chứa nhiều commit**, gồm các fix và bump. Đây là ngoại lệ duy nhất của luật "mỗi PR một commit" (CLAUDE.md). Commit merge vẫn tạo một bong bóng trên `main`.
- **Ruleset `protect-main` áp dụng cho cả `release/**`** (§10): bắt buộc PR, check `build` xanh, cấm push thẳng, force push và xoá branch.
